#!/bin/bash

set -e

TAG=$(git log --pretty=format:"%h" -n 1)
REPO=gcr.io/dynatrace-demoability/easytrade

skaffold build -d ${REPO} -t ${TAG} "$@"
