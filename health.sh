#!/usr/bin/env bash

# Configurable base URL for routing API
ROUTING_API_HOSTNAME="18.141.234.100"
ROUTING_API_ADDR="http://${ROUTING_API_HOSTNAME}:8080"

echo "Routing API addr: ${ROUTING_API_ADDR}"
echo

echo "Checking health of Routing API at $ROUTING_API_ADDR/health"
curl -s -w "\nHTTP Status: %{http_code}\n" "$ROUTING_API_ADDR/health"

echo
echo "Fetching server list from $ROUTING_API_ADDR/server/list"
curl -s -w "\nHTTP Status: %{http_code}\n" "$ROUTING_API_ADDR/server/list"

echo
echo "Done."
