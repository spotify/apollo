package com.spotify.apollo.core;

/**
 * Thrown if the Apollo configuration is invalid due to a programmer error, e.g. asking for an
 * instance of an unloaded module. Clients should stop execution if this exception occurs.
 */
public class ApolloConfigurationException extends RuntimeException {

  public ApolloConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

}
