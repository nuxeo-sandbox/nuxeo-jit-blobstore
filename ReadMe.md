
### Context

The project provides tools and building blocks that are used to help doing a 10B benchmark.

In order to do a 10B benchmark, you need to:

 - have 10B files
 - have meta-data for these 10B files
 - find a way to send this information to the Cloud
 - find a way to share this information between your repository and you test scenario

### Sub modules

 - `nuxeo-data-generator` : a java Library
    - provides API to generate meta-data
    - provides API to generate PDF files
 - `nuxeo-datagen-cli` : a CLI
    - provide CLI on top of the lib
    - can be used to feed the Gatling tests
    - can be used to fill a SnowBall
 - `nuxeo-jitgen-blobstore`: a Nuxeo Plugin
    - custom BlobStore
    - custom Thumbnails
    - contribution to Stream Importer 
 - package: a Marketplace package

### Build

   mvn clean package


