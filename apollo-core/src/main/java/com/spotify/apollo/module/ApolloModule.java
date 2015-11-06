package com.spotify.apollo.module;

import com.google.inject.Key;
import com.google.inject.Module;

import java.util.Set;

/**
 * A module that can be loaded by Apollo to add more functionality to a service.  <strong>DO NOT
 * USE</strong> this interface directly.  It is not part of the public API.  Instead, extend {@link
 * AbstractApolloModule} which <strong>is</strong> part of the public API.
 */
public interface ApolloModule extends Module {

  /**
   * Returns the id for this module.
   *
   * @return the id for this module.
   */
  String getId();

  /**
   * Returns the priority of this module relative to other modules.  A higher priority means that
   * initialization will happen earlier for this module.  This only matters for independent
   * components of module dependencies; dependencies will always be initialized before the
   * dependees.  The default ("don't care") priority is {@code 0.0}.
   *
   * @return the priority of this module relative to other modules.
   */
  double getPriority();

  /**
   * Returns the immutable, idempotently determined and stable set of injection keys that should be
   * bound to the service lifecycle; i.e. created on service start-up and destroyed on shutdown. All
   * the instances for the keys returned by this method that implement {@link java.io.Closeable}
   * will be closed on shutdown.
   *
   * @return the set of injection keys that should be bound to the service lifecycle.
   */
  Set<? extends Key<?>> getLifecycleManaged();
}
