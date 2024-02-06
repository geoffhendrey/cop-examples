How to test this solution:
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
   and searching for codexworkflow 
7. check that metric is being populated (you need to have k8s monitoring enabled in order to see some data) by navigating to **Query Builder**:
   ```
   https://{your tenant}.observe.appdynamics.com/ui/cco/tools/melt/query
   ``` 
   and pasting the following UQL query:
   ```
   SINCE now - 1h FETCH metrics('codexworkflow:healthrule.violation.count') {timestamp, value} FROM entities(k8s:workload)
   ```