#!/usr/bin/env bash

ROUTING_API_HOSTNAME="3.1.6.30"
#ROUTING_API_HOSTNAME="localhost"

ENDPOINT="http://${ROUTING_API_HOSTNAME}:8080"
REQUEST_COUNT=100
INTERVAL=1

echo "Sending $REQUEST_COUNT requests to $ENDPOINT every $INTERVAL second(s)..."
echo

for i in $(seq 1 $REQUEST_COUNT); do
  RAND=$((RANDOM % 1000))  # Random number 0–999
  JSON="{\"number\": $RAND}"

  echo "[$i] -> $JSON"
  curl -s -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "$JSON" \
    -w "\nHTTP Status: %{http_code}\n"

  echo "----"
  sleep "$INTERVAL"
done

echo "Done."
