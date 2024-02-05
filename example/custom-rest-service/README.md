Our goal in this example is to provide step-by-step insructions for creating a solution that contains a custom REST endpoint. 

You will learn:

1. Create a service with a REST endpoint
1. Package the service inside a container image
1. Publish the container image to a container repository
1. Create a knowlege object to link your solution with the newly created image
1. Push the solution to the platform
1. Subscribe to the solution
1. Test the solution by invoking the new REST API with your platform credentials.


Create a simple REST service
---------------

The `service` folder contains the files for the service.
```text
├── Dockerfile
├── go.mod
└── main.go
```

`main.go` contains the minimal code to start a webserver on port 8080. Please remember, it is mandatory to run your http server on port 8080 to deploy on the COP platform.

This step requires:

- go
- docker

Change to the service directory, build and run the application

```shell
cd service
go build
go run .
```
This should print
```shell
Starting server...
```


Packaging and publishing the image to a public container repository
---------------


Build and run the container image.
Please remeber to replace <USER-NAME> with your github username.

```shell
docker build -t  ghcr.io/ <USER-NAME>/cop-examples/restdemo:latest .
docker run -t  ghcr.io/<USER-NAME>/cop-examples/restdemo:latest
```

[Authenticating with container registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#authenticating-to-the-container-registry)

Login to github  container respository.


```shell
 echo <TOKEN> | docker login ghcr.io -u <USER-NAME> --password-stdin
```


Push the image to the github registry or to any other public registry.

```shell
docker -v push  ghcr.io/<USER-NAME>/cop-examples/restdemo:latest
```

Creating the solution package
---------------

The `package` folder contains the files required to create the solution.

```text
├── manifest.json
└── objects
    └── function.json
```

Edit the function.json file to use the image from your repository.

```json
{
    "name": "example1",
    "image": "ghcr.io/<USER-NAME>/cop-examples/restdemo:latest"
}
```

Edit the manifest file to set the email attribute.

```json
{
    {
    "manifestVersion": "1.0.0",
    "name": "cop-example-rest",
    "solutionVersion": "1.0.0",
    "dependencies": ["zodiac"],
    "description": "Simple REST example on CO platform",
    "contact": "<your-email-here>",
    "gitRepoUrl": "-",
    "readme" : "readme.md",
    "types" : [],
    "objects": [
        {
            "type": "zodiac:function",
            "objectsDir": "objects/functions"
        }
    ]
  }
}

From the package folder, use fsoc command to push the package to the plaform.

```shell
fsoc solution push --stable
```
  