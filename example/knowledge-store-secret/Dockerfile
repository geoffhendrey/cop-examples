# Official Node.js runtime
FROM node:20.5.1

# Set the working directory inside the container
WORKDIR /usr/src/app

# Bundle the application source code inside the container
COPY package.json package-lock.json aws-service-example.js ./

# Install application dependencies
RUN npm install

# Grant execute permissions for the stated-workflow-docker.js file
RUN chmod +x aws-service-example.js

# Start an example workflow which listens on port 8080 for cloud events
CMD ["node", "--experimental-vm-modules", "./aws-service-example.js"]
