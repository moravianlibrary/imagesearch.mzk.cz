#!/bin/bash

/etc/init.d/lighttpd start
curl -0 http://localhost/v1/commit

while :; do /bin/bash; sleep 1; done
