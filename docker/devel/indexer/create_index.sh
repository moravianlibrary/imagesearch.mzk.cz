#!/bin/bash

cd /home/liresolr/import;\
find /var/liresolr/images -type f -printf "/var/liresolr/images/%f\n" > images;\
java -jar indexer.jar index /home/liresolr/solr-server/server/solr/liresolr/data/index images
