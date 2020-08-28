#!/bin/bash


#export AWS_ACCESS_KEY_ID="XXX"
#export AWS_SECRET_ACCESS_KEY="XXX"
#export AWS_SESSION_TOKEN="XXX"

if [ -z "$CLI" ]
then
      BASEDIR=$(dirname "$0")/../target
else
      BASEDIR=$CLI
fi

java -Xms1G -Xmx2G  -cp $BASEDIR/nuxeo-datagen-cli-11.2-SNAPSHOT.jar org.nuxeo.data.gen.cli.CSVRestImporter $@                      

