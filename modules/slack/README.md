# Slack

The `slack` module lets your application talk to Slack.

## Configuration

`slack.webhook` - (required) The webhook to post to, for example
`https://hooks.slack.com/services/PATH/TO/WEBHOOK`.

`slack.enabled` - (default `true`) If messages should be posted to slack. This is useful to
disable message posting, e.g. for local development.

`slack.username` - (default `service name`) The name to use.

`slack.emoji` - (default `":spoticon:"`) The icon to use.

`slack.messages.startup` - (default `""`) A message to post on service startup (if not empty).

`slack.messages.shutdown` - (default `""`) A message to post on service shutdown (if not empty).

## Example

```java
public static void main(String[] args) throws IOException, InterruptedException {
  Service service = Services.usingName("test")
      .withModule(SlackModule.create())
      .build();

  try (Service.Instance instance = service.start(args)) {
    Slack slack = instance.resolve(Slack.class);
    slack.post("hello there slack channel!");
    instance.waitForShutdown();
  }
}
```
