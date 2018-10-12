Music Store
=====================

## A simple CRUD rest application with HTTP4S, Slick, Kafka and Elastic Search

## Requisites

* Scala 2.12.7
* Slick 3.2.3
* Http4s 0.18.11
* Circe 0.10.0
* Mysql/Postgres/Oracle (for Oracle - put ojdbc6.jar in lib directory)
* Kafka 2.0.0 (optional - enable it in application_{db_env}.conf)
* Elastic Search 6

## Run Mysql/Postgres, Kafka (optional) and Elasticsearch on Docker

#### Run Mysql

    docker run -d --name MYSQL-music_store -p 3306:3306 -e MYSQL_ROOT_PASSWORD=music_store mysql

#### Configure Mysql

    docker exec -i MYSQL-music_store mysql -uroot -pmusic_store  << EOF
    CREATE USER 'music_store'@'%' IDENTIFIED BY 'music_store';
    CREATE DATABASE music_store DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
    GRANT ALL ON music_store.* TO 'music_store'@'%';
    CREATE USER 'music_store'@'%' IDENTIFIED BY 'music_store';
    CREATE DATABASE music_store DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
    GRANT ALL ON music_store.* TO 'music_store'@'%';
    FLUSH PRIVILEGES;
EOF

#### Run Postgres

    docker run -d --name POSTGRES-music_store -p 5432:5432 -e POSTGRES_USER=music_store -e POSTGRES_PASSWORD=music_store postgres

#### Run Elasticsearch

    docker run -d --name ELASTIC-music_store -p 9200:9200 -p 5601:5601 nshou/elasticsearch-kibana

#### Run Kafka Development Environment and web interface at http://127.0.1.1:3030

    docker run -d --name KAFKA-music_store -p 2181:2181 -p 3030:3030 -p 8081:8081 -p 8082:8082 -p 8083:8083 -p 9092:9092 -e ADV_HOST=127.0.0.1 landoop/fast-data-dev

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
                },
              "title" : { "type" : "text", "fields" : {"raw" : {"type":"keyword"} } },
              "publishDate": {
              "type":   "date",
              "format": "yyyy-MM-dd"
              }
            }
           }
        }
    }'
```

## Run integration test

    ./test.sh

## Build e deploy

    sbt dist

    ./bin/music-store  -Dconfig.resource=application_{db_env}.conf

    replace {db_env} with H2, MYSQL, ORACLE

## Check

    curl -v http://localhost:8080/admin/check

    returns 200 OK

## Run with sbt

    sbt -Dconfig.resource=application_{db_env}.conf run

## Create sql schema

    curl -v http://localhost:8080/rest/create_sql_schema


## Stress test - Jmeter

    ./test_jmeter.sh

### Operations

#### generate random artist list

    curl http://localhost:8080/rest/artist/random/3

#### generate random album list

    curl http://localhost:8080/rest/album/random/5

#### insert artist Iron Maiden

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

#### insert album Killers

```
curl -v -X PUT http://localhost:8080/rest/album/00000000-0001-47d7-8c55-398030b2ab4f/00000000-0000-47d7-8c55-398030b2ab4f -H 'Content-Type: application/json; charset=utf-8' -d'
{
  "title" : "Killers",
  "publishDate" : "1980-02-02",
  "length" : 2298,
  "price" : 20.22,
  "tracks": ["The Ides of March", "Wrathchild", "Murders in the Rue Morgue", "Another Life", "Genghis Khan", "Innocent Exile", "Killers", "Prodigal Son", "Purgatory", "Drifter"],
  "quantity" : 70,
  "discount" : 3,
  "seller" : "seller1",
  "code" : "122"
}'
```

#### insert album The Number of the Beast

```
curl -v -X PUT http://localhost:8080/rest/album/00000000-0002-47d7-8c55-398030b2ab4f/00000000-0000-47d7-8c55-398030b2ab4f -H 'Content-Type: application/json; charset=utf-8' -d'
{
  "title" : "The Number of the Beast",
  "publishDate" : "1982-03-22",
  "length" : 2351,
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
  "length" : 2351,
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

#### Get albums avg length by artist_id

    curl http://localhost:8080/rest/aggregations/artist/album/length/avg/00000000-0002-47d7-8c55-398030b2ab4f

#### Delete album

    curl -v -X DELETE http://localhost:8080/rest/album/00000000-0002-47d7-8c55-398030b2ab4f/00000000-0000-47d7-8c55-398030b2ab4f


#### Delete artist (and his albums)

    curl -v -X DELETE http://localhost:8080/rest/artist/00000000-0000-47d7-8c55-398030b2ab4f

#### Histogram by artist year

```
curl -v -X POST localhost:9200/music/_search?size=0 -H 'Content-Type: application/json; charset=utf-8' -d'
{

   "aggs":{
      "release":{
         "histogram":{
            "field":"year",
            "interval" :10
         }
      }
   }
}'
 ```

 #### Histogram by album publication year

 ```
 curl -v -X POST localhost:9200/music/_search?size=0 -H 'Content-Type: application/json; charset=utf-8' -d'
 {
    "aggs":{
       "events_last_week_histogram":{
          "date_histogram":{
          	 "min_doc_count": 0,
             "field":"publishDate",
             "format": "yyyy-MM-dd",
             "interval" :"year"
          }
       }
    }
 }'
 ```
