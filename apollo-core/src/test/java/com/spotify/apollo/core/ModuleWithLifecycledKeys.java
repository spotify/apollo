/*
 * -\-\-
 * Spotify Apollo Service Core (aka Leto)
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.apollo.core;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import com.spotify.apollo.module.AbstractApolloModule;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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
