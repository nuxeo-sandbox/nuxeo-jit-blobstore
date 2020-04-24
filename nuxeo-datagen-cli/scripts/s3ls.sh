#!/bin/bash


java -Xms1G -Xmx2G  -cp target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar org.nuxeo.data.gen.cli.S3LS $@ 


