# imagesearch.mzk.cz

Imagesearch.mzk.cz is web service that provides services for content based image searching. You can find your image
for within a second. You can ask whether there is an image, which is identical to yours, or you can request a set
of similar ones.

## Requirements

* [Docker](https://www.docker.com/)

## Installation

On debian based systems you can create init script. Create file /etc/init.d/imagesearch with following content

```bash
#!/bin/bash

### BEGIN INIT INFO
# Provides:          imagesearch
# Required-Start:    docker
# Required-Stop:     docker
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Imagesearch application
# Description:       Runs docker image.
### END INIT INFO

LOCK=/var/lock/imagesearch
CONTAINER_NAME=imagesearch
IMAGE_NAME=moravianlibrary/imagesearch.mzk.cz

case "$1" in
  start)
    if [ -f $LOCK ]; then
      echo "Imagesearch is running yet."
    else
      touch $LOCK
      echo "Starting imagesearch.."
      docker run --name $CONTAINER_NAME -p 8080:80 -v /var/imagesearch/:/data $IMAGE_NAME &
      echo "[OK] Imagesearch is running."
    fi
    ;;
  stop)
    if [ -f $LOCK ]; then
      echo "Stopping imagesearch.."
      rm $LOCK \
        && docker kill $CONTAINER_NAME \
        && docker rm $CONTAINER_NAME \
        && echo "[OK] Imagesearch is stopped."
    else
      echo "Imagesearch is not running."
    fi
    ;;
  restart)
    $0 stop
    $0 start
  ;;
  status)
    if [ -f $LOCK ]; then
      echo "Imagesearch is running."
    else
      echo "Imagesearch is not running."
    fi
  ;;
  update)
    docker pull $IMAGE_NAME
    $0 restart
  ;;
  *)
    echo "Usage: /etc/init.d/imagesearch {start|stop|restart|status|update}"
    exit 1
    ;;
esac

exit 0

```

After it run these commands

```
# chmod 755 /etc/init.d/imagesearch
# update-rc.d imagesearch defaults
```

Now the imagesearch service will be automatically start at server startup.

Docker container expects directory /var/imagesearch. You must create it and set owner of this directory to uid 33.

```
# mkdir -p /var/imagesearch
# chowner 33:33 /var/imagesearch
```

Now you can start service by

```
# /etc/init.d/imagesearch start
```

The service listen on port 8080. Now you should install and setup apache http server, which will forward requests to docker container and it also will protect endpoints /v1/ingest and /v1/commit by password.

```
# apt-get update
# apt-get install apache2
```

In directory /etc/apache2/sites-available create file with name imagesearch.mzk.cz and with content:

```
<VirtualHost *:80>
        ServerName imagesearch.mzk.cz

        <IfModule mod_rewrite.c>
              RewriteEngine on
              Options +FollowSymlinks

              ProxyPassMatch ^/(.*)$  http://localhost:8080/$1
              ProxyPass / http://localhost:8080
        </IfModule>

        <Location "/v1/ingest">
              AuthType Basic
              AuthName "Restricted area"
              AuthBasicProvider file
              AuthUserFile /usr/local/apache/passwd/imagesearch
              Require valid-user
        </Location>

        <Location "/v1/commit">
              AuthType Basic
              AuthName "Restricted area"
              AuthBasicProvider file
              AuthUserFile /usr/local/apache/passwd/imagesearch
              Require valid-user
        </Location>


        ErrorLog ${APACHE_LOG_DIR}/imagesearch-error.log

        # Possible values include: debug, info, notice, warn, error, crit,
        # alert, emerg.
        LogLevel warn

        CustomLog ${APACHE_LOG_DIR}/imagesearch-access.log combined
</VirtualHost>
```

You must create credential file /usr/local/apache/passwd/imagesearch, where your username and password will be stored.
You can create it by command:

```
# htpasswd -c /usr/local/apache/passwd/imagesearch yourusername
```

After it enable new settings and reload apache server.

```
# a2ensite imagesearch.mzk.cz
# /etc/init.d/apache2 reload
```

**Attention:** Docker's default behavior is that it modifies iptables configuration and enables exposed ports defined by -p option. It is possible override this behavior by adding --iptables=false to the Docker daemon.

On Debian based systems, you can edit /etc/default/docker and uncomment the DOCKER_OPTS line:

```
DOCKER_OPTS="--dns 8.8.8.8 --dns 8.8.4.4 --iptables=false"
```

After doing so, you need to restart Docker with

```
# /etc/init.d/docker restart
```

## API

Imagesearch.mzk.cz provides several endpoints, which are described bellow. You can use them using the basic GET or POST
requests. Each response contains element *status* which can take values "ok" or "error".

### Uploading data

For uploading data use ingest enpdoint:

```
/v1/ingest
```

Uploading is done via POST request, which has following structure:
```json
[
  {
    "id": "example1",
    "thumbnail": "http://example.com/path/to/thumbnail1.jpg",
    "metadata": {
      "key1": "value1",
      "key2": "value2",
      "key3": "value3"
    },
    "image_url": "...",
    "image_base64": "...",
    "blockHash": "...",
    "dHash": "...",
    "cannyDHash": "..."
  },
  {
    "id": "example2",
    "thumbnail": "http://example.com/path/to/thumbnail2.jpg",
    "metadata": {
      "key1": "value1",
      "key2": "value2",
      "key3": "value3"
    },
    "image_url": "...",
    "image_base64": "...",
    "blockHash": "...",
    "dHash": "...",
    "cannyDHash": "..."
  }
]
```

* Each record is identified by id. If you upload image with the same id, the data will be overriden.
* Metadata element can contain arbitrary JSON value.
* Image data can be send by several ways:
  - image_url: You can specify url, where the image can be downloaded.
  - image_base64: You can send image data in base64 format.
  - blockHash, dHash, cannyDHash: You can compute hashes by yourselves and send their hexadecimal representations.
* From listed methods how to upload image data you should choose only one. If you use several methods at once
  the behaviour is not defined.

### Deleting data

For deleting data use also ingest enpdoint:

```
/v1/ingest
```

Deleting is done via POST request, which has following structure:

```json
[
  {
    "id": "example1",
    "status": "deleted"
  },
  {
    "id": "example2",
    "status": "deleted"
  }
]
```

* JSON is self explanatory.
* In one POST request you can add, modify and delete data.

### Commiting

After that you do some changes you must commit your newly uploaded or deleted data. This is done by endpoint:

```
/v1/commit
```

### Searching

There are two endpoints you can use:

```
/v1/searchIdentical
/v1/seachSimilar
```

#### Search identical

You can seach identical images using both GET or POST requests.

##### GET

```
/v1/searchIdentical?blockHash=ffff816180a18239c731a381e087b8cf883df971dc01bc858003800dd80fffff&dHash=2592989844c4e0d8
/v1/searchIdentical?url=http://www.example.com/path/to/image.jpg
```

##### POST

```json
{
  "image_base64": "..."
}
```

or

```json
{
  "image_url": "..."
}
```

Response has form:

```json
  {
    "data": {
        "distance": 0,
        "found": true,
        "record": {
            "id": "test",
            "metadata": {
                "numGcps": 5,
                "rmsError": 0.52
            },
            "thumbnail": "http://example.com/thumbnail.jpg"
        }
    },
    "status": "ok"
}
```

* Important element is data.found, which says whether the identical image was found or not.

#### Search similar

You can seach similar images using both GET or POST requests.

##### GET

```
/v1/searchSimilar?cannyDHash=0f13332927217ada&count=10
```

* By parameter count you can specify how many the most similar images will be returned.

##### POST

```json
{
  "image_base64": "..."
}
```

or

```json
{
  "image_url": "..."
}
```

## Build and run

You can use following commands:

```
$ make build
$ make run
```

* Persistent data are stored in directory /var/imagesearch, so you check whether it exists, or you can change it in Makefile.
* Web service listen on port 8080, you can also change it in Makefile.
