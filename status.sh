#!/bin/bash

source setSolutionEnvs.sh

# Check if SOLUTION_PREFIX is set
if [ -z "$SOLUTION_PREFIX" ]; then
  echo "Warning: SOLUTION_PREFIX environment variable is not set."
  exit 1
fi

# Define the source directory
PACKAGE_SOURCE_DIR="package"

# Read the package name from package/manifest.json
package_name=$(jq -r '.name' "$PACKAGE_SOURCE_DIR/manifest.json")

# Remove the SOLUTION_PREFIX from the package name to use as the destination directory suffix
destination_suffix="${package_name#SOLUTION_PREFIX}"

# Define the destination directory
DESTINATION_DIR_NAME="${SOLUTION_PREFIX}${destination_suffix}"

fsoc solution status $DESTINATION_DIR_NAME