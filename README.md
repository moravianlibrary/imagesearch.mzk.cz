LIRE Solr Integration Project
=============================

Includes a RequestHandlers and some utility classes for a fast start.

The request handlers supports six different types of queries

1.  Get random images ...
2.  Get images that are looking like the one with id ...
3.  Get images looking like the one found at url ...
4.  Get images with a feature vector like ...
5.  Extract histogram from an image URL ...
6.  Determine if the solr contains an identical image (differing by 2D transformations) ...

Preliminaries
-------------
Supported values for feature field parameters, e.g. lireq?field=cl_ha:

-  **cl_ha** .. ColorLayout
-  **ph_ha** .. PHOG
-  **oh_ha** .. OpponentHistogram
-  **eh_ha** .. EdgeHistogram
-  **jc_ha** .. JCD
-  **su_ha** .. SURF

Getting random images
---------------------
Returns randomly chosen images from the index.

Parameters:

-   **rows** ... indicates how many results should be returned (optional, default=60). Example: lireq?rows=30

Search by ID
------------
Returns images that look like the one with the given ID.

Parameters:

-   **id** .. the ID of the image used as a query as stored in the "id" field in the index.
-   **field** .. gives the feature field to search for (optional, default=cl_ha, values see above)
-   **rows** .. indicates how many results should be returned (optional, default=60).

Search by URL
-------------
Returns images that look like the one found at the given URL.

Parameters:

-   **url** .. the URL of the image used as a query. Note that the image has to be accessible by the web server Java has to be able to read it.
-   **field** .. gives the feature field to search for (optional, default=cl_ha, values see above)
-   **rows** .. indicates how many results should be returned (optional, default=60).

Search by feature vector
------------------------
Returns an image that looks like the one the given features were extracted. This method is used if the client
extracts the features from the image, which makes sense if the image should not be submitted.

Parameters:

-  **hashes** .. Hashes of the image feature as returned by BitSampling#generateHashes(double[]) as a String of white space separated numbers.
-  **feature** .. Base64 encoded feature histogram from LireFeature#getByteArrayRepresentation().
-  **field** .. gives the feature field to search for (optional, default=cl_ha, values see above)
-  **rows** .. indicates how many results should be returned (optional, default=60).

Extracting histograms
---------------------
Extracts the histogram of an image for use with the lire sorting function.

Parameters:

-   **extract** .. the URL of the image. Note that the image has to be accessible by the web server Java has to be able to read it.
-   **field** .. gives the feature field to search for (optional, default=cl_ha, values see above)

Try to find identical image
---------------------------
Combines more methods to find out identical image. If there is no identical image, handler return in the response **"identity":false**.

USAGE:
[solrurl]/[core]/lireId?url=<URL>

Parameters:

-   **url** .. the URL of the image. Note that the image has to be accessible by the web server Java has to be able to read it.

Find similar images
-------------------
Combines Color layout and SURF methods to find similar images. You can parametrize searching in **config.properties** file.

USAGE:
[solrurl]/[core]/lireSim?url=<URL>

Parameters

-  **url** .. the URL of the image. Note that the image has to be accessible by the web server Java has to be able to read it.


Installation
============

First run the dist task to create a jar. This should be integrated in the Solr class-path.
You can specify the Solr class-path in the solrconfig.xml. You also must add dependency libraries
to the class-path. **lire-request-handler.jar** search them in the lib directory.

     <lib dir="../../handlers" regex="lire-request-handler.jar" />
     <lib dir="../../handlers/lib" regex=".*\.jar" />

Then add the new request handlers has to be registered in the solrconfig.xml file:

     <requestHandler name="/lireq" class="net.semanticmetadata.lire.solr.LireRequestHandler">
        <lst name="defaults">
          <str name="echoParams">explicit</str>
          <str name="wt">json</str>
          <str name="indent">true</str>
        </lst>
     </requestHandler>
     
     <requestHandler name="/lireId" class="net.semanticmetadata.lire.solr.handler.IdentityRequestHandler">
        <lst name="defaults">
          <str name="echoParams">explicit</str>
          <str name="wt">json</str>
          <str name="indent">true</str>
        </lst>
     </requestHandler>

     <requestHandler name="/lireSim" class="net.semanticmetadata.lire.solr.handler.SimilarRequestHandler">
        <lst name="defaults">
          <str name="echoParams">explicit</str>
          <str name="wt">json</str>
          <str name="indent">true</str>
        </lst>
     </requestHandler>

Use of the request handlers is detailed above.

