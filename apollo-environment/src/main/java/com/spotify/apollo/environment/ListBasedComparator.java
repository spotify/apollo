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

import com.google.common.primitives.Ints;

import java.util.Comparator;
import java.util.List;

/**
 * Compares client decorators based on a list of known ids. Known client decorators
 * are smaller than unknown ones. When two client decorators that both are known are
 * compared, then the one that comes first in the list is the smallest.
 */
public class ListBasedComparator implements Comparator<ClientDecorator.Id> {

  private static final int EQUAL = 0;
  private static final int LEFT_SMALLER = -1;
  private static final int RIGHT_SMALLER = 1;

  private final List<ClientDecorator.Id> specifiedIds;

  public ListBasedComparator(List<ClientDecorator.Id> specifiedIds) {
    this.specifiedIds = specifiedIds;
  }

  @Override
  public int compare(ClientDecorator.Id left, ClientDecorator.Id right) {
    int leftIndex = specifiedIds.indexOf(left);
    int rightIndex = specifiedIds.indexOf(right);

    if (unknown(leftIndex) && unknown(rightIndex)) {
      return EQUAL;
    }

    if (unknown(leftIndex)) {
      return RIGHT_SMALLER;
    }

    if (unknown(rightIndex)) {
      return LEFT_SMALLER;
    }

    return Ints.compare(leftIndex, rightIndex);
  }

  private boolean unknown(int index) {
    return index < 0;
  }
}
