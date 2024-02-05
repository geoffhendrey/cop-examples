#!/bin/bash

source setSolutionEnvs.sh

# Check if the destination directory exists and ask for confirmation to overwrite
if [ -d "$DESTINATION_DIR_NAME" ]; then
    read -p "The directory '$DESTINATION_DIR_NAME' already exists. Do you want to overwrite it? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled."
        exit 1
    fi
    rm -rf "$DESTINATION_DIR_NAME"
fi

# Copy the files from source to destination
cp -r "$PACKAGE_SOURCE_DIR/" "$DESTINATION_DIR_NAME/"

# Iterate over all files in the destination directory and replace SOLUTION_PREFIX
while IFS= read -r -d '' file; do
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS requires an empty string as an argument to -i
        sed -i "" "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    else
        # Linux
        sed -i "s/SOLUTION_PREFIX/$SOLUTION_PREFIX/g" "$file"
    fi
done < <(find "$DESTINATION_DIR_NAME" -type f -print0)

echo "Replacement complete."
