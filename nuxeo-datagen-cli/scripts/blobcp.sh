#!/bin/bash

if [ -z "$CLI" ]
then
      BASEDIR=$(dirname "$0")/../target
else
      BASEDIR=$CLI
fi


java -Xms1G -Xmx2G  -cp $BASEDIRt/nuxeo-datagen-cli-1.0-SNAPSHOT.jar org.nuxeo.data.gen.cli.BlobCP $@ 


