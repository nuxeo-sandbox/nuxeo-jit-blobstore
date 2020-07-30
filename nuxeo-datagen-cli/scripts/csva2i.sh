#!/bin/bash

BASEDIR=$(dirname "$0")

java -Xms1G -Xmx2G  -cp $BASEDIR/../target/nuxeo-datagen-cli-11.2-SNAPSHOT.jar org.nuxeo.data.gen.cli.CSVAccount2IDCards $@ 


