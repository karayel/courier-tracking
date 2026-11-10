# Courier Tracking

A RESTful Java application that receives streaming courier geolocations, logs when a courier enters a Migros store's 100 m radius, and reports the total distance each courier has travelled.

Case requirements: [docs/requirement.md](docs/requirement.md)
## Quick start

Requires Java 21 (the Gradle wrapper downloads Gradle and all dependencies on first run), **or** Docker.

```bash
./scripts/demo.sh
```

One command: it starts the app if it is not running, sends a sample location stream, prints the results and stops the app again. Details in [Manual testing](#manual-testing).

## What's inside

| Requirement | How it is met |
| --- | --- |
| Receive streaming geolocations (`time`, `courier`, `lat`, `lng`) | `POST /api/couriers/locations`, validated; out-of-order records are ignored |
| Log an entrance into a store's 100 m radius | `StoreProximityListener` writes a log line and stores the entrance; `GET /api/store-entries` lists them |
| No new entrance on re-entry within 1 minute | Counted from the last exit, by record time ([why](#why-the-1-minute-counts-from-the-last-exit)) |
| Stores from `stores.json` | Loaded once at startup from `src/main/resources/stores.json` |
| Total travel distance query | `CourierQueryService.getTotalTravelDistance(courierId)`, exposed as `GET /api/couriers/{courierId}/distance` (m and km) |
| At least 2 design patterns | Observer, Strategy, Repository ([table](#design-patterns)) |
| Easy to run and test | `scripts/demo.sh`, 77 automated tests, Swagger UI, Dockerfile, GitHub Actions CI |

```
src/main/java/com/migros/couriertracking/
  api/                  REST controller, error handling
  api/dto/              request/response objects, mapped to/from the domain
  service/              ingestion service, location event, query service
  service/listener/     event listeners: store proximity, distance
  domain/               CourierLocation, GeoPoint, Store, StoreEntry, StoreVisit
  domain/distance/      DistanceCalculator strategy + Haversine
  repository/           repository interfaces only
  repository/inmemory/  in-memory implementations
  repository/file/      stores.json loader
src/test/java/...       tests, same package structure
scripts/demo.sh         end-to-end demo
docs/                   case text
Dockerfile              multi-stage image, runs as a non-root user
.github/workflows/      CI: tests + Docker build on every push
```

Built with Java 21, Spring Boot 4.1, Gradle (Kotlin DSL).

## Run

```bash
./gradlew bootRun
```

Or, without a local Java:

```bash
docker build -t courier-tracking .
docker run --rm -p 8080:8080 courier-tracking
```

The app listens on `http://localhost:8080`.

## Automated tests

```bash
./gradlew test
```

77 tests, no running app needed. HTML report: `build/reports/tests/test/index.html`. GitHub Actions runs the same tests on every push (`.github/workflows/ci.yml`).

| Test class | What it checks |
| --- | --- |
| `CourierTrackingIntegrationTest` | Full Spring context over HTTP: a location stream through every rule, an entrance at each of the 5 stores, unknown courier |
| `StoreProximityListenerTest` | Entrance, staying inside, re-entry within / after 1 minute from last exit, exact 100 m boundary, several and overlapping stores, log line |
| `LocationIngestionServiceTest` | Older or same-time records ignored, checked per courier; event carries the previous location; the future-record limit |
| `LocationIngestionConcurrencyTest` | Same record sent from many threads is processed once; a shuffled concurrent stream gives the same result as a sequential replay; couriers in parallel do not interfere |
| `DistanceTrackerListenerTest` | Distance added per leg, first record adds nothing |
| `HaversineDistanceCalculatorTest` | Known distances (1° of latitude, Ataşehir–Novada), zero, symmetry |
| `CourierTrackingControllerTest` | `200`, `400` with field errors, malformed JSON, missing body, `415`, response shapes |
| `LocationRequestTest` | Validation of every field, courier id rules, mapping to the domain |
| `CourierQueryServiceTest`, `ResponseMappingTest` | Queries and m/km mapping |
| `InMemoryRepositoriesTest`, `JsonStoreRepositoryTest` | Storage, concurrent distance updates, `stores.json` loading (fails fast on a missing file or coordinate) |
| `DomainInvariantsTest` | Domain objects reject missing values |
| `OpenApiDocsTest` | Swagger UI and OpenAPI docs are served |

Run one class:

```bash
./gradlew test --tests '*StoreProximityListenerTest'
```

## Manual testing

### Demo script

```bash
./scripts/demo.sh
```

If the app is not running on localhost, the script starts it and stops it again when done (needs `lsof`; the first run takes longer while Gradle builds). To use an app running elsewhere: `BASE_URL=http://host:port ./scripts/demo.sh`.

It sends two couriers at the same time:
- **A** passes Ataşehir MMM Migros: enter, leave, come back within 1 minute, an out-of-order record, come back after 2 minutes.
- **B** enters Novada MMM Migros and stays inside.

Each printed result is labelled with the expected value:

| Check | Expected |
| --- | --- |
| Entrances of A | 2 (10:01 and 10:09) |
| Entrances of B | 1 (10:01) |
| Unfiltered entrance list | the 3 above (plus earlier runs' if the app was already running) |
| Distance of A / B | ~1278.7 m / ~344.7 m |
| Courier with no records | 0 m |
| Invalid request | `400` with field errors |

The app log shows one line per entrance:

```
INFO ... StoreProximityListener : Courier demo-...-a entered Ataşehir MMM Migros at 2026-11-09T10:01:00Z
```

### Swagger UI

With the app running: `http://localhost:8080/swagger-ui/index.html`. Every endpoint can be tried from the browser.

### curl

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/couriers/locations` | Receive one location. `200 OK` once processed; `400` with field errors if invalid. |
| `GET` | `/api/couriers/{courierId}/distance` | Total distance travelled, in meters and kilometers. |
| `GET` | `/api/store-entries?courierId=` | Counted store entrances; `courierId` is optional. |

```bash
curl -X POST localhost:8080/api/couriers/locations -H 'Content-Type: application/json' \
  -d '{"time":"2026-11-09T10:01:00Z","courier":"c1","lat":40.9927,"lng":29.1244229}'

curl localhost:8080/api/couriers/c1/distance
# {"courierId":"c1","meters":0.0,"kilometers":0.0}

curl 'localhost:8080/api/store-entries?courierId=c1'
# [{"courier":"c1","store":"Ataşehir MMM Migros","time":"2026-11-09T10:01:00Z"}]
```

`time` is an ISO-8601 instant.

## Design

```
REST controller ──┐
(Kafka listener)  ├─► LocationIngestionService ─► LocationReceivedEvent ─┬─► StoreProximityListener ─► store entries + log
(WebSocket)      ─┘    rejects out-of-order        (Spring events)       └─► DistanceTrackerListener ─► courier distances
```

- **Transport independent ingestion.** The controller only calls `LocationIngestionService.ingest(...)`. A Kafka or WebSocket adapter can be added by calling the same method; no business rule changes.
- **Per-courier ordering.** Ingestion serialises each courier's records and drops records not newer than the courier's last accepted one. Listeners run synchronously inside that section.

### Design patterns

| Pattern | Where | Why |
| --- | --- | --- |
| Observer | `LocationReceivedEvent`, `StoreProximityListener`, `DistanceTrackerListener` | Each concern reacts to a location independently; new reactions are new listeners. |
| Strategy | `DistanceCalculator`, `HaversineDistanceCalculator` | The distance formula is swappable; used both for the 100 m check and total distance. |
| Repository | `repository/*Repository` interfaces, `repository/inmemory/*` | Storage is in memory today; a database is one new implementation class, services do not change. |

## Business rules and assumptions

| Rule | Behaviour |
| --- | --- |
| Entrance | A move from outside to inside a store's radius (distance ≤ 100 m). Staying inside is one entrance. |
| Re-entry | Re-entering the same store within 1 minute of the last **exit** is not a new entrance. |
| Time | Measured by the record's `time`, not arrival time. |
| Out-of-order records | A record not newer than the courier's last accepted record is ignored (still `200`). |
| Future records (known limit) | Record time is trusted as sent. A record stamped in the future (wrong device clock) makes the courier's records up to that time count as out of order. No server-clock check is made, by design: the business rules use record time only. |
| Courier id | 1-64 characters of `A-Z a-z 0-9 _ -`; anything else is rejected with `400` (the id is logged and kept in memory). |
| Distance | Sum of Haversine distances between consecutive accepted records; no GPS noise filtering. |
| Unknown courier | Distance is `0`, entrance list is empty. |
| Several stores | Each store is tracked separately. |
| Storage | In memory; data is lost on restart. Stores are read once at startup. |

### Why the 1 minute counts from the last exit

The rule exists to stop GPS noise at the edge of the radius from creating fake entrances. Counting from the last exit handles that; counting from the last entrance does not:

| Situation | From last entrance | From last exit (ours) |
| --- | --- | --- |
| Courier inside for 10 min; GPS jumps outside for 5 s and back | New entrance (last entrance was 10 min ago) | Not counted (exit was 5 s ago) |
| Enters, leaves after 50 s, comes back at 61 s | New entrance (61 s since entrance) | Not counted (11 s since exit) |
| Leaves and really comes back 2 min later | New entrance | New entrance |

The case also says "re-entries", and a re-entry can only follow an exit.
