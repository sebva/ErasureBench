#!/usr/bin/env python3
import json
import os

import subprocess

results_dir = '/opt/erasuretester/results/'


def gp(output, data, **kwargs):
    """
    Plot the data using Gnuplot.
    :param output: Path for the PDF output
    :param data: Data to plot in the following format:
        [
            {'title':'Title 1', 'points': [[1,9], [2,8], [3,7], [4,6]]},
            {'title':'Title 2', 'points': [[1,5], [2,4], [3,3], [4,2]]},
        ]
    :param kwargs: Various parameters. Required: title, xlabel, ylabel
    :return:
    """
    script = """
    set terminal pdf

    # some line types with different colors, you can use them by using line styles in the plot command afterwards (linestyle X)
    set style line 1 lt 1 lc rgb "#FF0000" lw 3 # red
    set style line 2 lt 1 lc rgb "#00FF00" lw 3 # green
    set style line 3 lt 1 lc rgb "#0000FF" lw 3 # blue
    set style line 4 lt 1 lc rgb "#000000" lw 3 # black
    set style line 5 lt 1 lc rgb "#CD00CD" lw 3 # purple
    set style line 6 lt 3 lc rgb "#ffa500" lw 3 # orange

    set title "{title}"

    # indicates the labels
    set xlabel "{xlabel}"
    set ylabel "{ylabel}"

    # set the grid on
    set grid x,y

    # set the key, options are top/bottom and left/right
    set key top right

    # indicates the ranges
    set yrange [0:]
    set xrange [1:]

    plot """.format(**kwargs)

    for i in range(len(data)):
        line_sep = "," if i < len(data) - 1 else ""
        filename = "-" if i == 0 else ""
        script += '"{0}" with lines linestyle {3} title "{1}"{2}'.format(filename, data[i]['title'], line_sep, i + 1)

    script += '\n' + '\ne\n'.join(['\n'.join(["%d\t%d" % x for x in plot['points']]) for plot in data]) + '\ne'

    gnuplot = subprocess.Popen(['gnuplot5'], stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    with open(output, 'wb') as file:
        file.write(gnuplot.communicate(script.encode())[0])
    print('Plot ready at %s' % output)


def main(output_file):
    result_file = [x for x in sorted(os.listdir(results_dir), reverse=True) if x.endswith('.json')][0]
    with open(results_dir + '/' + result_file) as fp:
        results = json.load(fp)
        isolate_points = lambda metric: sorted([(x['config'][1], x['results'][metric]) for x in results if
                                                x['config'][0] == 'Null' and x['bench'] == 'bench_dd'])

        data = [
            {'title': 'Write', 'points': isolate_points('write')},
            {'title': 'Read', 'points': isolate_points('read')},
        ]

        gp(output_file, data, title="Redis cluster size performance", xlabel='Redis size', ylabel='Throughput (kB/s)')


if __name__ == '__main__':
    main('test.pdf')
