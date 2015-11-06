/*
 * Copyright © 2014 Spotify AB
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
