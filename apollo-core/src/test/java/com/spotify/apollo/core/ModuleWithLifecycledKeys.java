package com.spotify.apollo.core;

import com.google.inject.Inject;
import com.google.inject.name.Names;

import com.spotify.apollo.module.AbstractApolloModule;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Named;

class ModuleWithLifecycledKeys extends AbstractApolloModule {

  private final AtomicBoolean created;
  private final AtomicBoolean closed;

  public ModuleWithLifecycledKeys(AtomicBoolean created, AtomicBoolean closed) {
    this.created = created;
    this.closed = closed;
  }

  @Override
  protected void configure() {
    bind(AtomicBoolean.class).annotatedWith(Names.named("created")).toInstance(created);
    bind(AtomicBoolean.class).annotatedWith(Names.named("closed")).toInstance(closed);

    bind(Foo.class).to(FooImpl.class);
    manageLifecycle(Foo.class);
  }

  @Override
  public String getId() {
    return "lifecycle-module";
  }

  interface Foo {
  }

  static class FooImpl implements Foo, Closeable {

    final AtomicBoolean closed;

    @Inject
    FooImpl(@Named("created") AtomicBoolean created,
            @Named("closed") AtomicBoolean closed) {
      this.closed = closed;
      created.set(true);
    }

    @Override
    public void close() throws IOException {
      closed.set(true);
    }
  }
}
