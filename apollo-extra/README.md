# Apollo Extra

This Apollo library contains some utilities that may make your life easier.

## com.spotify.apollo.concurrent

Defines a couple of utilities that make it easier to move between `ListenableFuture`s and
`CompletionStage`s. Example usage:

```java
    ListenableFuture<Message> future = listenableFutureClient.send(myRequest);

    CompletionStage<Response<String>> response = Util.asStage(future)
        .thenApply(message -> Response.forPayload(message.data()));
```

## com.spotify.apollo.route

Contains some serializer middlewares, and utilities for versioning endpoints.

## TODO logging utilities
