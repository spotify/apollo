# Apollo Metrics

This module integrates the [semantic-metrics](https://github.com/spotify/semantic-metrics) library
with the Apollo request/response handling facilities.

Including this this module in your assembly means that Apollo will add some metrics tracking
requests per endpoint.

## Metrics

All metrics will be tagged with the following tags:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| service     | name of Apollo application |                                                      |
| endpoint    | method:uri-path-segment    | For instance, GET:/android/v1/search-artists/<query> |

The following metrics are recorded:

### Endpoint request rate

A Meter metric, per status-code, tagged with:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "endpoint-request-rate"    | Enable/disable with ENDPOINT_REQUEST_RATE            |
| status-code | *                          | "200", "404", "418", etc.                            |
| unit        | "request"                  |                                                      |

### Endpoint request duration

A Timer metric, tagged with:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "endpoint-request-duration"|  Enable/disable with ENDPOINT_REQUEST_DURATION       |

### Fan-out

A Histogram metric, tagged with:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "request-fanout-factor"    | Enable/disable with REQUEST_FANOUT_FACTOR            |
| unit        | "request/request"          |                                                      |

This metric will show you how many downstream requests each endpoint tends to make.

### Request payload size

A Histogram metric, tagged with:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "request-payload-size"     | Enable/disable with REQUEST_PAYLOAD_SIZE             |
| unit        | "B"                        | size in bytes                                        |

### Response payload size

A Histogram metric, tagged with:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "response-payload-size"    | Enable/disable with RESPONSE_PAYLOAD_SIZE            |
| unit        | "B"                        | size in bytes                                        |

### Error ratios

Three groups of Gauges:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "error-ratio"              | Enable/disable with ERROR_RATIO                      |
| stat        | "1m", "5m", "15m"          | indicates the time interval in minutes               |

A response is classified as successful if its status code is in the INFORMATIONAL or SUCCESSFUL
status families. (See the `Family` enum in
[`StatusType`](../../apollo-api/src/main/java/com/spotify/apollo/StatusType.java)).

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "error-ratio-4xx           | Enable/disable with ERROR_RATIO_4XX                  |
| stat        | "1m", "5m", "15m"          | indicates the time interval in minutes               |

A response is classified as successful if its status code is _not_ in the CLIENT_ERROR
status family.

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "error-ratio-5xx"          | Enable/disable with ERROR_RATIO_5XX                  |
| stat        | "1m", "5m", "15m"          | indicates the time interval in minutes               |

A response is classified as successful if its status code is _not_ in the SERVER_ERROR
status family.

The '1m' value shows the error ratio in the last minute, the '5m' value shows the ratio in
the last 5 minutes, etc. The metrics do not include dropped requests.

### Dropped Requests

A Meter, tagged with: 

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "dropped-request-rate"     | Enable/disable with DROPPED_REQUEST_RATE             |
| unit        | "request"                  |                                                      |

Requests are dropped when they expire: if they have a time-to-live and are older than
that TTL, Apollo will not try to respond. Apollo may also drop requests if it is 
overloaded and cannot respond to all incoming requests.


## [ffwd](https://github.com/spotify/ffwd) reporter

The metrics module includes the [ffwd reporter](https://github.com/spotify/semantic-metrics#provided-plugins)
easily configurable from the apollo service configuration.

## Configuration

key | type | required | note
--- | ---- | -------- | ----
`metrics.server` | string list | optional | list of [`What`](src/main/java/com/spotify/apollo/metrics/semantic/What.java) names to enable; defaults to [ENDPOINT_REQUEST_RATE, ENDPOINT_REQUEST_DURATION, DROPPED_REQUEST_RATE, ERROR_RATIO]
`metrics.precreate-codes` | int list | optional | list of status codes to precreate request-rate meters for, default empty
`ffwd.interval` | int | optional | interval in seconds of reporting metrics to ffwd; default 30
`ffwd.host` | string | optional | host where ffwd is running; default localhost
`ffwd.port` | int | optional | port where ffwd is running; default 19091

You may not want to enable all the metrics Apollo can create, since some of them can be expensive 
(in particular on the alerting and graphing side), hence the ability to configure which
metrics to emit via `metrics.server`.

The `metrics.precreate-codes` setting is there since the request-rate meters
are lazily created. Apollo doesn't emit any metrics for status codes it hasn't
seen. This can lead to strange effects when, for instance, an error shows up
for the first time after a restart. Pre-creating meters for status codes you 
want to alert on makes it less likely to get false positives, since Apollo
will then emit a '0' value until the first time a certain status code shows up.

## Custom Metrics

To set up custom metrics on the application level you'll need to get hold of the
`SemanticMetricRegistry` object. Use:
```java
  environment.resolve(SemanticMetricRegistry.class);
```

You can then construct `MetricId`:s and emit metrics in handlers and middleware.

To tag custom metrics with endpoint information, you could pass in the endpoint name, or transform
whole routes. The example below shows both approaches.

Alternatively, you could set up an `EndpointRunnableFactoryDecorator` and register
it, similar to how the metrics module does:
```java
    Multibinder.newSetBinder(binder(), EndpointRunnableFactoryDecorator.class)
        .addBinding().to(MetricsCollectingEndpointRunnableFactoryDecorator.class);
```
See [Extending incoming/outgoing request handling]
(https://github.com/spotify/apollo/tree/master/apollo-environment#extending-incomingoutgoing-request-handling)
for some description of how to do request handling decorations.

For client-side metrics, wrap the ```Client``` you get from the ```RequestContext``` in a similar
way to how ```DecoratingClient``` is implemented. In your wrapper, ensure that the right metrics
are tracked.

