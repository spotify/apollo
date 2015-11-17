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
package com.spotify.apollo.test.response;

import com.google.common.base.Preconditions;

import com.spotify.apollo.Request;

import java.util.Iterator;

class SequenceResponseSource implements ResponseSource {

  private final Iterator<ResponseWithDelay> responseWithDelayIterator;

  public SequenceResponseSource(Iterable<ResponseWithDelay> responseWithDelayIterable) {
    this.responseWithDelayIterator = responseWithDelayIterable.iterator();
  }

  @Override
  public ResponseWithDelay create(Request request) {
    Preconditions.checkState(responseWithDelayIterator.hasNext(), "No more responses specified!");
    return responseWithDelayIterator.next();
  }
}
