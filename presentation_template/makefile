#
# Macro pour la compilation de fichiers LaTeX optimisé pour les notes de cours
# 13 septembre 2013
#

# -------------------------------------------------------------------------------

# Nom du fichier final (jobname)
SRC = epto-middleware2015

# -------------------------------------------------------------------------------

.PHONY : nobib lua clean cleanfinal

# -------------------------------------------------------------------------------

# % For sans serif fonts compiled with lualatex. Better for presentations.

lua:
	make clean

	lualatex -synctex=1 --jobname=$(SRC)_p canvas.tex
#	lualatex -synctex=1 --jobname=$(SRC)_p canvas.tex

	make clean

	lualatex  -synctex=1  --jobname=$(SRC) "\def\ishandout{1} \input{canvas.tex}"
#	lualatex  -synctex=1  --jobname=$(SRC) "\def\ishandout{1} \input{canvas.tex}"

	make clean
	make cleanfinal

	open -a Skim $(SRC)_p.pdf	

# -------------------------------------------------------------------------------

# For regular serif fonts compiled with pdflatex. Useful for written / printed documents.

nobib:

	make clean

	pdflatex -synctex=1 --jobname=$(SRC)_p canvas.tex
#	pdflatex -synctex=1 --jobname=$(SRC)_p canvas.tex

	make clean

	pdflatex  -synctex=1  --jobname=$(SRC) "\def\ishandout{1} \input{canvas.tex}"
#	pdflatex  -synctex=1  --jobname=$(SRC) "\def\ishandout{1} \input{canvas.tex}"

	make clean
	make cleanfinal

	open -a Skim $(SRC)_p.pdf	

# -------------------------------------------------------------------------------

# Elimine les fichiers temporaires.

clean:
# Le - permet d'éviter des erreurs si la commande échoue.
	-rm *.log *.aux *.aux *.bbl *.blg *.info *.toc *.lof *.out *.lot *.dvi *.snm *.nav

cleanfinal:
	-rm *.synctex.gz *~ 
	-rm -R auto

# -------------------------------------------------------------------------------

