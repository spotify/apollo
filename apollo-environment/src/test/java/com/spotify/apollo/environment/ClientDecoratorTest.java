/*
 * Copyright (c) 2015 Spotify AB
 */

package com.spotify.apollo.environment;

import com.google.inject.multibindings.Multibinder;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.module.AbstractApolloModule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static java.util.Optional.empty;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ClientDecoratorTest {

  Service.Builder service;

  @Mock IncomingRequestAwareClient mockClient;

  @Before
  public void setUp() throws Exception {
    ClientDecorator clientDecorator = c -> mockClient;

    service = Services.usingName("ping")
        .withModule(new DecoratingModule(clientDecorator))
        .withModule(ApolloEnvironmentModule.create());
  }

  @Test
  public void shouldBindDecoratedClient() throws Exception {
    try (Service.Instance i = service.build().start()) {
      Client client = ApolloEnvironmentModule.environment(i).environment().client();
      Request request = Request.forUri("http://example.com/");
      Request withService = request.withService("ping");

      client.send(request);
      verify(mockClient).send(eq(withService), eq(empty()));
    } catch (IOException e) {
      fail();
    }
  }

  private static class DecoratingModule extends AbstractApolloModule {

    private final ClientDecorator clientDecorator;

    private DecoratingModule(ClientDecorator clientDecorator) {
      this.clientDecorator = clientDecorator;
    }

    @Override
    protected void configure() {
      Multibinder.newSetBinder(binder(), ClientDecorator.class)
          .addBinding().toInstance(clientDecorator);
    }

    @Override
    public String getId() {
      return "test-decorator";
    }
  }
}
