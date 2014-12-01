#!/bin/bash

rm -rf /home/liresolr/solr-server/server/solr/liresolr/data
ln -s /var/liresolr/data /home/liresolr/solr-server/server/solr/liresolr/data

/home/liresolr/solr-server/bin/solr start

while :; do /bin/bash; sleep 1; done
