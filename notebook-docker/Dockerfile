# vim:set ft=dockerfile:
FROM       openjdk:11-jdk
MAINTAINER Nuxeo <packagers@nuxeo.com>

ENV DEBIAN_FRONTEND noninteractive

# Add needed convert tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    python3.6 \
    python3-pip \
    python3-setuptools \
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
 && mkdir -p /nxbench/scripts
 
# update path
ENV PATH /nxbench/scripts:$PATH

# add the java CLI
COPY /nuxeo-datagen-cli/target/nuxeo-datagen-cli-11.2-SNAPSHOT.jar /nxbench/cli/.
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

