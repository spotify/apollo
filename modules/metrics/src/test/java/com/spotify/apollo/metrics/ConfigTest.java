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

import static org.junit.Assert.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ConfigTest {
  public static class FfwdSettings {
    @Test
    public void ffwdScheduleDefaultsToEvery39Seconds() {
      String json = "{}";

      FfwdConfig.Agent ffwdCompletelyEmpty = (FfwdConfig.Agent) FfwdConfig.fromConfig(conf(json));

      String ffwdjson = "{\"ffwd\":{}}";
      FfwdConfig.Agent ffwdEmpty = (FfwdConfig.Agent) FfwdConfig.fromConfig(conf(ffwdjson));

      assertEquals(Optional.empty(), ffwdCompletelyEmpty.getHost());
      assertEquals(Optional.empty(), ffwdCompletelyEmpty.getPort());
      assertEquals(Boolean.TRUE, ffwdCompletelyEmpty.getFlush());
      assertEquals(30, ffwdCompletelyEmpty.getInterval());
      assertEquals(30, ffwdEmpty.getInterval());
    }

    @Test
    public void agentConfigNoFlush() {
      String ffwdjson = "{\"ffwd\":{\"flush\":\"false\"}}";
      FfwdConfig.Agent ffwdFlush = (FfwdConfig.Agent) FfwdConfig.fromConfig(conf(ffwdjson));

      assertEquals(30, ffwdFlush.getInterval());
      assertEquals(Boolean.FALSE, ffwdFlush.getFlush());
    }

    @Test
    public void ffwdScheduleCanBeSetFromConfig() {
      String json = "{\"ffwd\":{ \"interval\": 15}}";
      FfwdConfig.Agent ffwd = (FfwdConfig.Agent) FfwdConfig.fromConfig(conf(json));

      assertEquals(15, ffwd.getInterval());
    }
  }

  @Test
  public void ffwdHttpClassesAvailable() throws ClassNotFoundException {
    Class.forName(com.spotify.ffwd.http.google.common.base.Ascii.class.getName());
  }

  private static Config conf(String json) {
    return ConfigFactory.parseString(json);
  }
}
