```
# Database Credentials Management Solution

This guide outlines the steps to deploy a solution that integrates with the Cisco Full-Stack Observability platform for managing database credentials securely. Our solution leverages the Knowledge Store to define and store database credentials securely, ensuring sensitive information like passwords are stored in an encrypted format and accessible based on role-based access controls.

## Learning Objectives

By following this guide, you will:

1. Understand the structure of a solution package for the Cisco Observability Platform.
2. Learn how to define a new knowledge type for securely managing database credentials.
3. Deploy your solution to the Cisco Observability Platform.
4. Query your knowledge type and objects within the platform.
5. Apply access control to secure your knowledge model.

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
│       └── databasecredentials.json
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

1. **Update the Solution Manifest**: Your solution folder, named `<USERNAME>secretexample`, contains the `manifest.json` file. Ensure your username replaces `SOLUTION_PREFIX` in the manifest file, setting the solution's name to your unique identifier.

2. **Define the Knowledge Type**: Review the `databasecredentials.json` in the `types` directory. This file defines the structure for securely storing database credentials within the Knowledge Store.

3. **Add Database Credentials Object**: Use the command below to add a knowledge object for your database credentials. This command utilizes the provided `dbcredentialexample.json` as an example.

    ```shell
    fsoc knowledge create --type=<USERNAME>secretexample:databasecredentials --layer-type=TENANT --object-file=objects/example/dbcredentialexample.json
    ```

4. **Validate Your Solution**: Run `./validate.sh` to check for any errors in your solution package.

5. **Deploy the Solution**: Execute `./push.sh` to deploy your solution to the Cisco Observability Platform. This script uses the FSOC CLI for deployment.

6. **Verify Deployment**: Utilize `./status.sh` to check the status of your solution deployment. Ensure that the solution name matches your `<USERNAME>secretexample` and that the installation was successful.

## Querying and Managing Access

1. **Query Knowledge Type**: Use the FSOC CLI to retrieve the definition of your knowledge type:

    ```shell
    fsoc knowledge get-type --type "<USERNAME>secretexample:databasecredentials"
    ```

2. **Access Control**: Review the `permissions.json` and `role-to-permission-mappings.json` files in the `objects` directory. These files define the access controls for your knowledge objects, ensuring only authorized users can access or modify the database credentials.

## Next Steps

Congratulations on deploying your Database Credentials Management Solution! You've learned how to securely manage sensitive information using the Cisco Observability Platform. To further customize or update your solution, remember to increment the `solutionVersion` in your `manifest.json` before redeploying.

```
