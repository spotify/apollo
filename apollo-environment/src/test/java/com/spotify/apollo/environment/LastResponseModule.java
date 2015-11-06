package com.spotify.apollo.environment;

import com.google.inject.multibindings.Multibinder;

import com.spotify.apollo.Response;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.request.EndpointRunnableFactory;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import okio.ByteString;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.GONE;

/**
 * A module used for testing. It remembers the last response of any matching endpoint as a String
 * and always replies to the incoming request with an empty GONE status.
 */
class LastResponseModule extends AbstractApolloModule
    implements EndpointRunnableFactoryDecorator {

  private final AtomicReference<String> lastResponseRef;

  LastResponseModule(AtomicReference<String> lastResponseRef) {
    this.lastResponseRef = lastResponseRef;
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), EndpointRunnableFactoryDecorator.class)
        .addBinding().toInstance(this);
  }

  @Override
  public String getId() {
    return "other";
  }

  @Override
  public EndpointRunnableFactory apply(EndpointRunnableFactory ignored) {
    return (request, requestContext, endpoint) -> {

      // we'll intercept and do the endpoint call
      Response<ByteString> response = getUnchecked(endpoint.invoke(requestContext));
      Optional<ByteString> payload = response.payload();
      if (payload.isPresent()) {
        lastResponseRef.set(payload.get().utf8());
      }

      // return a GONE response
      return () -> request.reply(forStatus(GONE));
    };
  }

  private static <T> T getUnchecked(CompletionStage<T> stage) {
    try {
      return stage.toCompletableFuture().get();
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
