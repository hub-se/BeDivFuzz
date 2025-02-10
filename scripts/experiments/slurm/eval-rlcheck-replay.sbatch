#!/bin/bash
#SBATCH --job-name=eval-rlcheck-5min-replay
#SBATCH --output=/vol/tmp/nguyehoa/slurm-logs/%x-%A/task-%a.out
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem-per-cpu=8000
#SBATCH --time=4-00:00:00
#SBATCH --array=1-360%180

dry_run_base_dir=/vol/tmp/nguyehoa/eval-results/eval-rlcheck-182707/rlcheck
STATS_REFRESH_MS=1000

BENCHMARKS=(ant.ProjectBuilderTest closure.CompilerTest maven.ModelReaderTest nashorn.CompilerTest rhino.CompilerTest tomcat.WebXmlTest)
GENERATORS=(xml.XmlRLGenerator js.JavaScriptRLGenerator xml.XmlRLGenerator js.JavaScriptRLGenerator js.JavaScriptRLGenerator xml.XmlRLGenerator)

declare -a combinations
index=1
for benchmark_index in `seq 0 5`
do
  for trial in `seq 1 30`
  do
      for metrics in 'UPATHS:BEDIV' 'UPATHS:BEDIV:SEMCOV'
      do
        combinations[$index]="$benchmark_index $trial $metrics"
        index=$((index + 1))
      done  
  done
done

parameters=(${combinations[${SLURM_ARRAY_TASK_ID}]})

subject=${BENCHMARKS[${parameters[0]}]}
generator=${GENERATORS[${parameters[0]}]}
trial=${parameters[1]}
metrics=${parameters[2]}

# Split subject into project and class
IFS='.' read -r project class <<< "$subject"

dry_run_dir=$dry_run_base_dir/dry-run/$project/trial-$trial
if [[ $metrics == *SEMCOV* ]]; then
    output_dir=$dry_run_base_dir/replay-semantic-$SLURM_ARRAY_JOB_ID/$project/trial-$trial
else
    output_dir=$dry_run_base_dir/replay-default-$SLURM_ARRAY_JOB_ID/$project/trial-$trial
fi

num_execs=$(tail -n 1 $dry_run_dir/plot_data | awk -F', ' '{print $12 + $13}')
echo "[$(date)] Task $SLURM_ARRAY_TASK_ID: Running RLCheck replay on $project (trial $trial, execs: $num_execs), writing results to $output_dir."
echo "bin/jqf-rl -f -r $STATS_REFRESH_MS -N $num_execs -m $metrics -c \$(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithInputStream edu.berkeley.cs.jqf.examples.$generator rlcheck-config/$project.json $output_dir"

bin/jqf-rl -f -r $STATS_REFRESH_MS -N $num_execs -m $metrics -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithInputStream edu.berkeley.cs.jqf.examples.$generator rlcheck-config/$project.json $output_dir
PID=$!
wait $PID

echo "[$(date)] Finished task."
