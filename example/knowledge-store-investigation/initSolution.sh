#!/usr/bin/env bash

source ./setSolutionPrefix.sh

# Check if SOLUTION_PREFIX is set
if [ -z "$SOLUTION_PREFIX" ]; then
  echo "Warning: SOLUTION_PREFIX environment variable is not set."
  exit 1
fi

# Define the destination directory for the manifest
manifest_dir="$SOLUTION_PREFIX-example-ks-investigation"

# Create the destination directory if it doesn't exist
mkdir -p "$manifest_dir"

# Load the existing manifest JSON content
manifest_file="$manifest_dir/manifest.json"
if [ -f "$manifest_file" ]; then
  manifest_content=$(cat "$manifest_file")
else
  echo "Error: Manifest file '$manifest_file' not found."
  exit 1
fi

# Replace all occurrences of $SOLUTION_PREFIX with the actual value
manifest_content=$(echo "$manifest_content" | sed "s/\$SOLUTION_PREFIX/$SOLUTION_PREFIX/g")

# Write the updated manifest JSON content back to the file
echo "$manifest_content" > "$manifest_file"

# Check if the write was successful
if [ $? -eq 0 ]; then
  echo "Manifest file 'manifest.json' updated in '$manifest_dir'."
else
  echo "Error: Failed to update the manifest file."
fi

# End of script
