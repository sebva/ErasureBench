#!/usr/bin/env python3
import argparse
import json
import matplotlib.pyplot as plt


def plot_results(filename):
    with open(filename) as fp:
        results = json.load(fp)

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
    parser.add_argument('file')
    args = parser.parse_args()
    plot_results(args.file)
