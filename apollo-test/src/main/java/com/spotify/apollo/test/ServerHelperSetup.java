/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test;

import com.spotify.apollo.module.ApolloModule;

interface ServerHelperSetup<T extends ServerHelperSetup<T>> {

  T domain(String domain);

  T disableMetaApi();

  T args(String... args);

  T conf(String key, String value);

  T conf(String key, Object value);

  T resetConf(String key);

  T forwardingNonStubbedRequests(boolean forward);

  T startTimeoutSeconds(int timeoutSeconds);

  T withModule(ApolloModule module);

  T scheme(String scheme);
}
