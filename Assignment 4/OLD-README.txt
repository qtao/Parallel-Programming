Assignment #4: Cellular Automata in MPI
Michael Stewart
mstewa34@jhu.edu

This implementation relies on mpich2 to compile and run

To compile:
    "make" or "make all"
To run after compilation:
    "mpirun gameoflife"
To run with a custom number of processes (X = # of processes):
    "mpirun -n X gameoflife"