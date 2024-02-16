#!/usr/bin/env bash

source ./setSolutionPrefix.sh

# Check if SOLUTION_PREFIX is set
if [ -z "$SOLUTION_PREFIX" ]; then
  echo "Warning: SOLUTION_PREFIX environment variable is not set."
  exit 1
fi
fsoc solution validate --directory ${SOLUTION_PREFIX}awscreds --tag stable -v