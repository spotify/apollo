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
      assertEquals(30, ffwdCompletelyEmpty.getInterval());
      assertEquals(30, ffwdEmpty.getInterval());
    }

    @Test
    public void httpConfig() {
      String json =
          "{\"ffwd\":{\"type\":\"http\",\"discovery\":{\"type\":\"srv\",\"record\":\"hello\"}}}";
      FfwdConfig config = FfwdConfig.fromConfig(conf(json));

      assertEquals(FfwdConfig.Http.class, config.getClass());

      final FfwdConfig.Http http = (FfwdConfig.Http) config;

      assertEquals(30, http.getInterval());
    }

    @Test
    public void ffwdScheduleCanBeSetFromConfig() {
      String json = "{\"ffwd\":{ \"interval\": 15}}";
      FfwdConfig.Agent ffwd = (FfwdConfig.Agent) FfwdConfig.fromConfig(conf(json));

      assertEquals(15, ffwd.getInterval());
    }
  }

  public static class DiscoveryConfigTest {
    @Test
    public void testSrv() {
      final DiscoveryConfig discovery =
          DiscoveryConfig.fromConfig(conf("{\"type\":\"srv\",\"record\":\"foo\"}"));

      assertEquals(DiscoveryConfig.Srv.class, discovery.getClass());
      final DiscoveryConfig.Srv srv = (DiscoveryConfig.Srv) discovery;

      assertEquals("foo", srv.getRecord());
    }

    @Test(expected = ConfigException.Missing.class)
    public void testSrvMissingRecord() {
      DiscoveryConfig.fromConfig(conf("{\"type\":\"srv\"}"));
    }

    @Test
    public void testStatic() {
      final DiscoveryConfig discovery =
          DiscoveryConfig.fromConfig(conf("{\"type\":\"static\",\"host\":\"foo\",\"port\":12345}"));

      assertEquals(DiscoveryConfig.Static.class, discovery.getClass());
      final DiscoveryConfig.Static st = (DiscoveryConfig.Static) discovery;

      assertEquals("foo", st.getHost());
      assertEquals(12345, st.getPort());
    }

    @Test(expected = ConfigException.Missing.class)
    public void testStaticMissingHost() {
      DiscoveryConfig.fromConfig(conf("{\"type\":\"static\",\"port\":12345}"));
    }

    @Test(expected = ConfigException.Missing.class)
    public void testStaticMissingPort() {
      DiscoveryConfig.fromConfig(conf("{\"type\":\"static\",\"host\":\"foo\"}"));
    }
  }

  private static Config conf(String json) {
    return ConfigFactory.parseString(json);
  }
}
