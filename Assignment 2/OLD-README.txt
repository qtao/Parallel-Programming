Assignment #2: Measuring Parallel Performance
Michael Stewart
mstewa34@jhu.edu

To Run: cd to source folder
    make all
    java ParallelSealedDES NUM_THREADS NUM_BITS [LIMITED_OUTPUT]
    ** if a third argument is present then the output is limited to the **
    ** elapsed time and startup time. **

ParallelSealedDES.java contains the master class which generates a random key,
encrypts a string once for each thread, initializes the threads
(divides workload), executes them, and then waits for them to finish to print
the elapsed times.

DESDecrypter.java implements Runnable. It is given start (inclusive) and end
(exclusive) keys from which it attempts to brute force decrypt the given
encrypted object.

Skew should not be much of an issue since the threads will have at most 1 more
key to attempt to decrypt the object with than any other thread will have.