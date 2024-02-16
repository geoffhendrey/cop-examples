#!/usr/bin/env bash

# Get the username of the current user using `whoami`
username=$(whoami)

# Truncate SOLUTION_PREFIX to 9 characters (if it's longer) so that when combined with "extendedsolution" length is <= 25 chars
SOLUTION_PREFIX="${username:0:10}"

# Set the SOLUTION_PREFIX environment variable
export SOLUTION_PREFIX="$SOLUTION_PREFIX"

# Display a message to confirm the change
echo "SOLUTION_PREFIX set to: $SOLUTION_PREFIX"

