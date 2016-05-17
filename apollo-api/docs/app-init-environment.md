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

```java
public class DataService {

  /**
   * An implementation of the AppInit functional interface which simply sets up a
   * connection with a cassandra cluster based on some configuration values
   * and registers an address resource that will interact with it.
   */
  static void init(Environment environment) {
    String clusterName = environment.config().getString("data_store/cluster_name");
    String keySpace = environment.config().getString("data_store/key_space");
    String tableName = environment.config().getString("data_store/table");

    ClusterConnection cassandraClusterConnection = createClusterConnection(
        environment.domain(), clusterName, keySpace, tableName);

    environment.closer().register(cassandraClusterConnection);

    RouteProvider addressResource = new AddressResource(cassandraClusterConnection);
    environment.routingEngine().registerAutoRoutes(addressResource);
  }

  private static class AddressResource implements RouteProvider {

    private final String clusterConnection;

    AddressResource(ClusterConnection cassandraClusterConnection) {
      this.clusterConnection = cassandraClusterConnection;
    }

    @Override
    public Stream<? extends Route<? extends AsyncHandler<?>>> routes() {
      return Stream.of(
          Route.sync("GET", "/v1/address/<name>", requestContext ->
              getAddress(requestContext.pathArgs().get("name"))),

          Route.async("PUT", "/v1/address/<name>", requestContext ->
              putAddress(requestContext.pathArgs().get("name"),
                         requestContext.request().payload()))
      );
    }

    // implementations of getAddress and putAddress
  }

  /**
   * The main entry point for the service, referencing init
   */
  public static void main(String... args) throws LoadingException {
    HttpService.boot(DataService::init, "ping", args);
  }
}
```
