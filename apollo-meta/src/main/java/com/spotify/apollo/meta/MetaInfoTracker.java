/*
 * -\-\-
 * Spotify Apollo Metadata
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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
package com.spotify.apollo.meta;

import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.meta.model.MetaGatherer;

import java.util.List;

/**
 * Defines the contract for tracking metadata about an apollo application.
 */
public interface MetaInfoTracker {

  MetaGatherer getGatherer();

  <E extends Endpoint> void gatherEndpoints(List<E> endpoints);

  IncomingCallsGatherer incomingCallsGatherer();

  OutgoingCallsGatherer outgoingCallsGatherer();
}
