# Apollo Metrics

This module integrates the [semantic-metrics](https://github.com/spotify/semantic-metrics) library
with the Apollo request/response handling facilities.

Including this this module in your assembly means that Apollo will add some metrics tracking
requests per endpoint.


A *note* on histograms. This metrics module now uses a [ReservoirWithTtl](https://github.com/spotify/semantic-metrics/tree/9b51f4f6febe7ca251c9410592324645bdb87d6a#histogram-with-ttl) as the **default** reservoir for histograms. This eliminates the impact of latencies like p99 being stuck when instances are drained of traffic or the request throughput of a service or endpoint are low.


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

### Endpoint Duration Goal

A Meter, tagged with:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "endpoint-request-duration-threshold-rate"     | Enable/disable with endpoint-duration-goal config           |
| threshold   | *                 | The request duration goal, in milliseconds                   |

This meter will only be created if a duration goal is set in the configuration (`endpoint-duration-goal`). The meter will be marked when a request meets its goal.



## [ffwd](https://github.com/spotify/ffwd) reporter

The metrics module includes the [ffwd reporter](https://github.com/spotify/semantic-metrics#provided-plugins)
easily configurable from the apollo service configuration.

## Configuration

key | type | required | note
--- | ---- | -------- | ----
`metrics.server` | string list | optional | list of [`What`](src/main/java/com/spotify/apollo/metrics/semantic/What.java) names to enable; defaults to [ENDPOINT_REQUEST_RATE, ENDPOINT_REQUEST_DURATION, ENDPOINT_REQUEST_DURATION_THRESHOLD_RATE, DROPPED_REQUEST_RATE, ERROR_RATIO, ERROR_RATIO_4XX, ERROR_RATIO_5XX]
`metrics.precreate-codes` | int list | optional | list of status codes to precreate request-rate meters for, default empty
`metrics.reservoir-ttl` | int | optional | When to purge old values from the histogram, defaults to 300 seconds. Note, setting this to a large value will increase the amount of memory used to keep track samples.
`ffwd.type` | string | optional | indicates which type of ffwd reporter to use. Available types are `agent` and `http`. see below for details. default is `agent`.
`endpoint-duration-goal`  | int map  | optional |  sets request duration thresholds in milliseconds to track how many requests meet a duration objective

You may not want to enable all the metrics Apollo can create, since some of them can be expensive
(in particular on the alerting and graphing side), hence the ability to configure which
metrics to emit via `metrics.server`.

The `metrics.precreate-codes` setting is there since the request-rate meters
are lazily created. Apollo doesn't emit any metrics for status codes it hasn't
seen. This can lead to strange effects when, for instance, an error shows up
for the first time after a restart. Pre-creating meters for status codes you
want to alert on makes it less likely to get false positives, since Apollo
will then emit a '0' value until the first time a certain status code shows up.

### ffwd.type = `agent`

key | type | required | note
--- | ---- | -------- | ----
`ffwd.interval` | int | optional | interval in seconds of reporting metrics to ffwd; default 30
`ffwd.flush` | boolean | optional | include a final flush of metrics on service shut down; default `False`
`ffwd.host` | string | optional | host where the ffwd agent is running. default `localhost`
`ffwd.port` | int | optional | port where the ffwd agent is running. default `19091`

#### Example

```
ffwd.type = agent
ffwd.interval = 30
ffwd.host = "localhost"
ffwd.port = 19091
```

### ffwd.type = `http`

key | type | required | note
--- | ---- | -------- | ----
`ffwd.interval` | int | optional | interval in seconds of reporting metrics to ffwd; default 30
`ffwd.flush` | boolean | optional | include a final flush of metrics on service shut down; default `False`
`ffwd.discovery.type` | string | required | indicates how to discovery http endpoints. Available options are `static` and `srv`. See below for details.

#### Example

```
ffwd.type = http
ffwd.interval = 30
ffwd.discovery = {
  type = "srv"
  record = "_metrics-api._http.example.com."
}
```

### ffwd.discovery.type = `static`

Provides a "hardcoded" endpoint to send metrics to. This is primarily useful during testing since it doesn't provide any fallback mechanisms in case a remote endpoint goes down.

key | type | required | note
--- | ---- | -------- | ----
`ffwd.discovery.host` | string | required | host to send metric batches to.
`ffwd.discovery.port` | int | required | port to send metric batches to.

#### Example

```
ffwd.discovery = {
  type = static
  host = "localhost"
  port = "8080"
}
```

### ffwd.discovery.type = `srv`

key | type | required | note
--- | ---- | -------- | ----
`ffwd.discovery.record` | string | required | SRV record to use when looking up http endpoints. if this _does not_ end with a dot (`.`), it will use `apollo.domain` as a search domain.

#### Example

```
ffwd.discovery = {
  type = srv
  record = "_metrics-api._http.example.com."
}
```

### endpoint-duration-goal

key | type | required | note
--- | ---- | -------- | ----
`endpoint-duration-goal.{endpoint}.{method}` | int | required | Duration threshold in milliseconds for an endpoint/method combination

**Note** If an endpoint has a parameter it needs to be included. For example a route defined as `GET:/example/<name>` would have
be configured as `/v1/example-b/<name>.GET`

#### Example

```
endpoint-duration-goal = {
  /v1/example-a.GET        = 200 # meter marked every time this endpoint resolves in under 200 ms
  /v1/example-b/<name>.GET = 100 # meter marked every time this endpoint with a named parameter resolves in under 100 ms
}
```

key | type | required | note
--- | ---- | -------- | ----
`endpoint-duration-goal.all-endpoints` | int | required | Global duration threshold in milliseconds, will be overridden by endpoint specific goals

#### Example

```
endpoint-duration-goal = {
  all-endpoints = 500 # unless individually overridden, a goal of 500ms for all endpoints
}
```

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

## Custom Tags

Metrics can be decorated with custom tags by setting environment variables.

- `FFWD_TAG_environment=production` will append `environment: production` to all metrics.
- `FFWD_TAG_foo=bar` will tag metrics with `foo: bar`
