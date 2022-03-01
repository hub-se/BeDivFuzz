#!/bin/bash

# Implementation based on the following script: https://github.com/sameerreddy13/rlcheck/blob/master/scripts/run_java_exps.sh

print_usage() {
  echo "Usage: $0 -o out_dir -t timeout -n repetitions [-r]"
}

if [ "$#" -lt 6 ]; then
	print_usage >&1
	exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
JQF_DIR=$DIR/../
RLCHECK_DIR=$DIR/../RLCheck/jqf

while getopts ":o:t:n:r" opt; do
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
    r)
      COV_REPLAY=1
      ;;
  esac
done
shift $((OPTIND-1))

OUT_DIR=$BASE_OUT_DIR/java-data
mkdir -p $OUT_DIR

# Set name of the log file depending on whether coverage replay is enabled or not
LOG_FILE=$([ -n "$COV_REPLAY" ] && echo "coverage-experiments.log" || echo "crash-experiments.log")

echo "[$(date)] Log file: $LOG_FILE"

touch $LOG_FILE
echo "Start time: $(date)" > $LOG_FILE
echo "Experiment settings: writing to $OUT_DIR, doing $N_TRIALS repetitions with $TIMEOUT seconds timeout per trial." >> $LOG_FILE

if [ -n "$COV_REPLAY" ]; then
	echo "Coverage replay is enabled for RLCheck and QuickCheck." >> $LOG_FILE
else
	echo "Coverage replay (-r option) is not enabled. For RLCheck and QuickCheck, only blackbox stats (e.g., crashes) are recorded." >> $LOG_FILE
fi

BENCHMARKS=(ant maven closure rhino nashorn tomcat)
TEST_CLASSES=(ant.ProjectBuilderTest maven.ModelReaderTest closure.CompilerTest rhino.CompilerTest nashorn.CompilerTest tomcat.WebXmlTest)
TEST_GENS=(edu.berkeley.cs.jqf.examples.xml.XmlRLGenerator edu.berkeley.cs.jqf.examples.xml.XmlRLGenerator edu.berkeley.cs.jqf.examples.js.JavaScriptRLGenerator edu.berkeley.cs.jqf.examples.js.JavaScriptRLGenerator edu.berkeley.cs.jqf.examples.js.JavaScriptRLGenerator edu.berkeley.cs.jqf.examples.xml.XmlRLGenerator)
TEST_METHOD_DIV=(testWithSplitGenerator testWithSplitGenerator testWithSplitGenerator testWithSplitGenerator testWithSplitGenerator testWithSplitGenerator)
TEST_METHOD_ZEST=(testWithGenerator testWithGenerator testWithGenerator testWithGenerator testWithGenerator testWithGenerator)

dir_does_not_exist() {
  if [ -d $1 ]; then
    echo "$1 already exists, I won't re-run this experiment. Delete the directory and re-run the script if you want me to" >> $LOG_FILE
  return 1
    else
  return 0
    fi
}

trap "trap - SIGTERM && killall java && echo 'Terminated' >> $LOG_FILE && exit " SIGINT SIGTERM EXIT

