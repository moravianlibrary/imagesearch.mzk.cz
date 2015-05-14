# Imagesearch.mzk.cz

This project modifies [LIRE Solr Integration Project](https://bitbucket.org/dermotte/liresolr) that implements content-based image retrieval system. The basic idea is to reduce problem of searching similar images to problem of searching similar texts. More information you can find in the site of [LIRE project](http://www.semanticmetadata.net/lire/).

## Build and Run

The whole system is builded, set up and it runs in the [Docker](https://www.docker.com/). If you have the Docker installed, you can build it by command.

```
$ make
```

Preparation of docker image takes some time, after that you can run it by command.

**Note:** The program expects that there is a directory */var/liresolr* which contains two subdirectories, namely *data* and *images*. These subdirectories must set owner pid to 8000. Location of the directory you can change in the *Makefile*.

```
$ make run
```

The running instance listen now on the url http://localhost:8983. Administration of the Solr core is on the url http://localhost:8983/solr and the sample web application is on the url http://localhost:8983/lire/

## API

### Indexing and deleting images

For indexing and deleting images there is an endpoint listen on the http://localhost:8983/lire/liresolr/indexImage. If you want to index or delete some image you must send POST request which must have following structure.

```json
[
  {
    "id": "ID-1",
    "url": "http://provider.eu/link/to/highres/image.jpg",
    "rights": "CC-BY",
    "provider": "Name of institution",
    "provider_link": "http://provider.eu/link-to-image"
  },
  {
    "id": "ID-2",
    "status": "deleted"
  }
]
```

The image processing methods such as SURF or SIFT need to create additional data structure called *clusters*.  If you want to use these methods to search similar images you have to send GET requests on following two endpoints in the order as is shown.

1. http://localhost:8983/lire/liresolr/createClusters
2. http://localhost:8983/lire/liresolr/reindexBOVW

The first endpoint creates mentioned clusters data structure and the second endpoint reindex all images in the database (use data from clusters). Clusters data structure can be made only if enough images are indexed (500 and more).

### Searching similar images

For searching similar images there is an endpoint http://localhost:8983/lire/liresolr/lireSim. It is important to note that the searching consists of two steps:
1. finding set of candidates (performs by search descriptor)
2. reranking set of candidates by similarity (performs by rerank descriptor)

Queries for searching similar images are send by GET requests, which expects four parameters:
* searchDescriptor - descriptor which is used to find set of candidates
* searchCount - size of set of candidates
* rerankDescriptor - descriptor which is used to rerank set of candidates
* rerankCount - size of returned set of similar images

List of descriptors which can be used.

* ColorLayout
* CEDD
* AutoColorCorrelogram
* BinaryPatternsPyramid
* SimpleColorHistogram
* EdgeHistogram
* FCTH
* Gabor
* JCD
* JointHistogram
* JpegCoefficientHistogram
* LocalBinaryPatterns
* OpponentHistogram
* PHOG
* RotationInvariantLocalBinaryPatterns
* ScalableColor
* Tamura
* LuminanceLayout
* Surf

**Warning:** this project is obsolete. See master branch instead.
