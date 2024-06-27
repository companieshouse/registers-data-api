#!/bin/bash

# Start script for registers-data-api

PORT=8080
exec java -jar -Dserver.port="${PORT}" "registers-data-api.jar"
