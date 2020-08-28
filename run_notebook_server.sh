#!/bin/bash

BASEDIR=$(dirname "$0")

#PARENTDIR=$(dirname "$(pwd)")
PARENTDIR=$(pwd)

echo $PARENTDIR
echo $NXUSER
echo $M2_HOME

docker rm benchnb

docker run -e NXUSER -e NXPWD -e TITI=yes -e PARENTDIR --name benchnb --rm -p 127.0.0.1:8888:8888 -v $PARENTDIR/nuxeo-datagen-cli/scripts:/nxbench/scripts -v $PARENTDIR/notebooks:/nxbench/notebooks nxbench/notebook:latest
