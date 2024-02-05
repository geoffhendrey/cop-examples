#!/usr/bin/env bash

source setSolutionEnvs.sh

fsoc solution validate --directory $DESTINATION_DIR_NAME --tag stable -v
