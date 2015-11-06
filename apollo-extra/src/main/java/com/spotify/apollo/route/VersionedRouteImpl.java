package com.spotify.apollo.route;

import com.google.auto.value.AutoValue;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@AutoValue
abstract class VersionedRouteImpl implements VersionedRoute {

  @Override
  public VersionedRoute validFrom(int validFrom) {
    checkArgument(validFrom >= 0, "validFrom must be non-negative, got %s", validFrom);
    verifyRangeNonEmpty(validFrom, removedIn());

    return new AutoValue_VersionedRouteImpl(route(), validFrom, removedIn());
  }

  @Override
  public VersionedRoute removedIn(int removedIn) {
    checkArgument(removedIn >= 0, "removedIn must be non-negative, got %s", removedIn);
    verifyRangeNonEmpty(validFrom(), Optional.of(removedIn));

    return new AutoValue_VersionedRouteImpl(route(), validFrom(), Optional.of(removedIn));
  }

  private void verifyRangeNonEmpty(int validFrom, Optional<Integer> removedIn) {
    int effectiveRemovedIn = removedIn.orElse(validFrom + 1);
    checkArgument(validFrom < effectiveRemovedIn,
                  "empty version range: [%s, %s)", validFrom, effectiveRemovedIn);
  }
}
