/*
 * -\-\-
 * Spotify Apollo Metrics Module
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
package com.spotify.apollo.metrics;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class ConfigTest {

  public static class FfwdSettings {
    @Test
    public void ffwdScheduleDefaultsToEvery39Seconds() {
      String json = "{}";
      FfwdConfig ffwdCompletelyEmpty = new FfwdConfig(conf(json));

      String ffwdjson = "{\"ffwd\":{}}";
      FfwdConfig ffwdEmpty = new FfwdConfig(conf(ffwdjson));

      assertEquals(30, ffwdCompletelyEmpty.getInterval());
      assertEquals(30, ffwdEmpty.getInterval());
    }

    @Test
    public void ffwdScheduleCanBeSetFromConfig() {
      String json = "{\"ffwd\":{ \"interval\": 15}}";
      FfwdConfig ffwd = new FfwdConfig(conf(json));

      assertEquals(15, ffwd.getInterval());
    }
  }

  private static Config conf(String json) {
    return ConfigFactory.parseString(json);
  }
}
