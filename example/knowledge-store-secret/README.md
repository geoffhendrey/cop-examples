```
# AWS Credentials Management Solution

This guide outlines the steps to deploy a solution package for securely managing credentials within the Cisco Observability Platform. The solution package includes a new knowledge type for securely storing aws credentials.

<!-- TOC -->
  * [Initial Setup](#initial-setup)
  * [Deploying Your Solution](#deploying-your-solution)
  * [Querying Your Knowledge Type](#querying-your-knowledge-type)
  * [Next Steps](#next-steps)
<!-- TOC -->

## Learning Objectives

By following this guide, you will:

1. Understand the structure of a solution package for the Cisco Observability Platform.
2. Learn how to define a new knowledge type for securely managing credentials
3. Understand how to apply access control to secure your knowledge model.
3. Deploy your solution to the Cisco Observability Platform.
4. Query your knowledge type and objects within the platform. 

## Solution Structure

The solution package is organized as follows:

```bash
.
├── README.md
├── checkFSOC.sh
├── fork.sh
├── package
│   ├── example
│   │   └── dbcredentialexample.json
│   ├── manifest.json
│   ├── objects
│   │   ├── permissions.json
│   │   └── role-to-permission-mappings.json
│   └── types
│       └── awscreds.json
├── push.sh
├── setSolutionPrefix.sh
├── status.sh
└── validate.sh
```

Ensure all scripts have executable permissions:

```shell
chmod u+x *.sh
```

## Initial Setup

1. **Check FSOC CLI Installation**: Run the `./checkFSOC.sh` script to verify you have a recent version of the FSOC CLI installed.

2. **Fork the Solution**: Execute `./fork.sh` to create a copy of the solution package prefixed with your username. This process will also update various files within the solution with your username, preparing it for a personalized deployment.

## Deploying Your Solution

1. **Update the Solution Manifest**: Your solution folder, named `<USERNAME>awscreds`, contains the `manifest.json` file. Ensure your username replaces `SOLUTION_PREFIX` in the manifest file, setting the solution's name to your unique identifier.

2. **Define the Knowledge Type**: Review the `awscreds.json` in the `types` directory. This file defines the structure for securely storing database credentials within the Knowledge Store.

3. **Add Database Credentials Object**: Use the command below to add a knowledge object for your database credentials. The provided `awscredsexample.json` file contains an example of the knowledge object you will create.

    ```shell
    fsoc knowledge create --type=<USERNAME>awscreds:awscreds --layer-type=TENANT --object-file=objects/example/awscredsexample.json
    ```

4. **Validate Your Solution**: Run `./validate.sh` to check for any errors in your solution package.

5. **Deploy the Solution**: Execute `./push.sh` to deploy your solution to the Cisco Observability Platform. This script uses the FSOC CLI for deployment.

6. **Verify Deployment**: Utilize `./status.sh` to check the status of your solution deployment. Ensure that the solution name matches your `<USERNAME>awscreds` and that the installation was successful.

## Querying Your Knowledge Type

1. **Query Knowledge Type**: Use the FSOC CLI to retrieve the definition of your knowledge type:

    ```shell
    fsoc knowledge get-type --type "<USERNAME>awscreds:awscreds"
    ```

2. **Create new Knowledge Object**: Add a new knowledge object for your database credentials using the `fsoc knowledge create` command.
   ```shell
    fsoc knowledge create --type=sesergeeawscreds:awscreds --layer-type=TENANT --object-file=objects/example/awscredsexample.json
    ```

3. **Fetch the object**: Use the `fsoc knowledge get` command to retrieve the knowledge object you created.

    ```shell
   fsoc knowledge get --type=sesergeeawscreds:awscreds --layer-type=TENANT --object-id=MY_AWS_ACCESS_KEY_ID
   createdAt: "2024-02-16T01:38:57.800Z"
   data:
   id: MY_AWS_ACCESS_KEY_ID
   key: '**********'
   region: us-west-2
   id: MY_AWS_ACCESS_KEY_ID
   layerId: 2d4866c4-0a45-41ec-a534-011e5f4d970a
   layerType: TENANT
   objectMimeType: application/json
   objectType: sesergeeawscreds:awscreds
   patch: null
   targetObjectId: null
   updatedAt: "2024-02-16T01:38:57.800Z"
   ```
    Notic that the password is masked with asterisks which indicates it is stored as a secret.
4. **Fetch the secret**
    ```shell
   export TOKEN=`yq '.contexts[0].tokcludetags: false' -H 'Layer-Id: 2d4866c4-0a45-41ec-a534-011e5f4d970a' -H 'Layer-Type: TENANT' 'https://arch3.saas.appd-test.com/knowledge-store/v1/objects/sesergeeawscreds:awscreds/MY_AWS_ACCESS_KEY_ID' -s | jq
   ```
## Next Steps

Congratulations on deploying your Database Credentials Management Solution! You've learned how to securely manage sensitive information using the Cisco Observability Platform. To further customize or update your solution, remember to increment the `solutionVersion` in your `manifest.json` before redeploying.

```
