# Railway Service Variables

Railway 프로젝트 안에서 서비스 이름은 아래처럼 소문자로 맞추세요.

## mariadb

Docker image: `mariadb:11.4`

Volume mount path: `/var/lib/mysql`

```env
MARIADB_ROOT_PASSWORD=replace_with_openssl_rand_hex_32
MARIADB_DATABASE=finvibe
MARIADB_USER=finvibe
MARIADB_PASSWORD=replace_with_openssl_rand_hex_32
TZ=Asia/Seoul
```

## mongodb

Docker image: `mongo:8.0`

Volume mount path: `/data/db`

```env
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=replace_with_openssl_rand_hex_32
TZ=Asia/Seoul
```

## redis

Docker image: `redis:7.4-alpine`

Start command:

```bash
redis-server --appendonly yes
```

Volume mount path: `/data`

## kafka

Docker image: `apache/kafka:4.2.0`

Volume mount path: `/var/lib/kafka/data`

```env
KAFKA_NODE_ID=1
KAFKA_PROCESS_ROLES=broker,controller
KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka.railway.internal:9092
KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER
KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1
KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0
KAFKA_NUM_PARTITIONS=3
```
