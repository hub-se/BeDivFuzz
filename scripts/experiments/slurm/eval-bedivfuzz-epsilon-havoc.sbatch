#!/bin/bash
#SBATCH --job-name=eval-epsilon-havoc-24h
#SBATCH --output=slurm-logs/%x-%A/task-%a.out
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem-per-cpu=8000
#SBATCH --time=24:15:00
#SBATCH --array=1-810%90

# Ensure tasks are only run on gruenau[3-6]
#SBATCH --constraint=ivybridge

# E-mail notifications
##SBATCH --mail-user=nguyehoa@informatik.hu-berlin.de
##SBATCH --mail-type=ALL

# Exclusively allocate node for task (use sparingly!)
##SBATCH --exclusive

timeout=1445m

declare -a combinations
index=1
for epsilon in '0.2' '0.5' '0.8'
do
  for havoc in '0.2' '0.5' '0.8'
  do
    for subject in 'ant.ProjectBuilderTest' 'bcel.ParserTest' 'chocopy.SemanticAnalysisTest' 'closure.CompilerTest' 'maven.ModelReaderTest' 'nashorn.CompilerTest' 'pngj.PngReaderTest' 'rhino.CompilerTest' 'tomcat.WebXmlTest'
    do
      for trial in `seq 1 10`
      do
        combinations[$index]="bedivfuzz $subject $trial $epsilon $havoc"
        index=$((index + 1))
      done
    done
  done
done

parameters=(${combinations[${SLURM_ARRAY_TASK_ID}]})

fuzzer=${parameters[0]}
subject=${parameters[1]}
trial=${parameters[2]}
epsilon=${parameters[3]}
havoc=${parameters[4]}

# Split subject into project and class
IFS='.' read -r project class <<< "$subject"

output_dir=eval-results/$SLURM_JOB_NAME-$SLURM_ARRAY_JOB_ID/$fuzzer-$epsilon-$havoc/$project/trial-$trial
echo "[$(date)] Task $SLURM_ARRAY_TASK_ID: Running $fuzzer on $project (trial $trial), writing results to $output_dir."


timeout $timeout bin/jqf-bedivfuzz -r 3000 -h $havoc -e $epsilon -m UPATHS:BEDIV -f -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.$subject testWithSplitGenerator /vol/tmp/nguyehoa/$output_dir
PID=$!
wait $PID

echo ""
echo "[$(date)] Finished task. Files have been written to /vol/tmp/nguyehoa/$output_dir."

