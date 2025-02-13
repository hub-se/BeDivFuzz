#!/bin/bash

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT_DIR=`dirname $SCRIPT_DIR`

# Find JQF classes and JARs
project="bedivfuzz"
version="1.0-SNAPSHOT"
jqf_version="2.0"

FUZZ_DIR="${ROOT_DIR}/core/target/"
INST_DIR="${ROOT_DIR}/core/target/"

FUZZ_JAR="${FUZZ_DIR}/$project-core-$version.jar"
INST_JAR="${FUZZ_DIR}/dependency/jqf-instrument-$jqf_version.jar"

INST_CLASSPATH="${INST_JAR}:${FUZZ_DIR}/dependency/asm-9.5.jar"
FUZZ_CLASSPATH="${FUZZ_DIR}/classes:${FUZZ_JAR}"

# If user-defined classpath is not set, default to '.'
if [ -z "${CLASSPATH}" ]; then
  CLASSPATH="."
fi  

# Java Agent config (can be turned off using env var)
if [ -z "$JQF_DISABLE_INSTRUMENTATION" ]; then
  JAVAAGENT="-javaagent:${INST_JAR}"
fi

# Run Java
if [ -n "$JAVA_HOME" ]; then
    java="$JAVA_HOME"/bin/java
else
    java="java"
fi

export JVM_OPTS="$JVM_OPTS -Xms8g -XX:-UseGCOverheadLimit"

"$java" -ea \
  -Xbootclasspath/a:"$INST_CLASSPATH" \
  ${JAVAAGENT} \
  -Djanala.conf="${SCRIPT_DIR}/janala.conf" \
  -cp "${FUZZ_CLASSPATH}:${CLASSPATH}" \
  ${JVM_OPTS} \
  $@

