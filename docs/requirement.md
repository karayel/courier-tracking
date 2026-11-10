# Case: Courier Tracking

## Project Description

In this project, you are expected to create a **RESTful web application using Java** that primarily receives streaming geolocation data from couriers.

Each location record should contain the following information:

- `time`: Timestamp of the location record.
- `courier`: Courier identifier.
- `lat`: Latitude.
- `lng`: Longitude.

## Requirements

### 1. Store Proximity Tracking

- Log the courier and the store whenever a courier enters a **100-meter radius** of a Migros store.
- Re-entries into the same store's 100-meter radius within **1 minute** should not be counted as a new entrance.
- Store locations are provided in the `stores.json` file.

### 2. Total Travel Distance

The application must provide a way to query the total distance traveled by any courier.

A sample method signature could be:

```java
Double getTotalTravelDistance(courierId);
```

### 3. Design Patterns

Implement at least **2 design patterns** of your choice.

### 4. Documentation and Execution

Provide instructions to easily run and test your application.

A `../README.md` file or an executable script would be a good way to achieve this.

## Store Locations

Store locations are provided in `stores.json`:

```json
[
  {
    "name": "Ataşehir MMM Migros",
    "lat": 40.9923307,
    "lng": 29.1244229
  },
  {
    "name": "Novada MMM Migros",
    "lat": 40.986106,
    "lng": 29.1161293
  },
  {
    "name": "Beylikdüzü 5M Migros",
    "lat": 41.0066851,
    "lng": 28.6552262
  },
  {
    "name": "Ortaköy MMM Migros",
    "lat": 41.055783,
    "lng": 29.0210292
  },
  {
    "name": "Caddebostan MMM Migros",
    "lat": 40.9632463,
    "lng": 29.0630908
  }
]
```