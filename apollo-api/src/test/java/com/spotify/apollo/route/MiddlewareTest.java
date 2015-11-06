package com.spotify.apollo.route;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MiddlewareTest {

  @Test
  public void shouldCompose() throws Exception {
    Middleware<String, Integer> a = s -> s.length();
    Middleware<Integer, Boolean> b = i -> i % 2 == 0;

    assertEquals(4, a.apply("four").intValue());
    assertTrue(b.apply(4));
    assertFalse(b.apply(5));

    Middleware<String, Boolean> composed = a.and(b);
    assertTrue(composed.apply("even"));
    assertTrue(composed.apply("evener"));
    assertFalse(composed.apply("odd"));
    assertFalse(composed.apply("odder"));
  }
}
