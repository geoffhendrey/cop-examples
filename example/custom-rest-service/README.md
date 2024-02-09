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
Please remeber to replace <GITHUB-USER-NAME> with your github username.

```shell
docker build -t  ghcr.io/ <GITHUB-USER-NAME>/cop-examples/restdemo:latest .
docker run -t  ghcr.io/<GITHUB-USER-NAME>/cop-examples/restdemo:latest
```

[Authenticating with container registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#authenticating-to-the-container-registry)

Login to github  container respository.


```shell
 echo <TOKEN> | docker login ghcr.io -u <GITHUB-USER-NAME> --password-stdin
```


Push the image to the github registry or to any other public registry.

```shell
docker -v push  ghcr.io/<GITHUB-USER-NAME>/cop-examples/restdemo:latest
```

Creating the solution package
---------------

The `package` folder contains the files required to create the solution.

```text
├── manifest.json
└── objects
    └── function.json
```

Copy the `package` folder to a new folder restdemo

```shell
cp -R package restdemo
```


Edit the function.json file to use the image from your repository.

```json
{
    "name": "example1",
    "image": "ghcr.io/<GITHUB-USER-NAME>/cop-examples/restdemo:latest"
}
```


Solution names should be unique on the plaform. Prefix the solution name in the manifest with your user name. 


```json
{
    {
    "manifestVersion": "1.0.0",
    "name": "<USER-NAME>restdemo",
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
```

From the restdemo folder, use fsoc command to push the package to the plaform.

```shell
fsoc solution push --stable
```

 When successful this command should print the following message

 ```shell
 Successfully uploaded solution <USER-NAME>restdemo version 1.0.0 with tag stable.
 ``` 

 This will create a new REST api accesible with the scheme

```shell
https://<HOST>/rest/<SOLUTION-NAME>/<SERVICE-NAME>/*'

```

For this example, it will create two apis with URLs:

```shell
https://<TENANT-HOST-NAME>/rest/<USER-NAME>restdemo/example1/hello
https://<TENANT-HOST-NAME>/rest/<USER-NAME>prrestdemo/example1/headers

````

 Subscribe to the newly installed solution using fsoc command:
 ```shell
 fsoc solution subscribe <USER-NAME>restdemo
 ```

All custom REST endpoints requires authentication and authorization.
For authentication, copy the token present in the .fsoc file under your home directory and paste it as <TOKEN> in the below example

Use curl to execute a GET request on the newly deployed service, and it should return a 200 response code with `hello` as the body

```shell
curl --location 'https://<HOST>/rest/<SOLUTION-NAME>/<FUNCTION-NAME>/' --header 'Authorization: Bearerer <TOKEN> ' -v
< HTTP/2 200
hello
```
