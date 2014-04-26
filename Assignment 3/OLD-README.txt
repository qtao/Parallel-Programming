I created a new Elastic MapReduce cluster with 1 m1.small Master and 4 c1.medium
Core instances.

I set the log folder location to be "s3://mstewa34-2014/log/"

To configure/run the streaming python version:
    1) Add a new "Streaming program" step
    2) The location of my mapper is "s3://mstewa34-2014/fof.mapper.py"
    3) The location of my reducer is "s3://mstewa34-2014/fof.reducer.py"
    4) The s3 input location is "s3://friends1000"
    5) I used "s3://mstewa34-2014/streaming_output/" as the output location

To configure the Java/custom jar version:
    1) Add a new "Custom JAR" step
    2) The location of my JAR is "s3://mstewa34-2014/FoF.jar"
    3) The arguments are as follows (with newlines separating arguments):
        FoF
        s3://friends1000/
        s3://mstewa34-2014/jar_output/

To run one or both of these steps after setting them up, all that is left is
to create the cluster.