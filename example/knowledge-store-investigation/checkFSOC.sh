#!/usr/bin/env bash

# Check if jq is installed
jq_path=$(which jq)

if [ -z "$jq_path" ]; then
  echo "jq is not found in your PATH. Please make sure it's installed and in your PATH."
  exit 1
fi

# Get the location of the fsoc binary
fsoc_path=$(which fsoc)

if [ -z "$fsoc_path" ]; then
  echo "fsoc is not found in your PATH. Please make sure it's installed and in your PATH."
  exit 1
fi

# Get the fsoc version
fsoc_version_json=$(fsoc version -o json)
fsoc_version=$(echo $fsoc_version_json | jq -r '.version')
fsoc_major=$(echo $fsoc_version_json | jq -r '.major')
fsoc_minor=$(echo $fsoc_version_json | jq -r '.minor')

# Error out if fsoc doesn't meet the minimum required version
fsoc_req_major=0
fsoc_req_minor=620
if (( fsoc_major < fsoc_req_major || (fsoc_major == fsoc_req_major && fsoc_minor < fsoc_req_minor) )); then
  echo "fsoc version $fsoc_version is not supported. Please install version $fsoc_req_major.$fsoc_req_minor.0 or higher from https://github/cisco-open/fsoc."
  exit 1
fi

echo "jq path: $jq_path"
echo "fsoc binary path: $fsoc_path"
echo "fsoc version: $fsoc_version"
echo "all required utilities are present"
