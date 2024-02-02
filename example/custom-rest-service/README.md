Our goal in this example is to provide step-by-step insructions for creating a solution that contains a custom REST endpoint. 

You will learn:

1. Create a service with a REST endpoint
1. Package the service inside a container image
1. Publish the container image to a container repository
1. Create a knowlege object to link your solution with the newly create image
1. Push the solution to the platform
1. Subscribe to the solution
1. Test the solution by invoking the new REST API with your platform credentials



docker build -t myapp .
docker run -t myapp