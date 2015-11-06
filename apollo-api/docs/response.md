# Response

[`Response<T>`](/apollo-api/src/main/java/com/spotify/apollo/Response.java) is an optional wrapper
for the return type of route handlers. Use it when you want to control additional parameters of the
service reply like setting a different status code or adding headers.

Examples:

```java
Response<String> handle(RequestContext requestContext) {
  String s = stringResponse();
  return Response.forPayload(s)
      .withHeader("X-Payload-Length", String.valueOf(s.length()));
}
```

```java
Response<String> handle(RequestContext requestContext) {
  String arg = requestContext.request().getParameter("arg");
  if (arg == null || arg.isEmpty()) {
    return Response.forStatus(
        Status.BAD_REQUEST.withReasonPhrase("Mandatory query parameter 'arg' is missing"));
  }

  return Response.forPayload("Your " + arg + " is valid");
}
```
