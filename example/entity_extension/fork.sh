#!/bin/bash

source ./setSolutionPrefix.sh

# Check if SOLUTION_PREFIX is set
if [ -z "$SOLUTION_PREFIX" ]; then
    echo "Warning: SOLUTION_PREFIX environment variable is not set."
    exit 1
fi

# Define the source and destination directories
source_dir1="base"
source_dir2="extended"
source_dir3="test"
destination_dir1="${SOLUTION_PREFIX}base"
destination_dir2="${SOLUTION_PREFIX}extended"
destination_dir3="${SOLUTION_PREFIX}test"

# Verify the current directory
expected_folder="entity_extension"
if [ "$(basename "$(pwd)")" != "$expected_folder" ]; then
    echo "Error: You are not in the '$expected_folder' folder."
    exit 1
fi

# Check if the source directory exists
if [ ! -d "$source_dir1" ]; then
    echo "Error: Source directory '$source_dir1' does not exist."
    exit 1
fi
if [ ! -d "$source_dir2" ]; then
    echo "Error: Source directory '$source_dir2' does not exist."
    exit 1
fi
if [ ! -d "$source_dir3" ]; then
    echo "Error: Source directory '$source_dir3' does not exist."
    exit 1
fi

# Check if the destination directory exists and ask for confirmation to overwrite
if [ -d "$destination_dir1" ]; then
    read -p "The directory '$destination_dir1' already exists. Do you want to overwrite it? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled."
        exit 1
    fi
    rm -rf "$destination_dir1"
fi

if [ -d "$destination_dir2" ]; then
    read -p "The directory '$destination_dir2' already exists. Do you want to overwrite it? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled."
        exit 1
    fi
    rm -rf "$destination_dir2"
fi

if [ -d "$destination_dir3" ]; then
    read -p "The directory '$destination_dir3' already exists. Do you want to overwrite it? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled."
        exit 1
    fi
    rm -rf "$destination_dir3"
fi

# Copy the files from source to destination
cp -r "$source_dir1/" "$destination_dir1/"
cp -r "$source_dir2/" "$destination_dir2/"
cp -r "$source_dir3/" "$destination_dir3/"

# Iterate over all files in the destination directory and replace SOLUTION_PREFIX
while IFS= read -r -d '' file; do
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS requires an empty string as an argument to -i
        sed -i "" "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    else
        # Linux
        sed -i "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    fi
done < <(find "$destination_dir1" -type f -print0)

while IFS= read -r -d '' file; do
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS requires an empty string as an argument to -i
        sed -i "" "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    else
        # Linux
        sed -i "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    fi
done < <(find "$destination_dir2" -type f -print0)

while IFS= read -r -d '' file; do
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS requires an empty string as an argument to -i
        sed -i "" "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    else
        # Linux
        sed -i "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    fi
done < <(find "$destination_dir3" -type f -print0)

echo "Replacement complete."
