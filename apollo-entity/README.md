Apollo Entity
=============

A set of Apollo Middleware for working with entity types in route handlers.

## Why

A common pattern when writing route handlers is to serialize and deserialize to some API level
entity types. Writing such (de)serialization code quickly becomes repetitive and always looks
the same.

Ideally, one would like to have endpoint handlers with straight forward signatures like

```java
Person updatePerson(String id, Person person) {
  // ...
}
```

With a route defined as:

```java
Route.sync(
    "PUT", "/person/<id>",
    rc -> person -> updatePerson(rc.pathArgs().get("id"), person))
```

However, this does not work. Theres nothing handling how the `Person` type is being read to and
from a `ByteString` which is the payload type Apollo works with.

At this point, you'll start writing some custom middlewares using something like Jackson. This
library contains exactly those middlewares, ready to use directly with your routes.


## Getting started

Add Maven dependency:

```xml
<dependency>
    <groupId>com.spotify</groupId>
    <artifactId>apollo-entity</artifactId>
    <version>1.1.0</version>
</dependency>
```

Create your Jackson `ObjectMapper` and the [`EntityMiddleware`][1] factory:

```java
class MyApplication {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static void init(Environment environment) {
    MyApplication app = new MyApplication();
    EntityMiddleware entity = EntityMiddleware.forCodec(JacksonEntityCodec.forMapper(OBJECT_MAPPER));

    Route<SyncHandler<Response<ByteString>>> route = Route.with(
        entity.direct(Person.class),
        "PUT", "/person/<id>",
        rc -> person -> app.updatePerson(rc.pathArgs().get("id"), person));

    // ...
  }

  Person updatePerson(String id, Person person) {
    // ...
  }
}
```

[`EntityMiddleware`][1] also support custom codecs through the `forCodec(EntityCodec)` static
constructor. An [`EntityCodec`][2] defines how to read and write entity types from the `ByteString`
payloads that apollo handle natively. Since Jackson is so commonly used, we have a bundled
implementation called [`JacksonEntityCodec`][4].

## Handler signatures

The [`EntityMiddleware`][1] interface can create middlewares for several types of handler
signatures.

* `direct(E.class[, R.class])` middleware : `EntityHandler<E, R>`
 - `R handler(E)`
* `response(E.class[, R.class])` middleware : `EntityResponseHandler<E, R>`
 - `Response<R> handler(E)`
* `asyncDirect(E.class[, R.class])` middleware : `EntityAsyncHandler<E, R>`
 - `CompletionStage<R> handler(E)`
* `asyncResponse(E.class[, R.class])` middleware : `EntityAsyncResponseHandler<E, R>`
  - `CompletionStage<Response<R>> handler(E)`

For all of the above, if the reply entity type `R.class` is omitted it will be set to the same as `E.class`.

On top of these, there's also just a serializing middleware for handlers that do not take a
request entity as an input.

* `serializerDirect(R.class)` middleware : `SyncHandler<R>`
 - `R handler()`
* `serializerResponse(R.class)` middleware : `SyncHandler<Response<R>>`
 - `Response<R> handler()`
* `asyncSerializerDirect(R.class)` middleware : `AsyncHandler<R>`
 - `CompletionStage<R> handler()`
* `asyncSerializerResponse(R.class)` middleware : `AsyncHandler<Response<R>>`
 - `CompletionStage<Response<R>> handler()`

### Curried form

All of the `Entity*Handler<E, R>` handler signatures are in curried form with a `RequestContext`
as the first argument. This give you the flexibility to add more arguments to your handlers as you
see fit, while also being easy to compose.

```java
rc -> entity -> handler(entity, ...)
```

Works well with closures around handlers:

```java
rc -> updatePersonWithId(rc.pathArgs().get("id"))

UnaryOperator<Person> updatePersonWithId(String id) {
  return person -> ...;
}
```

In the case nothing else is needed from the request context, this reduces to:

```java
rc -> this::handler
```

---

See [`EntityMiddlewareTest`][3] for a complete list of route options and tests.

[1]: src/main/java/com/spotify/apollo/entity/EntityMiddleware.java
[2]: src/main/java/com/spotify/apollo/entity/EntityCodec.java
[3]: src/test/java/com/spotify/apollo/entity/EntityMiddlewareTest.java
[4]: src/main/java/com/spotify/apollo/entity/JacksonEntityCodec.java
