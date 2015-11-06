package com.spotify.apollo.core;

import java.io.IOException;

public class ApolloException extends IOException {

  public ApolloException() {
    super();
  }

  public ApolloException(String message) {
    super(message);
  }

  public ApolloException(String message, Throwable cause) {
    super(message, cause);
  }

  public ApolloException(Throwable cause) {
    super(cause);
  }
}
