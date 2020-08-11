# AppInit & Environment

Apollo applications are initialized through the
[`AppInit`](/apollo-api/src/main/java/com/spotify/apollo/AppInit.java) interface either by
implementing it directly or through a method reference. This allows you to have different
initialization methods for running the service and testing it.

```java
public interface AppInit {
  void create(Environment environment);
}
```

A typical application will read values from
[`Environment.config()`](/apollo-api/src/main/java/com/spotify/apollo/Environment.java#L48) and set
up any application specific resources. To be properly closed when shutting down, these resources
should be registered with
[`Environment.closer()`](/apollo-api/src/main/java/com/spotify/apollo/Environment.java#L70). In
addition to the resources the application should register one or more endpoints (routes) using
[`Environment.routingEngine()`](/apollo-api/src/main/java/com/spotify/apollo/Environment.java#L72).
