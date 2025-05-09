#!/bin/bash

set -e

TAG=$(git log --pretty=format:"%h" -n 1)
REPO=gcr.io/dynatrace-demoability/easytrade

skaffold deploy -d ${REPO} -t ${TAG} "$@"
