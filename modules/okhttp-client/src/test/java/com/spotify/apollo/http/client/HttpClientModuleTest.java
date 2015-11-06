package com.spotify.apollo.http.client;

import com.google.inject.Injector;
import com.google.inject.Key;

import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.environment.ClientDecorator;

import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class HttpClientModuleTest {

  Service service() {
    return Services.usingName("ping")
        .withModule(HttpClientModule.create())
        .build();
  }

  @Test
  public void testShouldInsertHttpClientDecorator() throws Exception {
    try (Service.Instance i = service().start()) {
      final Set<ClientDecorator> clientDecorators =
          i.resolve(Injector.class).getInstance(new Key<Set<ClientDecorator>>() {
          });
      assertThat(clientDecorators, hasItem(instanceOf(HttpClientDecorator.class)));
    }
  }

}
