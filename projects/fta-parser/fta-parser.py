#!/usr/bin/env python3

import sqlite3

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


def plot_trace(pdf, x_axis_np, y_axis_np, set_name):
    # Any filter available in scipy.signal can be applied
    # y_axis_np = signal.medfilt(y_axis_np, 51)

    locator = dates.AutoDateLocator()
    plt.gca().xaxis.set_major_formatter(dates.AutoDateFormatter(locator))
    plt.gca().xaxis.set_major_locator(locator)
    plt.title(set_name.capitalize())
    plt.ylabel("Alive nodes")
    plt.plot(x_axis_np, y_axis_np)
    plt.gcf().autofmt_xdate()
    pdf.savefig()
    plt.close()


if __name__ == '__main__':
    with PdfPages('test.pdf') as pdf:
        plot_trace(pdf, *parse_trace_method1('databases/cae.db'), set_name='cae')
        plot_trace(pdf, *parse_trace_method1('databases/cs.db'), set_name='cs')
        plot_trace(pdf, *parse_trace_method1('databases/dath14.db'), set_name='dath14')
        plot_trace(pdf, *parse_trace_method1('databases/deug.db'), set_name='deug')
        plot_trace(pdf, *parse_trace_method1('databases/g5k06.db'), set_name='g5k06')
        plot_trace(pdf, *parse_trace_method1('databases/glow.db'), set_name='glow')
        plot_trace(pdf, *parse_trace_method1('databases/lanl05_union.db'), set_name='lanl05')
        plot_trace(pdf, *parse_trace_method1('databases/ldns04.db'), set_name='ldns')
        plot_trace(pdf, *parse_trace_method1('databases/lri.db'), set_name='lri')
        plot_trace(pdf, *parse_trace_method1('databases/microsoft99.db'), set_name='microsoft')
        plot_trace(pdf, *parse_trace_method1('databases/notre_dame07_cpu1.db'), set_name='nd_cpu')
        plot_trace(pdf, *parse_trace_method1('databases/notre_dame07_rood.db'), set_name='nd_rood')
        plot_trace(pdf, *parse_trace_method1('databases/oneping.db'), set_name='oneping')
        plot_trace(pdf, *parse_trace_method2('databases/overnet03.db'), set_name='overnet')
        plot_trace(pdf, *parse_trace_method1('databases/pnnl07.db'), set_name='pnnl')
        plot_trace(pdf, *parse_trace_method1('databases/sdsc.db'), set_name='sdsc')
        plot_trace(pdf, *parse_trace_method1('databases/teragrid2.db'), set_name='teragrid')
        plot_trace(pdf, *parse_trace_method1('databases/ucb.db'), set_name='ucb')
        # In websites_02, the 0 epoch = 26/09/2001 16:11:10
        # In pyplot, the 0 epoch = 01/01/0001 -1
        #                Days    +1   16:11:10
        websites_epoch = 730753 + 1 + 86400. / (16 * 3600 + 11 * 60 + 10)
        plot_trace(pdf, *parse_trace_method1('databases/websites02.db', epoch=websites_epoch), set_name='websites')