You'll also need the respective fields in the schema.xml file:

    <fields>
       <!-- file path for ID -->
       <field name="id" type="string" indexed="true" stored="true" required="true" multiValued="false" />
       <!-- the solr file name -->
       <field name="title" type="text_general" indexed="true" stored="true" multiValued="true"/>
       <!-- Edge Histogram -->
       <field name="eh_ha" type="text_ws" indexed="true" stored="false" required="false"/>
       <field name="eh_hi" type="binaryDV"  indexed="false" stored="true" required="false"/>
       <!-- ColorLayout -->
       <field name="cl_ha" type="text_ws" indexed="true" stored="false" required="false"/>
       <field name="cl_hi" type="binaryDV"  indexed="false" stored="true" required="false"/>
       <!-- PHOG -->
       <field name="ph_ha" type="text_ws" indexed="true" stored="false" required="false"/>
       <field name="ph_hi" type="binaryDV"  indexed="false" stored="true" required="false"/>
       <!-- JCD -->
       <field name="jc_ha" type="text_ws" indexed="true" stored="false" required="false"/>
       <field name="jc_hi" type="binaryDV"  indexed="false" stored="true" required="false"/>
       <!-- OpponentHistogram -->
       <!--field name="oh_ha" type="text_ws" indexed="true" stored="false" required="false"/-->
       <!--field name="oh_hi" type="binary"  indexed="false" stored="true" required="false"/-->
       <!-- SURF -->
       <field name="su_ha" type="text_ws" indexed="true" stored="false" required="false"/>
       <field name="su_hi" type="binary"  indexed="false" stored="true" required="false" multiValued="true"/>
       <!-- Needed for SOLR -->
       <field name="_version_" type="long" indexed="true" stored="true"/>
    </fields>

Do not forget to add the custom field at the very same file:

    <fieldtype name="binaryDV" class="net.semanticmetadata.lire.solr.BinaryDocValuesField"/>
   
On the end you'll have to add configuration file liresolr.properties to the config directory of
your solr core (to the same directory where is solrconfig.xml).

```properties
# It defines how many images will be obtained from solr and then they will be compared
# with the queried image.
# Visual Words
numVisualWordsImages = 30
# Color Layout
numColorLayoutImages = 2000
# It defines how many images will be obtained from solr at similar searching (/lireSim)
numColorLayoutSimImages = 10
numSurfSimImages = 30
# Count of images returned by /lireSim handler:
numSimilarImages = 30
# It defines if system should resize queried image. Parameter defines shorter side of the image.
# You can use value 0 for no resize.
resizeQueryImage = 400
# It defines treshold of Color Layout method. Images, which have the distance less than the
# threshold, will be marked as potentional identical images to the queried image.
thresholdCLIdentity1 = 7.0
# It defines the second level of threshold. If it exists exactly one image, which has
# distance less than first threshold and it has also distance less than the second
# threshold, system says, that this image is identical to the queried image.
thresholdCLIdentity2 = 5.0
# It defines threshold of the SURF method. Images which have distance greater or equals
# to this value will be removed from a list of candidates to the identical image.
thresholdSUIdentity = 0.9
```
There is also a sort function based on LIRE. The function parser needs to be added to the
solrconfig.xml file like this:

      <valueSourceParser name="lirefunc"
        class="net.semanticmetadata.lire.solr.LireValueSourceParser" />

Then the function lirefunc(arg1,arg2) is available for function queries. Two arguments are necessary and are defined as:

-  Feature to be used for computing the distance between result and reference image. Possible values are {cl, ph, eh, jc}
-  Actual Base64 encoded feature vector of the reference image. It can be obtained by calling LireFeature.getByteRepresentation() and by Base64 encoding the resulting byte[] data.

Examples:

-  [solrurl]/select?q=*:*&fl=id,lirefunc(cl,"FQY5DhMYDg...AQEBA%3D") – adding the distance to the reference image to the results
-  [solrurl]/select?q=*:*&sort=lirefunc(cl,"FQY5DhMYDg...AQEBA%3D")+asc – sorting the results based on the distance to the reference image



Indexing
========

Check ParallelSolrIndexer.java for indexing. It creates XML documents (either one per image or one single large file)
to be sent to the Solr Server.

To index images by the SURF method check a link http://www.semanticmetadata.net/wiki/doku.php?id=lire:bovw

You can also use a [LireSolrIndexer](https://bitbucket.org/dudaerich/liresolrindexer) This is preferred method if you want to use **/lireId** or **/lireSim** requests.

Simple Web Application
======================

Compilation
-----------

Simple run:

```shell
ant web
```

This command creates a war file. You could find it in the dist directory. After it you can integrate this war package to the Jetty. If you would change some behaviour or properties of this application you just see and change the source code.
