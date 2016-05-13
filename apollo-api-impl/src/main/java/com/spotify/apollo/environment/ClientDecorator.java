/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.environment;

import com.google.auto.value.AutoValue;

import java.util.function.UnaryOperator;

/**
 * Allows the user to decorate the managed {@link IncomingRequestAwareClient} with additional behavior.
 */
public interface ClientDecorator
    extends UnaryOperator<IncomingRequestAwareClient> {

  default Id id() {
    return Id.UNKNOWN;
  }

  /**
   * Identifier for a function performed by a client decorator; used to decouple client decorator
   * roles from their implementing classes.
   */
  @AutoValue
  abstract class Id {
    static final Id UNKNOWN = Id.of(ClientDecorator.class, "UNKNOWN");

    /**
     * For namespacing, to allow choosing good names without risking conflicts with names defined
     * elsewhere.
     */
    public abstract Class<?> definingClass();
    public abstract String id();

    public static Id of(Class<?> definingClass, String id) {
      return new AutoValue_ClientDecorator_Id(definingClass, id);
    }
  }
}
