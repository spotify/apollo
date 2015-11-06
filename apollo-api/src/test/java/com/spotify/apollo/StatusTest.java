package com.spotify.apollo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class StatusTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldAllowSettingReasonPhrase() throws Exception {
    assertThat(Status.OK.withReasonPhrase("this is toe-dally OK").reasonPhrase(),
               equalTo("this is toe-dally OK"));
  }

  @Test
  public void shouldReplaceNewlineInReasonPhrase() throws Exception {
    StatusType statusType = Status.ACCEPTED.withReasonPhrase("with\nnewline");
    assertThat(statusType.reasonPhrase(), is("with newline"));
  }

  @Test
  public void shouldReplaceCRInReasonPhrase() throws Exception {
    StatusType statusType = Status.ACCEPTED.withReasonPhrase("with\rcarriage return");
    assertThat(statusType.reasonPhrase(), is("with carriage return"));
  }

  @Test
  public void shouldReplaceControlCharsInReasonPhrase() throws Exception {
    StatusType statusType = Status.ACCEPTED.withReasonPhrase("with control\7");
    assertThat(statusType.reasonPhrase(), is("with control "));
  }
}