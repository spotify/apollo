package com.spotify.apollo.dispatch;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;

import java.util.concurrent.CompletionStage;

import okio.ByteString;

/**
 * An application endpoint that can handle a specific uri pattern. It specifies how to serialize the
 * response and what content type to use.
 */
public interface Endpoint {

  CompletionStage<Response<ByteString>> invoke(RequestContext requestContext);

  EndpointInfo info();
}
