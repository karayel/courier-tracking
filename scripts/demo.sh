#!/usr/bin/env bash
# Sends a sample location stream to a running app and prints the results.
# Usage: ./scripts/demo.sh            (starts the app itself if it is not running)
#        BASE_URL=http://host:port ./scripts/demo.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
RUN="$(date +%s)-$$"
COURIER_A="demo-$RUN-a"
COURIER_B="demo-$RUN-b"
ATASEHIR_LNG=29.1244229   # Ataşehir MMM Migros: 40.9923307, 29.1244229
NOVADA_LNG=29.1161293     # Novada MMM Migros:   40.986106,  29.1161293

send() {  # send <courier> <lng> <time> <lat> <comment>
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/api/couriers/locations" \
    -H 'Content-Type: application/json' \
    -d "{\"time\":\"$3\",\"courier\":\"$1\",\"lat\":$4,\"lng\":$2}")
  printf '  %s  lat=%-8s -> HTTP %s  %s\n' "$3" "$4" "$code" "$5"
}

app_up() { curl -s -o /dev/null "$BASE_URL/api/store-entries"; }

if ! app_up; then
  case "$BASE_URL" in
    http://localhost:*|http://127.0.0.1:*) ;;
    *) echo "App is not reachable at $BASE_URL" >&2; exit 1 ;;
  esac
  PORT="${BASE_URL##*:}"; PORT="${PORT%%/*}"
  LOG="$(mktemp)"
  echo "App is not running; starting it on port $PORT (log: $LOG)..."
  (cd "$(dirname "$0")/.." && SERVER_PORT="$PORT" exec ./gradlew bootRun -q) > "$LOG" 2>&1 &
  GRADLE_PID=$!
  # the app runs under the Gradle daemon, not this shell, so stop it by the port it listens on
  trap 'kill $GRADLE_PID $(lsof -ti tcp:"$PORT" -sTCP:LISTEN 2>/dev/null) 2>/dev/null || true' EXIT
  for _ in $(seq 120); do
    app_up && break
    sleep 1
  done
  if ! app_up; then
    echo "App did not start within 120 s. Last log lines:" >&2
    tail -20 "$LOG" >&2
    exit 1
  fi
  echo
fi

a() { send "$COURIER_A" "$ATASEHIR_LNG" "$@"; }
b() { send "$COURIER_B" "$NOVADA_LNG" "$@"; }

echo "Courier A ($COURIER_A) near Ataşehir MMM Migros:"
a 2026-11-09T10:00:00Z 40.9950 "~300 m away"
a 2026-11-09T10:01:00Z 40.9927 "~44 m away -> entrance"
a 2026-11-09T10:05:00Z 40.9950 "leaves the radius"
a 2026-11-09T10:05:30Z 40.9927 "back within 1 min -> NOT a new entrance"
a 2026-11-09T10:04:00Z 40.9950 "older than last record -> ignored"
a 2026-11-09T10:07:00Z 40.9950 "leaves again"
a 2026-11-09T10:09:00Z 40.9927 "back after 2 min -> entrance"

echo
echo "Courier B ($COURIER_B) near Novada MMM Migros, same times as A:"
b 2026-11-09T10:00:00Z 40.9890 "~322 m away"
b 2026-11-09T10:01:00Z 40.9864 "~33 m away -> entrance"
b 2026-11-09T10:02:00Z 40.9862 "still inside -> NOT a new entrance"
b 2026-11-09T10:03:00Z 40.9865 "still inside -> NOT a new entrance"

echo
echo "Courier A entries (expected: 2, at 10:01 and 10:09):"
curl -s "$BASE_URL/api/store-entries?courierId=$COURIER_A"
echo
echo
echo "Courier B entries (expected: 1, at 10:01):"
curl -s "$BASE_URL/api/store-entries?courierId=$COURIER_B"
echo
echo
echo "All entries, no filter (expected: the 3 above, plus earlier runs' if the app was already running):"
curl -s "$BASE_URL/api/store-entries"
echo
echo
echo "Courier A distance (expected: ~1278.7 m):"
curl -s "$BASE_URL/api/couriers/$COURIER_A/distance"
echo
echo
echo "Courier B distance (expected: ~344.7 m):"
curl -s "$BASE_URL/api/couriers/$COURIER_B/distance"
echo
echo
echo "Courier with no records (expected: 0 m):"
curl -s "$BASE_URL/api/couriers/demo-$RUN-nobody/distance"
echo
echo
echo "Invalid request (expected: HTTP 400 with field errors):"
curl -s -X POST "$BASE_URL/api/couriers/locations" -H 'Content-Type: application/json' \
  -d '{"time":"2026-11-09T10:00:00Z","courier":"","lat":91,"lng":29.12}'
echo
