#!/bin/bash


export AWS_ACCESS_KEY_ID="XXX"
export AWS_SECRET_ACCESS_KEY="XXX"
export AWS_SESSION_TOKEN="XXX"


if [ -z "$CLI" ]
then
      BASEDIR=$(dirname "$0")/../target
else
      BASEDIR=$CLI
fi

JAR=$(ls -1 $BASEDIR/nuxeo-datagen-cli*.jar | tail -1)

java -Xms1G -Xmx2G  -jar $JAR $@ 


