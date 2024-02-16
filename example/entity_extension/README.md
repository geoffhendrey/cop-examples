# Entity Extension Solution

This example demonstrates how existing entities that model a particular domain can be extended without changing the original domain model. Each new domain that wants to add their perspective in terms of additional attributes and/or metrics can do so without the knowledge of the original solution developer. This is one of the many aspects of extensibility offered by COP. Each additional extension makes the existing entities further valuable to customers, showcasing the power of the platform ecosystem.

In this example, we'll define a base solution with a single base_entity with two attributes and a metric. Another solution will extend the base_entity to add an additional attribute and a metric.

You will learn:

1. How to extend an existing solution
2. How to add attributes and metrics as extensions to an existing entity
3. Push the solution to the platform
4. Subscribe to the solution
5. Test the solution using fsoc melt tool that can generate test load

## Solution file structure

The `entity_extension` folder contains the solution structure.
```text

.
├── README.md
|── base
|   ├── manifest.json
|   └── objects
|       └── model
|           ├── entities 
|           |    └── base_entity.json
|           |   
|           ├── metrics
|           |   └── base_metric.json
|           |   
|           ├── namespaces
|           |   └── base.json
|           |   
|           └── resource-mappings
|               └── base_entity-resourceMapping.json
├── extended
|   ├── manifest.json
|   └── objects
|       └── model
|           ├── extensions 
|           |    └── extensions.json
|           |   
|           ├── metrics
|           |   └── extended_metric.json
|           |   
|           └── namespaces
|               └── extendedsolution.json
└── test       
    └── test-config.yaml

```
## Entity Model

The base solution defines a single entity called `base_entity` with attributes `name` and `base_attribute`.

```json
      "attributes": {
            "name": {
                "type": "string",
            },
            "base_attribute" :{
                "type": "long",
            }
        }   
```

```json
    "metricTypes": [
        "base_metric"
    ]
```
The extended solution extends the `base_entity` to add an attribute `extended_attribute` and a metric `extended_metric`.

## Preparing Solution Package

1. Make sure all the script have executable permission by running this command in the `entity_extension` folder.
```shell
chmod u+x *.sh
```

2. Run the `fork.sh`script. It will copy the solution folders for `base` and `extended` into new solution folders prefixed with your username. The original files cloned from Github will remain unchanged.


## Publishing Solution Package


3. Run the `checkFSOC.sh`script to verify you have a recent version of the COP CLI (https://github.com/cisco-open/fsoc)
```shell
./checkFSOC.sh
```
4. Login (the identity that fsoc is configured with needs to have sufficient privileges for the below solution operations)
    ```shell
   fsoc login
   ```
5. Check and validate the base solution inside `<your-username>base` folder 
   ```shell
   cd <your-username>base
   fsoc solution check --all
   fsoc solution validate --tag stable
   ```
6. Push the base solution
   ```shell
   fsoc solution push --tag stable
   ```
7. Subscribe to this solution
   ```shell
   fsoc solution subscribe <your-username>basesolution
   ```
8. Check and validate the extended solution inside `<your-username>extended` folder 
   ```shell
   cd <your-username>extended
   fsoc solution check --all
   fsoc solution validate --tag stable
   ```
9. Push the extended solution
   ```shell
   fsoc solution push --tag stable
   ```
10. Subscribe to this solution
   ```shell
   fsoc solution subscribe <your-username>extendedsolution
   ```    

## Testing

11. Check that the entity and metric types were created successfully by navigating to **Schema Browser**:
   ```
   https://{your tenant}.observe.appdynamics.com/ui/cco/tools/melt/schema
   ``` 
   and searching for `<your-username>basesolution` 
   It should list the `base_entity` with both the base and extended attributes and metrics.

12. Generate some sample load using the fsoc melt tool
```
cd <your-username>test
fsoc melt send -v test-config.yaml
```

13. Check that data is being populated (you need to have k8s monitoring enabled in order to see some data) by navigating to **Query Builder**:
   ```
   https://{your tenant}.observe.appdynamics.com/ui/cco/tools/melt/query
   ``` 
   and pasting the following UQL query:
   ```
   SINCE -2h fetch attributes('name'), attributes('base_attribute'), attributes('extended_attribute'), metrics('basesolution1:base_metric'),
 metrics('extendedsolution1:extended_metric') from entities('basesolution1:base_entity')
   ```