#!/bin/bash

command=$1

RED='\033[0;31m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

if [ "$command" == "" ]
  then
    printf "${RED}your user must be in docker group${NC}\n"
    printf "use:\n"
    printf "$0 start\n"
    printf "$0 run n_thread seconds\n"
    printf "$0 stop\n"
    exit 1
fi

http_port=8282
docker_image_elastic="JMETER_ELASTIC"
docker_image_kafka="JMETER_KAFKA"
docker_image_postgres="JMETER_POSTGRES"
elastic_port=9233
postgres_port=5432

check_http_code() {
	echo $(curl -s -o /dev/null -w "%{http_code}" $1)
}

ok() {
	 printf "${GREEN}ok${NC}\n"
}

start_elastic(){
   

    echo "starting docker image $docker_image_elastic..."
    docker run -d --rm --name $docker_image_elastic -p $elastic_port:9200 -p 5633:5601 nshou/elasticsearch-kibana

    COUNTER=0
    while [[  $COUNTER -lt 15 && $(check_http_code http://localhost:$elastic_port) -ne "200" ]]; do
          printf "."
          let COUNTER=COUNTER+1
          sleep 4
    done

    if [ $(check_http_code http://localhost:$elastic_port) -ne "200" ]
     then
        printf "${RED}ERROR!${NC}\n"
        exit 1
    fi

    printf "   ${BLUE}creating mapping...${NC}\n"
    curl -v -X PUT http://localhost:$elastic_port/music -H 'Content-Type: application/json; charset=utf-8' -d'
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
    }' >/dev/null 2>/dev/null
}

start_kafka(){
    echo "starting docker image $docker_image_kafka..."
    docker run -d --rm --name $docker_image_kafka -p 2181:2181 -p 3030:3030 -p 8081:8081 -p 8082:8082 -p 8083:8083 -p 9092:9092 -e ADV_HOST=127.0.0.1 landoop/fast-data-dev
}

start_postgres()
{
    echo "starting docker image $docker_image_postgres..."
    docker run -d --name $docker_image_postgres -p $postgres_port:5432 -e POSTGRES_USER=music_store -e POSTGRES_PASSWORD=music_store postgres
}

create_sql_schema()
{
   printf "${BLUE}creating sql schema...${NC}\n"
   COUNTER=0
   while [[  $COUNTER -lt 30 && $(check_http_code http://localhost:$http_port/admin/check) -ne "200" ]]; do
      printf "."
      let COUNTER=COUNTER+1
      sleep 5
      curl http://localhost:$http_port/rest/create_sql_schema >/dev/null 2>/dev/null
   done
   ok
}

start_app()
{
    sbt -Dconfig.resource=application_JMETER.conf run >music_store.log &

    echo `echo $!` >.pid
    create_sql_schema

    if [ $(check_http_code http://localhost:$http_port/admin/check) -ne "200" ]
     then
        printf "${RED}ERROR!${NC}\n"
        curl http://localhost:$http_port/admin/check
        exit 1
    fi

}

run_jmeter()
{
    cd jmeter

    dur=$((($seconds / 2)+1))

    rm artist_request_url.log  album_request_url.log jmeter.log 2>/dev/null

    jmeter -n -t put_artist.jmx -Jgroup1threads=$n_thread -Jduration=$dur -Jhost=localhost -Jport=$http_port >artist.log
    jmeter -n -t put_album.jmx -Jgroup1threads=$n_thread -Jduration=$dur -Jhost=localhost -Jport=$http_port >album.log

    echo
    cat artist.log
    cat album.log
    echo
    printf "${GREEN}Dont't forget to stop ($0 stop)${NC}\n"
}

stop()
{
    pid=$(<.pid)
    printf "${BLUE}stopping music_store...${NC}"
    pkill -P $pid 2>/dev/null
    ok
    printf "${BLUE}stopping elasticsearch...${NC}"
    docker rm -f $docker_image_elastic
    ok
    printf "${BLUE}stopping kafka...${NC}"
    docker rm -f $docker_image_kafka
    ok
    printf "${BLUE}stopping postgres...${NC}"
    docker rm -f $docker_image_postgres
    ok
}

if [ "$command" == "stop" ]
  then
   stop
fi


if [ "$command" == "start" ]
  then
    printf "${BLUE}starting postgres...${NC}"
    start_postgres
    ok
    printf "${BLUE}starting elasticsearch...${NC}"
    start_elastic
    ok
    printf "${BLUE}starting kafka...${NC}"
    start_kafka
    ok
    sleep 4
    printf "${BLUE}starting music_store...${NC}"
    start_app
    ok
    printf "Elastic search responds on port $elastic_port \n"
    printf "Postgres responds on port $postgres_port \n"
    printf "${GREEN}Next step $0 run n_thread seconds_seconds${NC}\n"
fi

if [ "$command" == "run" ]
  then
  export n_thread=$2
  export seconds=$3
  if [ "$seconds" == "" ]
    then
      printf "${BLUE}your user must be in docker group${NC}\n"
      echo "use: $0 n_thread seconds"
      exit 1
  fi
  run_jmeter
fi


exit 0

