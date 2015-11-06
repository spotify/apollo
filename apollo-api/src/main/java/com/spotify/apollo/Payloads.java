/**
 * Copyright (c) 2014 Spotify AB
 */
package com.spotify.apollo;

import com.spotify.apollo.route.Middleware;

import java.util.Objects;
import java.util.Optional;

import okio.ByteString;

import static com.spotify.apollo.Serializer.Payload;

/**
 * Factory for creating {@link Payload}
 *
 * @deprecated the new preferred way to do response serialization is
 * via a {@link Middleware}, see {@link Serializer}.
 */
@Deprecated
public final class Payloads {

  private Payloads() {
    // no instantiation
  }

  /**
   * Create a {@link Payload} from ByteString without contentType
   */
  public static Payload create(ByteString byteString) {
    return new PayloadImpl(byteString, Optional.empty());
  }

  /**
   * Create a {@link Payload} from ByteString with the
   * specified contentType
   */
  public static Payload create(ByteString byteString, String contentType) {
    return new PayloadImpl(byteString, Optional.ofNullable(contentType));
  }


  /**
   * Create a {@link Payload} from ByteString with an
   * optional contentType
   */
  public static Payload create(ByteString byteString, Optional<String> contentType) {
    return new PayloadImpl(byteString, contentType);
  }

  static class PayloadImpl implements Payload {

    private final ByteString byteString;
    private final Optional<String> contentType;

    private PayloadImpl(ByteString byteString, Optional<String> contentType) {
      this.byteString = Objects.requireNonNull(byteString);
      this.contentType = Objects.requireNonNull(contentType);
    }

    @Override
    public ByteString byteString() {
      return byteString;
    }

    @Override
    public Optional<String> contentType() {
      return contentType;
    }

    @Override
    public Payload withContentTypeIfAbsent(String contentType) {
      return create(byteString, this.contentType.orElse(contentType));
    }
  }
}
