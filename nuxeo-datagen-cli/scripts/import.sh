#!/bin/bash

if [ -z "$CLI" ]
then
      BASEDIR=$(dirname "$0")/../target
else
      BASEDIR=$CLI
fi

JAR=$(ls -1 $BASEDIR/nuxeo-datagen-cli*.jar | tail -1)

java -Xms1G -Xmx2G  -cp $JAR org.nuxeo.data.gen.cli.RestCli $@
