
### Context

In the context of the 10B benchmark, we need to generate a very large number of files (PDFS, docx and png).

This project packaged the `nuxeo-data-generator` as a CommandLine utility so that it can be used:

 - to generate data to feed Gatling tests
 - to generate files to fill an AWS snowball
 - to inject dans directly inside Nuxeo

### Build

   mvn clean package

The Maven build uses the `shade-plugin` in order to produce a "uber-jar" that includes all the dependencies.

### Main entry point

The main entry point is about generating data.

Run the exectable jar:

    java -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar -h

Current command line options 

	usage: DataGenCLI	
	 -aws_endpoint <arg>   AWS_ENDPOINT
	 -aws_key <arg>        AWS_ACCESS_KEY_ID
	 -aws_secret <arg>     AWS_SECRET_ACCESS_KEY
	 -aws_session <arg>    AWS_SESSION_TOKEN
	 -d,--months <arg>     Number of months of statements to generate
	 -f,--filter <arg>     rendition to be applied to the pdf: tiff, jpeg
	 -h,--help             Help
	 -j,--jump <arg>       Jump to later in the sequence
	 -m,--mode <arg>       define generation mode: id (default), metadata, pdf
	 -monthOffset <arg>    Months offset
	 -n,--nbDoc <arg>      Number of Documents to generate
	 -o,--output <arg>     generate and output PDF : mem (default), tmp,
	                       file:<path>, fileDigest:<path>, s3:<bucketName>,
	                       s3tm:<bucketName>, s3tma:<bucketName>,
	                       s3a:<bucketName>
	 -p,--pictures <arg>   path to read the pictures from
	 -s,--seed <arg>       Seed
	 -t,--threads <arg>    Number of threads
	 -x,--model <arg>      define the pdf model: statement (default), id or
	                       letter

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

### Templates and documents generation

There currently 3 supported templates:

 - statement: generate a bank statement
    - iText PDF based
 - id: generate a ID card 
    - iText PDF based + filter to convert to JPG or TIFF
 - letter: generate an Account Opening letter
    - docx based

### How the data is generated

#### Principles

Each `accountID` is in the format: `03110-00CFE-5BF31E3-01`

 - the 19 first characters (i.e. `03110-00CFE-5BF31E3`) identify the customer (`customerID`)
 - the last 2 characters identify the account

The customerID is generated using a Random sequence initialised with the `seed` passed as parameter ( `-s` or `--seed`).
By default, the seed is initialized to always the same value used in the StreamImporter in order to make the sequence reproductible.

When using multi-threads ( `-t` or `--threads`), each thread is actually initialized with `seed++` in order to avoid any synchronization need between the threads,

For each `customerID`, there can be 1 to 6 `accountID`.
Then for each `accoundID` there will be 1 statement per month depending on the parameter `-d` or `--months`. 

#### Meta-data collections and logs

By *"MetaData collection"* we mean: persisting the data that were used to generate each PDF file

 - the metadata 
 - the digest (or BlobKey) for the pdf file 

The file `metadata.csv` is used to store these informations.

The file `injector.log` contains the log of the steps of the import.

#### Example execution

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

**Generating Account Opening Letter**

    java -jar target/nuxeo-datagen-cli-1.0-SNAPSHOT.jar -t 1 -m pdf -d 1 -n 100   -x letter -o fileDigest:myoutputfolder

### Injecting Data inside Nuxeo

#### Principles

We need to address different types of content:

 - hierarchy (i.e. Folders)
   - generated at import time using Stream/Importer DocumentProducers triggered via Automation
 		- see `StreamImporter.runConsumerFolderProducers`
 - Statements (i.e. Nuxeo StatementDocument + attached PDF)
   - generated at import time using Stream/Importer DocumentProducers triggered via Automation
     	- see `StreamImporter.runStatementProducers`
 - Customers, ID Cards and letters
   - pre-generated using this CLI as csv files and a collection of PNG files on the AWS Snowball
   - imported via CSV using `StreamImporter.runConsumerProducers`
   
#### Generating the base hierarchy   

Generate the messages for consumers tree in the stream `import/hierarchy` in single repository mode:

    scripts/import.sh -o consumertree -l import/hierarchy 

    
Generate the messages for consumers tree in the stream `import/hierarchy` in multi-repository repository mode:

    scripts/import.sh -o consumertree -l import/hierarchy -m 
    
This will generate messages in 2 different streams `import/hierarchy-us-east` and `import/hierarchy-us-west`, one for each target repository.

#### Import the hierarchy

Trigger import on repository `us-east` using "/customers" as base::

    scripts/import.sh -o import -l import/hierarchy-us-east -r us-east -b customers
     

Trigger import in async mode on repository `us-west` using "/customers" as base::

    scripts/import.sh -o import -l import/hierarchy-us-west -r us-west -a -b customers
      
#### Generate ID Cards document messages

Using from pre-generated CSV using 8 threads for parallel chunks upload, with multi-repositories:

    scripts/csvImport.sh -t 8 -p 8 -b 1000 -f metadata-xxx.csv -m -l import/Customers

This will generate messages in 2 different streams `import/Customers-us-east` and `import/Customers-us-west`, one for each target repository.

 - t: number of thread allocated client side to do the import
 - p: number of partition in the target stream
 - b: number of lines sent in each CSV chunk
 - l: target topic name

#### Import ID Cards 

Trigger import on repository `us-east` using "/customers" as base::

    scripts/import.sh -o -t 8 import -l import/Customers-us-east -r us-east -b /    

Trigger import in async mode on repository `us-west` using "/customers" as base::

    scripts/import.sh -o -t 8 import -l import/Customers-us-west -r us-west -a -b /
 
