package com.spotify.apollo.core;

/**
 * An exception indicating that the command-line could not be parsed according to the Apollo
 * convention.
 */
public class ApolloCompatException extends ApolloCliException {

  public ApolloCompatException(String message) {
    super(message);
  }
}
