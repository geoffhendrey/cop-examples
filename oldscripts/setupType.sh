#!/bin/bash

source ./setSolutionPrefix.sh

# Check if SOLUTION_PREFIX is set
if [ -z "$SOLUTION_PREFIX" ]; then
  echo "Warning: SOLUTION_PREFIX environment variable is not set."
  exit 1
fi

# Define the expected folder name
expected_folder="knowledge-store-investigation"

# Verify the current directory
if [ "$(basename "$(pwd)")" != "$expected_folder" ]; then
  echo "Error: You are not in the '$expected_folder' folder."
  exit 1
fi

# Define the source file path (assuming it's in the current directory)
source_file="./investigation.json"

# Define the destination directory
destination_dir="${SOLUTION_PREFIX}malwareexample/types"

# Create the destination directory if it doesn't exist
mkdir -p "$destination_dir"

# Copy the local JSON file to the destination
cp "$source_file" "$destination_dir/investigation.json"

# Check if the copy was successful
if [ $? -eq 0 ]; then
  echo "File 'investigation.json' copied to '$destination_dir'."
else
  echo "Error: Failed to copy the file."
  exit 1
fi

# End of script
