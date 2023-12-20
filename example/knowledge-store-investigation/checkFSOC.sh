#!/usr/bin/env bash

# Get the location of the fsoc binary
fsoc_path=$(which fsoc)

if [ -z "$fsoc_path" ]; then
  echo "fsoc is not found in your PATH. Please make sure it's installed and in your PATH."
  exit 1
fi

# Get the fsoc version
fsoc_version=$(fsoc version 2>&1 | awk '{print $3}')

# Check if the version is at least 0.57.0
if [[ "$fsoc_version" < "0.57.0" ]]; then
  echo "fsoc version $fsoc_version is not supported. Please install version 0.57.0 or higher."
  exit 1
fi

echo "fsoc binary path: $fsoc_path"
echo "fsoc version: $fsoc_version"
