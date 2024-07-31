#!/bin/bash
#SBATCH --job-name=eval-fast
#SBATCH --output=slurm-logs/%x-%A/task-%a.out
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem-per-cpu=16G
#SBATCH --time=03:15:00
#SBATCH --array=1-600%40

# Ensure tasks are only run on gruenau[3-6]
#SBATCH --constraint=ivybridge

# E-mail notifications
##SBATCH --mail-user=nguyehoa@informatik.hu-berlin.de
##SBATCH --mail-type=ALL

# Exclusively allocate node for task (use sparingly!)
##SBATCH --exclusive

timeout=181m

declare -a combinations
index=1
for fuzzer in 'zest' 'bedivfuzz' 'blind'
do
  for subject in 'ant.ProjectBuilderTest' 'bcel.ParserTest' 'chocopy.SemanticAnalysisTest' 'closure.CompilerTest' 'imageio.PngReaderTest' 'maven.ModelReaderTest' 'nashorn.CompilerTest' 'pngj.PngReaderTest' 'rhino.CompilerTest' 'tomcat.WebXmlTest'
  do
      for trial in `seq 1 20`
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


if [[ "$fuzzer" == "zest" ]]; then
  timeout $timeout bin/jqf-zest -r 3000 -m UPATHS:BEDIV -f -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithGenerator /vol/tmp/nguyehoa/$output_dir
  PID=$!
  wait $PID
elif [[ "$fuzzer" == "bedivfuzz" ]]; then
  timeout $timeout bin/jqf-bedivfuzz -r 3000 -h 0.1 -m UPATHS:BEDIV -f -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithSplitGenerator /vol/tmp/nguyehoa/$output_dir
  PID=$!
  wait $PID
elif [[ "$fuzzer" == "blind" ]]; then
  timeout $timeout bin/jqf-blind -r 3000 -m UPATHS:BEDIV -f -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithGenerator /vol/tmp/nguyehoa/$output_dir
  PID=$!
  wait $PID
else
  echo "Unknown fuzzer: $fuzzer"
fi

echo ""
echo "[$(date)] Finished task. Files have been written to /vol/tmp/nguyehoa/$output_dir."
#mkdir -p "$HOME/BeDivFuzz/$output_dir"
#cp -r "/vol/tmp/nguyehoa/$output_dir/." "$HOME/BeDivFuzz/$output_dir/"

#echo "[$(date)] Done."

