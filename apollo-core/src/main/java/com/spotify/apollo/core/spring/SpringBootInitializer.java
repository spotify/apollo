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

package com.spotify.apollo.core.spring;

import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.List;
import org.springframework.boot.builder.SpringApplicationBuilder;

public class SpringBootInitializer {

  public Injector initialize(String serviceName,
                             Iterable<Module> allModules,
                             List<Class<?>> springBootAnnotatedClasses) {
    final SpringApplicationBuilder springApplicationBuilder;
    springApplicationBuilder = new SpringApplicationBuilder()
        .properties(
            //Prefer Spring bean if present in both Guice and application context
            "spring.guice.dedup=true",
            //Do not create default instances if there is no instance in the context
            "spring.guice.autowireJIT=false",
            //Allow overriding of bean definitions from different modules
            "spring.main.allow-bean-definition-overriding=true")
        .sources(GuiceSpringBridge.class);
    springBootAnnotatedClasses.forEach(
        springApplicationBuilder::sources
    );

    springApplicationBuilder.initializers(new ApolloInitializer(serviceName, allModules));

    springApplicationBuilder.run();
    return springApplicationBuilder.context().getBean(Injector.class);
  }
}
