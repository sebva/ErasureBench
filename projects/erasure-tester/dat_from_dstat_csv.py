#!/usr/bin/env python3
import os
import re
from datetime import datetime, timezone


def main():
    files = os.listdir('.')
    lines = dict()
    min_timestamp = 99999999999999
    for f in files:
        match = re.search(r'rediscapture_([0-9]+)_[0-9a-f]+\.csv', f)
        if match is not None:
            timestamp = int(match.group(1))
            if timestamp < min_timestamp:
                min_timestamp = timestamp
            timestamp -= timestamp % 5
            with open(f) as file:
                for line in file.readlines():
                    cols = line.split(',')
                    if len(cols) == 2:
                        try:
                            recv = float(cols[0])
                            send = float(cols[1])
                            thrghpt = (recv + send) / 5
                            if timestamp not in lines:
                                lines[timestamp] = thrghpt
                            else:
                                lines[timestamp] += thrghpt
                            timestamp += 5
                        except ValueError:
                            pass
    print('timestamp,hour,throughput')
    aggregate = 30
    current_value = 0
    current_timestamp = 0
    for timestamp in sorted(lines.keys()):
        if timestamp // aggregate == current_timestamp:
            current_value += lines[timestamp] / aggregate
        elif current_timestamp == 0:
            current_timestamp = timestamp // aggregate
            current_value = lines[timestamp] / aggregate
        else:
            hour = (current_timestamp * aggregate - min_timestamp) / 3600
            print(','.join(str(x) for x in [current_timestamp * aggregate, hour] + [current_value]))
            current_timestamp = timestamp // aggregate
            current_value = lines[timestamp]
    hour = (current_timestamp * aggregate - min_timestamp) / 3600
    print(','.join(str(x) for x in [current_timestamp * aggregate, hour] + [current_value]))


if __name__ == '__main__':
    main()
