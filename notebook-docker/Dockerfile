# vim:set ft=dockerfile:
FROM       openjdk:11-jdk
MAINTAINER Nuxeo <packagers@nuxeo.com>

ENV DEBIAN_FRONTEND noninteractive

# Add jupyer and dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    python3.6 \
    python3-pip \
    python3-setuptools \
    fonts-freefont-ttf \
 && rm -rf /var/lib/apt/lists/* \
 && pip3 -q install --upgrade pip \
 && pip3 install jupyter \
 && pip3 install --upgrade ipython \
 && pip3 install pandas \
 && pip3 install matplotlib



# Build folder structure
RUN mkdir -p /nxbench/data \
 && mkdir -p /nxbench/cli \
 && mkdir -p /nxbench/notebooks \
 && mkdir -p /nxbench/scripts \
 && mkdir -p /nxbench/blobs \
 && mkdir -p /nxbench/nuxeo-conf
 
# Add the Mongo Shell client
RUN curl -fsSL https://downloads.mongodb.org/linux/mongodb-shell-linux-x86_64-ubuntu1804-4.4.0.tgz -o /tmp/mongodb-shell.tgz \
    && tar -xzvf /tmp/mongodb-shell.tgz -C /nxbench/cli/ \
    && rm /tmp/mongodb-shell.tgz
ENV PATH /nxbench/cli/mongodb-linux-x86_64-ubuntu1804-4.4.0/bin:$PATH

# Add Kafka CLI
RUN curl -fsSL http://apache.mirrors.hoobly.com/kafka/2.3.1/kafka_2.11-2.3.1.tgz -o /tmp/kafka-cli.tgz \
    && tar -xzvf /tmp/kafka-cli.tgz -C /nxbench/cli/ \
    && rm /tmp/kafka-cli.tgz
ENV PATH /nxbench/cli/kafka_2.11-2.3.1/bin:$PATH

# Add AWS CLI
RUN curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "/tmp/awscli-bundle.zip" \
 && unzip /tmp/awscli-bundle.zip -d /tmp/ \ 
 && /tmp/awscli-bundle/install -b /nxbench/aws
ENV PATH /nxbench:$PATH

# update path
ENV PATH /nxbench/scripts:$PATH

# add the java CLI
COPY /nuxeo-datagen-cli/target/nuxeo-datagen-cli-11.2-SNAPSHOT.jar /nxbench/cli/.
# set the env variable used by the scripts to locate the Java CLI
ENV CLI /nxbench/cli

# entry point
ENV TINI_VERSION v0.19.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini
ENTRYPOINT ["/tini", "--"]

WORKDIR /nxbench
#COPY docker-entrypoint.sh /
#ENTRYPOINT ["/docker-entrypoint.sh"]

EXPOSE 8888
CMD ["jupyter", "notebook", "--port=8888", "--no-browser", "--ip=0.0.0.0", "--allow-root"]

#USER 1000

