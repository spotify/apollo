/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.meta.model.benchmark;

import com.google.common.collect.Sets;

import com.spotify.apollo.meta.model.Model;
import com.spotify.apollo.meta.model.Meta;
import com.spotify.apollo.meta.model.MetaGatherer;
import com.spotify.apollo.meta.model.MetaInfoBuilder;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

@State(Scope.Thread)
public class GathererBenchmark {

  @State(Scope.Benchmark)
  public static class Shared {
    private MetaGatherer gatherer;

    @Setup
    public void init() {
      Model.MetaInfo metaInfo = new MetaInfoBuilder()
          .buildVersion("benchmark 0.0.0")
          .containerVersion("main 0.0.0")
          .build();

      gatherer = Meta.createGatherer(metaInfo);
    }
  }

  String fromService = "ap";
  String uri = "http://some/uri";
  String method = "GET";
  String endpointMethodName = "handleGet";
  Set<String> parameterKeys =
      Sets.newLinkedHashSet(Arrays.asList("foo", "bar"));

  Set<String> outgoingServices =
      Sets.newLinkedHashSet(Arrays.asList("metadata", "search", "suggest", "browse"));

  public static void main(String[] args) throws IOException, RunnerException {
    Options opt = new OptionsBuilder()
        .include(GathererBenchmark.class.getSimpleName())
        .warmupIterations(5)
        .measurementIterations(10)
        .threads(1)
        .forks(1)
        .build();

    new Runner(opt).run();
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void doRequest(Shared shared) {
    final MetaGatherer gatherer = shared.gatherer;

    // account incoming call
    MetaGatherer.CallsGatherer callsGatherer =
        gatherer.getIncomingCallsGatherer(fromService);
    MetaGatherer.EndpointGatherer endpointGatherer =
        callsGatherer.namedEndpointGatherer(endpointMethodName);

    endpointGatherer.setUri(uri);
    endpointGatherer.addMethod(method);
    for (String name : parameterKeys) {
      endpointGatherer.addQueryParameterName(name);
    }

    // account outgoing calls
    for (String outgoingService : outgoingServices) {
      gatherer.getOutgoingCallsGatherer(outgoingService);
    }
  }
}
