Music Store
=====================

## A simple CRUD rest application with HTTP4S, Slick, Kafka and Elastic Search

## Requisites

* Scala 2.12.4
* Slick 3.2.2
* Http4s 0.18.2
* Circe 0.9.2
* Mysql/Postgres/Oracle (for Oracle - put ojdbc6.jar in lib directory)
* Kafka 1.0.0 (optional - enable it in application_{db_env}.conf)
* Elastic Search 6

## Install Mysql, Kafka and ElasticSearch on Docker

#### Install Mysql

    docker run -d --name MYSQL-music_store -p 3306:3306 -e MYSQL_ROOT_PASSWORD=music_store mysql

#### Install Postgres

    docker run -d --name POSTGRES-music_store -p 5432:5432 -e POSTGRES_USER=music_store -e POSTGRES_PASSWORD=music_store postgres

#### Configure Mysql

    docker exec -i MYSQL-music_store mysql -uroot -pmusic_store  << EOF
    CREATE USER 'music_store'@'%' IDENTIFIED BY 'music_store';
    CREATE DATABASE music_store DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
    GRANT ALL ON music_store.* TO 'music_store'@'%';
    CREATE USER 'music_store_jmeter'@'%' IDENTIFIED BY 'music_store_jmeter';
    CREATE DATABASE music_store_jmeter DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
    GRANT ALL ON music_store_jmeter.* TO 'music_store_jmeter'@'%';
    FLUSH PRIVILEGES;
EOF

#### Configure Postgres

    docker exec -i POSTGRES-music_store psql -U music_store -c "GRANT ALL PRIVILEGES ON DATABASE music_store TO music_store"

#### Install Elastic Search

    docker run -d --name ELASTIC-music_store -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:6.1.3

#### Install Kafka Development Environment and web interface at http://127.0.1.1:3030

    docker run -d --name KAFKA-music_store -p 2181:2181 -p 3030:3030 -p 8081:8081 -p 8082:8082 -p 8083:8083 -p 9092:9092 -e ADV_HOST=127.0.0.1 landoop/fast-data-dev

## Run integration test

    ./test.sh

## Build e deploy

    sbt dist

    ./bin/music-store  -Dconfig.resource=application_{db_env}.conf

    replace {db_env} with H2, MYSQL or ORACLE

## Check

    curl -v http://localhost:8080/admin/check

    returns 200 OK

## Run with sbt

    sbt -Dconfig.resource=application_{db_env}.conf run

## Create sql schema

    curl -v http://localhost:8080/rest/create_sql_schema

## Elastic Search mapping artist/album

```
curl -v -X PUT http://localhost:9200/music -H 'Content-Type: application/json; charset=utf-8' -d'
{
    "mappings": {
       "doc":{
          "properties":{
             "my_join_field": {
              "type": "join",
              "relations": {
                "artist": "album"
              }
            }
          }
       }
     }
}'
```

## Stress test - Jmeter

    ./test_jmeter.sh

### Operations

#### generate random artist list

    curl http://localhost:8080/rest/artist/random/3

#### generate random album list

    curl http://localhost:8080/rest/album/random/5

#### insert artist

```
curl -v -X PUT http://localhost:8080/rest/artist/00000000-0000-47d7-8c55-398030b2ab4f -H 'Content-Type: application/json; charset=utf-8' -d'
{
  "name" : "Iron Maiden",
  "genres":	["Heavy metal"],
  "origin":	"Leyton, London, England",
  "year": 1975,
  "members" : ["Steve Harris","Dave Murray","Adrian Smith","Bruce Dickinson","Nicko McBrain","Janick Gers"],
  "url" : "https://ironmaiden.com",
  "activity" : true,
  "description" : "Iron Maiden are an English heavy metal band"
}'
```

#### insert album

```
curl -v -X PUT http://localhost:8080/rest/album/00000000-0002-47d7-8c55-398030b2ab4f/00000000-0000-47d7-8c55-398030b2ab4f -H 'Content-Type: application/json; charset=utf-8' -d'
{
  "title" : "The Number of the Beast",
  "publishDate" : "1982-03-22",
  "duration" : 2351,
  "price" : 21.14,
  "tracks": ["Invaders","Children of the Damned","The Prisoner","22 Acacia Avenue","The Number of the Beast","Run to the Hills","Gangland","Hallowed Be Thy Name"],

  "quantity" : 100,
  "discount" : 0,
  "seller" : "seller1",
  "code" : "123"
}'
```

#### Search album by track

    curl 'http://localhost:8080/rest/album/track/invaders'

#### Search artist by name

    curl 'http://localhost:8080/rest/artist/name/iron'

#### Update artist

```
curl -v -X POST http://localhost:8080/rest/artist/00000000-0000-47d7-8c55-398030b2ab4f -H 'Content-Type: application/json; charset=utf-8' -d'
{
  "name" : "Iron Maiden",
  "genres":	["Heavy metal"],
  "origin":	"Leyton, London, England",
  "year": 1975,
  "members" : ["Steve Harris","Dave Murray","Adrian Smith","Bruce Dickinson","Nicko McBrain","Janick Gers"],
  "url" : "https://ironmaiden.com",
  "activity" : true,
  "description" : "Iron Maiden are an English heavy metal band formed in Leyton, East London, in 1975 by bassist and primary songwriter Steve Harris. The band s discography has grown to thirty-eight albums, including sixteen studio albums, twelve live albums, four EPs, and seven compilations."
}'
```

#### update an album

```
curl -v -X POST http://localhost:8080/rest/album/00000000-0002-47d7-8c55-398030b2ab4f/00000000-0000-47d7-8c55-398030b2ab4f -H 'Content-Type: application/json; charset=utf-8' -d'
{
  "title" : "The Number of the Beast",
  "publishDate" : "1982-03-22",
  "duration" : 2351,
  "price" : 21.14,
  "tracks": ["Invaders","Children of the Damned","The Prisoner","22 Acacia Avenue","The Number of the Beast","Run to the Hills","Gangland","Hallowed Be Thy Name"],

  "quantity" : 100,
  "discount" : 0,
  "seller" : "seller2",
  "code" : "999"
}'
```

#### Load an artist by id

    curl http://localhost:8080/rest/artist/00000000-0000-47d7-8c55-398030b2ab4f

#### Load an album by id

    curl http://localhost:8080/rest/album/00000000-0002-47d7-8c55-398030b2ab4f

#### Delete album

    curl -v -X DELETE http://localhost:8080/rest/album/00000000-0002-47d7-8c55-398030b2ab4f/00000000-0000-47d7-8c55-398030b2ab4f


#### Delete artist (and his albums)

    curl -v -X DELETE http://localhost:8080/rest/artist/00000000-0000-47d7-8c55-398030b2ab4f

