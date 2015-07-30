# imagesearch.mzk.cz

Imagesearch.mzk.cz is web service that provides services for content based image searching. You can find your image
for within a second. You can ask whether there is an image, which is identical to yours, or you can request a set
of similar ones.

## Requirements

* Docker

## Installation

TODO

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

## Build and run

You can use following commands:

```
$ make build
$ make run
```

* Persistent data are stored in directory /var/imagesearch, so you check whether it exists, or you can change it in Makefile.
* Web service listen on port 8080, you can also change it in Makefile.
