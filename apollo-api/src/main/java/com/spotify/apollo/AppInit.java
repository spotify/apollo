/*
 * -\-\-
 * Spotify Apollo API Interfaces
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
package com.spotify.apollo;

/**
 * An application initializer that will set up the application using an {@link Environment}.
 *
 * A typical application will read the {@link Environment#config()} and set up any application
 * specific resources. These resources should be registered with the {@link Environment#closer()}
 * in order to be properly closed when shutting down.
 */
public interface AppInit {

  /**
   * Sets up an application.
   *
   * @param environment  The Environment in which the application should be initialized
   */
  void create(Environment environment);
}
