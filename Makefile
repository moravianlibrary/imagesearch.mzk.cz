all: run

run: build
	docker rm imagesearch.mzk.cz
	docker run -i -t -p 8080:80 -v /var/imagesearch:/data --name imagesearch.mzk.cz imagesearch.mzk.cz

build:
	docker build -t imagesearch.mzk.cz .
