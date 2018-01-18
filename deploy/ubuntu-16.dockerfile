FROM wtanaka/ubuntu-1604-oracle-java-9
MAINTAINER  Eric <git@letsmakeit.com>

### Input variables you can set here or overwrite when building with --build-art

ARG REPO_URL="https://github.com/gittyeric/freql-recommendation-engine.git"
ARG FREQL_JVM_RAM="256M"

### Create new Docker image

## Java Env stuff

RUN echo "export JAVA_HOME=/usr/lib/jvm/java-9-oracle" >> ~/.bashrc
RUN echo "export JAVA_OPTS=\"-Xmx${FREQL_JVM_RAM} -Xms${FREQL_JVM_RAM} -XX:+UseG1GC\"" >> ~/.bashrc

### Install Maven and Git

RUN apt-get update
RUN apt-get install git -y
RUN apt-get install maven -y
VOLUME /root/.m2
CMD ["mvn"]

### Clean apt-get dependencies

RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

### Install FREQL

RUN cd /opt && git clone $REPO_URL
RUN cd /opt/freql-recommendation-engine && mvn compile
