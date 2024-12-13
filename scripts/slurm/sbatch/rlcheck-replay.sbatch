#!/bin/bash
#SBATCH --job-name=eval-rlcheck-replay
#SBATCH --output=/vol/tmp/nguyehoa/slurm-logs/%x-%A/task-%a.out
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem-per-cpu=8000
#SBATCH --time=0
#SBATCH --array=1-360%360

# Ensure tasks are only run on gruenau[3-6]
#SBATCH --constraint=ivybridge

# Exclusively allocate node for task (use sparingly!)
##SBATCH --exclusive

dry_run_base_dir=/vol/tmp/nguyehoa/eval-results/eval-rlcheck-dry-run-182707/rlcheck

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
output_dir=$dry_run_base_dir/replay-$SLURM_ARRAY_JOB_ID/$project/trial-$trial

num_execs=$(tail -n 1 $dry_run_dir/plot_data | awk -F', ' '{print $12 + $13}')
echo "[$(date)] Task $SLURM_ARRAY_TASK_ID: Running RLCheck replay on $project (trial $trial, execs: $num_execs), writing results to $output_dir."
echo "bin/jqf-rl -f -r 3000 -N $num_execs -m $metrics -c \$(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithInputStream edu.berkeley.cs.jqf.examples.$generator rlcheck-config/$project.json $output_dir"

bin/jqf-rl -f -r 3000 -N $num_execs -m $metrics -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithInputStream edu.berkeley.cs.jqf.examples.$generator rlcheck-config/$project.json $output_dir
PID=$!
wait $PID

echo "[$(date)] Finished task."
