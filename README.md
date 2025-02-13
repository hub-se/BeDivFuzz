# BeDivFuzz: Behavioral Diversity Fuzzing
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.6320229.svg)](https://doi.org/10.5281/zenodo.6320229)

BeDivFuzz is a generator-based fuzzer (powered by [JQF](https://github.com/rohanpadhye/JQF)) that distuingishes between *structure-preserving* and *structure-changing* mutations
to diversely explore the space of valid program behaviors.

## Research Paper
To learn more about BeDivFuzz, please refer to our research paper  ([ICSE'2022](https://arxiv.org/pdf/2202.13114.pdf)):

> Hoang Lam Nguyen and Lars Grunske. 2022.
> **BeDivFuzz: integrating behavioral diversity into generator-based fuzzing.**
> In Proceedings of the 44th International Conference on Software Engineering (ICSE '22).
> Association for Computing Machinery, New York, NY, USA, 249–261.
> https://doi.org/10.1145/3510003.3510182


## Setup
Install BeDivFuzz using Maven:
```
git clone https://github.com/hub-se/BeDivFuzz.git && cd BeDivFuzz
checkout standalone
mvn install
```

## Fuzzing with BeDivFuzz
We provide two methods to run BeDivFuzz: Using a shell script and as a maven plugin.

### Using the `jqf-bedivfuzz` script:
```
bin/jqf-bedivfuzz -c $(scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD $OUTPUT_DIR
```
`$TEST_CLASS`: The FQN of the test class, which must be annotated with `@RunWith(BeDivFuzz.class)`.

`$TEST_METHOD`: The test method (annotated with `@Fuzz`) inside `$TEST_CLASS`.

`$OUTPUT_DIR`: The directory where all output (fuzz corpus, failures, plot data) will be saved.
Defaults to `fuzz-results`.

#### Example: Fuzzing Apache Maven
```
bin/jqf-bedivfuzz -c $(scripts/examples_classpath.sh) de.hub.se.bedivfuzz.examples.maven.ModelReaderTest testWithSplitGenerator
```

<details>
<summary><h4 style="display:inline-block">Additional options</h4></summary>

`-e $EPSILON`: The exploration vs. exploitation trade-off of the (epsilon-greedy) adaptive mutation strategy (default: `0.2`).

`-h $HAVOC_RATE`: The probability of performing a havoc (untargeted) mutation (default: `0.1`).

`-T $TIMEOUT`: The total time to run the fuzzing campaign (default: no timeout).

`-f`: Enables fast, non-colliding instrumentation, which improves fuzzer throughput.

`-s`: Enables input structure feedback, favoring valid inputs with novel input structures.

#### Example: Fuzzing Google Closure for 1 hour with fast instrumentation and input structure feedback
```
bin/jqf-bedivfuzz -T 1h -fs -c $(scripts/examples_classpath.sh) de.hub.se.bedivfuzz.examples.closure.CompilerTest testWithSplitGenerator
```
</details>


### Using `mvn bedivfuzz:fuzz`
> [!NOTE]
> The BeDivFuzz Maven plugin needs to be launched from the `examples` directory.
```
mvn bedivfuzz:fuzz -Dclass=$TEST_CLASS -Dmethod=$TEST_METHOD -Dout=$OUTPUT_DIR
```
`$TEST_CLASS`: The FQN of the test class, which must be annotated with `@RunWith(BeDivFuzz.class)`.

`$TEST_METHOD`: The test method (annotated with `@Fuzz`) inside `$TEST_CLASS`.

`$OUTPUT_DIR`: The output directory. Defaults to `examples/target/fuzz-results/$TEST_CLASS/$TEST_METHOD`.

#### Example: Fuzzing Mozilla Rhino
```
mvn bedivfuzz:fuzz -Dclass=de.hub.se.bedivfuzz.examples.rhino.CompilerTest -Dmethod=testWithSplitGenerator
```

<details>
<summary><h4 style="display:inline-block">Additional options</h4></summary>

`-Depsilon $EPSILON`: The exploration vs. exploitation trade-off of the (epsilon-greedy) adaptive mutation strategy (default: `0.2`).

`-DhavocRate $HAVOC_RATE`: The probability of performing a havoc (untargeted) mutation (default: `0.1`).

`-Dtime $TIMEOUT`: The total time to run the fuzzing campaign (default: no timeout).

`-DfastInstrumentation`: Enables fast, non-colliding instrumentation, which improves fuzzer throughput.

`-DstructuralFeedback`: Enables input structure feedback, favoring valid inputs with novel input structures.
</details>
