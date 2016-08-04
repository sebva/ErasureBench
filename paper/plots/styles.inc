#set style line 1 lt 1 lw 8
#set style line 2 lt 3 lw 8
#set style line 3 lt 1 lw 2
#set style line 4 lt 3 lw 2
#set style line 5 lt 0 lw 8
#set style line 6 lt 0 lw 4

#set style line 1 lc rgb 'gray90' lt 1 lw 1 pt 9 ps 2
#set style line 2 lc rgb 'gray60' lt 1 lw 1
#set style line 3 lc rgb 'gray30' lt 1 lw 1
#set style line 4 lc rgb 'black'  lt 1 lw 1

#FIG1 spread,binpack,random,custom
set pointsize 2
set style line 1 lt 1 lc "red"       lw 2 pt 9 ps 1.5
set style line 2 lt 2 lc "orange"    lw 2 pt 11 ps 1.5
set style line 3 lt 3 lc "blue"      lw 2 pt 13 ps 1
set style line 4 lt 4 lc "black"     lw 2 pt 12 ps 1.5

set style line 5 lt 2 lc "black" lw 4 pt 10 ps 1.8
set style line 6 lt 2 lc "black" lw 4 pt 12 ps 2

set style line 7 lt 1 lc "black" lw 4 pt 6 ps 1.8
set style line 8 lt 1 lc "black" lw 4 pt 3 ps 1.8
set style line 9 lt 2 lc "black" lw 4 pt 4 ps 1.5

#set style line 8 lt 3 lw 2 pt 6
#set style line 9 lt 0 lw 2 pt 12

set style line 13 lt 5 lc "black" lw 4 pt 14 ps 2


# LINESTYLE USED IN FILLEDCURVES PLOTS
set style line 10 lt 1 lc "black" lw 1
set style arrow 1 nohead ls 10

set style line 11 lt 1 lc "#CCCCCC" lw 3
set style arrow 2 nohead ls 11

set style line 12 lt 1 lc "#888888" lw 4

#LINESTYLE for CHURN TRACES
set style line 20 lt 1 lc "black" lw 0.5

set style line 30 lt 1 lc "black" lw 0.1

#STYLES FOR MAP
set style line 40 lt 1 lc "grey" lw 5
set style line 41 lt 1 lc "red" lw 0.1 pt 7 ps 0.8

#DIFFERENT COLORS FOR LINES USED IN PERCENTILES
set style line 105 lt 1 lc rgb "#404040" lw 1 pt 9 ps 2
set style line 125 lt 1 lc rgb "#595959" lw 1 pt 9 ps 2
set style line 150 lt 1 lc rgb "#808080" lw 1 pt 9 ps 2
set style line 175 lt 1 lc rgb "#A6A6A6" lw 1 pt 9 ps 2
set style line 190 lt 1 lc rgb "#D9D9D9" lw 1 pt 9 ps 2

set style line 101 lt 1 lc "black" lw 2
set style line 102 lt 1 lc "black" lw 6
set style line 103 lt 2 lc "black" lw 2
set style line 104 lt 2 lc "black" lw 6

# style for number of packets (small dashed)
set style line 200 lt 21 lc "black" lw 2


# DASHED LINE STYLES
set style line 300  linewidth 1.000 pointtype 2 dashtype 1 pointsize default pointinterval 0
set style line 301  linewidth 2.000 pointtype 2 dashtype 2 pointsize default pointinterval 0
set style line 302  linewidth 2.000 pointtype 2 dashtype 4 pointsize default pointinterval 0
set style line 303  linewidth 2.000 pointtype 2 dashtype 5 pointsize default pointinterval 0
set style line 304  linewidth 2.000 pointtype 2 dashtype 6 pointsize default pointinterval 0
set style line 305  linewidth 2.000 pointtype 2 dashtype 7 pointsize default pointinterval 0
set style line 306  linewidth 2.000 pointtype 2 dashtype 8 pointsize default pointinterval 0
