/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.environment;

import com.spotify.apollo.request.RequestRunnableFactory;

import java.util.function.UnaryOperator;

/**
 * A decorator for modifying the behavior of a {@link RequestRunnableFactory}.
 *
 * This interface extends from {@link UnaryOperator} to easier be used in functional code.
 */
@FunctionalInterface
public interface RequestRunnableFactoryDecorator
    extends UnaryOperator<RequestRunnableFactory> {

}
