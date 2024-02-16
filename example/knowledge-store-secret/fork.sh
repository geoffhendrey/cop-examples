#!/bin/bash

source ./setSolutionPrefix.sh

# Check if SOLUTION_PREFIX is set
if [ -z "$SOLUTION_PREFIX" ]; then
    echo "Warning: SOLUTION_PREFIX environment variable is not set."
    exit 1
fi

# Define the source and destination directories
source_dir="package"
destination_dir="${SOLUTION_PREFIX}awscreds"

# Verify the current directory
expected_folder="knowledge-store-secret"
if [ "$(basename "$(pwd)")" != "$expected_folder" ]; then
    echo "Error: You are not in the '$expected_folder' folder."
    exit 1
fi

# Check if the source directory exists
if [ ! -d "$source_dir" ]; then
    echo "Error: Source directory '$source_dir' does not exist."
    exit 1
fi

# Check if the destination directory exists and ask for confirmation to overwrite
if [ -d "$destination_dir" ]; then
    read -p "The directory '$destination_dir' already exists. Do you want to overwrite it? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled."
        exit 1
    fi
    rm -rf "$destination_dir"
fi

# Copy the files from source to destination
cp -r "$source_dir/" "$destination_dir/"

# Iterate over all files in the destination directory and replace SOLUTION_PREFIX
while IFS= read -r -d '' file; do
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS requires an empty string as an argument to -i
        sed -i "" "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    else
        # Linux
        sed -i "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    fi
done < <(find "$destination_dir" -type f -print0)

echo "Replacement complete."
