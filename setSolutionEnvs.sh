#!/usr/bin/env bash

# Function to list directories in the ./example folder and allow the user to choose one
choose_example_directory() {
  echo "Listing available examples..."
  select dir in ./example/*/; do
    if [ -n "$dir" ]; then
      echo "You selected $(basename "$dir")"
      export EXAMPLE_HOME="$dir"
      break
    else
      echo "Invalid selection. Please try again."
    fi
  done
}

# Get the username of the current user using `whoami`
username=$(whoami)

# Truncate SOLUTION_PREFIX to 11 characters (if it's longer) so that when combined with "MalwareExample" length is <= 25 chars
SOLUTION_PREFIX="${username:0:11}" # Adjusted index to ensure the prefix is up to 11 characters long

# Set the SOLUTION_PREFIX environment variable
export SOLUTION_PREFIX="$SOLUTION_PREFIX"

# Check if SOLUTION_PREFIX is set
if [ -z "$SOLUTION_PREFIX" ]; then
  echo "Warning: could not set SOLUTION_PREFIX environment variable."
  exit 1
fi

# Display a message to confirm the change
echo "SOLUTION_PREFIX set to: $SOLUTION_PREFIX"

# If EXAMPLE_HOME is set, use it to find the manifest.json; otherwise, use the current directory

MANIFEST_PATH="${EXAMPLE_HOME:-.}/package/manifest.json"

echo "Looking for manifest.json in $MANIFEST_PATH"
if [ ! -f "$MANIFEST_PATH" ]; then
  echo "manifest.json not found in $MANIFEST_PATH"
  if [ -d "./example" ]; then
    echo "please select an example to use:"
    choose_example_directory
  else
    echo "Error: ./example directory does not exist. Cannot proceed."
    exit 1
  fi
fi
MANIFEST_PATH="${EXAMPLE_HOME:-.}/package/manifest.json"


# Verify the manifest.json file exists
if [ ! -f "$MANIFEST_PATH" ]; then
  echo "Error: manifest.json not found in the selected directory."
  exit 1
fi

# Define the source directory based on EXAMPLE_HOME if set
PACKAGE_SOURCE_DIR="${EXAMPLE_HOME:-.}/package"

# Read the package name from manifest.json
package_name=$(jq -r '.name' "$MANIFEST_PATH")

# Remove the SOLUTION_PREFIX from the package name to use as the destination directory suffix
destination_suffix="${package_name#SOLUTION_PREFIX}"

# Define the destination directory
export DESTINATION_DIR_NAME="${SOLUTION_PREFIX}${destination_suffix}"

if [ -z "$DESTINATION_DIR_NAME" ]; then
  echo "Warning: could not set DESTINATION_DIR_NAME environment variable."
  exit 1
fi

echo "Configuration complete. DESTINATION_DIR_NAME set to: $DESTINATION_DIR_NAME"
