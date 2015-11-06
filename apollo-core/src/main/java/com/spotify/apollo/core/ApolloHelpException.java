package com.spotify.apollo.core;

/**
 * An exception indicating that the user wants to show help information.
 */
public class ApolloHelpException extends ApolloCliException {

  public ApolloHelpException(String message) {
    super(message);
  }
}
