#!/bin/bash

source setSolutionEnvs.sh

cd $DESTINATION_DIR_NAME
fsoc melt model
fsoc melt send < ${DESTINATION_DIR_NAME}*.yaml
cd ../