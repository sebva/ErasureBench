#!/usr/bin/env python3

import sqlite3
from datetime import timezone, tzinfo, datetime

import matplotlib.pyplot as plt
import numpy as np
from matplotlib import dates
from matplotlib.backends.backend_pdf import PdfPages


def parse_trace_method1(db_file, epoch=719163):
    print(db_file)
    sql = sqlite3.connect(db_file)

    cur = sql.cursor()

    cur.execute(r'SELECT event_start_time, event_type FROM event_trace ORDER BY event_start_time;')

    print("End query")

    x_axis = list()
    y_axis = list()

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

    x_axis_np = np.array([x / 86400. + epoch for x in x_axis])
    y_axis_np = np.array(y_axis)
    return x_axis_np, y_axis_np


def parse_trace_method2(db_file, epoch=719163):
    print(db_file)
    sql = sqlite3.connect(db_file)

    cur = sql.cursor()

    cur.execute(r'SELECT event_start_time, event_type FROM event_trace ORDER BY event_start_time;')

    print("End query")

    x_axis = list()
    y_axis = list()

    count = 0
    events = cur.fetchall()
    for event in events:
        x_axis.append(event[0])
        y_axis.append(count)

        if event[1] == 0:
            count += 1
        elif event[1] == 1:
            count -= 1

        x_axis.append(event[0])
        y_axis.append(count)

    cur.close()
    sql.close()

    x_axis_np = np.array([x / 86400. + epoch for x in x_axis])
    y_axis_np = np.array(y_axis)
    return x_axis_np, y_axis_np


def parse_trace_method3(db_file, epoch=719163):
    print(db_file)
    sql = sqlite3.connect(db_file)

    cur = sql.cursor()

    cur.execute(r'SELECT event_start_time, event_type FROM event_trace WHERE node_id IN (SELECT DISTINCT node_id FROM event_trace WHERE event_start_time=0) ORDER BY event_start_time;')

    print("End query")

    x_axis = list()
    y_axis = list()

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

    x_axis_np = np.array([x / 86400. + epoch for x in x_axis])
    y_axis_np = np.array(y_axis)
    return x_axis_np, y_axis_np


def plot_trace(x_axis_np, y_axis_np, set_name):
    # Any filter available in scipy.signal can be applied
    # y_axis_np = signal.medfilt(y_axis_np, 51)

    locator = dates.AutoDateLocator()
    plt.gca().xaxis.set_major_formatter(dates.AutoDateFormatter(locator))
    plt.gca().xaxis.set_major_locator(locator)
    plt.title(set_name.capitalize())
    plt.ylabel("Alive nodes")
    plt.plot(x_axis_np, y_axis_np)
    plt.gcf().autofmt_xdate()
#    pdf.savefig()
    plt.show()


def compute_histogram1(db_file):
    sql = sqlite3.connect(db_file)
    cur = sql.cursor()

    cur.execute("""
      SELECT event_end_time-event_start_time, count(event_end_time-event_start_time)
        from event_trace
        where event_type=1
        GROUP BY event_end_time-event_start_time;
    """)

    results = cur.fetchall()

    cur.close()
    sql.close()

    return [x[0] for x in results], [x[1] for x in results]


def compute_histogram2(db_file):
    sql = sqlite3.connect(db_file)
    cur = sql.cursor()

    cur.execute("""
      SELECT event_end_time-event_start_time, count(event_end_time-event_start_time)
        from event_trace
        where event_type=0
        GROUP BY event_end_time-event_start_time;
    """)

    results = cur.fetchall()

    cur.close()
    sql.close()

    return [x[0] for x in results], [x[1] for x in results]


def compute_histogram3(db_file):
    sql = sqlite3.connect(db_file)
    cur = sql.cursor()

    cur.execute("""
      SELECT event_end_time-event_start_time, count(event_end_time-event_start_time)
        from event_trace
        where event_type=1
        and node_id in
          (SELECT DISTINCT node_id from event_trace WHERE event_start_time=0)
        GROUP BY event_end_time-event_start_time;
    """)

    results = cur.fetchall()

    cur.close()
    sql.close()

    return [x[0] for x in results], [x[1] for x in results]


