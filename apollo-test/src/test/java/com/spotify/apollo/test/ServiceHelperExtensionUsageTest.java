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

import static org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor.ENGINE_ID;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

class ServiceHelperExtensionUsageTest {

  @AfterEach
  void afterEach() {
    ServiceHelperCorrectUsage.resetAppInitInvocationCount();
  }

  @Test
  void shouldFailToInstantiateTestClassWhenExtensionDeclaredUsingExtendWith() {
    EngineTestKit.engine(ENGINE_ID)
        .selectors(selectClass(ServiceHelperIncorrectUsage.class))
        .execute()
        .containers()
        .assertStatistics(stats -> stats.failed(1));
  }

  @Test
  void shouldExecuteTestsWhenExtensionDeclaredProgrammatically() {
    EngineTestKit.engine(ENGINE_ID)
        .selectors(selectClass(ServiceHelperCorrectUsage.class))
        .execute()
        .tests()
        .assertStatistics(stats -> stats.started(2).succeeded(2));
  }
}
