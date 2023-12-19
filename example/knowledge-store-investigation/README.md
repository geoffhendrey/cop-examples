Our goal in this example is to provide step-by-step insructions for creating a solution
that contains a knowledge Type definiton for a hypothetical security investigation. Once the `investigation` Type
is created and pushed to the platform, we will learn how we can include investigation objects
in solutions, as well as CRUD them with REST APIs.

From a `bash` prompt set a SOLUTION_PREFIX environment variable. This will allow you to 
use `fsoc` commands without overwriting the local folders of this example

```shell
SOLUTION_PREFIX=<your-initials-here>
```

use the fsoc command to initialize a solution in this folder:
```shell
fsoc solution init $SOLUTION_PREFIX-example-ks-investigation
```
`cd` into the solution folder that was just created
```shell
cd $SOLUTION_PREFIX-example-ks-investigation
```
A file called `manifest.json` was created.
```shell
cat manifest.json #dump the file to termimnal so you can read it
```

```shell
{
    "manifestVersion": "1.0.0",
    "name": "<your-prefix>-example-ks-investigation",
    "solutionVersion": "1.0.0",
    "dependencies": [],
    "description": "description of your solution",
    "contact": "the email for this solution's point of contact",
    "homepage": "the url for this solution's homepage",
    "gitRepoUrl": "the url for the git repo holding your solution",
    "readme": "the url for this solution's readme file"
}
```
As you can see this is essentially a blank manifest. We will now run an 'fsoc'
command to create a knowledge Type, and update the manifest
```shell
fsoc solution extend --add-knowledge=investigation
```
now `cat` your manifest and note that it has been augmented with a `types` array. This tells the knowledge store
that your solution contains a type, and where to find it.
```bash
cat manifest.json
```
```json
{
    "manifestVersion": "1.0.0",
    "name": "gh-example-ks-investigation",
    "solutionVersion": "1.0.0",
    "dependencies": [],
    "description": "description of your solution",
    "contact": "the email for this solution's point of contact",
    "homepage": "the url for this solution's homepage",
    "gitRepoUrl": "the url for the git repo holding your solution",
    "readme": "the url for this solution's readme file",
    "types": [
        "types/investigation.json"
    ]
}
```
change directory into the `types` folder
```shell
cd types
```
...and `cat` the investigations type definition
```shell
cat investigation.json
```
```json
{
    "name": "investigation",
    "allowedLayers": [
        "TENANT"
    ],
    "identifyingProperties": [
        "/name"
    ],
    "secureProperties": [
        "$.secret"
    ],
    "jsonSchema": {
        "$schema": "http://json-schema.org/draft-07/schema#",
        "additionalProperties": false,
        "description": "",
        "properties": {
            "name": {
                "description": "this is a sample attribute",
                "type": "string"
            },
            "secret": {
                "description": "this is a sample secret attribute",
                "type": "string"
            }
        },
        "required": [
            "name"
        ],
        "title": "investigation knowledge type",
        "type": "object"
    }
}
```
Now we can start to understand what a Type is. Foremost, a Type has a 'name' and as we would expect it is "investigation".
The type also has a `jsonSchema` to instruct the knowledge store on what kind of content it should
expect/enforce around investigations. Start by removing the property called 'secret'. We won't need
secrets for this example. Paste this entire bash script into your console. It will remove the `secret'
property from the json schema.

```shell
#!/bin/bash

# Specify the input JSON file (change as needed)
input_file="investigation.json"

# Create a temporary file
temp_file=$(mktemp)

# Apply jq operation to the temporary file
jq 'del(.jsonSchema.properties.secret) | del(.secureProperties)' "$input_file" > "$temp_file"

# Overwrite the original file with the contents of the temporary file
mv "$temp_file" "$input_file"

# Optionally, display a message to indicate success
echo "JSON file '$input_file' has been updated."

# End of script

```

now `cat` the investigation type again and confirm `secret` has been stripped out.
```bash
cat investigation.json
```
```json
{
  "name": "investigation",
  "allowedLayers": [
    "TENANT"
  ],
  "identifyingProperties": [
    "/name"
  ],
  "jsonSchema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "additionalProperties": false,
    "description": "",
    "properties": {
      "name": {
        "description": "this is a sample attribute",
        "type": "string"
      }
    },
    "required": [
      "name"
    ],
    "title": "investigation knowledge type",
    "type": "object"
  }
}
```

This is a minimal json schema. In fact it defines a document that can allow only one field, `name`
Before we beef up the schema to represent an `investigation` we need to understand `allowedLayers' and 
'identifyingProperties'.
* `allowedLayers` - The knowledge store allows `SOLUTION`, `TENANT`, and `USER` access. These are
called 'layers' because the knowledge store manages a hierarchy.