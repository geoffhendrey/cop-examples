#!/bin/bash

source ./setSolutionPrefix.sh

# Check if SOLUTION_PREFIX is set
if [ -z "$SOLUTION_PREFIX" ]; then
  echo "Warning: SOLUTION_PREFIX environment variable is not set."
  exit 1
fi
fsoc solution push --directory ${SOLUTION_PREFIX}awscreds --subscribe --wait --tag=stable