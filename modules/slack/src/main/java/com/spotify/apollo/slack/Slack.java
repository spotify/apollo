package com.spotify.apollo.slack;

import java.io.Closeable;

public interface Slack extends Closeable {

  /**
   * Post a message to slack.
   * @param message the message to post.
   * @return {@code true} if the message was posted successfully.
   */
  boolean post(String message);

}
