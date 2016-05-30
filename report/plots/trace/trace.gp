set term postscript color eps enhanced 22
set output "trace.eps"
load "../styles.inc"

X_MARGIN=0.14
Y_MARGIN=0.04
WIDTH_IND=0.46
HEIGHT_IND=0.29
WIDTH_BETWEEN_X=0.03
WIDTH_BETWEEN_Y=-0.02

set size 1.0,0.6

#set multiplot #layout 4, 1

#X_POS=0
#Y_POS=0
#set origin X_MARGIN+(X_POS*(WIDTH_IND+WIDTH_BETWEEN_X)), Y_MARGIN+(Y_POS*(HEIGHT_IND+WIDTH_BETWEEN_Y))
#set size WIDTH_IND,HEIGHT_IND

set bmargin 3
set tmargin 3
set lmargin 8
set rmargin 3

set grid x front
#set xtics ("0" 0,"1m" 60,"2m" 120,"3m" 180,"4m" 240,"5m" 300) font "Arial, 16pt"
#set xrange [0:240]
set title "Availability of nodes - WebSites trace"
set grid y
set ylabel "alive nodes" #font "Arial, 16pt" offset -1,0
#set ytics ("0" 0, "1" 1, "2" 2, "3" 3, "4" 4) font "Arial, 16pt"
set yrange [0:130]
unset key
set datafile separator ","
#This plot uses an hack to print values bigger than 0
plot \
  'data/websites_size.csv' every 1\
     using 1:2 with l ls 3 notitle 'alive' ,\
  'data/websites_size.csv' every 100\
     using 1:2 with p ls 3 notitle 'alive',\
  10000\
    w lp ls 3 title 'random'

#commented, in case you want to plot another trace here
#X_POS=0
#Y_POS=1
#set origin X_MARGIN+(X_POS*(WIDTH_IND+WIDTH_BETWEEN_X)), Y_MARGIN+(Y_POS*(HEIGHT_IND+WIDTH_BETWEEN_Y))
#set size WIDTH_IND,HEIGHT_IND
#
#set ylabel "memory (GB)" font "Arial, 16pt" offset 1,0
#set ytics ("0" 0,"0.5" 0.5, "1" 1,"1.5" 1.5, "2" 2, "2.5" 2.5, "4" 4) font "Arial, 16pt"
#set yrange [0:3]
#plot \
#  'data/random.dat'\
#   using 1:($5/1000) with lines ls 3 notitle 'random', \
#  'data/random.dat' every 10\
#   using 1:($5/1000) with p ls 3 notitle 'random', \
#  100000000\
#    w lp ls 3 title 'random'
#


!epstopdf "trace.eps"
!rm "trace.eps"
quit
