#!/bin/bash

for i in `find . -name *.eps`
do
    f_=${i%.eps}
    if [[ $f_.eps -nt $f_.pdf || ! -e $f_.pdf ]]
    then
	epstopdf $i
    fi
done

pdflatex -synctex=1 -shell-escape -interaction=nonstopmode main
bibtex main
pdflatex -synctex=1 -shell-escape -interaction=nonstopmode main
pdflatex -synctex=1 -shell-escape -interaction=nonstopmode main

