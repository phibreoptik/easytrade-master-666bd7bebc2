#/bin/bash

docker container prune -f
docker image prune -a -f

skaffold build -d gcr.io/dynatrace-demoability/easytrade -t latest
