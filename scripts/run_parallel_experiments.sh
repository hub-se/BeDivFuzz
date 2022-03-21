#!/bin/bash

# This script allows to run multiple trials of the evaluation in parallel.

print_usage() {
    echo "Usage: $0 -o out_dir -t timeout -n repetitions -p parallel_workers [-r]"
}

if [ "$#" -lt 8 ]; then
    print_usage >&1
    exit 1
fi

mkdir -p logs

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

while getopts ":o:t:n:p:r" opt; do
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
    r)
      COV_REPLAY=1
      ;;
  esac
done
shift $((OPTIND-1))


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
if [ -n "$COV_REPLAY" ]; then
    echo "Coverage replay is enabled for RLCheck and QuickCheck."
else
    echo "Coverage replay (-r option) is not enabled. For RLCheck and QuickCheck, only blackbox stats (e.g., crashes) are recorded."
fi
echo "Logfiles for each individual worker are written to: logs/"


# Setup new tmux session
SESSION_NAME=$([ -n "$COV_REPLAY" ] && echo "bedivfuzz-coverage-evaluation" || echo "bedivfuzz-crash-evaluation")
tmux new-session -d -s $SESSION_NAME


# Create a separate window for each parallel worker within the tmux session and run evaluation script
for (( WORKER_ID=1; WORKER_ID<=$N_WORKERS; WORKER_ID++ )); do
    
    tmux new-window -t $SESSION_NAME:$WORKER_ID

    if [ -n "$COV_REPLAY" ]; then
        tmux send-keys -t $SESSION_NAME:$WORKER_ID "$SCRIPT_DIR/parallel_worker.sh -o $BASE_OUT_DIR -t $TIMEOUT -n $TRIALS_PER_WORKER -w $WORKER_ID -r" ENTER
    else
        tmux send-keys -t $SESSION_NAME:$WORKER_ID "$SCRIPT_DIR/parallel_worker.sh -o $BASE_OUT_DIR -t $TIMEOUT -n $TRIALS_PER_WORKER -w $WORKER_ID" ENTER
    fi

done
