Our goal in this example is to provide step-by-step insructions for creating a solution that contains a custom REST endpoint. 

You will learn:

1. Create a service with a REST endpoint
1. Package the service inside a container image
1. Publish the container image to a container repository
1. Create a knowlege object to link your solution with the newly create image
1. Push the solution to the platform
1. Subscribe to the solution
1. Test the solution by invoking the new REST API with your platform credentials

Create a simple REST service
---------------

The `service` folder contains the files for the service.
```text
├── Dockerfile
├── go.mod
└── main.go
```

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
Now, build and run the container image

```shell
docker build -t myapp .
docker run -t myapp
```
