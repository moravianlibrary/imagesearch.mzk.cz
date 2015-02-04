FROM debian:wheezy

MAINTAINER Erich Duda <erich@duda.cz>

ENV REFRESHED_AT 2014-11-21

ENV PATH /usr/bin:/bin:/usr/sbin:/sbin
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get -y update && \
    apt-get -y install wget adduser vim less xmlstarlet openjdk-7-jre

# download SOLR
RUN wget --no-verbose http://apache.miloslavbrada.cz/lucene/solr/4.10.2/solr-4.10.2.tgz -O /tmp/solr.tgz

# creates liresolr user
RUN adduser --disabled-password --gecos "" --uid 8000 liresolr
USER liresolr

# unpack solr
RUN tar xf /tmp/solr.tgz --directory=/home/liresolr && mv /home/liresolr/solr-* /home/liresolr/solr-server
# create new solr server
RUN cp -r /home/liresolr/solr-server/example /home/liresolr/solr-server/server
# create solr core
RUN cp -r /home/liresolr/solr-server/server/solr/collection1 /home/liresolr/solr-server/server/solr/liresolr
RUN echo "name=liresolr" > /home/liresolr/solr-server/server/solr/liresolr/core.properties

# add libraries
RUN mkdir /home/liresolr/solr-server/server/solr/liresolr/lib
ADD solrcore/lire.jar /home/liresolr/solr-server/server/solr/liresolr/lib/lire.jar
ADD solrcore/JOpenSurf.jar /home/liresolr/solr-server/server/solr/liresolr/lib/JOpenSurf.jar
ADD solrcore/liresolr-handlers.jar /home/liresolr/solr-server/server/solr/liresolr/lib/liresolr-handlers.jar
#ADD solrcore/lib/* /home/liresolr/solr-server/server/solr/liresolr/lib/

RUN xmlstarlet ed -L \
      -s /config -t elem -n requestHandlerTMP -v "" \
      -i /config/requestHandlerTMP -t attr -n name -v "/lireId" \
      -i /config/requestHandlerTMP -t attr -n class -v "net.semanticmetadata.lire.solr.requesthandler.IdentityRequestHandler" \
      -s /config/requestHandlerTMP -t elem -n lst -v "" \
      -i /config/requestHandlerTMP/lst -t attr -n name -v "defaults" \
      -s /config/requestHandlerTMP/lst -t elem -n str1 -v "explicit" \
      -s /config/requestHandlerTMP/lst -t elem -n str2 -v "json" \
      -s /config/requestHandlerTMP/lst -t elem -n str3 -v "true" \
      -i /config/requestHandlerTMP/lst/str1 -t attr -n name -v "echoParams" \
      -i /config/requestHandlerTMP/lst/str2 -t attr -n name -v "wt" \
      -i /config/requestHandlerTMP/lst/str3 -t attr -n name -v "indent" \
      -r /config/requestHandlerTMP/lst/str1 -v str \
      -r /config/requestHandlerTMP/lst/str2 -v str \
      -r /config/requestHandlerTMP/lst/str3 -v str \
      -r /config/requestHandlerTMP -v requestHandler \
      -s /config -t elem -n requestHandlerTMP -v "" \
      -i /config/requestHandlerTMP -t attr -n name -v "/lireSim" \
      -i /config/requestHandlerTMP -t attr -n class -v "net.semanticmetadata.lire.solr.requesthandler.SimilarRequestHandler" \
      -s /config/requestHandlerTMP -t elem -n lst -v "" \
      -i /config/requestHandlerTMP/lst -t attr -n name -v "defaults" \
      -s /config/requestHandlerTMP/lst -t elem -n str1 -v "explicit" \
      -s /config/requestHandlerTMP/lst -t elem -n str2 -v "json" \
      -s /config/requestHandlerTMP/lst -t elem -n str3 -v "true" \
      -i /config/requestHandlerTMP/lst/str1 -t attr -n name -v "echoParams" \
      -i /config/requestHandlerTMP/lst/str2 -t attr -n name -v "wt" \
      -i /config/requestHandlerTMP/lst/str3 -t attr -n name -v "indent" \
      -r /config/requestHandlerTMP/lst/str1 -v str \
      -r /config/requestHandlerTMP/lst/str2 -v str \
      -r /config/requestHandlerTMP/lst/str3 -v str \
      -r /config/requestHandlerTMP -v requestHandler \
      /home/liresolr/solr-server/server/solr/liresolr/conf/solrconfig.xml

RUN rm /home/liresolr/solr-server/server/solr/liresolr/conf/schema.xml
ADD solrcore/schema.xml /home/liresolr/solr-server/server/solr/liresolr/conf/schema.xml
ADD solrcore/liresolr.properties /home/liresolr/solr-server/server/solr/liresolr/conf/liresolr.properties

# import
RUN mkdir /home/liresolr/import
ADD indexer/indexer.jar /home/liresolr/import/indexer.jar
ADD indexer/indexer.properties /home/liresolr/import/indexer.properties
ADD indexer/create_index.sh /home/liresolr/import/create_index.sh

USER root
RUN chmod a+x /home/liresolr/import/create_index.sh

USER liresolr

# web application
ADD web/web.war /home/liresolr/solr-server/server/webapps/web.war
ADD web/web-context.xml /home/liresolr/solr-server/server/contexts/web-context.xml
ADD web/data-context.xml /home/liresolr/solr-server/server/contexts/data-context.xml

USER root
ADD init.sh /init.sh
RUN chmod ugo+x /init.sh

ENTRYPOINT "/init.sh"
