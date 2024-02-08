This example demonstrates how existing entities that model a particular domain can be extended without changing the original domain model. Each new domain that wants to add their perspective in terms of additional attributes and metrics can do so. All the additional metrics and attributes are available for querying and visualizing in dashboards.
In this example, we'll be extending workload entity in k8s domain to add a risk_score metric. It represents security risk score of a workload, typically obtained from CVSS or custom sources with custom algorithms to calculate such score.

You will learn:

1. How to declare dependencies on existing entities
2. How to extend existing entity to add a metric and an attribute
3. Push the solution to the platform
4. Subscribe to the solution
5. Test the solution 

The `entity_extension` folder contains the solution structure.
```text


├── manifest.json
├── objects
│   ├── malwareInvestigationDefaults.json
│   ├── permissions.json
│   └── role-to-permission-mappings.json
└── types
       └── investigation.json
```

1. install and configure fsoc: https://github.com/cisco-open/fsoc
2. login (the identity that fsoc is configured with needs to have sufficient privileges for the below solution operations)
    ```shell
   fsoc login
   ```
3. check and validate the solution
   ```shell
   fsoc solution check --all
   fsoc solution validate
   ```
4. push the solution
   ```shell
   fsoc solution push
   ```
5. subscribe to this solution
   ```shell
   fsoc solution subscribe codexworkflow
   ```
6. check that the metric type was created successfully by navigating to **Schema Browser**:
   ```
   https://{your tenant}.observe.appdynamics.com/ui/cco/tools/melt/schema
   ``` 
   and searching for entity-extension 
7. check that metric is being populated (you need to have k8s monitoring enabled in order to see some data) by navigating to **Query Builder**:
   ```
   https://{your tenant}.observe.appdynamics.com/ui/cco/tools/melt/query
   ``` 
   and pasting the following UQL query:
   ```
   SINCE now - 6h FETCH metrics('entity-extension:security.risk_score') {timestamp, value} FROM entities(k8s:workload)
   ```