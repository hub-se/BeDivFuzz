# BeDivFuzz: Behavioral Diversity Fuzzing
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.6320229.svg)](https://doi.org/10.5281/zenodo.6320229)

This repository provides the code and replication instructions for our paper *BeDivFuzz: Integrating Behaviorial Diversity into Generator-based Fuzzing* ([ICSE'2022](https://arxiv.org/pdf/2202.13114.pdf)).

BeDivFuzz is implemented as an extension of [JQF](https://github.com/rohanpadhye/JQF). 


## Usage Guide
Coming soon.

## Replication/Experimental Setup

## Prerequisites
- Java 8-11
- Apache Maven
- Python 3

We also provide a Dockerfile to build a Docker image with all required tools. 

## Step 0: Setup Environment
### Local setup 
Install the required Python packages with `pip`:
```
pip install -r requirements.txt
```

### Using Docker
First, build the image:
```
docker build -t bedivfuzz .
```

Then, run the container (with the current directory mounted to `/workspace` inside the container):
```
docker run -it --rm -v ${PWD}:/workspace bedivfuzz
```

## Step 1: Build BeDivFuzz, Zest, and RLCheck

To build BeDivFuzz and Zest, run:
```
mvn package
```

RLCheck needs to be build separately:
```
cd RLCheck/jqf/
mvn package
cd ../..
```

## Optional: Test BeDivFuzz

We can now perform a test run of BeDivFuzz (e.g., on Rhino) as follows:
```
bin/jqf-bediv -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.rhino.CompilerTest testWithSplitGenerator
```

After a while, you should see a status screen similar to this:
 ```
BeDivFuzz: Behavioral Diversity Fuzzing.
Test name:            edu.berkeley.cs.jqf.examples.rhino.CompilerTest#testWithSplitGenerator
Results directory:    /Users/lam/_projects/clustering-guided-fuzzing/code/BeDivFuzz/experiments/fuzz-results
Elapsed time:         30s (no time limit)
Number of executions: 4,577
Valid inputs:         3,607 (78.81%)
Cycles completed:     0
Unique failures:      1
Queue size:           153 (0 favored last cycle)
Current parent input: 14 (favored) {229/360 mutations}
Execution speed:      215/sec now | 150/sec overall
Valid coverage:       5,236 branches (7.99% of map)
Behavioral Diversity: (B(0): 5367 | B(1): 3001 | B(2): 2563)
Unique valid inputs:  1,588 (34.70%)
Unique valid paths:   3,607
Structure-changing mutations (exploration):    216,776
Overall exploration score: 0.295
Structure-preserving mutations (exploitation):    611,515
Overall exploitation score: 0.990
 ```

Since this process runs without a timeout, you have to manually abort it with `Ctrl+C`.

## Step 2: Perform the Evaluation
The evaluation script can be executed as follows:
 ```
scripts/run_parallel_experiments.sh -o out_dir -t timeout -n repetitions -p parallel_workers [-r]
 ```

- `out_dir` is the folder where the results should be saved
- `timeout` is the timeout (in seconds) per trial
- `repetitions` is the number of repetitions to perform
- `parallel_workers` is the number of parallel trials to perform and must be a factor of `repetitions` to evenly distribute the workload (e.g., with repetitions=30 and parallel_workers=10, each instance will perform 3 repetitions)
- The `-r` flag enables coverage replay and is required to collect coverage data for RLCheck and QuickCheck 

In our original evaluation, we first performed experiments with 1 hour timeout and 30 repetitions to answer RQ1 (input diversity) and RQ2 (behavioral diversity). The command for this setup (with 15 parallel instances) is:
```
scripts/run_parallel_experiments.sh -o coverage-results -t 3600 -n 30 -p 15 -r 
```

To answer RQ3 (fault finding capabilities), we extended the timeout to 24 hours, but did not measure any coverage (i.e., no `-r` flag):
```
scripts/run_parallel_experiments.sh -o crash-results -t 86400 -n 30 -p 15
```


## Step 3: Generate the figures

For this step, we assume that the results are stored under `coverage-results` (RQ1/RQ2) and `crash-results` (RQ3). 

To generate the plots for Figure 3 (diverse valid inputs) and Figure 4 (behavioral diversity), use:
```
python3 scripts/gen_figures.py coverage-results
```
The plots will be produced in the subdirectory `coverage-results/figs`.

To generate the crash table (Table 1) from `crash-results`, the following command can be used:
```
python3 scripts/gen_crash_table.py crash-results
```
The table will be printed on the terminal, but also saved as `crash-results/crash_table.txt`.
