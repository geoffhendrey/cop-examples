#!/usr/bin/env bash

# Get the username of the current user using `whoami`
username=$(whoami)

# Truncate SOLUTION_PREFIX to 11 characters (if it's longer) so that when combined with "MalwareExample" length is <= 25 chars
SOLUTION_PREFIX="${username:0:12}"

# Set the SOLUTION_PREFIX environment variable
export SOLUTION_PREFIX="$SOLUTION_PREFIX"

# Check if SOLUTION_PREFIX is set
if [ -z "$SOLUTION_PREFIX" ]; then
  echo "Warning: could not set SOLUTION_PREFIX environment variable."
  exit 1
fi

# Display a message to confirm the change
echo "SOLUTION_PREFIX set to: $SOLUTION_PREFIX"

# Verify the current directory
# The script now dynamically checks if the current directory matches the expected folder name from manifest.json
test -d ./package || { echo "Error: package folder does not exist, check your directory"; exit 1; }
test -f ./package/manifest.json || { echo "Error: package folder with the solution source and manifest.json should exist"; exit 1; }

# Define the source directory
PACKAGE_SOURCE_DIR="package"

# Read the package name from package/manifest.json
package_name=$(jq -r '.name' "$PACKAGE_SOURCE_DIR/manifest.json")

# Remove the SOLUTION_PREFIX from the package name to use as the destination directory suffix
destination_suffix="${package_name#SOLUTION_PREFIX}"

# Define the destination directory
export DESTINATION_DIR_NAME="${SOLUTION_PREFIX}${destination_suffix}"

if [ -z "$DESTINATION_DIR_NAME" ]; then
  echo "Warning: could not set DESTINATION_DIR_NAME environment variable"
  exit 1
fi