def bar(pdf, x_axis, y_axis, set_name):
    plt.title(set_name.capitalize())
    plt.ylabel("Occurrences")
    plt.xlabel("Uptime")
    plt.bar(x_axis, y_axis, color='r')
    pdf.savefig()
    plt.close()


def plot_data(db_file):
    sql = sqlite3.connect(db_file)
    cur = sql.cursor()

    # HUGE performance boost
    cur.execute(r'CREATE INDEX IF NOT EXISTS start_time_index ON event_trace (event_start_time)')
    cur.fetchone()

    size = 0
    for row in cur.execute(r'SELECT event_start_time, SUM(CASE event_type WHEN 1 THEN 1 ELSE -1 END) FROM event_trace GROUP BY event_start_time'):
        size += row[1]
        print("%d,%d" % (row[0], size))

    cur.close()
    sql.close()


def simulate(db_file):
    sql = sqlite3.connect(db_file)
    cur = sql.cursor()

    # HUGE performance boost
    cur.execute(r'CREATE INDEX IF NOT EXISTS start_time_index ON event_trace (event_start_time)')
    cur.fetchone()

    cur.execute(r'SELECT MIN(event_start_time), MAX(event_start_time) FROM event_trace')
    mintime, maxtime = cur.fetchone()
    current = mintime

    cur.execute('''
      SELECT DISTINCT node_id FROM event_trace
        WHERE node_id IN (SELECT DISTINCT node_id FROM event_trace WHERE event_start_time=?)
        ORDER BY node_id
        ''', (mintime, ))
    nodes_id = [x[0] for x in cur.fetchall()]
    reverse_nodes_id = dict(zip(nodes_id, range(len(nodes_id))))

    timeres = 60

    while current + timeres < maxtime:
        cur.execute('''
          SELECT node_id, event_type FROM event_trace
            WHERE event_start_time >= ?
              AND event_start_time < ?
              AND node_id IN (SELECT DISTINCT node_id FROM event_trace WHERE event_start_time=?)
            ORDER BY node_id, event_start_time''',
                    (current, current + timeres, mintime))
        events = cur.fetchall()
        if len(events) != 0:
            print('At t=%d' % current)
            for event in events:
                server_position, is_up = reverse_nodes_id[event[0]], event[1] == 1
                print("\tServer #%d -> %r" % (server_position, is_up))
        current += timeres

    cur.close()
    sql.close()


def pgfplotsfile(db_file, epoch_delta=0, start_time=0, end_time=10000000000, show_every=1):
    sql = sqlite3.connect(db_file)

    cur = sql.cursor()
    cur.execute(r'SELECT event_start_time, event_type FROM event_trace WHERE event_start_time > ? AND event_start_time < ? ORDER BY event_start_time;',
                (start_time, end_time))

    print("date,size")

    size = 0
    count = 0
    last_date_str = ""
    events = cur.fetchall()
    for event in events:
        if event[1] == 1:
            size += 1
        elif event[1] == 0:
            size -= 1

        date = datetime.fromtimestamp(event[0] + epoch_delta, timezone.utc)
        date_str = date.strftime('%Y-%m-%d %H:%M')
        if date_str != last_date_str:
            last_date_str = date_str
            if count % show_every == 0:
                print('%s,%d' % (date_str, size))
            count += 1

    cur.close()
    sql.close()


