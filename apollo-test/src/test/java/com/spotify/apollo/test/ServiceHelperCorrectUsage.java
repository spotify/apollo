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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.spotify.apollo.Environment;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ServiceHelperCorrectUsage {

  @RegisterExtension
  static final ServiceHelperExtension serviceHelperExtension =
      ServiceHelperExtension.create(ServiceHelperCorrectUsage::appInit, "test-service");

  private static final AtomicInteger appInitInvocationCount = new AtomicInteger();

  @Test
  void testWithAccessor() {
    assertStarted(serviceHelperExtension.getServiceHelper());
  }

  @Test
  void testWithParameter(ServiceHelper serviceHelper) {
    assertStarted(serviceHelper);
  }

  static int getAppInitInvocationCount() {
    return appInitInvocationCount.get();
  }

  static void resetAppInitInvocationCount() {
    appInitInvocationCount.set(0);
  }

  private void assertStarted(ServiceHelper serviceHelper) {
    assertNotNull(serviceHelper);
    assertNotNull(serviceHelper.getInstance());
  }

  private static void appInit(Environment environment) {
    assertNotNull(environment);
    appInitInvocationCount.incrementAndGet();
  }
}
