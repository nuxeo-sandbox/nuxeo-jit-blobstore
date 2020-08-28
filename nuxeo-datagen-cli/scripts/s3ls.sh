#!/bin/bash

if [ -z "$CLI" ]
then
      BASEDIR=$(dirname "$0")/../target
else
      BASEDIR=$CLI
fi

java -Xms1G -Xmx2G  -cp $BASEDIR/nuxeo-datagen-cli-11.2-SNAPSHOT.jar org.nuxeo.data.gen.cli.S3LS $@ 


