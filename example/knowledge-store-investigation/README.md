Our goal in this example is to provide step-by-step insructions for creating a solution
that contains a knowledge Type definiton for a network/malware security investigation.

You will learn:
1. How a solution package is structured
2. How to define a new Type of knowledge
3. How to push your solution
4. How to query for your Type, and actual knowledge objects
5. How to apply access control to your knowledge model


The `package` folder contains the solution structure.
```text

├── README.md
├── checkFSOC.sh
├── fork.sh
├── package
│   ├── manifest.json
│   ├── objects
│   │   ├── malwareInvestigationDefaults.json
│   │   ├── permissions.json
│   │   └── role-to-permission-mappings.json
│   └── types
│       └── investigation.json
├── push.sh
├── setSolutionPrefix.sh
├── status.sh
└── validate.sh
```
Make sure all the script have executable permission by running this command in the `knowledge-store-investigation`
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
│       └── investigation.json
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
    "types/investigation.json"
  ],
  "objects":[
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
can be used to uniquely identify it. We will be changing the identifyingProperties from `["/name"]` to `["/name", "/caseID"]`
```json
    "identifyingProperties": [
        "/name", "/caseID"
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

Recall that the name of your solution is `$SOLUTION_PREFIXmalwareexample`. This
means tht the fully qualifed name of your investigation Type is:
```
$SOLUTION_PREFIXmalwareexample:investigation`
```
* `jsonSchema` - this is where we need to define our type's json document structure. As you can
see from the JSON below, the json schema for the ingestigation contains numerous fields ranging from 
the identifying property (`caseID`) to `description`, `severity`, and `affectedSystems`

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
Now run `validate.sh` to check your solution for errors:
```shell
./validate.sh
```

The next step is to push your solution to the platform. This assumes
you already have familiarity with [fsoc](https://github.com/cisco-open/fsoc). Run the
`push.sh` script.
```shell
./push.sh
```
The script uses the `fsoc` command like this:
```shell
GHENDREY-M-NWK4:ghendreymalwareexample ghendrey$ fsoc solution push --stable
Creating solution zip: "/var/folders/_h/gk53dw4j4vx9k0zjtpy_k3wm0000gn/T/ghendreymalwareexample3885208973.zip"
Deploying solution ghendreymalwareexample version 1.0.0 with tag stable
   • Current token is no longer valid; trying to refresh
Successfully uploaded solution ghendreymalwareexample version 1.0.0 with tag stable.
```
Subscribe to your solution
```shell
fsoc solution subscribe <USERNAME>malwareexample
```
Check the status of your subscription using the included `status.sh`:
```shell
GHENDREY-M-NWK4:knowledge-store-investigation ghendrey$ ./status.sh
SOLUTION_PREFIX set to: ghendrey
                       Solution Name: ghendreymalwareexample
        Solution Subscription Status: Subscribed
     Current Solution Upload Version: 1.0.1
   Current Solution Upload Timestamp: 2024-01-19T04:25:40.187Z
     Last Successful Install Version: 1.0.1
    Current Solution Install Version: 1.0.1
Current Solution Install Successful?: true
       Current Solution Install Time: 2024-01-19T20:54:14.415Z
    Current Solution Install Message:
```
Query the Type definition for investigation. Be sure to replace your username in the fsoc command below :
```shell
fsoc knowledge get-type --type "USERNAMEmalwareexample:investigation" 
```
```yaml
allowedLayers:
    - SOLUTION
    - TENANT
createdAt: "2024-01-19T20:54:14.757Z"
identifyingProperties:
    - /name
    - /caseID
jsonSchema:
    $schema: http://json-schema.org/draft-07/schema#
    properties:
        affectedSystems:
            items:
                type: string
            type: array
        affectedUsers:
            items:
                type: string
            type: array
        attackVectors:
            items:
                type: string
            type: array
        caseID:
            type: string
        description:
            type: string
        endTime:
            format: date-time
            type: string
        evidenceAndArtifacts:
            items:
                type: string
            type: array
        evidencePreservation:
            type: string
        incidentClassification:
            type: string
        incidentResponseActions:
            type: string
        intrusionType:
            type: string
        investigators:
            items:
                type: string
            type: array
        ipAddresses:
            properties:
                source:
                    type: string
                target:
                    type: string
            type: object
        legalAndCompliance:
            type: string
        lessonsLearned:
            type: string
        name:
            type: string
        networkTrafficLogs:
            type: string
        notes:
            type: string
        recommendations:
            type: string
        remediationActions:
            type: string
        reporting:
            type: string
        severity:
            enum:
                - low
                - medium
                - high
            type: string
        startTime:
            format: date-time
            type: string
        status:
            enum:
                - open
                - closed
                - in progress
            type: string
        thirdPartyInvolvement:
            type: string
        timestamps:
            properties:
                end:
                    format: date-time
                    type: string
                start:
                    format: date-time
                    type: string
            type: object
    required:
        - name
        - caseID
        - investigators
        - startTime
        - description
        - status
    type: object
name: investigation
solution: ghendreymalwareexample
updatedAt: "2024-01-19T20:54:14.757Z"
```
In addition to the type definition, an object included in `package/objects/malwareInvestigationDefaults.json` has been added 
has been inserted into the store. In fact, any objects included in your solution are added. As mentioned
earlier, solution objects are added to the knowledge store at the SOLUTION layer. Now query the knowledge store
to read back the object and verify it is in the store. In the fsoc command below, be sure to replace USERNAME
with your username.
```shell
fsoc  knowledge get --layer-type=SOLUTION  --type=USERNAMEmalwareexample:investigation  --layer-id=USERNAMEmalwareexample --object-id='USERNAMEmalwareexample:/name=Malware Incident Report;/caseID=Example:00000'
```
In the response, note that your object is contained in the `data` field
```YAML
createdAt: "2024-01-19T20:54:14.768Z"
data:
  affectedSystems:
    - Workstation-1
  affectedUsers:
    - User-C
  attackVectors:
    - Email attachment
    - Drive-by download
  caseID: Example:00000
  description: Investigation of a suspected malware infection on a workstation.
  endTime: "2023-12-15T11:30:00Z"
  evidenceAndArtifacts:
    - Malware executable
    - Suspicious email
  evidencePreservation: Evidence preserved according to security policy.
  incidentClassification: Malware Infection
  incidentResponseActions: Isolated the workstation from the network, initiated malware scan.
  intrusionType: Malware
  investigators:
    - Security Team
  ipAddresses:
    source: 192.168.1.50
    target: 104.20.2.17
  legalAndCompliance: Compliance with data protection regulations
  lessonsLearned: Enhanced endpoint security measures.
  name: Malware Incident Report
  networkTrafficLogs: Unusual network activity detected on the workstation.
  notes: The affected workstation has been isolated and is undergoing analysis.
  recommendations: Implement email filtering and endpoint protection measures.
  remediationActions: Cleaned malware, implemented preventive measures.
  reporting: Internal incident reporting
  severity: high
  startTime: "2023-12-15T10:00:00Z"
  status: open
  thirdPartyInvolvement: Consulted with cybersecurity experts.
  timestamps:
    end: "2023-12-15T11:30:00Z"
    start: "2023-12-15T10:00:00Z"
id: ghendreymalwareexample:/name=Malware Incident Report;/caseID=Example:00000
layerId: ghendreymalwareexample
layerType: SOLUTION
objectMimeType: application/json
objectType: ghendreymalwareexample:investigation
patch: null
targetObjectId: null

```
Note, if you want to query all existing investigations, lust leave off the `--objectID` (as usual, USERNAME is your username).
This query returns a page of objects.
```shell
fsoc  knowledge get --layer-type=SOLUTION  --type=USERNAMEmalwareexample:investigation  --layer-id=USERNAMEmalwareexample
```
The Cisco Observability Platform contains several built-in roles. This example solution grants some of those
roles with permission to access the malware investigation Type. The solution includes `permissions.json` and
`role-to-permission-mappings.json`. Note that after running the `fork.sh` script, SOLUTION_PREFIX has been replaced with your username from your local operating system.

Here is `permissions.json`
```json
  {
    "name": "readMalwareInvestigation",
    "displayName": "SOLUTION_PREFIXmalwareexample:readMalwareInvestigation",
    "description": "Read Malware Investigation",
    "actionAndResources": [
      {
        "action": {
          "classification": "READ"
        },
        "resource": {
          "type": "SOLUTION_PREFIXmalwareexample:investigation"
        }
      }
    ]
  }
```
Here is `role-to-permission-mappings.json`
```json
{
  "name": "malwareRoleMappings",
  "roles": [
    "iam:configManager",
    "iam:troubleshooter",
    "iam:observer"
  ],
  "permissions": [
    {
      "id": "SOLUTION_PREFIXmalwareexample:readMalwareInvestigation"
    }
  ]
}
```
If you looked closely at `manifest.json` you may have noticed the line `"dependencies": ["iam"]`. Dependency management is
a key aspect of the Cisco Observability Platform. In order to add the permission and role files, the manifest declares
a dependency on the system `iam` solution that defines Types `iam:Permission` and `iam:RoleToPermissionMapping`. This allows
the solution to add objects whose types are defined in other solutions.

Next steps: If you want to modify your solution, remember to bump the `solutionVersion` in `manifest.json`. 

Congratulations! You now understand the basics of knowledge modeling. To Review, you leared:
1. How a solution package is structured
2. How to define a new Type of knowledge
3. How to push your solution
4. How to query for your Type, and actual knowledge objects
5. How to apply access control to your knowledge model