/*-
 * -\-\-
 * Spotify Apollo Service Core (aka Leto)
 * --
 * Copyright (C) 2013 - 2021 Spotify AB
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

package com.spotify.apollo.core;

import com.google.inject.Injector;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestSpringBootApplication {

  private final Injector injector;

  public static Service.Instance instance;

  public TestSpringBootApplication(Injector injector) {
    this.injector = injector;
  }


  public static void main(String[] args, Map<String, String> env) throws IOException {
    Service service = ServiceImpl.builder("test").build();
    instance = service.start(args, env);
  }
}
