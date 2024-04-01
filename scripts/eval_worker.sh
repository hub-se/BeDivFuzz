#!/bin/bash

# Slightly modified version of run_experiments.sh to run multiple trials in parallel. To be used by calling run_parallel_experiments.sh

print_usage() {
  echo "Usage: $0 -o out_dir -t timeout -n repetitions -w worker_id"
}

if [ "$#" -lt 8 ]; then
	print_usage >&1
	exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
JQF_DIR=$DIR/../

while getopts ":o:t:n:w:" opt; do
  case $opt in
    \?)
      echo "Invalid option: -$OPTARG" >&2
      print_usage >&1
      exit 1
      ;;
    o)
      BASE_OUT_DIR="$OPTARG"
      ;;
    t)
      TIMEOUT="$OPTARG"
      ;;
    n) 
      N_TRIALS="$OPTARG"
      ;;
    w)
      WORKER_ID="$OPTARG"
      ;;
  esac
done
shift $((OPTIND-1))

OUT_DIR=$BASE_OUT_DIR/java-data
mkdir -p $OUT_DIR

# Compute range of trial ids for this worker. 
# E.g., assuming a total of 30 trials with 5 parallel workers, worker 1 performs trials 1-6, worker 2 performs trials 7-12, etc.
LOWER_TRIAL_ID=$(( ($WORKER_ID - 1) * $N_TRIALS + 1 ))
UPPER_TRIAL_ID=$(( $WORKER_ID * $N_TRIALS ))

# Set name of the log file depending on whether coverage replay is enabled or not
LOG_FILE="$BASE_OUT_DIR/logs/eval-worker-$WORKER_ID.log"

echo "[$(date)] Log file: $LOG_FILE"

touch $LOG_FILE
echo "Start time: $(date)" > $LOG_FILE
echo "Worker-ID: $WORKER_ID" >> $LOG_FILE
echo "Experiment settings: writing to $OUT_DIR, doing $N_TRIALS repetitions (trials $LOWER_TRIAL_ID to $UPPER_TRIAL_ID) with $TIMEOUT seconds timeout per trial." >> $LOG_FILE

BENCHMARKS=(ant maven closure rhino bcel chocopy pngj)
TEST_CLASSES=(ant.ProjectBuilderTest maven.ModelReaderTest closure.CompilerTest rhino.CompilerTest bcel.ParserTest chocopy.SemanticAnalysisTest pngj.PngReaderTest)

dir_does_not_exist() {
  if [ -d $1 ]; then
    echo "$1 already exists, I won't re-run this experiment. Delete the directory and re-run the script if you want me to" >> $LOG_FILE
  return 1
    else
  return 0
    fi
}

# Disabled since one worker finishing early should not interfere with the other processes
# trap "trap - SIGTERM && killall java && echo 'Terminated' >> $LOG_FILE && exit " SIGINT SIGTERM EXIT

for bench_index in {0..6}; do
  BENCHMARK=${BENCHMARKS[$bench_index]}
  TEST_CLASS=edu.berkeley.cs.jqf.examples.${TEST_CLASSES[$bench_index]}

  echo "======= Starting benchmark: $BENCHMARK =======" >> $LOG_FILE
  for (( REP=$LOWER_TRIAL_ID; REP<=$UPPER_TRIAL_ID; REP++ )); do
    echo "----- REP: $REP (started at $(date)) -----" >> $LOG_FILE

    # Run jqf-bedivfuzz
    #DIRNAME=${OUT_DIR}/bedivfuzz-$BENCHMARK-$REP
    #if dir_does_not_exist $DIRNAME ; then
    #  echo "[$(date)] Starting BeDivFuzz. Writing results to $DIRNAME." >> $LOG_FILE
    #  timeout $TIMEOUT $JQF_DIR/bin/jqf-bedivfuzz -f -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithSplitGenerator $DIRNAME &
    #  PID=$!
    #  wait $PID
    #  echo "[$(date)] Finished BeDivFuzz." >> $LOG_FILE
    #fi

    # Run jqf-tracking
    DIRNAME=${OUT_DIR}/bedivfuzz-$BENCHMARK-$REP
    if dir_does_not_exist $DIRNAME ; then
      echo "[$(date)] Starting BeDivFuzz-tracking. Writing results to $DIRNAME." >> $LOG_FILE
      timeout $TIMEOUT $JQF_DIR/bin/jqf-tracking -h 0.1 -m "BEDIV:UPATHS" -f -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithSplitGenerator $DIRNAME &
      PID=$!
      wait $PID
      echo "[$(date)] Finished BeDivFuzz." >> $LOG_FILE
    fi

    # Let's do Zest
    DIRNAME=${OUT_DIR}/zest-$BENCHMARK-$REP
    if dir_does_not_exist $DIRNAME ; then
      echo "[$(date)] Starting Zest. Writing results to $DIRNAME." >> $LOG_FILE
      timeout $TIMEOUT $JQF_DIR/bin/jqf-zest -m "BEDIV:UPATHS" -f -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator $DIRNAME &
      PID=$!
      wait $PID
      echo "[$(date)] Finished Zest." >> $LOG_FILE
    fi

  done # Done rep
done # Done bench

echo "======= End of script reached at $(date) =======" >> $LOG_FILE
