This is extension of knowledge-store-investigation. Please refer to ../knowledge-store-investigation/readme.md before starting. Steps to install this solution is similar to the above mentioned solution.

Our goal in this example is to provide working sample of solution with object referring to another object.

You will learn:
1. How to create a type with reference properties.
2. How to create objects with reference to another object.

The `package` folder contains the solution structure.
```text

├── README.md
├── checkFSOC.sh
├── fork.sh
├── package
│   ├── manifest.json
│   ├── objects
│   │   ├── defaultSystem.json
│   │   ├── malwareInvestigationDefaults.json
│   │   ├── permissions.json
│   │   └── role-to-permission-mappings.json
│   └── types
│       └── investigation.json
│       └── system.json
├── push.sh
├── setSolutionPrefix.sh
├── status.sh
└── validate.sh
```
Make sure all the script have executable permission by running this command in the `knowledge-store-prefetch`
folder.
```shell
chmod u+x *.sh
```

run the `checkFSOC.sh`script to verify you have a recent version of the COP CLI
```shell
./checkFSOC.sh
```

run the `fork.sh`script. It will copy the solution `package` folder into a new solution folder prefixed with
your username. There are also several file in the solution where your username will be injected.

```text
.
├── README.md
├── checkFSOC.sh
├── fork.sh
├── <USERNAME>malwareexample
│   ├── manifest.json
│   ├── objects
│   │   ├── defaultSystem.json
│   │   ├── malwareInvestigationDefaults.json
│   │   ├── permissions.json
│   │   └── role-to-permission-mappings.json
│   └── types
│       └── investigation.json
├── package
│   ├── manifest.json
│   ├── objects
│   │   ├── malwareInvestigationDefaults.json
│   │   ├── permissions.json
│   │   └── role-to-permission-mappings.json
│   └── types
│       ├── investigation.json
│       └── system.json
├── push.sh
├── setSolutionPrefix.sh
├── status.sh
└── validate.sh
```

Verify you have a folder whose name is `<your-username>malwareexample`
You now have a solution manifest file `<your-username>malwareexample/manifest.json` that looks like this, with `$SOLUTION_PREFIX` replaced by your username:
```json
{
  "manifestVersion": "1.1.0",
  "name": "SOLUTION_PREFIXmalwareexample",
  "solutionVersion": "1.0.1",
  "dependencies": ["iam"],
  "description": "network intrusion investigation",
  "contact": "-",
  "homepage": "-",
  "gitRepoUrl": "-",
  "readme": "-",
  "types": [
    "types/investigation.json",
    "types/system.json"
  ],
  "objects":[
    {
      "type": "SOLUTION_PREFIXmalwareexample:system",
      "objectsFile": "objects/defaultSystem.json"
    },
    {
      "type": "SOLUTION_PREFIXmalwareexample:investigation",
      "objectsFile": "objects/malwareInvestigationDefaults.json"
    },
    {
      "type": "iam:Permission",
      "objectsFile": "objects/permissions.json"
    },
    {
      "type": "iam:RoleToPermissionMapping",
      "objectsFile": "objects/role-to-permission-mappings.json"
    }
  ]
}
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
  "referenceProperties": {
    "/affectedSystems": {
      "types": ["SOLUTION_PREFIXmalwareexample:system"],
      "optional": false
    }
  },
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
In addition to the sections mentioned in the ../knowledge-store-investigation/readme.md this sample include referenceProperties field:
* `referenceProperties` - This indicates list of properties inside the json schema which can have reference to another field.
```json
"referenceProperties": {
    "/affectedSystems": {
      "types": ["SOLUTION_PREFIXmalwareexample:system"],
      "optional": false
    }
}
```
In above definition `affectedSystems` refers to the property under json schema that holds the reference to another object. You can note that in this case  affectedSystems is list of string which means it can hold list of references.
`types` inside the `referenceProperties` indicate the list of object types `affectedSystems` can refer to. One can also choose to use wildcards like `*` to indicate broader spectrum of object types that adhere to a pattern.
In the object definition of `malwareInvastigation` you can see the `affectedSystems` field contains a long string. This is what we call fqid
```json
{
  "name": "Malware Incident Report",
  "caseID": "Example:00000",
  "investigators": ["Security Team"],
  "startTime": "2023-12-15T10:00:00Z",
  "endTime": "2023-12-15T11:30:00Z",
  "description": "Investigation of a suspected malware infection on a workstation.",
  "severity": "high",
  "intrusionType": "Malware",
  "affectedSystems": ["SOLUTION_PREFIXmalwareexample:system/SOLUTION_PREFIXmalwareexample:/name=default;/machineId=AA-00-01-04-05-BB;layerId=SOLUTION_PREFIXmalwareexample;layerType=SOLUTION"],
  "attackVectors": ["Email attachment", "Drive-by download"],
  "ipAddresses": {
    "source": "192.168.1.50",
    "target": "104.20.2.17"
  }
  ...
}
```
fqid uniquely identifies an object and is of following format
`<<FQTN>>/<<ObjectId>>;layerId=<<layerId>>;layerType=<<layerType>>`
In above case
**FQTN** => SOLUTION_PREFIXmalwareexample:system
**ObjectId** => SOLUTION_PREFIXmalwareexample:/name=default;/machineId=AA-00-01-04-05-BB
**layerId** => SOLUTION_PREFIXmalwareexample
**layerType** => SOLUTION

<<To Be Continued>>