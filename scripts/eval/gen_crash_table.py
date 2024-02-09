#!/usr/bin/env python
# coding: utf-8

import csv, os, sys
import numpy as np
from tabulate import tabulate

def get_log_crash_stats(approach, benchmark, base_dir, num_trials=30):
    results_dir = os.path.join(base_dir, 'java-data')
    all_crashes = {}
    for i in range(1, num_trials+1):
        trial_dir = os.path.join(results_dir, '-'.join([approach, benchmark, str(i)]))
        failures_dir = os.path.join(trial_dir, 'failures')
        log_data = os.path.join(trial_dir, 'fuzz.log')

        # Crashes found during this run
        crashes = {}
        
        # Check if failures folder contains failing inputs
        if len(os.listdir(failures_dir)) != 0:
            crash_count = 0
            
            with open(log_data, 'r') as logfile:
                lines = logfile.readlines()
                for line in lines:
                    if 'Found crash:' in line:
                        crash_info = line.split(' ')
                        crash_time = int(crash_info[0])
                        crash_id = crash_info[5]
                        
                        # We skip these errors since they have resulted from the technique itself
                        if 'OutOfMemoryError' in crash_id:
                            continue

                        # Add first occurence of crash to dict
                        if not crash_id in crashes:
                            crashes[crash_id] = crash_time
                        crash_count += 1
                
            # Update all crashes found
            for crash_id in crashes.keys():
                if crash_id in all_crashes:
                    all_crashes[crash_id].append(crashes[crash_id])
                else:
                    all_crashes[crash_id] = [crashes[crash_id]]
                    
    return all_crashes


def aggregate_crash_stats(crash_dict, num_trials=30):
    result_dict = {}
    for crash in crash_dict.keys():
        crash_times = crash_dict[crash]
        mean_crash_time = np.mean(crash_times)
        reliability = len(crash_times)/num_trials * 100
        result_dict[crash] = (mean_crash_time, reliability)
    return result_dict


# Main
if len(sys.argv) < 2:
    print(f'Usage: python {sys.argv[0]} results_dir [num_trials]')
    sys.exit()

base_dir = sys.argv[1] 
if not os.path.isdir(base_dir):
    print(f"Usage: python {sys.argv[0]} results_dir [num_trials]")
    print("ERROR: {} is not a directory".format(base_dir))
    sys.exit() 
    
num_trials = int(sys.argv[2]) if len(sys.argv) > 2 else 30

approaches = ['bediv-simple', 'bediv-structure', 'zest','quickcheck', 'rl']
benchmarks = ['ant', 'maven', 'closure', 'rhino', 'tomcat', 'nashorn']

all_rows = []
all_rows.append(['Crash-ID', 'bediv-simple', 'bediv-structure', 'zest','quickcheck', 'rl'])
for bench in benchmarks:
    crashes = set()
    results = []
    for approach in approaches:
        result = aggregate_crash_stats(get_log_crash_stats(approach, bench, base_dir, num_trials))
        results.append(result)
        crashes.update(result.keys())
    for crash in crashes:
        row = []
        row.append(bench + '.' + crash[crash.find('java.lang.')+10:])
        for result in results:
            if crash in result:
                # Convert to minute
                mean_t_crash = result[crash][0]/60000
                
                # Some bugs may have been found under a minute
                if mean_t_crash >= 1:
                    row.append("%.0f (%d\%%)" % (result[crash][0]/60000, result[crash][1]))
                else:
                    row.append("<1 (%d\%%)" % (result[crash][1]))
            else:
                row.append('-')
        all_rows.append(row)

output = tabulate(all_rows, tablefmt='fancy_grid') + "\n\t\tTable 1: Average time (in minutes) and reliability of triggering a particular crash."

with open (os.path.join(base_dir,'crash_table.txt'), 'w') as f:
    f.write(output)
    print(output)
