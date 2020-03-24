
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

