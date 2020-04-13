
### Context

In the context of the 10B benchmark, we need to generate a very large number of files (for now PDFs).

This project packaged the `nuxeo-data-generator` as a CommandLine utility so that it can be used:

 - to generate data to feed Gatling tests
 - to gnerate files to fill a snowball

### Build

   mvn clean package

The Maven build uses the `shade-plugin` in order to produce a "uber-jar" that includes all the dependencies.

### Execution

Run the exectable jar:

    java -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar -h

Current command line options 

    -aws_key <arg>       AWS_ACCESS_KEY_ID
    -aws_secret <arg>    AWS_SECRET_ACCESS_KEY
    -aws_session <arg>   AWS_SESSION_TOKEN
    -d,--months <arg>    Number of months of statements to generate
    -h,--help            Help
    -p,--pictures <arg>   path to read the pictures from
    -s,--seed <arg>      Seed used to initialize the Random sequence 
    -f,--filter <arg>    Apply fliter to convert generated PDF: jpeg or tiff
    -m,--mode <arg>      define generation mode: id (default), metadata, pdf
    -n,--nbDoc <arg>     Number of Documents to generate
    -o,--output <arg>    generate and output PDF : mem (default), tmp,
                         file:<path>, fileDigest:<path>, s3:<bucketName>, s3tm:<bucketName>,
                         s3tma:<bucketName>
    -t,--threads <arg>   Number of threads
    -x,--model <arg>      define the pdf model: statement (default) or id


Mode options are:

 - `id`: only AccountIDs are generated 
 - `metadata` : AccountID and UserInformation meta-data are generation 
    - first name last name
    - street
    - city
    - statement date
    - blob-key
 - 'pdf': generate the full PDF statement for each entry
    - pdf statement include operation that occured during the month

Output options are:

 - `mem`: generate the PDFs purely in memory (no network or disk IO)
 - `tmp`: store the PDFs in a java temporary file
 - `file`: store the PDFs in the directory passed 
 - `fileDigest`: store the PDFs in the directory passed using the md5 digest as filename
 - `s3`: store the PDFs in S3 using the std PUTObject API
 - `s3tm`: store the PDFs in S3 using the TransferManager
 - `s3tma`: store the PDFs in S3 using the TransferManager asynchronous API

NB: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` and `AWS_SESSION_TOKEN` can also be set as environment variables.

### How the data is generated

Each `accountID` is in the format: `03110-00CFE-5BF31E3-01`

 - the 19 first characters (i.e. `03110-00CFE-5BF31E3`) identify the customer (`customerID`)
 - the last 2 characters identify the account

The customerID is generated using a Random sequence initialised with the `seed` passed as parameter ( `-s` or `--seed`).
By default, the seed is initialized to always the same value used in the StreamImporter in order to make the sequence reproductible.

When using multi-threads ( `-t` or `--threads`), each thread is actually initialized with `seed++` in order to avoid any synchronization need between the threads,

For each `customerID`, there can be 1 to 6 `accountID`.
Then for each `accoundID` there will be 1 statement per month depending on the parameter `-d` or `--months`. 

### Meta-data collections and logs

By *"MetaData collection"* we mean: persisting the data that were used to generate each PDF file

 - the metadata 
 - the digest (or BlobKey) for the pdf file 

The file `metadata.csv` is used to store these informations.

The file `injector.log` contains the log of the steps of the import.

### Example execution

**Generating 10,000 AccountID**


    java -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar -t 1 -m id -n 10000

Generated AccountIDs are available in `metadata.csv`.

**Generating 10,000 AccountID with meta-data**


    java -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar -t 1 -m metadata -n 10000

Generated metadata are available in `metadata.csv`.

**Generating 10,000 PDF Statements**


    java -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar -t 1 -m pdf -n 10000 -o file:myoutputfolder

**Generating 100 JPEG Statements**


    java -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar -t 1 -m pdf -n 100 -f jpeg -o file:myoutputfolder

NB: Generation of Jpeg is much slower than PDF.

**Generating 100 TIFF Statements**


    java -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar -t 1 -m pdf -n 100 -f tiff -o file:myoutputfolder

NB: Generation of Tiff is much much slower than PDF.

**Generating ID cards**

    java -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar -t 1 -m pdf -d 1 -n 100 -f jpeg  -x id -o fileDigest:myoutputfolder

