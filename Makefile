all: devel

devel: DOCKER_TARGET=devel
devel: build

build:
	mvn clean package
	cp handlers/lib/lire.jar docker/${DOCKER_TARGET}/solrcore
	cp handlers/lib/JOpenSurf.jar docker/${DOCKER_TARGET}/solrcore
	cp handlers/target/liresolr-handlers-*.jar docker/${DOCKER_TARGET}/solrcore/liresolr-handlers.jar
	cp web/target/liresolr-webapp-*.war docker/${DOCKER_TARGET}/web/web.war
	cd docker; ./build.sh ${DOCKER_TARGET}

clean:
	rm -f docker/*/solrcore/lire.jar
	rm -f docker/*/solrcore/JOpenSurf.jar
	rm -f docker/*/solrcore/liresolr-handlers.jar
	rm -f docker/*/web/web.war
