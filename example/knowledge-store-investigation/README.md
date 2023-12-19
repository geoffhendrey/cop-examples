Our goal in this example is to provide step-by-step insructions for creating a solution
that contains a knowledge Type definiton for a network security investigation. Once the `investigation` Type
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
In the manifest above note these lines which are the part that was added :
```json
    "types": [
        "types/investigation.json"
    ]
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
The knowledge type that has been created is just a skeleton. We are going to take that skeleton and customize 
it to our needs. Let's go field by field, and then create a simple bash script to make all the changes so 
you don't have to type them manually which can result in 'fat fingers'
* `name` - the name field is fine. It is set to 'investigation'. When we push this solution, it means that tenants
who subscribe to your solution (inlcuding you!) will be able to access a totally new knowledge type. You can 
think of  a type as similar to a table in a traditional database
* `allowedLayers` - The knowledge store allows `SOLUTION`, `TENANT`, and `USER` access. Objects placed in solution packages,
  are replicated to all Cells in the COP. Default knowledge layering policies make your SOLUTION objects visible to TENANT
  and USER principals in every cell. To illustrate how layering works, in this example we wish to include an object with
  suitable defaults for network intrusion investigations.  However, we want tenant admin to be able to
  customize investigation defaults which may have different requirements in different regions. For this
  reason we set `allowdLayers` to `TENANT` in the investigation type definitoin, `investigation.json`.
  However, we also want to allow USERs to create investigations within a tenant, and to see the tenant-specific
  defaults that are applicable to, for example European Union. For this reason we add USER to the `allowedLayers`
  ![Layering](https://raw.githubusercontent.com/geoffhendrey/cop-examples/main/assets/knowledge%20replication.png)
* `identifyingProperties` - When we store an actual investigation object, whether it is coming from a solution package
as we will do, or a user or UI via an API call, the Knowledge Store must have a way to uniquely identify the
object. `identifyingProperties` is a JSON Pointer that tells the system which field of an ingvestigation object
can be used to uniquely identify it. We will be changing the identifyingProperties from `["/name"]` to `["/name", "/caseId"]`
```json
    "identifyingProperties": [
        "/name", "caseId"
    ]
```
Let's briefly discuss how the 
Knowledge Store REST APIs leverage object _identity_. Consider that two solutions can both create a type called
investigation. This clash is resolved by using _fully qualified_ id's. For example, try this command to list solutions
in your cell:
```shell
fsoc solution list --verbose
```
you will see that one of the lines printed to the terminal is 
```
* Calling the observability platform API method=GET path=knowledge-store/v1/objects/extensibility:solution
```
This means the `fsoc` command is making a REST call to lisst all objects of with fully qualified type `extensibility:solution`
```html
https://<your-tenant-hostname>/knowledge-store/v1/objects/extensibility:solution
```
In the API call above, `extensibility:solution` is a fully qualifed type. We can infer that solution management
in the platform is implemented as a System Solution called `extensibility` that has defined a Type
called `solution`, in which it stores details about every solution that has been pushed to the platform.

Recall that the name of your solution is `$SOLUTION_PREFIX-example-ks-investigation`. This
means tht the fully qualifed name of your investigation Type is:
```
$SOLUTION_PREFIX-example-ks-investigation:investigation`
```
* `secureProperties` - To keep it simple we won't be storing any secrets like API keys in the investigation object
therefore we will remove this field.
* `jsonSchema` - this is where we need to define our type's json document structure. As you can
see from the JSON below, the json schema for the ingestigation contains numerous fields ranging from 
the identifying property (`caseId`) to `description`, `severity`, and `affectedSystems`

Let's put it all together now. Replace the content of `investigation.json` with this file
that brings together all the changes we discussed.
```json
{
  "name": "investigation",
  "allowedLayers": [
    "TENANT"
  ],
  "identifyingProperties": [
    "/name","/caseID"
  ],
  "jsonSchema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
      "name": {
        "type": "string"
      },
      "caseID": {
        "type": "string"
      },
      "investigators": {
        "type": "array",
        "items": {
          "type": "string"
        }
      },
      "startTime": {
        "type": "string",
        "format": "date-time"
      },
      "endTime": {
        "type": "string",
        "format": "date-time"
      },
      "description": {
        "type": "string"
      },
      "severity": {
        "type": "string",
        "enum": ["low", "medium", "high"]
      },
      "intrusionType": {
        "type": "string"
      },
      "affectedSystems": {
        "type": "array",
        "items": {
          "type": "string"
        }
      },
      "attackVectors": {
        "type": "array",
        "items": {
          "type": "string"
        }
      },
      "ipAddresses": {
        "type": "object",
        "properties": {
          "source": {
            "type": "string"
          },
          "target": {
            "type": "string"
          }
        }
      },
      "networkTrafficLogs": {
        "type": "string"
      },
      "incidentResponseActions": {
        "type": "string"
      },
      "evidenceAndArtifacts": {
        "type": "array",
        "items": {
          "type": "string"
        }
      },
      "recommendations": {
        "type": "string"
      },
      "status": {
        "type": "string",
        "enum": ["open", "closed", "in progress"]
      },
      "notes": {
        "type": "string"
      },
      "timestamps": {
        "type": "object",
        "properties": {
          "start": {
            "type": "string",
            "format": "date-time"
          },
          "end": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "reporting": {
        "type": "string"
      },
      "incidentClassification": {
        "type": "string"
      },
      "legalAndCompliance": {
        "type": "string"
      },
      "affectedUsers": {
        "type": "array",
        "items": {
          "type": "string"
        }
      },
      "evidencePreservation": {
        "type": "string"
      },
      "thirdPartyInvolvement": {
        "type": "string"
      },
      "remediationActions": {
        "type": "string"
      },
      "lessonsLearned": {
        "type": "string"
      }
    },
    "required": ["name", "caseID", "investigators", "startTime", "description", "status"]
  }
}


```
Now let's put together a fully qualified type name, with the `identifyingProperties` of an investigation to
understand what the REST URL for a particular investigation looks like.
```html
https://<your-tenant-hostname>/knowledge-store/v1/objects/$SOLUTION_PREFIX-example-ks-investigation:investigation/$SOLUTION_PREFIX-example-ks-investigation:name=
```



