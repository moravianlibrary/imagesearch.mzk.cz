#!/bin/bash

function printHelp {
        echo "Possible arguments:"
        echo "clean - Remove temp files."
        echo "pdf - Generate pdf file."
	echo "markdown - Generate markdown file."
}

if [ $# == 1 ]
then
	if [ $1 == "clean" ]
	then rm documentation.aux documentation.bcf documentation.blg documentation.out documentation.log documentation.bbl documentation.toc documentation.run.xml
	elif [ $1 == "pdf" ]
	then
		pdflatex -interaction=nonstopmode -shell-escape documentation.tex
		biber documentation.bcf
		pdflatex -interaction=nonstopmode -shell-escape documentation.tex
	elif [ $1 == "markdown" ]
	then
		pandoc -s documentation.tex -o documentation.md
	else
		echo "Wrong argument."
		printHelp
	fi
else
	echo "Wrong number of arguments."
	printHelp
fi
