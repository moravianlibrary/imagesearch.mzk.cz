#!/bin/bash

cd images
python -m SimpleHTTPServer &
SERVER_PID=$!
trap 'kill ${SERVER_PID}' SIGINT
trap 'kill ${SERVER_PID}' EXIT
cd ..
mvn clean compile exec:java -Dexec.args="images http://localhost:8000/" -pl importer

