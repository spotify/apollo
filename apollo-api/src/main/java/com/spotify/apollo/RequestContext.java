/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.apollo;

import java.util.Map;

/**
 * This object contains all the needed information related to an incoming request.
 */
public interface RequestContext {

  /**
   * Get the incoming request message.
   *
   * @return the request message
   */
  Request request();

  /**
   * Get an Apollo client that can be used to make backend service requests. The requests will have
   * the auth context of the incoming request applied.
   *
   * For a non-scoped Client, see {@link Environment#client()}.
   *
   * @return A {@link Client}.
   */
  Client requestScopedClient();

  /**
   * Gets a map of parsed path arguments. If Route is defined as having the path
   * "/somewhere/<param>/<param2:path>", and it gets invoked with a URI with the path
   * "/somewhere/over/the%32rainbow", then the map will be
   * { "param" : "over", "param2": "the%32rainbow" }.
   */
  Map<String, String> pathArgs();
}
