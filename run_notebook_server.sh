#!/bin/bash

BASEDIR=$(dirname "$0")

#PARENTDIR=$(dirname "$(pwd)")
PARENTDIR=$(pwd)

echo $PARENTDIR
echo $NXUSER
echo $M2_HOME

docker rm benchnb

docker run --env-file $PARENTDIR/notebook.env -e PARENTDIR --name benchnb --rm -p 127.0.0.1:8888:8888 -v $PARENTDIR/nuxeo-datagen-cli/scripts:/nxbench/scripts -v $PARENTDIR/notebooks:/nxbench/notebooks -v $PARENTDIR/blobstore:/nxbench/blobs -v /home/tdelprat/bench/injector/data:/nxbench/data -v /apps/nuxeo/conf/:/nxbench/nuxeo-conf:ro --net host nxbench/notebook:latest
