command=$1

if [ "$command" == "" ]
  then
    echo "your user must be in docker group.."
    printf "use:\n"
    printf "$0 start\n"
    printf "$0 run n_thread duration_seconds\n"
    printf "$0 stop\n"
fi

http_port=8282
docker_image_elastic="JMETER_ELASTIC"
docker_image_kafka="JMETER_KAFKA"

start_elastic(){
    docker rm -f $docker_image_elastic 2>/dev/null
    echo "start docker image $docker_image_elastic..."
    docker run -d --rm --name $docker_image_elastic -p 9233:9200 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:6.1.3
}

start_kafka(){
    docker rm -f $docker_image_kafka 2>/dev/null
    echo "start docker image $docker_image_kafka..."
    docker run -d --rm --name $docker_image_kafka -p 2181:2181 -p 3030:3030 -p 8081:8081 -p 8082:8082 -p 8083:8083 -p 9092:9092 -e ADV_HOST=127.0.0.1 landoop/fast-data-dev
}


create_sql_schema()
{
   COUNTER=0
   while [[  $COUNTER -lt 30 && status_code -ne "200" ]]; do
      printf "."
      let COUNTER=COUNTER+1
      sleep 5
      curl http://localhost:$http_port/rest/create_sql_schema >/dev/null 2>/dev/null
      status_code=`curl -s -o /dev/null -w "%{http_code}" http://localhost:$http_port/admin/check`
   done
}

start_app()
{
    printf "starting app"
    sbt -Dconfig.resource=application_JMETER_MYSQL.conf run >build.log &

    echo `echo $!` >.pid
    create_sql_schema

    status_code=`curl -s -o /dev/null -w "%{http_code}" http://localhost:$http_port/admin/check`

    if [ $status_code -ne "200" ]
     then
        echo "error"
        curl http://localhost:$http_port/admin/check
        exit 1
    fi
    echo "app started, next step $0 run n_thread duration_seconds"

}

run_jmeter()
{
    cd jmeter

    rm artist_request_url.log  album_request_url.log jmeter.log 2>/dev/null

    jmeter -n -t put_artist.jmx -Jgroup1threads=$n_thread -Jduration=$duration -Jhost=localhost -Jport=$http_port >artist.log
    jmeter -n -t put_album.jmx -Jgroup1threads=$n_thread -Jduration=$duration -Jhost=localhost -Jport=$http_port >album.log

    echo
    cat artist.log
    cat album.log
    echo
    echo "Dont't forget to stop ($0 stop)"
}

stop()
{

    pid=$(<.pid)
    pkill -P $pid 2>/dev/null
    docker rm -f $docker_image_elastic
    docker rm -f $docker_image_kafka
}

if [ "$command" == "stop" ]
  then
   stop
fi


if [ "$command" == "start" ]
  then
    start_elastic
    start_kafka
    sleep 4
    start_app
fi

if [ "$command" == "run" ]
  then
  export n_thread=$2
  export duration=$3
  if [ "$duration" == "" ]
    then
      echo "your user must be in docker group.."
      echo "use: $0 n_thread duration_seconds"
      exit 1
  fi
  run_jmeter
fi


exit 0

