#!/bin/bash

module load Java/21.0.

OUT="results/$(date "+%Y%m%d_%H%M%S")"

mkdir -p $OUT/sources
mkdir -p $OUT/compiled
mkdir -p $OUT/records

cp tests/* $OUT/sources

C_COMPILER_ARGS="-g"
C_RECORD_ARGS="-F 10000 -e cycles:u -g"
C_ANNOTATE_ARGS="--stdio --source -n"

gcc $C_COMPILER_ARGS -o $OUT/compiled/SOR tests/SOR.c
perf record $C_RECORD_ARGS -o $OUT/records/c_SOR.data $OUT/compiled/SOR 10000
perf annotate $C_ANNOTATE_ARGS -i $OUT/records/c_SOR.data > $OUT/records/c_SOR_annotate.txt

gcc $C_COMPILER_ARGS -o $OUT/compiled/LU tests/LU.c
perf record $C_RECORD_ARGS -o $OUT/records/c_LU.data $OUT/compiled/LU 300000
perf annotate $C_ANNOTATE_ARGS -i $OUT/records/c_LU.data > $OUT/records/c_LU_annotate.txt

gcc $C_COMPILER_ARGS -o $OUT/compiled/SPARSE tests/SPARSE.c
perf record $C_RECORD_ARGS -o $OUT/records/c_SPARSE.data $OUT/compiled/SPARSE 100000000
perf annotate $C_ANNOTATE_ARGS -i $OUT/records/c_SPARSE.data > $OUT/records/c_SPARSE_annotate.txt

gcc $C_COMPILER_ARGS -o $OUT/compiled/QuickSort tests/QuickSort.c
perf record $C_RECORD_ARGS -o $OUT/records/c_QuickSort.data $OUT/compiled/QuickSort numbers.txt
perf annotate $C_ANNOTATE_ARGS -i $OUT/records/c_QuickSort.data > $OUT/records/c_QuickSort_annotate.txt


J_RECORD_ARGS="-F 10000 -e cycles:u -g -k 1"
J_RECORD_JVM_ARGS="-cp $OUT/compiled -Xmx16g -agentpath:/usr/lib64/libperf-jvmti.so -XX:+UnlockDiagnosticVMOptions -XX:+PreserveFramePointer"
J_INJECT_ARGS="--jit"
J_ANNOTATE_ARDS="--stdio --source -n"

javac -d $OUT/compiled tests/SOR.java
perf record $J_RECORD_ARGS -o $OUT/records/java_SOR.data java $J_RECORD_JVM_ARGS SOR 10000
perf inject $J_INJECT_ARGS -o $OUT/records/java_SOR.data.jitted -i $OUT/records/java_SOR.data
perf annotate $J_ANNOTATE_ARDS -i $OUT/records/java_SOR.data.jitted > $OUT/records/java_SOR.annotate.txt

javac -d $OUT/compiled tests/LU.java
perf record $J_RECORD_ARGS -o $OUT/records/java_LU.data java $J_RECORD_JVM_ARGS LU 300000
perf inject $J_INJECT_ARGS -o $OUT/records/java_LU.data.jitted -i $OUT/records/java_LU.data
perf annotate $J_ANNOTATE_ARDS -i $OUT/records/java_LU.data.jitted > $OUT/records/java_LU.annotate.txt

javac -d $OUT/compiled tests/SPARSE.java
perf record $J_RECORD_ARGS -o $OUT/records/java_SPARSE.data java $J_RECORD_JVM_ARGS SPARSE 100000000
perf inject $J_INJECT_ARGS -o $OUT/records/java_SPARSE.data.jitted -i $OUT/records/java_SPARSE.data
perf annotate $J_ANNOTATE_ARDS -i $OUT/records/java_SPARSE.data.jitted > $OUT/records/java_SPARSE.annotate.txt

javac -d $OUT/compiled tests/QuickSort.java
perf record $J_RECORD_ARGS -o $OUT/records/java_QuickSort.data java $J_RECORD_JVM_ARGS QuickSort numbers.txt
perf inject $J_INJECT_ARGS -o $OUT/records/java_QuickSort.data.jitted -i $OUT/records/java_QuickSort.data
perf annotate $J_ANNOTATE_ARDS -i $OUT/records/java_QuickSort.data.jitted > $OUT/records/java_QuickSort.annotate.txt



# viewer
mkdir -p $OUT/viewer
cp Main.java $OUT/viewer
javac -d $OUT/viewer Main.java

java -cp $OUT/viewer Main $OUT/records/c_QuickSort_annotate.txt $OUT/records/c_QuickSort_annotate.txt

# flamegraphs
#perf record -g -k mono java -cp "$classpath" -XX:+UnlockDiagnosticVMOptions -XX:+PreserveFramePointer -agentpath:"$jvmtisopath":perf-map-agent/$OUT/libperfmap.so "$classinput"
#perf script -F+srcline -i "$perf_data" | flamegraph/stackcollapse-perf.pl > "$perf_folded"
#flamegraph/flamegraph.pl "$perf_folded" > "$flamegraph_svg"

# to connect
#ssh  basto97@login.deucalion.macc.fccn.pt
#srun --time=04:00:00 --partition=normal-arm --account=f202412862cpcaa1a --nodes=1 --pty bash