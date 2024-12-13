#!/bin/bash
#SBATCH --job-name=eval-rlcheck-dry-run
#SBATCH --output=/vol/tmp/nguyehoa/slurm-logs/%x-%A/task-%a.out
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem-per-cpu=8000
#SBATCH --time=03:15:00
#SBATCH --array=1-180%60

# Ensure tasks are only run on gruenau[3-6]
#SBATCH --constraint=ivybridge

# Exclusively allocate node for task (use sparingly!)
##SBATCH --exclusive

BENCHMARKS=(ant.ProjectBuilderTest closure.CompilerTest maven.ModelReaderTest nashorn.CompilerTest rhino.CompilerTest tomcat.WebXmlTest)
GENERATORS=(xml.XmlRLGenerator js.JavaScriptRLGenerator xml.XmlRLGenerator js.JavaScriptRLGenerator js.JavaScriptRLGenerator xml.XmlRLGenerator)

declare -a combinations
index=1
for benchmark_index in `seq 0 5`
do
  for trial in `seq 1 30`
  do
      combinations[$index]="$benchmark_index $trial"
      index=$((index + 1))
  done
done

parameters=(${combinations[${SLURM_ARRAY_TASK_ID}]})

subject=${BENCHMARKS[${parameters[0]}]}
generator=${GENERATORS[${parameters[0]}]}
trial=${parameters[1]}

# Split subject into project and class
IFS='.' read -r project class <<< "$subject"

output_dir=eval-results/$SLURM_JOB_NAME-$SLURM_ARRAY_JOB_ID/rlcheck/dry-run/$project/trial-$trial

echo "[$(date)] Task $SLURM_ARRAY_TASK_ID: Running RLCheck dry-run on $project (trial $trial), writing results to $output_dir."

bin/jqf-rl -n -T 3h -r 3000 -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithInputStream edu.berkeley.cs.jqf.examples.$generator rlcheck-config/$project.json /vol/tmp/nguyehoa/$output_dir
PID=$!
wait $PID

echo "[$(date)] Finished task. Files have been written to /vol/tmp/nguyehoa/$output_dir."
