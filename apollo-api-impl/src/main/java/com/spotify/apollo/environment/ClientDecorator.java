/*
 * Copyright (c) 2015 Spotify AB
 */

package com.spotify.apollo.environment;

import java.util.function.UnaryOperator;

/**
 * Allows the user to decorate the managed {@link IncomingRequestAwareClient} with additional behavior.
 */
public interface ClientDecorator
    extends UnaryOperator<IncomingRequestAwareClient> {

}
