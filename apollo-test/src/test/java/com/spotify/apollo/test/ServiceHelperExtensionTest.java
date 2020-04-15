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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ServiceHelperExtensionTest {

  @RegisterExtension
  static final ServiceHelperExtension serviceHelperExtension =
      ServiceHelperExtension.create(env -> {}, "test-service")
          .conf("test.string", "value")
          .conf("test.integer", 1);

  @Test
  void shouldPropagateStringConfigValue(ServiceHelper serviceHelper) {
    assertThat(serviceHelper.getInstance().getConfig().getString("test.string"), is("value"));
  }

  @Test
  void shouldPropagateIntegerConfigValue(ServiceHelper serviceHelper) {
    assertThat(serviceHelper.getInstance().getConfig().getInt("test.integer"), is(1));
  }
}
