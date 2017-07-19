# TiniBot2.0


## Installation

Create an application.conf file; You will find the example in the root directory.

### Bot Token
First, slot in your own `discord-bot.token`
You obtain it by adding an App via [Discord Developer Portal](https://discordapp.com/developers/applications/me).

* Choose "New App"
* Create and select a new App
* Click "Create Bot User". 

### Secret
Update your secret with your own password, or anyone will be able to kill your bot.

### kafka
Update the kafka configuration with your own parameters described below.



## Infrastrtucture

Get yourself a kafka cluster running. 

### Using docker: 

Start zookeeper: 
```
docker run -d \
    --net=host \
    --name=zookeeper \
    -e ZOOKEEPER_CLIENT_PORT=32181 \
    confluentinc/cp-zookeeper:3.2.1
```

Start kafka broker:
```
docker run -d \
    --net=host \
    --name=kafka \
    -e KAFKA_ZOOKEEPER_CONNECT=localhost:32181 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://your.ip.goes.here:port \
    confluentinc/cp-kafka:3.2.1
```

Create a topic: 
```
docker run \
    --net=host \
    --rm confluentinc/cp-kafka:3.2.1 \
    kafka-topics --create --topic tini-test1 --partitions 1 --replication-factor 1 --if-not-exists --zookeeper localhost:32181
```

If you need to check if a topic is present:
```
docker run \
    --net=host \
    --rm \
    confluentinc/cp-kafka:3.2.1 \
    kafka-topics --describe --topic tini-test1 --zookeeper localhost:32181
```

And, of course, if you need to start over:
```
docker rm -f $(docker ps -a -q)
```



## Commands

* `!ping`

