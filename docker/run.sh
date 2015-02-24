#!/bin/bash

docker run -i -t -u liresolr --net host -p 8983:8983 -v /var/liresolr/:/var/liresolr moravianlibrary/imagesearch:$1
