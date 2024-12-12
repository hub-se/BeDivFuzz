#!/bin/bash
#SBATCH --job-name=eval-bedivfuzz-metrics-baseline
#SBATCH --output=/vol/tmp/nguyehoa/slurm-logs/%x-%A/task-%a.out
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem-per-cpu=8000
#SBATCH --time=03:15:00
#SBATCH --array=1-540%60

# Ensure tasks are only run on gruenau[3-6]
#SBATCH --constraint=ivybridge

# Exclusively allocate node for task (use sparingly!)
##SBATCH --exclusive

declare -a combinations
index=1
for subject in 'ant.ProjectBuilderTest' 'bcel.ParserTest' 'chocopy.SemanticAnalysisTest' 'closure.CompilerTest' 'maven.ModelReaderTest' 'nashorn.CompilerTest' 'pngj.PngReaderTest' 'rhino.CompilerTest' 'tomcat.WebXmlTest'
do
  for trial in `seq 1 30`
  do
    for fuzzer in 'bedivfuzz-simple' 'bedivfuzz-structure'
    do
      combinations[$index]="$fuzzer $subject $trial"
      index=$((index + 1))
    done
  done
done

parameters=(${combinations[${SLURM_ARRAY_TASK_ID}]})

fuzzer=${parameters[0]}
subject=${parameters[1]}
trial=${parameters[2]}

# Split subject into project and class
IFS='.' read -r project class <<< "$subject"

output_dir=eval-results/$SLURM_JOB_NAME-$SLURM_ARRAY_JOB_ID/$fuzzer/$project/trial-$trial
echo "[$(date)] Task $SLURM_ARRAY_TASK_ID: Running $fuzzer on $project (trial $trial), writing results to $output_dir."


if [[ "$fuzzer" == "bedivfuzz-simple" ]]; then
  bin/jqf-bedivfuzz-baseline -T 3h -r 3000 -m UPATHS:BEDIV -f -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithSplitGenerator /vol/tmp/nguyehoa/$output_dir
  PID=$!
  wait $PID
elif [[ "$fuzzer" == "bedivfuzz-structure" ]]; then
  bin/jqf-bedivfuzz-baseline -s -T 3h -r 3000 -m UPATHS:BEDIV -f -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithSplitGenerator /vol/tmp/nguyehoa/$output_dir
  PID=$!
  wait $PID
else
  echo "Unknown fuzzer: $fuzzer"
fi

echo ""
echo "[$(date)] Finished task. Files have been written to /vol/tmp/nguyehoa/$output_dir."
