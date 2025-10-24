#!/bin/bash

# create folders for keeping the results
output_folder="results/$(date "+%Y%m%d_%H%M%S")"

mkdir -p $output_folder/sources
mkdir -p $output_folder/compiled
mkdir -p $output_folder/records

# copy sources
cp tests/* $output_folder/sources

# C QuickSort
gcc -g -o "$output_folder/compiled/QuickSort" "tests/QuickSort.c"
perf record -e cycles:u -g -o "$output_folder/records/c_QuickSort.data" -- "$output_folder/compiled/QuickSort" numbers.txt
perf annotate --stdio --source -n -i "$output_folder/records/c_QuickSort.data" > "$output_folder/records/c_QuickSort_annotate.txt"

# C SOR
gcc -g -o "$output_folder/compiled/SOR" "tests/SOR.c"
perf record -e cycles:u -g -o "$output_folder/records/c_SOR.data" -- "$output_folder/compiled/SOR" 10000
perf annotate --stdio --source -n -i "$output_folder/records/c_SOR.data" > "$output_folder/records/c_SOR_annotate.txt"

# C LU
gcc -g -o "$output_folder/compiled/LU" "tests/LU.c"
perf record -e cycles:u -g -o "$output_folder/records/c_LU.data" -- "$output_folder/compiled/LU" 300000
perf annotate --stdio --source -n -i "$output_folder/records/c_LU.data" > "$output_folder/records/c_LU_annotate.txt"

# C SPARSE
gcc -g -o "$output_folder/compiled/SPARSE" "tests/SPARSE.c"
perf record -e cycles:u -g -o "$output_folder/records/c_SPARSE.data" -- "$output_folder/compiled/SPARSE" 100000000
perf annotate --stdio --source -n -i "$output_folder/records/c_SPARSE.data" > "$output_folder/records/c_SPARSE_annotate.txt"

# Java SOR
javac -d $output_folder/compiled tests/SOR.java
perf record -e cycles:u -g -k 1 -o $output_folder/records/java_SOR.data -- java -agentpath:/usr/lib64/libperf-jvmti.so -XX:+UnlockDiagnosticVMOptions -XX:+PreserveFramePointer -cp $output_folder/compiled SOR 10000
perf inject --jit -i $output_folder/records/java_SOR.data -o $output_folder/records/java_SOR.data.jitted
perf annotate --stdio --source -n -i $output_folder/records/java_SOR.data.jitted > $output_folder/records/java_SOR.annotate.txt

# Java LU
javac -d $output_folder/compiled tests/LU.java
perf record -e cycles:u -g -k 1 -o $output_folder/records/java_SOR.data -- java -agentpath:/home/basto/lib64/libperf-jvmti.so -XX:+UnlockDiagnosticVMOptions -XX:+PreserveFramePointer -cp $output_folder/compiled -Xmx8g LU 30000

# Java SPARSE
javac -d $output_folder/compiled tests/SPARSE.java
perf record -e cycles:u -g -k 1 -o $output_folder/records/java_SOR.data -- java -agentpath:/home/basto/lib64/libperf-jvmti.so -XX:+UnlockDiagnosticVMOptions -XX:+PreserveFramePointer -cp $output_folder/compiled -Xmx8g SPARSE 100000000

# Java QuickSort
javac -d $output_folder/compiled tests/QuickSort.java
perf record -e cycles:u -g -k 1 -o $output_folder/records/java_SOR.data -- java -agentpath:/home/basto/lib64/libperf-jvmti.so -XX:+UnlockDiagnosticVMOptions -XX:+PreserveFramePointer -cp $output_folder/compiled -Xmx8g QuickSort numbers.txt



##### TODO

#cp Viewer.java $output_folder/viewer

# compile viewer
#javac -d $output_folder/viewer Viewer.java


#perf record -g -k mono java -cp "$classpath" -XX:+UnlockDiagnosticVMOptions -XX:+PreserveFramePointer -agentpath:"$jvmtisopath":perf-map-agent/out/libperfmap.so "$classinput"
# Generate folded stack traces
#perf script -F+srcline -i "$perf_data" | flamegraph/stackcollapse-perf.pl > "$perf_folded"
# Generate flame graph
#flamegraph/flamegraph.pl "$perf_folded" > "$flamegraph_svg"