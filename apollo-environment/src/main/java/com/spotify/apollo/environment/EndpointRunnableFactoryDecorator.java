/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.environment;

import com.spotify.apollo.request.EndpointRunnableFactory;

import java.util.function.UnaryOperator;

/**
 * A decorator for modifying the behavior of a {@link EndpointRunnableFactory}.
 *
 * This interface extends from {@link UnaryOperator} to easier be used in functional code.
 */
@FunctionalInterface
public interface EndpointRunnableFactoryDecorator
    extends UnaryOperator<EndpointRunnableFactory> {

}