for bench_index in {0..5}; do
  BENCHMARK=${BENCHMARKS[$bench_index]}
  TEST_CLASS=edu.berkeley.cs.jqf.examples.${TEST_CLASSES[$bench_index]}
  TEST_METHOD_RL=testWithInputStream
  TEST_GEN=${TEST_GENS[$bench_index]}
  CONFIG_FILE=${BENCHMARK}Config.json
  TEST_METHOD_DIV=${TEST_METHOD_DIV[$bench_index]}
  TEST_METHOD_ZEST=${TEST_METHOD_ZEST[$bench_index]}

  echo "======= Starting benchmark: $BENCHMARK =======" >> $LOG_FILE
  for (( REP=1; REP<=$N_TRIALS; REP++ )); do
    echo "----- REP: $REP (started at $(date)) -----" >> $LOG_FILE

    # First run RLCheck
    DIRNAME=${OUT_DIR}/rl-$BENCHMARK-$REP
    if dir_does_not_exist $DIRNAME ; then
      echo "[$(date)] Starting RLCheck. Writing results to $DIRNAME." >> $LOG_FILE
      NEW_CONFIG=${DIRNAME}-${CONFIG_FILE}
      echo "{\"params\": [ { \"name\":\"seed\", \"type\":\"long\", \"val\": $RANDOM }," > $NEW_CONFIG
      tail -n+2 $RLCHECK_DIR/configFiles/$CONFIG_FILE >> $NEW_CONFIG
      timeout $TIMEOUT $RLCHECK_DIR/bin/jqf-rl -n -c $($RLCHECK_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_RL $TEST_GEN $NEW_CONFIG $DIRNAME &
      PID=$!
      wait $PID
      ln -s rl-$BENCHMARK-$REP $OUT_DIR/rl-blackbox-$BENCHMARK-$REP
      echo "[$(date)] Finished regular RLCheck." >> $LOG_FILE
    fi

    # Now, do the replay to collect coverage data
    REPLAYNAME=$DIRNAME-replay
    if [ -n "$COV_REPLAY" ] && dir_does_not_exist $REPLAYNAME ; then
      echo "[$(date)] Starting RLCheck replay to collect coverage data. Writing results to $REPLAYNAME." >> $LOG_FILE
      NEW_CONFIG=${DIRNAME}-${CONFIG_FILE}
      REPLAYNUM=$(tail -n 1 $DIRNAME/plot_data | awk -F', ' '{print $5}')
      $RLCHECK_DIR/bin/jqf-rl -N $REPLAYNUM -c $($RLCHECK_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_RL $TEST_GEN $NEW_CONFIG $REPLAYNAME &
      PID=$!
      wait $PID
      ln -s rl-$BENCHMARK-$REP-replay $OUT_DIR/rl-blackbox-$BENCHMARK-$REP-replay
      echo "[$(date)] Finished RLCheck replay." >> $LOG_FILE
    else
      echo "[$(date)] Skipped replay for RLCheck." >> $LOG_FILE
    fi

    # Run jqf-bediv with default (simple) strategy (save new counts + new valid counts) 
    DIRNAME=${OUT_DIR}/bediv-simple-$BENCHMARK-$REP
    if dir_does_not_exist $DIRNAME ; then
      echo "[$(date)] Starting bediv-simple. Writing results to $DIRNAME." >> $LOG_FILE
      timeout $TIMEOUT $JQF_DIR/bin/jqf-bediv -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_DIV $DIRNAME &
      PID=$!
      wait $PID
      echo "[$(date)] Finished bediv-simple. No need to replay." >> $LOG_FILE
    fi

    # Run jqf-bediv with -s heuristic (save new counts + new valid counts for new structures only) 
    DIRNAME=${OUT_DIR}/bediv-structure-$BENCHMARK-$REP
    if dir_does_not_exist $DIRNAME ; then
      echo "[$(date)] Starting bediv-structure. Writing results to $DIRNAME." >> $LOG_FILE
      timeout $TIMEOUT $JQF_DIR/bin/jqf-bediv -s -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_DIV $DIRNAME &
      PID=$!
      wait $PID
      echo "[$(date)] Finished bediv-structure. No need to replay." >> $LOG_FILE
    fi

    # Let's do Zest
    DIRNAME=${OUT_DIR}/zest-$BENCHMARK-$REP
    if dir_does_not_exist $DIRNAME ; then
      echo "[$(date)] Starting Zest. Writing results to $DIRNAME." >> $LOG_FILE
      timeout $TIMEOUT $JQF_DIR/bin/jqf-zest -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_ZEST $DIRNAME &
      PID=$!
      wait $PID
      echo "[$(date)] Finished Zest. No need to replay." >> $LOG_FILE
    fi

    # Let's continue with quickcheck 
    DIRNAME=${OUT_DIR}/quickcheck-$BENCHMARK-$REP
    if dir_does_not_exist $DIRNAME ; then
      echo "[$(date)] Starting QuickCheck. Writing results to $DIRNAME." >> $LOG_FILE
      timeout $TIMEOUT $JQF_DIR/bin/jqf-quickcheck -n -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_ZEST $DIRNAME &
      PID=$!
      wait $PID
      echo "[$(date)] Finished QuickCheck." >> $LOG_FILE
    fi

   REPLAYNAME=$DIRNAME-replay
    if [ -n "$COV_REPLAY" ] && dir_does_not_exist $REPLAYNAME ; then
      echo "[$(date)] Starting QuickCheck replay to collect coverage data. Writing results to $REPLAYNAME." >> $LOG_FILE
      REPLAYNUM=$(tail -n 1 $DIRNAME/plot_data | awk -F', ' '{print $5}')
      $JQF_DIR/bin/jqf-quickcheck -N $REPLAYNUM -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_ZEST $REPLAYNAME &
      PID=$!
      wait $PID
      echo "[$(date)] Finished QuickCheck replay." >> $LOG_FILE
    else
      echo "[$(date)] Skipped replay for QuickCheck." >> $LOG_FILE
    fi

  done # Done rep
done # Done bench

echo "======= End of script reached at $(date) =======" >> $LOG_FILE
