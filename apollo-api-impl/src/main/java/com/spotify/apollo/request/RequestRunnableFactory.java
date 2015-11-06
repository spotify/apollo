/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

/**
 * A factory responsible for producing a {@link RequestRunnable} that is supposed to handle the
 * {@link OngoingRequest}.
 */
@FunctionalInterface
public interface RequestRunnableFactory {

  RequestRunnable create(OngoingRequest ongoingRequest);
}
