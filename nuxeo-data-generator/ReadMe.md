
### Context

In the context of the 10B benchmark, we need to generate a very large number of files (for now PDFs).

This project is a simple Java library that provides the basic tools to generate documents:

 - a way to generate random meta-data
 - a way to generate PDF files

The focus of this lib is to generate "fake" bank statements.

### Build

   mvn clean package

### Meta-Data Generation

#### Source data

Meta-Data is generated based on CSV file that provides valid text data for:

 - first name
 - last name
 - street name
 - city
 - state
 - company name

All of this data is loaded in memory and for each new "account" a combination of these is generated.

The data generation includes 2 aspects:

 - generating the customer info
    - name, address ....
 - generating the statement info
    - list of operation for each months

#### Stable Generation

We want to generate Random data for 10B documents, but at the same time we need to be able to feed Gatling tests with valid and stable meta-data.

Because we can not store efficiently the meta-data generated for 10B documents, the chosen approach is to have a Random generation that is reproducible.

This is achieved by allowing to configure the seeds used generate the random data.
There are 3 parameters used to generate the data set:

 - Accout Seed: the one used to generate the account information
 - Data Seed: the one used to generate the statement dats
 - Month: a simple integer defining the month (12 => 1 year ago, 24 => 2 years ago ...)


The Account Seed allows to generate long integrer that are then used to "decode" the account metadata:

 - 17bits for First Name index
 - 17bit for Last Name index
 - 15 bits for City index
 - 11 bits for Street index
 - 3 bits for Account Number

This approach allows to also create multiple account per customers.

AccountID Format: 

    FFFFF-LLLLL-CCCCSS-AA

Where

 - `FFFFF` (5 letters) is the Hexadecimal representation of FirstName index 
 - `LLLLL` (5 letters) is the Hexadecimal representation of LastName index 
 - `CCCC` (4 letters) is the Hexadecimal representation of City index 
 - `SS` (2 letters) is the Hexadecimal representation of Street index 
 - `AA` is the decimal representation of Account number

#### Usage

The `SequenceGenerator` provide a "high-level" API to generate simple sequence of accounts.

        int nbAccountsToGenerate=10;
        int nbMonths=48;
       
        SequenceGenerator sGen = new SequenceGenerator(nbMonths);               
        
        for (int i = 0; i < nbAccountsToGenerate*nbMonths; i++) {
             
            SequenceGenerator.Entry entry = sGen.next();
            
            // This is the account in the form
            // "FFFFF-LLLLL-CCCCSS-AA"
            String accountId = entry.getAccountID();                        
            
            // these are the customer related metadata
            String[] meta = entry.getMetaData();            
        }

`SequenceGenerator` is just a wrapper around the `RandomDataGenerator`.

Currently, only the account meta-data are exposed by the `SequenceGenerator`.

### PDF Generation

#### Principles

After doing tests with several libraries it seems that iText is the fastest option.

Because we want to geenarte 10B of PDF, the main requirement of this generation is:

 - be super fast
 - generate small files

The selected approach is:

 - generate a PDF template using Java code and insert inside replacement tags
 - index the location of these tags in the PDF
 - replace the tags by actual meta-data to generate the PDF files

This is a very basic system, but this seems good enough and this was by far the fastest option.

