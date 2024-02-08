How to test this solution:
1. install and configure fsoc: https://github.com/cisco-open/fsoc
2. login (the identity that fsoc is configured with needs to have sufficient privileges for the below solution operations)
    ```shell
   fsoc login
   ```
3. rename the solution in manifest.json
4. check and validate the solution
   ```shell
   fsoc solution check --all
   fsoc solution validate
   ```
5. run local tests (see tests/readme.md)
6. push the solution
   ```shell
   fsoc solution push
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