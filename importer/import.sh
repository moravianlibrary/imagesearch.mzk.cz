#!/bin/bash

cd dataset 
python -m SimpleHTTPServer &
SERVER_PID=$!
trap 'kill ${SERVER_PID}' SIGINT
trap 'kill ${SERVER_PID}' EXIT
cd ..
mvn clean compile exec:java -Dexec.args="dataset http://localhost:8000/" -pl importer

