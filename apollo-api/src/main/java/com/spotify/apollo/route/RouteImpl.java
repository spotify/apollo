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
package com.spotify.apollo.route;

import com.google.auto.value.AutoValue;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Abstract class to get an Auto Value implementation of the {@link Route} interface
 */
@AutoValue
abstract class RouteImpl<H> implements Route<H> {

  @Override
  public <K> Route<K> copy(String method, String uri, K handler, @Nullable DocString docString) {
    return new AutoValue_RouteImpl<>(method, uri, handler, Optional.ofNullable(docString));
  }
}
