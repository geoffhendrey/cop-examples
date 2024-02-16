Our goal in this example is to provide step-by-step instructions for creating a Codex workflow.

You will learn how to:

1. Create a Codex workflow
1. Create tests for the workflow
1. Push the solution to the platform
1. Subscribe to the solution
1. Test the solution by using Query Builder

# Create a Codex workflow
We'll create a workflow designed to count health rule violations for Kubernetes workloads and APM services. The logic of the workflow can be broken down into several steps:
1. Subscribe to the trigger event 
1. Validate event type and entity relevance 
1. Publish a measurement event


## 1. Subscribe to the trigger event

### Find the trigger event

Let’s query our trigger using [fsoc](https://github.com/cisco-open/fsoc):
```shell
fsoc knowledge get --type=contracts:cloudevent --object-id=platform:event.enriched.v1 --layer-type=TENANT
```
Output:
```yaml
type: event.enriched.v1
description: Indicates that an event was enriched with topology tags
dataschema: contracts:jsonSchema/platform:event.v1
category: data:trigger
extensions:
- contracts:cloudeventExtension/platform:entitytypes
- contracts:cloudeventExtension/platform:source
```

### Subscribe to the event

To subscribe to this event, you need to add an event definition and event state referencing this definition (note a nature of the reference to the event – it must be qualified with its knowledge type):
```yaml
events:
- name: EventReceived
  type: contracts:cloudevent/platform:event.enriched.v1
  kind: consumed
  dataOnly: false
  source: platform
states:
- name: event-received
  type: event
  onEvents:
   - eventRefs:
      - EventReceived
```
### Inspect the event

Since the data in workflows is received in JSON format, event data is described in JSON schema.\
Let’s look at the JSON schema of this event (referenced in dataschema), so you know what to expect in our workflow:
```shell
fsoc knowledge get --type=contracts:jsonSchema --object-id=platform:event.v1 --layer-type=TENANT
```

Result:
```yaml
$schema: http://json-schema.org/draft-07/schema#
title: Event
$id: event.v1
type: object
required:
   - entities
   - type
   - timestamp
properties:
   entities:
       type: array 
       minItems: 1 
       items: 
         $ref: '#/definitions/EntityReference' 
   type:
       $ref: '#/definitions/TypeReference' 
   timestamp:
       type: integer 
       description: The timestamp in milliseconds 
   spanId:
       type: string 
       description: Span id 
   traceId:
       type: string 
       description: Trace id 
   raw:
       type: string 
       description: The raw body of the event record 
   attributes:
       $ref: '#/definitions/Attributes' 
   tags:
       $ref: '#/definitions/Tags' 
additionalProperties: false
definitions:
   Tags:
       type: object 
       propertyNames: 
         minLength: 1 
         maxLength: 256 
       additionalProperties: 
         type: string 
   Attributes:
       type: object 
       propertyNames: 
         minLength: 1 
         maxLength: 256 
       additionalProperties: 
         type: 
           - string 
           - number 
           - boolean 
           - object 
           - array 
   EntityReference:
       type: object 
       required: 
         - id
         - type 
       properties: 
         id: 
           type: string 
         type: 
           $ref: '#/definitions/TypeReference' 
         additionalProperties: false 
   TypeReference:
       type: string 
       description: A fully qualified FMM type reference 
       example: k8s:pod
```

It’s straightforward – a single event, with one or more entity references. Since dataOnly=false, the payload of the event will be enclosed in the data field, and extension attributes will also be available to the workflow.\
Since we know the exact FMM event type we are interested in, you can also query its definition to understand the attributes that the workflow will be receiving and their semantics:
```shell
fsoc knowledge get --type=fmm:event --filter="data.name eq \"healthrule.violation\" and data.namespace.name eq \"alerting\"" --layer-type=TENANT
```

## 2. Validate event relevance

You need to ensure that the event you receive is of the correct FMM event type, and that referenced entities are relevant. To do this, you can write an expression in JSONata and then use it in an action condition:
```yaml
functions:
- name: checkType
  type: expression
  operation: |-
  data.type = 'alerting:healthrule.violation' and (
        'k8s:deployment' in data.entities.type or  
        'k8s:statefulset' in data.entities.type or  
        'k8s:daemonset' in data.entities.type or  
        'k8s:cronjob' in data.entities.type or  
        'k8s:managed_job' in data.entities.type or 
        'apm:service' in data.entities.type 
  )
states:
- name: event-received
  type: event
  onEvents:
   - eventRefs:
      - EventReceived
     actions:
      - name: createMeasurement
        condition: ${ fn:checkType }
```

## 3. Create and publish an event

Let’s find the measurement observation event that you need to publish:
```shell
fsoc knowledge get --type=contracts:cloudevent --object-id=platform:measurement.received.v1 --layer-type=TENANT
```

Output:
```yaml
type: measurement.received.v1
description: Indicates that measurements were received. Measurements are then aggregated into a metric.
dataschema: contracts:jsonSchema/platform:measurement.v1
category: data:observation
extensions:
- contracts:cloudeventExtension/platform:source
```

Now let’s look at the measurement schema, so you know how to produce a measurement event:
```shell
fsoc knowledge get --type=contracts:jsonSchema --object-id=platform:measurement.v1 --layer-type=TENANT
```

Output:
```yaml
$schema: http://json-schema.org/draft-07/schema#
title: Measurements for a specific metric
$id: measurement.v1
type: object
required:
- entity
- type
- measurements
properties:
  entity:
    $ref: '#/definitions/EntityReference' 
  type:
    $ref: '#/definitions/TypeReference' 
  attributes:
    $ref: '#/definitions/Attributes' 
measurements:
    type: array 
    minItems: 1 
    description: Measurement values with timestamp to be used for metric computation 
    items: 
      type: object 
      required: 
        - timestamp 
      oneOf: 
        - required: 
            - intValue 
        - required: 
            - doubleValue 
      properties: 
        timestamp: 
          type: integer 
          description: The timestamp in milliseconds 
        intValue: 
          type: integer 
          description: Long value to be used for metric computation. 
        doubleValue: 
          type: number 
          description: Double Measurement value to be used for metric computation. 
      additionalProperties: false 
additionalProperties: false
definitions:
  Attributes:
    type: object 
    propertyNames: 
      minLength: 1 
      maxLength: 256 
    additionalProperties: 
      type: 
        - string 
        - number 
        - boolean 
  EntityReference:
    type: object 
    required: 
      - id 
      - type 
    properties: 
      id: 
        type: string 
      type: 
        $ref: '#/definitions/TypeReference' 
      additionalProperties: false 
  TypeReference:
    type: string 
    description: A fully qualified FMM type name 
    example: k8s:pod 
```

### Create a measurement

Let's create another expression that takes the input event and generates a measurement as per the above schema, and use it in an action in the event state:

```yaml
- name: createMeasurement
  type: expression
  operation: |-
    {
        'entity': data.entities[0],  
        'type': 'sampleworkflow:healthrule.violation.count', 
        'attributes': {
            'violation_severity': data.attributes.violation_severity 
        }, 
        'measurements': [
            { 
                'timestamp': data.timestamp,  
                'intValue': $exists(data.attributes.'event_details.condition_details.violation_count')? data.attributes.'event_details.condition_details.violation_count': 1 
            } 
        ] 
    }
states:
- name: event-received
  type: event
  onEvents:
   - eventRefs:
      - EventReceived
     actions:
      - name: createMeasurement
        condition: '${ fn:checkType }'
        functionRef: createMeasurement
        actionDataFilter:
        toStateData: '${ measurement }'
```

Here we are preserving the violation_severity attribute from the original event and associating the measurement with the same entity. 

The state execution will result in a measurement field created by createMeasurement action, but only if the event was interesting based on the condition.

Note that since we are using a new FMM metric type - sampleworkflow:healthrule.violation.count - we need to register it via the extension on the target entity types. See `objects/fmm` folder for details.

### Publish an event

The next step is to check if the measurement was indeed created, and produce an event if it was. To do that, we will use a switch state:

```yaml
states:
- name: event-received
  type: event
  onEvents:
   - eventRefs:
      - EventReceived
     actions:
      - name: createMeasurement
        condition: ${ fn:checkType }
        functionRef: createMeasurement
        actionDataFilter:
          toStateData: ${ measurement }
  transition: check-measurement
- name: check-measurement
  type: switch
  dataConditions:
   - condition: ${ measurement != null }
     end:
        terminate: true
        produceEvents:
         - eventRef: CreateMeasurement 
           data: ${ measurement } 
  defaultCondition:
    end: true
```

That’s it!

## Test your solution

1. install and configure fsoc: https://github.com/cisco-open/fsoc
2. login (the identity that fsoc is configured with needs to have sufficient privileges for the below solution operations)
    ```shell
   fsoc login
   ```
3. rename the solution in manifest.json
4. check and validate the solution
   ```shell
   fsoc solution validate --tag=stable
   ```
5. run local tests (see `tests/readme.md`)
6. push the solution
   ```shell
   fsoc solution push --tag=stable
   ```
7. subscribe to this solution
   ```shell
   fsoc solution subscribe sampleworkflow
   ```
8. check that the metric type was created successfully by navigating to **Schema Browser**:
   ```
   https://{your tenant}.observe.appdynamics.com/ui/cco/tools/melt/schema
   ``` 
   and searching for sampleworkflow
9. check that metric is being populated by navigating to **Query Builder**:
   ```
   https://{your tenant}.observe.appdynamics.com/ui/cco/tools/melt/query
   ``` 
   and pasting the following UQL query:
   ```
   SINCE now - 1h FETCH metrics('sampleworkflow:healthrule.violation.count') {timestamp, value} FROM entities(k8s:workload, apm:service)
   ```
   Note that you need to have either k8s or apm monitoring enabled, and at least one health rule violation needs to occur since solution subscription
10. Alternatively, view the metrics by navigating to the metric explorer at *https://<your tenant>.observe.appdynamics.com/explore/cco/metric-explorer*