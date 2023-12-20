#!/usr/bin/env bash

# Get the username of the current user using `whoami`
username=$(whoami)

# Set the SOLUTION_PREFIX environment variable
export SOLUTION_PREFIX="$username"

# Display a message to confirm the change
echo "SOLUTION_PREFIX set to: $SOLUTION_PREFIX"
