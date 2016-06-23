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
                            if timestamp not in lines:
                                lines[timestamp] = [recv, send]
                            else:
                                lines[timestamp][0] += recv
                                lines[timestamp][1] += send
                            timestamp += 5
                        except ValueError:
                            pass
    print('timestamp,hour,recv,send')
    for timestamp in sorted(lines.keys()):
        hour = (timestamp - min_timestamp) / 3600
        print(','.join(str(x) for x in [timestamp, hour] + lines[timestamp]))


if __name__ == '__main__':
    main()
