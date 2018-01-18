## Building / Running

### Method 1 - Build Manually

Use Maven's compile goal to build from command line:

    cd /path/to/freql-recommendation-engine
    mvn compile

Then use Maven's exec goal to run (change Main class to your FreqlApp class):

    mvn exec:java -D"exec.mainClass"="com.lmi.examples.fakeyoutoob.Main"

### Method 2 - Easy build with Docker (Ubuntu 14+ only!)

Make sure Docker 1.7 or above is installed:

    sudo apt-get install docker-ce
    docker version

Then build the Docker image **(replace [MEM] with max JVM memory allowed, such as "512M"**:

    cd /path/to/ubuntu-16.dockerfile
    docker build -t freql/master --build-arg FREQL_JVM_MEMORY=[MEM]

Now you can run the image with:

    docker run freql/master