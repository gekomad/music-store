#!/bin/bash

#sudo usermod -aG docker $USER
docker_image="IT-ELASTIC-music_store"
docker rm -f $docker_image 2>/dev/null
echo "start docker image $docker_image..."
docker run -d --rm --name $docker_image -p 9222:9200 -p 5601:5601 nshou/elasticsearch-kibana
if [ "$?" == "0" ]
 then
    echo "sbt test.."
    sbt test
    echo "stop docker image $docker_image..."
fi
docker rm -f $docker_image

