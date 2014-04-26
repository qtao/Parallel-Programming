#!/bin/bash
make -s clean all
echo "Runs =,$2"
echo
echo "Num Threads,Data Size,Startup Time,Elapsed Time"

for (( tNum=1; tNum<=16; tNum=$(($tNum * 2)) ))
do
    for ((run_num=0; run_num<$2; run_num++))
    do
        printf "$tNum,$1,"
        echo $(java ParallelSealedDES $tNum $1 ignore)
    done
done
