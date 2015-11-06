package com.spotify.apollo.core;

/**
 * An exception related to command-line processing.  It is safe to display the exception's message
 * on the command-line.  {@link #getMessage()} returns the message to display.
 */
public class ApolloCliException extends ApolloException {

  public ApolloCliException(String message) {
    super(message);
  }

  public ApolloCliException(String message, Throwable cause) {
    super(message, cause);
  }
}
