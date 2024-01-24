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

# Define the destination directory
destination_dir="${SOLUTION_PREFIX}malwareexample/objects"

# Create the destination directory if it doesn't exist
mkdir -p "$destination_dir"

# Function to process and copy a JSON file
process_and_copy_json() {
    local source_file=$1
    local destination_file=$2

    if [ -f "$source_file" ]; then
        local content=$(cat "$source_file" | sed "s/\$SOLUTION_PREFIX/$SOLUTION_PREFIX/g")
        echo "$content" > "$destination_file"
        if [ $? -eq 0 ]; then
            echo "File '$source_file' copied to '$destination_file'."
        else
            echo "Error: Failed to copy the file '$source_file'."
            exit 1
        fi
    else
        echo "Error: Source file '$source_file' not found."
        exit 1
    fi
}

# Process permissions.json
process_and_copy_json "./permissions.json" "${destination_dir}/permissions.json"

# Process role-to-permission-mappings.json
process_and_copy_json "./role-to-permission-mappings.json" "${destination_dir}/role-to-permission-mappings.json"

echo "Successfully added permissions.json and role-to-permission-mappings.json to your solution"

# End of script
