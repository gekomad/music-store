#!/bin/bash

RED='\033[0;31m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

printf "${RED}your user must be in docker group${NC} ${BLUE}(sudo usermod -aG docker $USER)${NC}\n"
echo "to run test: 'sudo usermod -aG docker $USER' logout and login"
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

