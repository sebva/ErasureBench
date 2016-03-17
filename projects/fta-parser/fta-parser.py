#!/usr/bin/env python3

import sqlite3

import matplotlib.pyplot as plt
import numpy as np
from matplotlib import dates


def parse_trace(set_name, db_file):
    print(set_name)
    try:
        sql = sqlite3.connect(db_file)

        cur = sql.cursor()

        cur.execute(r'SELECT event_start_time, event_type FROM event_trace ORDER BY event_start_time;')

        print("End query")

        x_axis = list()
        y_axis = list()

        # In websites_02, the 0 epoch = 26/09/2001 16:11:10
        # In pyplot, the 0 epoch = 01/01/0001 -1
        #                   Days    +1   16:11:10
        pyplot_epoch_diff = 730753 + 1 + 86400. / (16 * 3600 + 11 * 60 + 10)
        count = 0
        events = cur.fetchall()
        for event in events:
            x_axis.append(event[0])
            y_axis.append(count)

            if event[1] == 1:
                count += 1
            elif event[1] == 0:
                count -= 1

            x_axis.append(event[0])
            y_axis.append(count)

        cur.close()
        sql.close()

        x_axis_np = np.array([x / 86400. + pyplot_epoch_diff for x in x_axis])
        y_axis_np = np.array(y_axis)

        # Any filter available in scipy.signal can be applied
        # y_axis_np = signal.medfilt(y_axis_np, 51)

        locator = dates.AutoDateLocator()
        plt.gca().xaxis.set_major_formatter(dates.AutoDateFormatter(locator))
        plt.gca().xaxis.set_major_locator(locator)
        plt.title(set_name.capitalize())
        plt.ylabel("Alive nodes")
        plt.plot(x_axis_np, y_axis_np)
        plt.gcf().autofmt_xdate()
        plt.show()

    except ValueError:
        pass


if __name__ == '__main__':
    parse_trace('websites', 'databases/websites02.db')
