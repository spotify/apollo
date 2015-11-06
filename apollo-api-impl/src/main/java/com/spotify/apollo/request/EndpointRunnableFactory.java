/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.dispatch.Endpoint;

/**
 * A factory responsible for producting a {@link Runnable} that encapsulates the handling of a
 * {@link OngoingRequest} together with the {@link RequestContext} that is made available to the
 * {@link Endpoint}.
 */
@FunctionalInterface
public interface EndpointRunnableFactory {

  Runnable create(OngoingRequest ongoingRequest, RequestContext requestContext, Endpoint endpoint);
}
