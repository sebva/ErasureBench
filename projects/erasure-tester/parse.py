#!/usr/bin/env python3
import csv
import re

results = []
with open('/home/sebastien/dev/matrix/logs.txt') as file:
    for i in range(50):
        results.append(list(range(50)))

    for line in file.readlines():
        match = re.search(r'_([0-9]{1,2}) +\|.\[0m ([0-9]{1,2}) ([OK]{2})', line)
        if match is not None:
            f, t, o = match.groups()
            results[int(f)-1][int(t)-1] = True if o == 'OK' else False


vm = list(range(50))
with open('/home/sebastien/dev/matrix/ps.txt') as file:
    for line in file.readlines():
        match = re.search(r'swarm-([0-9]{1,2})/erasuretester_benchmark_([0-9]{1,2})', line)
        if match is not None:
            vm[int(match.group(2)) - 1] = int(match.group(1))

with open('out.csv', 'w') as file2:
    c = csv.writer(file2)
    c.writerow(['VMs', ''] + vm)
    c.writerow(['', 'from/to'] + list(range(1, 51)))
    for row in zip(results, range(50)):
        c.writerow([vm[row[1]], row[1] + 1] + row[0])
