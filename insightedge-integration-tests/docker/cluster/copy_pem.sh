#!/usr/bin/env bash

docker cp $(docker ps -q):/home/ie-user/ie-user.pem .
docker cp $(docker ps -q):/home/ie-user/ie-user.pem.pub .
