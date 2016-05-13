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
package com.spotify.apollo.environment;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;

import com.spotify.apollo.environment.ClientDecorator.Id;

import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Compares client decorators based on two lists of known ids. Client decorators are classified as
 * 'in beginning list', 'unknown' or 'in ending list'. Client decorators in the 'beginning list' are
 * smaller than 'unknown' and 'in ending list', and 'unknown' are smaller than 'ending list'. When
 * two client decorators that both are known are compared, then the one that comes first in the list
 * is the smallest. Unknown decorators have no ordering imposed.
 */
public class ClientDecoratorOrder implements Comparator<Id> {

  private final List<Id> firstIds;
  private final List<Id> lastIds;

  private ClientDecoratorOrder(List<Id> firstIds, List<Id> lastIds) {
    checkArgument(!firstIds.stream().filter(lastIds::contains).findAny().isPresent(),
                  "Id collections have at least one overlapping element: %s, %s", firstIds, lastIds);

    this.firstIds = ImmutableList.copyOf(firstIds);
    this.lastIds = ImmutableList.copyOf(lastIds);
  }

  @Override
  public int compare(Id left, Id right) {
    int leftBeginningIndex = firstIds.indexOf(left);
    int rightBeginningIndex = firstIds.indexOf(right);
    int leftEndingIndex = lastIds.indexOf(left);
    int rightEndingIndex = lastIds.indexOf(right);

    Segment leftSegment = segment(leftBeginningIndex, leftEndingIndex, left);
    Segment rightSegment = segment(rightBeginningIndex, rightEndingIndex, right);

    return ComparisonChain.start()
        .compare(leftSegment, rightSegment)
        .compare(leftBeginningIndex, rightBeginningIndex)
        .compare(leftEndingIndex, rightEndingIndex)
        .result();
  }

  private Segment segment(int beginningIndex, int endingIndex, Id id) {
    // this check should be redundant, but I'm paranoid
    checkState(unknown(beginningIndex) || unknown(endingIndex),
               "Id %s found in both beginning and ending lists", id);

    if (unknown(beginningIndex) && unknown(endingIndex)) {
      return Segment.MIDDLE;
    }

    if (unknown(endingIndex)) {
      return Segment.FIRST;
    }

    return Segment.LAST;
  }

  private boolean unknown(int index) {
    return index < 0;
  }

  public static ClientDecoratorOrder beginWith(Id... beginning) {
    return new ClientDecoratorOrder(ImmutableList.copyOf(beginning), ImmutableList.of());
  }

  public ClientDecoratorOrder endWith(Id... ending) {
    return new ClientDecoratorOrder(firstIds, ImmutableList.copyOf(ending));
  }

  private enum Segment {
    FIRST,
    MIDDLE,
    LAST;
  }
}
