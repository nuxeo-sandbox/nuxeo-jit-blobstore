
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

### What does this module provide

 - BlobStore
   - a BlobStore that can deliver PDF BankStatements that are generated on the fly depending on the key
   - A BlobDispatcher configuration
 - Thumbnails
   - a Thumbnail generation on the fly to avoid storing 10B thumbnails
 - a Stream Importer contribution
   - leverage  Stream Importer to create BankStatements pointing to PDF in the custom BlobStore

### Build

   mvn clean package

### Deploy

Use the marketplace package

### Running the import

**Create the messages for the Hierarchy**

    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"nbMonths": 48 },"context":{}}'   -u Administrator:Administrator http://127.0.0.1:8080/nuxeo/api/v1/automation/StreamImporter.runStatementFolderProducers

**Create the Hierarchy in the repository**

    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"nbThreads": 1, "rootFolder": "/default-domain" },"context":{}}'   -u Administrator:Administrator http://127.0.0.1:8080/nuxeo/api/v1/automation/StreamImporter.runDocumentConsumers


**Create the messages for the Statements**

    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"nbMonths": 48, "nbDocuments": 120 },"context":{}}'   -u Administrator:Administrator http://127.0.0.1:8080/nuxeo/api/v1/automation/StreamImporter.runStatementProducers


**Create the Statements in the repository**

    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"nbThreads": 1, "rootFolder": "/default-domain" },"context":{}}'   -u Administrator:Administrator http://127.0.0.1:8080/nuxeo/api/v1/automation/StreamImporter.runDocumentConsumers







