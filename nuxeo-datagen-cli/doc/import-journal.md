
### Clean repositopories


    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"repositoryName":"*"},"context":{}}' -u nco-admin:XXXXXXXXXXXXX http://127.0.0.1:8080/nuxeo/api/v1/automation/Benchmark.cleanRepository

    {"entity-type":"string","value":"1453ebf6-06b9-49a8-9657-e08ff31573db"}

    curl -H 'Content-Type:application/json+nxrequest' -X POST -d '{"params":{"repositoryName":"*", "confirmationKey":"1453ebf6-06b9-49a8-9657-e08ff31573db"},"context":{}}' -u nco-admin:XXXXXXXXXXXXX http://127.0.0.1:8080/nuxeo/api/v1/automation/Benchmark.cleanRepository

    curl -X DELETE "https://vpc-es-nxbench-2826-benr-2irnnyd7d2shthhsne4oabjmru.us-east-1.es.amazonaws.com/nuxeo-audit" -H 'Content-Type: application/json' 

### Init repositories with State hierarchy

Generate and import the state hierarchy

    scripts/import.sh -o consumertree -l import/hierarchy -m 
    scripts/import.sh -o import -l import/hierarchy-us-east -r us-east -b /
    scripts/import.sh -o import -l import/hierarchy-us-west -r us-west -b /

### Import Customers with ID cards


Import the CSV for identity cards:

    scripts/csvImport.sh -t 10 -p 16 -serverThreads 32 -b 50000 -f ~/bench/injector/data/id-cards-meta-data/all-the-unik-idcards.csv -m -l import/Customers


Throughput:46,662 csv lines /s => 93,324 doc/s injected in Kafka


     scripts/import.sh -o import -t 16 -l import/Customers-us-east -r us-east -b / -a -bulk

Async Automation Execution Scheduled
  => status url:[http://127.0.0.1:8080/nuxeo/site/api/v1/automation/StreamImporter.runDocumentConsumers/@async/ec3e1f94-9370-4fa8-a5f0-03cc76813e55/status]


    Consumers status: threads: 16, failure 0, messages committed: 48265478, elapsed: 3038.34s, throughput: 15885.49 msg/s
  

    scripts/import.sh -o import -t 16 -l import/Customers-us-west -r us-west -b / -a -bulk

Async Automation Execution Scheduled
  => status url:[http://127.0.0.1:8080/nuxeo/site/api/v1/automation/StreamImporter.runDocumentConsumers/@async/a993ecbe-7dfc-48d9-8aab-db18e307154b/status]

Consumers status: threads: 16, failure 0, messages committed: 47298176, elapsed: 2968.30s, throughput: 15934.43 msg/s


### Import Accounts and letters


Import the CSV for Accout letters:

    scripts/csvImport.sh -t 10 -p 16 -serverThreads 32 -b 50000 -o AccountProducers -f ~/bench/injector/data/letters/verified-accounts.csv -m -l import/Accounts


Throughput:42,815 csv line/s => 85,630 docs/s in kafka


    scripts/import.sh -o import -t 16 -l import/Accounts-us-east -r us-east -b / -a -bulk

Async Automation Execution Scheduled
  => status url:[http://127.0.0.1:8080/nuxeo/site/api/v1/automation/StreamImporter.runDocumentConsumers/@async/9deafdc7-fba8-42ab-a8de-cb86fdccd41a/status]


    Consumers status: threads: 16, failure 0, messages committed: 96531616, elapsed: 46720.91s, throughput: 2066.13 msg/s

    

    scripts/import.sh -o import -t 16 -l import/Accounts-us-west -r us-west -b / -a -bulk

Async Automation Execution Scheduled
  => status url:[http://127.0.0.1:8080/nuxeo/site/api/v1/automation/StreamImporter.runDocumentConsumers/@async/fe707eb8-2e59-432c-bc66-90254b8cd3c6/status]


    Consumers status: threads: 16, failure 0, messages committed: 94595770, elapsed: 49029.43s, throughput: 1929.37 msg/s

