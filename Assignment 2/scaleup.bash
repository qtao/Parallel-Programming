#!/bin/bash
make -s clean all
echo "Runs =,$1"
echo
echo "Num Threads,Data Size,Startup Time,Elapsed Time"

dSize=16

for (( tNum=1; tNum<=16; tNum=$(($tNum * 2)) ))
do
    for ((run_num=0; run_num<$1; run_num++))
    do
        printf "$tNum,$dSize,"
        echo $(java ParallelSealedDES $tNum $dSize ignore)
    done
    dSize=$(($dSize+1))
done
