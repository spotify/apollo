package com.spotify.apollo.standalone;

public class LoadingException extends Exception {

  public LoadingException(final String message) {
    super(message);
  }

  public LoadingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