if __name__ == '__main__':
    #pgfplotsfile('databases/websites02.db', epoch_delta=1001779870, show_every=10)

    # simulate('databases/oneping.db')
    #
    # with PdfPages('fta.pdf') as pdf:
    #     plot_trace(pdf, *parse_trace_method1('databases/cae.db'), set_name='cae')
    #     bar(pdf, *compute_histogram1('databases/cae.db'), set_name='cae')
    #     plot_trace(pdf, *parse_trace_method1('databases/cs.db'), set_name='cs')
    #     bar(pdf, *compute_histogram1('databases/cs.db'), set_name='cs')
    #     plot_trace(pdf, *parse_trace_method1('databases/dath14.db'), set_name='dath14')
    #     #bar(pdf, *compute_histogram1('databases/dath14.db'), set_name='dath14')
    #     plot_trace(pdf, *parse_trace_method1('databases/deug.db'), set_name='deug')
    #     bar(pdf, *compute_histogram1('databases/deug.db'), set_name='deug')
    #     plot_trace(pdf, *parse_trace_method1('databases/g5k06.db'), set_name='g5k06')
    #     #bar(pdf, *compute_histogram1('databases/g5k06.db'), set_name='g5k06')
    #     plot_trace(pdf, *parse_trace_method1('databases/glow.db'), set_name='glow')
    #     bar(pdf, *compute_histogram1('databases/glow.db'), set_name='glow')
    #     plot_trace(pdf, *parse_trace_method1('databases/lanl05_union.db'), set_name='lanl05')
    #     bar(pdf, *compute_histogram1('databases/lanl05_union.db'), set_name='lanl05')
    #     plot_trace(pdf, *parse_trace_method1('databases/ldns04.db'), set_name='ldns')
    #     #bar(pdf, *compute_histogram1('databases/ldns04.db'), set_name='ldns')
    #     plot_trace(pdf, *parse_trace_method1('databases/lri.db'), set_name='lri')
    #     bar(pdf, *compute_histogram1('databases/lri.db'), set_name='lri')
    #     plot_trace(pdf, *parse_trace_method1('databases/microsoft99.db'), set_name='microsoft')
    #     #bar(pdf, *compute_histogram1('databases/microsoft99.db'), set_name='microsoft')
    #     plot_trace(pdf, *parse_trace_method1('databases/notre_dame07_cpu1.db'), set_name='nd_cpu')
    #     #bar(pdf, *compute_histogram1('databases/notre_dame07_cpu1.db'), set_name='nd_cpu')
    #     plot_trace(pdf, *parse_trace_method1('databases/notre_dame07_rood.db'), set_name='nd_rood')
    #     #bar(pdf, *compute_histogram1('databases/notre_dame07_rood.db'), set_name='nd_rood')
    #     plot_trace(pdf, *parse_trace_method3('databases/oneping.db'), set_name='oneping')
    #     bar(pdf, *compute_histogram3('databases/oneping.db'), set_name='oneping')
    #     plot_trace(pdf, *parse_trace_method2('databases/overnet03.db'), set_name='overnet')
    #     bar(pdf, *compute_histogram2('databases/overnet03.db'), set_name='overnet')
    #     plot_trace(pdf, *parse_trace_method1('databases/pnnl07.db'), set_name='pnnl')
    #     #bar(pdf, *compute_histogram1('databases/pnnl07.db'), set_name='pnnl')
    #     plot_trace(pdf, *parse_trace_method1('databases/sdsc.db'), set_name='sdsc')
    #     bar(pdf, *compute_histogram1('databases/sdsc.db'), set_name='sdsc')
    #     plot_trace(pdf, *parse_trace_method1('databases/teragrid2.db'), set_name='teragrid')
    #     bar(pdf, *compute_histogram1('databases/teragrid2.db'), set_name='teragrid')
    #     plot_trace(pdf, *parse_trace_method1('databases/ucb.db'), set_name='ucb')
    #     bar(pdf, *compute_histogram1('databases/ucb.db'), set_name='ucb')
    #     # In websites_02, the 0 epoch = 26/09/2001 16:11:10
    #     # In pyplot, the 0 epoch = 01/01/0001 -1
    #     #                Days    +1   16:11:10
    websites_epoch = 730753 + 1 + 86400. / (16 * 3600 + 11 * 60 + 10)
    plot_trace(*parse_trace_method1('databases/websites02.db', epoch=websites_epoch), set_name='websites')
    #     bar(pdf, *compute_histogram1('databases/websites02.db'), set_name='websites')
