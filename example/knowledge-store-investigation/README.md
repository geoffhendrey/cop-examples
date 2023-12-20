Our goal in this example is to provide step-by-step insructions for creating a solution
that contains a knowledge Type definiton for a network security investigation. Once the `investigation` Type
is created and pushed to the platform, we will learn how we can include investigation objects
in solutions, as well as CRUD them with REST APIs.

run the `initSolution.sh` script. It will copy `manifest.json` into a new solution folder prefixed with 
your username

```shell
initSolution.sh
```
You now have a solution manifest that looks like this, with `$SOLUTION_PREFIX` replaced by your username:
```json
{
  "manifestVersion": "1.0.0",
  "name": "$SOLUTION_PREFIX-example-ks-investigation",
  "solutionVersion": "1.0.0",
  "dependencies": [],
  "description": "network intrusion investigation",
  "contact": "-",
  "homepage": "-",
  "gitRepoUrl": "-",
  "readme": "-",
  "types": [
    "types/investigation.json"
  ],
  "objects":[
    {
      "type": "$SOLUTION_PREFIX-example-ks-investigation:investigation",
      "objectsFile": "objects/investigation.json"
    }
  ]
}
```
Now run `setupType.sh` to copy the type definition, `investigation.json` into the `types` folder of your new 
solution.
```shell
setupTypes.sh
```
Next run `setupObject.sh` to copy `malwareInvestigationDefaults.json" into the 
objects folder of your solution.
```shell
setupObject.sh
```
Let's look at the investigation Type definition. 

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
A Type declaration has these parts:
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
This means the `fsoc` command is making a REST call to list all objects of with fully qualified type `extensibility:solution`
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
* `jsonSchema` - this is where we need to define our type's json document structure. As you can
see from the JSON below, the json schema for the ingestigation contains numerous fields ranging from 
the identifying property (`caseId`) to `description`, `severity`, and `affectedSystems`

Let's now examine the  `malwareInvestigationDefaults.json` object. The first thing to note is that it complies
with the JSON schema for `investigation`

```json
{
  "name": "Malware Incident Report",
  "caseID": "MAL2023123456",
  "investigators": ["Security Team"],
  "startTime": "2023-12-15T10:00:00Z",
  "endTime": "2023-12-15T11:30:00Z",
  "description": "Investigation of a suspected malware infection on a workstation.",
  "severity": "high",
  "intrusionType": "Malware",
  "affectedSystems": ["Workstation-1"],
  "attackVectors": ["Email attachment", "Drive-by download"],
  "ipAddresses": {
    "source": "192.168.1.50",
    "target": "104.20.2.17"
  },
  "networkTrafficLogs": "Unusual network activity detected on the workstation.",
  "incidentResponseActions": "Isolated the workstation from the network, initiated malware scan.",
  "evidenceAndArtifacts": ["Malware executable", "Suspicious email"],
  "recommendations": "Implement email filtering and endpoint protection measures.",
  "status": "open",
  "notes": "The affected workstation has been isolated and is undergoing analysis.",
  "timestamps": {
    "start": "2023-12-15T10:00:00Z",
    "end": "2023-12-15T11:30:00Z"
  },
  "reporting": "Internal incident reporting",
  "incidentClassification": "Malware Infection",
  "legalAndCompliance": "Compliance with data protection regulations",
  "affectedUsers": ["User-C"],
  "evidencePreservation": "Evidence preserved according to security policy.",
  "thirdPartyInvolvement": "Consulted with cybersecurity experts.",
  "remediationActions": "Cleaned malware, implemented preventive measures.",
  "lessonsLearned": "Enhanced endpoint security measures."
}
```
The next step is to push your solution to the platform. This assumes
you already have familiarity with [fsoc](https://github.com/cisco-open/fsoc). Run the
`push.sh` script.
```shell
push.sh
```
The script uses the `fsoc` command like this:
```shell
fsoc solution push -d $SOLUTION_PREFIX-example-ks-investigation --wait --tag=dev
```





