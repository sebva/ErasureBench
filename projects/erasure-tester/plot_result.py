#!/usr/bin/env python3
import argparse
import json
import re
import subprocess

import matplotlib.pyplot as plt
from datetime import datetime
from matplotlib.backends.backend_pdf import PdfPages

# This scripts generates the plots from results in JSON format
# Multiple JSON files can be specified to merge them


def plot_results(filenames, pgf):
    results = []
    for filename in filenames:
        with open(filename) as fp:
            results += json.load(fp)
    plot_checksum(results, pgf)
    plot_throughput(results, pgf)


def plot_throughput(results, pgf):
    if pgf:
        _plot_throughput_pgf(results)
    else:
        _plot_throughput_pyplot(results)


def _plot_throughput_pgf(results):
    all_results = [x for x in results if x['bench'] == 'bench_net_throughput']
    cluster_sizes = {x['config'][1] for x in results}
    cols = []

    for cluster_size in cluster_sizes:
        results = [x for x in all_results if x['config'][1] == cluster_size]
        for result in results:
            (x_axis, y_axis) = _xy_from_capture(result['results']['write_capture'])
            cols.append(["write-{}nodes-{}-x".format(cluster_size, result['config'][0])] + x_axis)
            cols.append(["write-{}nodes-{}-y".format(cluster_size, result['config'][0])] + y_axis)

        for redis_current in {val for sublist in [[y['redis_current'] for y in x['results']['measures']] for x in results] for val in sublist}:
            for result in results:
                for measure in [x for x in result['results']['measures'] if x['redis_current'] == redis_current]:
                    (x_axis, y_axis) = _xy_from_capture(measure['capture'])
                    type_of_read = 'normal' if redis_current == cluster_size else 'degraded'
                    cols.append(["read-{}-{}nodes-{}-x".format(type_of_read, cluster_size, result['config'][0])] + x_axis)
                    cols.append(["read-{}-{}nodes-{}-y".format(type_of_read, cluster_size, result['config'][0])] + y_axis)

    nb_rows = max(len(x) for x in cols)
    for row in range(nb_rows):
        print('\t'.join(str(x[row] if len(x) > row else 'nan') for x in cols))


def _plot_throughput_pyplot(results):
    try:
        total_files = results[0]['results']['measures'][0]['ok']
    except:
        total_files = None
    with PdfPages('throughput_%s.pdf' % datetime.today().isoformat()) as pdf:
        all_results = [x for x in results if x['bench'] == 'bench_net_throughput']
        cluster_sizes = {x['config'][1] for x in results}
        for cluster_size in cluster_sizes:
            results = [x for x in all_results if x['config'][1] == cluster_size]
            for result in results:
                (x_axis, y_axis) = _xy_from_capture(result['results']['write_capture'])
                plt.plot(x_axis, y_axis, label=result['config'][0])

            plt.title("Write, %d nodes" % cluster_size)
            plt.xlabel("Time [s]")
            plt.ylabel("Throughput [kB/s]")
            plt.legend(loc='best')
            pdf.savefig()
            plt.close()

            for redis_current in {val for sublist in [[y['redis_current'] for y in x['results']['measures']] for x in results] for val in sublist}:
                for result in results:
                    for measure in [x for x in result['results']['measures'] if x['redis_current'] == redis_current]:
                        (x_axis, y_axis) = _xy_from_capture(measure['capture'])
                        if total_files is None:
                            label = result['config'][0]
                        else:
                            label = "%s (%d%% files OK)" % (result['config'][0], measure['ok'] / total_files * 100)
                        plt.plot(x_axis, y_axis, label=label)

                plt.title("Read on %d out of %d nodes" % (redis_current, cluster_size))
                plt.xlabel("Time [s]")
                plt.ylabel("Throughput [kB/s]")
                plt.legend(loc='best')
                pdf.savefig()
                plt.close()

        pdf.infodict()['Title'] = "Throughput benchmark"


def _xy_from_capture(capture_file):
    regex = re.compile(r'^\|\s*([0-9]+).+\|\s*([0-9]+)\s*\|\s*([0-9]+)\s*\|$')
    x = []
    y = []

    tshark_output = subprocess.check_output(['tshark', '-q', '-nr', capture_file, '-t', 'r', '-z', 'io,stat,5'])
    for line in tshark_output.decode().split('\n'):
        regex_match = regex.match(line)
        if regex_match:
            x.append(int(regex_match.group(1)))
            y.append(int(regex_match.group(3)))
    return x, y


def plot_checksum(results, pgf):
    results = [x for x in results if x['bench'] in ('bench_bc', 'bench_10bytes', 'bench_apache')]
    cluster_sizes = {x['config'][1] for x in results}

    for cluster_size in cluster_sizes:
        pgf_titles = []
        pgf_columns = []

        for result in [x for x in results if x['config'][1] == cluster_size]:
            sort = sorted(result['results'].values(), key=lambda x: x['RS'], reverse=True)
            x_axis = [(x['RS0'] - x['RS']) / cluster_size for x in sort]
            y_axis = [(x['OK Files'] / sort[0]['OK Files']) for x in sort]

            if pgf:
                pgf_titles.append('{}-{}'.format(result['bench'], result['config'][0]))
                pgf_columns.append(x_axis)
                pgf_columns.append(y_axis)
            else:
                plt.plot(x_axis, y_axis, label=result['bench'] + ' / ' + result['config'][0])
                plt.title("Checksum benchmark, %d nodes" % result['config'][1])

        if pgf:
            filename = 'checksum-%s.dat' % datetime.today().isoformat()
            print('%s = %d nodes' % (filename, result['config'][1]))
            with open(filename, 'w') as dat:
                for title in pgf_titles:
                    title = title.replace('_', '-')
                    dat.write('%s-x\t%s-y\t' % (title, title))
                dat.write('\n')
                for i in range(max(len(x) for x in pgf_columns)):
                    for x in range(len(pgf_columns)):
                        try:
                            value = str(pgf_columns[x][i])
                        except IndexError:
                            value = 1 if x % 2 == 0 else 0
                        dat.write('%s\t' % value)
                    dat.write('\n')
        else:
            plt.axis(ymin=0, ymax=1, xmin=0, xmax=1)
            plt.xlabel("Dead nodes")
            plt.ylabel("Successful queries")
            plt.legend()
            plt.show()


if __name__ == '__main__':
    parser = argparse.ArgumentParser("JSON results plotter")
    parser.add_argument('--pgf', action='store_true')
    parser.add_argument('file', nargs='+')
    args = parser.parse_args()
    plot_results(args.file, pgf=args.pgf)
