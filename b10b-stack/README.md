# b10b-stack

## About
This is a docker compose to run a full dev instance.

## Setup

1. Create a `.env` file in this directory with the following content:
```
NUXEO_CLID=<YOUR NUXEO CLID IN ONE LINE>
```

2. Build your nuxeo image:

   Go to your nuxeo source then:
   ```
   cd docker; mvn -nsu install
   ```
   This will build a `nuxeo/nuxeo:latest` image.
   

3. Make sure you have build the B10b package in `../package`
    you should have a zip package like: `../package/target/10B-benchmark-package-11.2.32-SNAPSHOT.zip`
    
    Make sure this version is referenced in the `docker-compose.yml` 

## Start / Stop

Make sure you don't use the Nuxeo VPN or package will not be downloaded

```
docker-compose up
```

To stop:
```
docker-compose down --volume
```

## Usage

Nuxeo: http://nuxeo.docker.localhost/nuxeo/

KafkaHQ: http://kafkahq.docker.localhost/
Grafana: http://grafana.docker.localhost/  (admin/admin)
Jaeger: http://jaeger.docker.localhost/
Elastic: http://elastic.docker.localhost/


A bash?
```
docker exec -it nuxeo bash
```

The server log?
```
docker logs -f nuxeo
```

How to debug from my IDE?
```
$ ./bin/debug-nuxeo.sh 
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' nuxeo
CONTAINER_IP=172.31.0.10
docker run --rm --net host alpine/socat TCP-LISTEN:8787,fork TCP-CONNECT:172.31.0.10:8787

# then attach localhost:8787 in your IDE
```

Thread dump?
```
./bin/threaddump.sh
```

Flight record?
```
./bin/flight-record.sh
```

Flame graph?
```
./bin/profiler.sh
```

Mongo client?
```
./bin/mongo.sh
```


Visit https://github.com/bdelbosc/nuxeo-stacks for more information,
especially if you use a MacOS.

