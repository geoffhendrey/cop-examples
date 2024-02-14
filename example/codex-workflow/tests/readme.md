How to run workflow tests:
1. install Node.js and npm: https://docs.npmjs.com/downloading-and-installing-node-js-and-npm
2. install stated:
   ```shell
   npm install -g stated-js
   ```
3. cd into codex workflow folder
   ```shell
   cd example/codex-workflow
   ```
4. run stated
   ```shell
   stated
   ```
5. execute tests
   ```shell
   .init tests/tests.yaml
   ```
6. check results
   ```shell
   .out /results
   ```