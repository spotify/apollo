package com.spotify.apollo.core;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class ServicesTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Test()
  public void testCtor() throws Exception {
    expectedException.expect(InvocationTargetException.class);
    expectedException.expectCause(is(Matchers.<Throwable>instanceOf(IllegalAccessError.class)));

    Constructor<Services> constructor = Services.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void testUsingName() throws Exception {
    Service service = Services.usingName("test").build();

    assertThat(service.getServiceName(), is("test"));
  }

  @Test
  public void testRun() throws Exception {
    String[] args = new String[]{"a", "b", "c"};
    Service service = mock(Service.class);
    Service.Instance instance = mock(Service.Instance.class);

    when(service.start(args)).thenReturn(instance);

    Services.run(service, args);

    InOrder inOrder = inOrder(service, instance);
    inOrder.verify(service, times(1)).start(args);
    inOrder.verify(instance, times(1)).waitForShutdown();
    inOrder.verify(instance, atLeastOnce()).close();
  }
}
