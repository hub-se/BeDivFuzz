#!/usr/bin/env python
# coding: utf-8

import itertools
import os
import subprocess
import shutil
import sys

if len(sys.argv) < 2:
    print("Usage: python script.py <argument>")
    sys.exit(1)

fuzzers = ['bedivfuzz-havoc', 'bedivfuzz-simple', 'bedivfuzz-structure', 'bedivfuzz-split', 'quickcheck', 'rlcheck', 'zest']
subjects = ['ant.ProjectBuilderTest', 'bcel.ParserTest', 'chocopy.SemanticAnalysisTest', 'closure.CompilerTest', 'maven.ModelReaderTest', 'nashorn.CompilerTest', 'pngj.PngReaderTest', 'rhino.CompilerTest', 'tomcat.WebXmlTest']
num_trials = 30

experiment_base_dir = sys.argv[1]

def count_files(directory):
    return len([f for f in os.listdir(directory) if os.path.isfile(os.path.join(directory, f)) and not f.startswith('.')])

for fuzzer, subject, trial in itertools.product(fuzzers, subjects, range(1, num_trials+1)):
    clazz, _, test_method = subject.partition('.')
    
    if fuzzer in ('quickcheck', 'rlcheck'):
        trial_dir = os.path.join(experiment_base_dir, fuzzer, 'dry-run', clazz,  f"trial-{trial}")
    else:
        trial_dir = os.path.join(experiment_base_dir, fuzzer, clazz, f"trial-{trial}")

    failure_dir = os.path.join(trial_dir, 'failures')

    # For bedivfuzz-split copy failures from zest campaign
    if fuzzer == 'bedivfuzz-split':
        zest_failure_dir = os.path.join(trial_dir, 'zest-results', 'failures')
        if os.path.isdir(zest_failure_dir):
            if not os.path.isdir(failure_dir):
                os.makedirs(failure_dir)
            for file_name in os.listdir(zest_failure_dir):
                source_file = os.path.join(zest_failure_dir, file_name)
                destination_file = os.path.join(failure_dir, file_name)
                shutil.copy2(source_file, destination_file)

    if not os.path.isdir(failure_dir):
        continue

    num_failures = count_files(failure_dir) 
    if num_failures > 0:
        print(f"Found {num_failures} failures for {fuzzer}-{clazz}-{trial}, running repro.")
    
        repro_command = [
            "bin/jqf-repro",
            "-l", os.path.join(trial_dir, 'failure_log.csv'),
            "-c", "$(scripts/examples_classpath.sh)",
            f"edu.berkeley.cs.jqf.examples.{subject}",
            'testWithInputStream' if fuzzer == 'rlcheck' else 'testWithGenerator',
            f"{failure_dir}/*"
        ]

        print(' '.join(repro_command))
    
        # Run the Bash script with arguments
        result = subprocess.run(" ".join(repro_command), shell=True, capture_output=True, text=True)

        # Verify we have repro information for each recorded crash
        with open(os.path.join(trial_dir, 'failure_log.csv'), 'r') as file:
            line_count = sum(1 for line in file)
            if line_count != num_failures + 1:
                print(f"Expected {num_failures + 1} lines in failure log, found {line_count}.")
