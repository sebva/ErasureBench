#!/usr/bin/env python3
import argparse
import json
import re
import subprocess

import matplotlib.pyplot as plt


def plot_results(filenames):
    results = []
    for filename in filenames:
        with open(filename) as fp:
            results += json.load(fp)
    plot_checksum(results)
    plot_throughput(results)


def plot_throughput(results):
    results = [x for x in results if x['bench'] == 'bench_net_throughput']
    cluster_sizes = {x['config'][1] for x in results}
    for cluster_size in cluster_sizes:
        for result in [x for x in results if x['config'][1] == cluster_size]:
            (x_axis, y_axis) = _xy_from_capture(result['results']['write_capture'])
            plt.plot(x_axis, y_axis, label="%s/write" % result['config'][0])

            for measure in result['results']['measures']:
                (x_axis, y_axis) = _xy_from_capture(measure['capture'])
                plt.plot(x_axis, y_axis, label="%s/read/%d dead nodes" % (result['config'][0], measure['redis_initial'] - measure['redis_current']))

        plt.title("Throughput benchmark, %d nodes" % cluster_size)
        plt.xlabel("Time [s]")
        plt.ylabel("Throughput [bytes/s]")
        plt.legend(loc='best')
        plt.show()


def _xy_from_capture(capture_file):
    regex = re.compile(r'^\|\s*([0-9]+).+\|\s*([0-9]+)\s*\|\s*([0-9]+)\s*\|$')
    x = []
    y = []

    tshark_output = subprocess.check_output(['tshark', '-q', '-nr', capture_file, '-t', 'r', '-z', 'io,stat,1'])
    for line in tshark_output.decode().split('\n'):
        regex_match = regex.match(line)
        if regex_match:
            x.append(regex_match.group(1))
            y.append(regex_match.group(3))
    return x, y


def plot_checksum(results):
    results = [x for x in results if x['bench'] in ('bench_bc', 'bench_10bytes', 'bench_apache')]
    cluster_sizes = {x['config'][1] for x in results}
    for cluster_size in cluster_sizes:
        for result in [x for x in results if x['config'][1] == cluster_size]:
            sort = sorted(result['results'].values(), key=lambda x: x['RS'], reverse=True)
            x_axis = [(x['RS0'] - x['RS']) / cluster_size for x in sort]
            y_axis = [(x['OK Files'] / sort[0]['OK Files']) for x in sort]
            print(x_axis)
            print(y_axis)

            plt.plot(x_axis, y_axis, label=result['bench'] + ' / ' + result['config'][0])
            plt.title("Checksum benchmark, %d nodes" % result['config'][1])

        plt.axis(ymin=0, ymax=1, xmin=0, xmax=1)
        plt.xlabel("Dead nodes")
        plt.ylabel("Successful queries")
        plt.legend()
        plt.show()


if __name__ == '__main__':
    parser = argparse.ArgumentParser("JSON results plotter")
    parser.add_argument('file', nargs='+')
    args = parser.parse_args()
    plot_results(args.file)
