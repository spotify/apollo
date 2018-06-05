/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *      http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.apollo.http.common;

import static com.spotify.apollo.Status.IM_A_TEAPOT;

import com.google.common.collect.Lists;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestHandler;
import java.util.List;

public class TestHandler implements RequestHandler {

    List<OngoingRequest> requests = Lists.newLinkedList();

    @Override
    public void handle(OngoingRequest request) {
      requests.add(request);
      request.reply(Response.forStatus(IM_A_TEAPOT));
    }

  public List<OngoingRequest> getRequests() {
    return requests;
  }
}