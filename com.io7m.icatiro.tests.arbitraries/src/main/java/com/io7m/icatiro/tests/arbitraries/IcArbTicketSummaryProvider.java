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

package com.io7m.icatiro.tests.arbitraries;

import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketID;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcTicketTitle;
import com.io7m.idstore.model.IdName;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link IcTicketSummary} values.
 */

public final class IcArbTicketSummaryProvider extends IcArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IcArbTicketSummaryProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IcTicketSummary.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    final var t0 =
      Arbitraries.defaultFor(IcTicketID.class);
    final var a1 =
      Arbitraries.strings();
    final var a2 =
      Arbitraries.strings()
        .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        .ofMinLength(1)
        .ofMaxLength(15);
    final var a3 =
      Arbitraries.longs();
    final var a4 =
      Arbitraries.strings();
    final var a5 =
      Arbitraries.defaultFor(OffsetDateTime.class);
    final var a6 =
      Arbitraries.defaultFor(IdName.class);
    final var a7 =
      Arbitraries.defaultFor(UUID.class);

    final Arbitrary<IcTicketSummary> a =
      Combinators.combine(t0, a1, a2, a3, a4, a5, a6, a7)
        .as((s0, s1, s2, s3, s4, s5, s6, s7) -> {
          return new IcTicketSummary(
            new IcProjectTitle(s1),
            new IcProjectShortName(s2),
            s0,
            new IcTicketTitle(s4),
            s5,
            s5,
            s7,
            s6
          );
        });

    return Set.of(a);
  }
}
