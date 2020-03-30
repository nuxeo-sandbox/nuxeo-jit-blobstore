
### Context

In the context of the 10B benchmark, we need to generate a very large number of files (for now PDFs).
The initial plan was to generate the files on a SnowBall and fill an S3 bucket with it.

Unfortunately the context of not helping (March 2020) and we do not know when the SnowBall will be available.
This BlobStore is a way to do without the snowball.

### Generate on the Fly

The initial plan was:

    generate -> ship (snowball) -> store (s3) -> BlobStore serving the files

the new plan is to have a BlobStore generating the PDF on the fly

    BlobStore -> generate files depending on the key

This approach should allow us:

 - to do without the snowball
 - to avoid having anything to store

### Principles

We generate random data (names, street, city, dates, values ...)  that needs to be used:

 - inside the PDF generation
 - as meta-data for the Document

The idea is that this data is generated using Random sequences.
If we store inside the BlobKey the seeds used to generate the different blocks of data, then knowing the key we can regenerate the same meta-data and then the same file.

In the current implementation there will be 3 seeds:

 - 1 seed used to generate Identification information
   -  name, address, account number
 - 1 seed used to generate the data of the statement
   - amount, operations ...
 - 1 sequence to select the month of the statement

### Build

   mvn clean package

### Deploy

Copy `nuxeo-jitgen-blobstore-1.X.jar` in `nxserver/bundles`.

**Dependencies**

Ensure the dependencies are deployed:

 - nuxeo-importer bundles (in `nxserver/bundles`)
 	- nuxeo-importer-core-11.X.jar
    - nuxeo-importer-jaxrs-11.X.jar
	- nuxeo-importer-stream-11.X.jar
 - itext lib (in `nxserver/lib`)
    - io-7.1.10.jar
	- kernel-7.1.10.jar
	- layout-7.1.10.jar

### Running the import

**Create the messages for the Hierarchy**

    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"nbMonths": 48 },"context":{}}'   -u Administrator:Administrator http://127.0.0.1:8080/nuxeo/api/v1/automation/StreamImporter.runStatementFolderProducers

**Create the Hierarchy in the repository**

    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"nbThreads": 1, "rootFolder": "/default-domain" },"context":{}}'   -u Administrator:Administrator http://127.0.0.1:8080/nuxeo/api/v1/automation/StreamImporter.runDocumentConsumers


**Create the messages for the Statements**

    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"nbMonths": 48, "nbDocuments": 120 },"context":{}}'   -u Administrator:Administrator http://127.0.0.1:8080/nuxeo/api/v1/automation/StreamImporter.runStatementProducers


**Create the Statements in the repository**

    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"nbThreads": 1, "rootFolder": "/default-domain" },"context":{}}'   -u Administrator:Administrator http://127.0.0.1:8080/nuxeo/api/v1/automation/StreamImporter.runDocumentConsumers







