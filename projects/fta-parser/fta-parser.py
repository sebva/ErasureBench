#!/usr/bin/env python3
import os
import random

import matplotlib.pyplot as plt
import sqlite3


def parse_trace(set_name, db_file):
    print(set_name)
    try:
        sql = sqlite3.connect(db_file)
        #sql.row_factory = sqlite3.Row

        cur = sql.cursor()
        cur.execute(r'SELECT event_start_time, event_type FROM event_trace JOIN component ON event_trace.component_id = component.component_id WHERE (event_type = 0 OR event_type = 1) AND component_type = 0 ORDER BY event_start_time;')

        count = 0

        x_axis = list()
        y_axis = list()

        event = cur.fetchone()
        while event is not None:
            x_axis.append(event[0])
            if event[1] == 1:
                count += 1
            else:
                count -= 1
            y_axis.append(count)
            event = cur.fetchone()

        count += cur.rowcount

        cur.close()
        sql.close()

        plt.plot(x_axis, y_axis)
        plt.show()

    except ValueError:
        pass


if __name__ == '__main__':
    databases = [x for x in os.listdir('databases') if x.endswith('.db')]
    random.shuffle(databases)
    for db in databases:
        parse_trace(db[:-3], 'databases/' + db)
