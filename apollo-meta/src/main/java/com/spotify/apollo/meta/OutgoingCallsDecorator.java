/*
 * -\-\-
 * Spotify Apollo API Environment
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

import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

public class OutgoingCallsDecorator implements ClientDecorator {
  private final OutgoingCallsGatherer callsGatherer;

  public OutgoingCallsDecorator(OutgoingCallsGatherer callsGatherer) {
    this.callsGatherer = callsGatherer;
  }

  @Override
  public IncomingRequestAwareClient apply(IncomingRequestAwareClient incomingRequestAwareClient) {
    return new OutgoingCallsGatheringClient(callsGatherer, incomingRequestAwareClient);
  }

  @Override
  public Id id() {
    return MetaModule.OUTGOING_CALLS;
  }
}
