# Implementation adapted from: https://github.com/sameerreddy13/rlcheck/blob/master/scripts/gen_fig6_fig7_fig8.py

import matplotlib.pyplot as plt
import matplotlib
import itertools

plt.rcParams.update({
    'font.size': 24,
    'legend.fontsize': 20,
    'font.family' : 'sans-serif',
    'xtick.labelsize': 24,
    'ytick.labelsize': 24
    
})    
matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42

import numpy as np
from load_data import DataLoader

import sys
import os
import os.path

if len(sys.argv) != 2:
    print("Usage: {} results-dir".format(sys.argv[0]))
    sys.exit()
basedir = sys.argv[1]
if not os.path.isdir(basedir):
    print("Usage: {} results-dir".format(sys.argv[0]))
    print("ERROR: {} is not a directory".format(basedir))
    sys.exit()
else:
    try:
        os.mkdir(os.path.join(basedir, "figs"))
    except FileExistsError:
        # That's ok, we just wanted to create it in case it didn't exist.
        pass

dl = DataLoader(os.path.join(basedir, 'java-data'))
dl.load_data()

### MAIN PLOTTING ###
def get_x(run):
    x = run[dl.sm['unix_time']]
    x = (x - min(x)) / 60. # absolue time values in minutes
    return x

def error_bars(runs, ytype):
    ax = 0

    if ytype == "percent_upaths":
        runs = [np.array(r[6])/np.array(r[4]) for r in runs]
    else:
        dtype_idx = dl.sm[ytype]
        runs = [r[dtype_idx] for r in runs]

    mean = np.mean(runs, axis=ax)
    stderr = np.std(runs, axis=ax)/np.sqrt(len(runs))
    return mean, stderr

def yFormat(x, pos):
    if x < 1000:
        return "%i" % x
    elif x < 1000000:
        if x % 1000 == 0:
            return "%ik" % (x//1000)
        else:
            return f"{(x/1000):.1f}k"
    else:
        if x % 1000000 == 0:
            return "%im" % (x//1000000)
        else:
            return f"{(x/1000000):.1f}m"

def plot(valid_bench, ytype):
    nicenames = {"quickcheck": "QuickCheck", 
                 "zest": "Zest", 
                 "rl": "RLCheck",
                 "bediv-simple": "BeDiv-simple",
                 "bediv-structure": "BeDiv-structure"}

    fig, ax = plt.subplots(figsize=((10,7)))
    lss = iter([':', '--', '-.', '-', '-'])
    i = 0
    approaches = [('quickcheck', True), ('zest', False), ('rl', True), ('bediv-simple', False), ('bediv-structure', False)]
    for tech, replay in approaches:
        color = 'C%i' % (i)
        i += 1
        ls = next(lss)
        base_dirname = f"{tech}-{valid_bench}"
        runs = dl.get_data(base_dirname, replay)
        x = np.linspace(0, 60, dl.plot_data_step)
        y, stderr = error_bars(runs, ytype)
        plt.plot(x, y, label=nicenames[tech], linestyle=ls, linewidth=2, color=color)
        plt.fill_between(x, y + stderr, y - stderr, alpha=0.3, color=color)
    
    plt.xlabel("Time (min)")
    if "percent" in ytype:
        ax = plt.gca()
        from matplotlib.ticker import PercentFormatter, FuncFormatter
        ax.yaxis.set_major_formatter(PercentFormatter(xmax=1, decimals=0))
        ax.xaxis.set_major_formatter(FuncFormatter(lambda x, pos: "%i"% x))
    else:
        ax = plt.gca()
        from matplotlib.ticker import FuncFormatter
        ax.yaxis.set_major_formatter(FuncFormatter(lambda x, pos: yFormat(x, pos)))
        ax.xaxis.set_major_formatter(FuncFormatter(lambda x, pos: "%i"% x))

        
    if ytype == "valid_paths":
        plt.ylabel("Diverse Valids")
        from matplotlib.ticker import FuncFormatter
        ax.yaxis.set_major_formatter(FuncFormatter(lambda x, pos: yFormat(x, pos)))
    elif ytype == 'percent_upaths':
        plt.ylabel('% Diverse Valid')
    elif ytype == "h0_uniquePaths":
        plt.ylabel('B(0)')
    elif ytype == "h1_uniquePaths":
        plt.ylabel('B(1)')
    elif ytype == "h2_uniquePaths":
        plt.ylabel('B(2)')
    else:
        plt.ylabel(ytype.split('_'))

    leg = plt.legend(loc='best')

    for line in leg.get_lines():
        line.set_linewidth(5)
        pass
    plt.tight_layout()

    if ytype == "percent_upaths":
        figname = "Fig3a_percent_dvalids"
    elif ytype == "valid_paths":
        figname = "Fig3b_total_dvalids"
    else:
        figname = f"Fig4_{ytype.split('_')[0]}"

    plt.title(valid_bench)
    # ax.get_legend().remove()
    plt.savefig(os.path.join(basedir, "figs/{}_{}.pdf".format(figname, valid_bench)), dpi=150)
    plt.close()

# Generate all figures
for YTYPE in ["percent_upaths", "valid_paths", "h0_uniquePaths", "h1_uniquePaths", "h2_uniquePaths"]:
    print(f"Generating %s" % YTYPE)
    for v in dl.validity:
        plot(v, YTYPE)
