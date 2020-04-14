#!/bin/bash


export AWS_ACCESS_KEY_ID="XXX"
export AWS_SECRET_ACCESS_KEY="XXX"
export AWS_SESSION_TOKEN="XXX"

java -Xms1G -Xmx2G  -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar $@ 


