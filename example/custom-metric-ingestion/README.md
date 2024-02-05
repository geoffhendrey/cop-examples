this example shows how to define and use a custom metric ingestion in a Cisco Observability Platform solution.

1. set up fsoc
2. fork this repo
3. use fsoc to mock a metric, ingest to the platform, and query it

## 1. set up fsoc & jq

```shell
brew install fsoc && brew install jq
```

## 2. configure fsoc to access your tenant
The easiest way to configure fsoc access is to use `fsoc config` with a new config file. Ensure you 
back up ~/.fsoc before running this command.

```bash
fsoc config set auth=oauth url=https://MYTENANT.observe.appdynamics.com
fsoc login  # test access
```

## 3. fork this solution
   This action is required to create a name-isolated solution.

```shell
./fork.sh
```

## 4. validate the solution


```shell
./validate.sh
```

## 5. publish & subscribe to the solution


```shell
./publish.sh
./subscribe.sh
./status.sh
```

## 6. mock a metric and ingest it

```shell
./generateMetrics.sh
```
