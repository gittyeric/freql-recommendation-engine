## Building / Running

### Method 1 - Build Manually

Use Maven's compile goal to build from command line:

    cd /path/to/freql-recommendation-engine
    mvn compile

Then use Maven's exec goal to run the first machine in your cluster (change Main class to your FreqlApp class):

    mvn exec:java -D"exec.mainClass"="com.lmi.examples.fakeyoutoob.MasterMain"

For all subsequent machines you add to the network, run the WorkerMain class instead:

    mvn exec:java -D"exec.mainClass"="com.lmi.examples.fakeyoutoob.WorkerMain"

### Method 2 - Easy build with Docker (Ubuntu 14+ only!)

Make sure Docker 1.7 or above is installed:

    sudo apt-get install docker-ce
    docker version

Then build the Docker image and provide 2 parameters:

1. Replace [MEM] with max JVM memory allowed, such as "512M"
2. Replace [MAIN] with the main class to run, such as "com.lmi.examples.fakeyoutoob.MasterMain" for the first machine in the cluster or "com.lmi.examples.fakeyoutoob.WorkerMain" for any additional machines joinging an existing cluster.

    cd /path/to/ubuntu-16.dockerfile
    docker build -t freql/master --build-arg FREQL_JVM_MEMORY=[MEM] --build-arg MAIN_CLASS=[MAIN]

Now you can run the image with:

    docker run freql/master