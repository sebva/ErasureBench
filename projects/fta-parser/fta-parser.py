#!/usr/bin/env python3
import os

import matplotlib.pyplot as plt


def parse_trace(set_name, filename):
    print(set_name)
    try:
        with open(filename) as file:
            all_events = list()
            for line in [x for x in file.readlines() if not x.startswith('#')]:
                splitted = line.split("\t")
                machine_name = splitted[4]
                is_up = splitted[5] == '1'
                time_start = float(splitted[6])
                time_end = float(splitted[7])
                all_events.append((machine_name, is_up, time_start, time_end))

            count = len({x[0] for x in all_events})
            events = [(x[2], False) for x in all_events if not x[1]] + [(x[3], True) for x in all_events if not x[1]]
            events.sort(key=lambda x: x[0])
            print(count)
            x_axis = list()
            y_axis = list()

            all_events.sort(key=lambda e: e[2])
            for event in events:
                x_axis.append(event[0])
                if event[1]:
                    count += 1
                else:
                    count -= 1
                y_axis.append(count)

            plt.plot(x_axis, y_axis)
            plt.show()

    except ValueError:
        pass


if __name__ == '__main__':
    sets_names = [x for x in os.listdir('traces') if os.path.isdir(os.path.join('traces', x))]
    files = [os.path.join('traces', x, 'event_trace.tab') for x in sets_names]
    for (set_name, filename) in zip(sets_names, files):
        parse_trace(set_name, filename)
