#!/bin/bash
#SBATCH --job-name=eval-quickcheck-5min
#SBATCH --output=/vol/tmp/nguyehoa/slurm-logs/%x-%A/task-%a.out
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem-per-cpu=8000
#SBATCH --time=00:07:00
#SBATCH --array=1-270%30

# Ensure tasks are only run on gruenau[3-6]
#SBATCH --constraint=ivybridge

TIMEOUT=5m # don't forget to also update the SBATCH timeout
STATS_REFRESH_MS=1000

declare -a combinations
index=1

for subject in 'ant.ProjectBuilderTest' 'bcel.ParserTest' 'chocopy.SemanticAnalysisTest' 'closure.CompilerTest' 'maven.ModelReaderTest' 'nashorn.CompilerTest' 'pngj.PngReaderTest' 'rhino.CompilerTest' 'tomcat.WebXmlTest'
do
  for trial in `seq 1 30`
  do
      combinations[$index]="$subject $trial"
      index=$((index + 1))
  done
done

parameters=(${combinations[${SLURM_ARRAY_TASK_ID}]})

subject=${parameters[0]}
trial=${parameters[1]}

# Split subject into project and class
IFS='.' read -r project class <<< "$subject"

output_dir=eval-results/$SLURM_JOB_NAME-$SLURM_ARRAY_JOB_ID/quickcheck/dry-run/$project/trial-$trial

echo "[$(date)] Task $SLURM_ARRAY_TASK_ID: Running QuickCheck dry-run on $project (trial $trial), writing results to $output_dir."
echo "Campaign config: timeout: $TIMEOUT, stats refresh every $STATS_REFRESH_MS ms"

bin/jqf-blind -n -T $TIMEOUT -r $STATS_REFRESH_MS -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithGenerator /vol/tmp/nguyehoa/$output_dir
PID=$!
wait $PID

echo "[$(date)] Finished task."
