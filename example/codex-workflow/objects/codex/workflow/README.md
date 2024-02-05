start stated in the `examples/codex-workflow` folder
```shell
$> cd example/codex-workflow
$> stated
```
load the template
```shell
> .init -f objects/codex/workflow/workflow.sw.yaml
```
view the output of built-in test cases
```shell
> .out /test/cases
{
  "testConditionIsFunction": "pass",
  "testConditionTriggers": "pass",
  "testMeasurementIsProduces": "pass"
}
```
