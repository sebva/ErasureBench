#!/usr/bin/env python3
import argparse
import json
import matplotlib.pyplot as plt

def plot_results(filename):
    with open(filename) as fp:
        results = json.load(fp)

    results = [x for x in results if x['bench'] == 'bench_apache' and x['config'][1] == 60]
    for result in results:
        sort = sorted(result['results'].values(), key=lambda x: x['RS'], reverse=True)
        x_axis = [x['RS0'] - x['RS'] for x in sort]
        y_axis = [x['OK Files'] - 1 for x in sort]
        print(x_axis)
        print(y_axis)

        plt.plot(x_axis, y_axis, label=result['config'][0])

    plt.axis([0, 60, 0, 2516])
    plt.xlabel("Dead nodes")
    plt.ylabel("Successful queries")
    plt.legend()
    plt.show()


if __name__ == '__main__':
    parser = argparse.ArgumentParser("JSON results plotter")
    parser.add_argument('file')
    args = parser.parse_args()
    plot_results(args.file)
