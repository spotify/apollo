package com.spotify.apollo.test.unit;

import com.spotify.apollo.StatusType;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.Matchers.equalTo;

/**
 * Provides Hamcrest matcher utilities for matching {@link StatusType}.
 */
public final class StatusTypeMatchers {

  private StatusTypeMatchers() {
    //Prevent instantiation
  }

  /**
   * Builds a matcher for {@link StatusType}s belonging to a {@link StatusType.Family}.
   * @param family The {@link StatusType.Family} to match.
   * @return A matcher
   */
  public static Matcher<StatusType> belongsToFamily(StatusType.Family family) {
    return new FeatureMatcher<StatusType, StatusType.Family>(equalTo(family),
                                                            "a status type belonging to family",
                                                            "family") {
      @Override
      protected StatusType.Family featureValueOf(StatusType item) {
        return item.family();
      }
    };
  }

  /**
   * Builds a matcher for {@link StatusType}s with matching reason phrase.
   * @param reasonPhraseMatcher {@link Matcher} for the reason phrase.
   * @return A matcher
   */
  public static Matcher<StatusType> withReasonPhrase(Matcher<String> reasonPhraseMatcher) {
    return new FeatureMatcher<StatusType, String>(reasonPhraseMatcher,
                                                  "a status type with reason phrase matching",
                                                  "reason phrase") {
      @Override
      protected String featureValueOf(StatusType item) {
        return item.reasonPhrase();
      }
    };
  }

  /**
   * Builds a matcher for {@link StatusType}s with specified status code.
   * @param code The status code to match.
   * @return A matcher
   */
  public static Matcher<StatusType> withCode(int code) {
    return new TypeSafeMatcher<StatusType>() {
      @Override
      protected boolean matchesSafely(StatusType item) {
        return item.code() == code;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a status type with status code equals to ").appendValue(code);
      }

      @Override
      protected void describeMismatchSafely(StatusType item, Description mismatchDescription) {
        mismatchDescription.appendText("the status code was ").appendValue(item.code());
      }
    };
  }

  /**
   * Builds a matcher for {@link StatusType}s whose {@link StatusType#code()} matches the
   * specified {@link StatusType}'s status code. Reason phrases are not included in the comparison.
   *
   * @param code The StatusType whose code should match.
   */
  public static Matcher<StatusType> withCode(StatusType code) {
    return new TypeSafeMatcher<StatusType>() {
      @Override
      protected boolean matchesSafely(StatusType item) {
        return item.code() == code.code();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a status type with status code equals to ").appendValue(code.code());
      }

      @Override
      protected void describeMismatchSafely(StatusType item, Description mismatchDescription) {
        mismatchDescription.appendText("the status code was ").appendValue(item.code());
      }
    };
  }
}
