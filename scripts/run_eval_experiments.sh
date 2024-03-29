#!/bin/bash

# This script allows to run multiple trials of the evaluation in parallel.

print_usage() {
    echo "Usage: $0 -o out_dir -t timeout -n repetitions -p parallel_workers"
}

if [ "$#" -lt 8 ]; then
    print_usage >&1
    exit 1
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

while getopts ":o:t:n:p:" opt; do
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
      TOTAL_REPETITIONS="$OPTARG"
      ;;
    p)
      N_WORKERS="$OPTARG"
      ;;
  esac
done
shift $((OPTIND-1))

mkdir -p $BASE_OUT_DIR/logs

# Basic sanity check
if [[ $(( $TOTAL_REPETITIONS % N_WORKERS )) == 0 ]]; then
    TRIALS_PER_WORKER=$(( $TOTAL_REPETITIONS / $N_WORKERS ))
else
    echo "Invalid configuration: $TOTAL_REPETITIONS repetitions cannot be evenly distributed among $N_WORKERS workers." >&2
    exit 1
fi

# Some info output
echo -e '\033[1mExperiment settings:\033[0m'
echo -e "Output dir: $BASE_OUT_DIR/ \nTimeout per trial: $TIMEOUT s \nRepetitions: $TOTAL_REPETITIONS ($N_WORKERS workers, $TRIALS_PER_WORKER trials/worker)"
echo "Logfiles for each individual worker are written to: logs/"


# Setup new tmux session
SESSION_NAME="bedivfuzz-evaluation"
tmux new-session -d -s $SESSION_NAME


# Create a separate window for each parallel worker within the tmux session and run evaluation script
for (( WORKER_ID=1; WORKER_ID<=N_WORKERS; WORKER_ID++ )); do
    
    tmux new-window -t $SESSION_NAME:$WORKER_ID
    tmux send-keys -t $SESSION_NAME:$WORKER_ID "$SCRIPT_DIR/eval_worker.sh -o $BASE_OUT_DIR -t $TIMEOUT -n $TRIALS_PER_WORKER -w $WORKER_ID" ENTER

done
