/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.icatiro.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.io7m.icatiro.model.IcTicketColumn.BY_ID;
import static java.time.ZoneOffset.UTC;

/**
 * The immutable parameters required to list tickets.
 *
 * @param timeCreatedRange Only tickets created within this time range are
 *                         returned
 * @param timeUpdatedRange Only tickets updated within this time range are
 *                         returned
 * @param ordering         The ordering specification
 * @param limit            The limit on the number of returned tickets
 */

public record IcTicketListParameters(
  IcTimeRange timeCreatedRange,
  IcTimeRange timeUpdatedRange,
  IcTicketOrdering ordering,
  int limit)
{
  /**
   * The immutable parameters required to list tickets.
   *
   * @param timeCreatedRange Only tickets created within this time range are
   *                         returned
   * @param timeUpdatedRange Only tickets updated within this time range are
   *                         returned
   * @param ordering         The ordering specification
   * @param limit            The limit on the number of returned tickets
   */

  public IcTicketListParameters
  {
    Objects.requireNonNull(timeCreatedRange, "timeCreatedRange");
    Objects.requireNonNull(timeUpdatedRange, "timeUpdatedRange");
    Objects.requireNonNull(ordering, "ordering");
  }

  private static final OffsetDateTime DEFAULT_TIME_LOW =
    Instant.ofEpochSecond(0L).atOffset(UTC);

  /**
   * @return Reasonable default parameters
   */

  public static IcTicketListParameters defaults()
  {
    final var now = OffsetDateTime.now();
    return new IcTicketListParameters(
      new IcTimeRange(DEFAULT_TIME_LOW, now.plusDays(1L)),
      new IcTimeRange(DEFAULT_TIME_LOW, now.plusDays(1L)),
      new IcTicketOrdering(List.of(new IcTicketColumnOrdering(BY_ID, false))),
      20
    );
  }
}
