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

import com.io7m.icatiro.model.IcAccessControlledType;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcTicketID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;

/**
 * A provider of access controlled object values.
 */

public final class IcArbAccessControlledObjectProvider extends IcArbAbstractProvider
{
  /**
   * A provider of access controlled object values.
   */

  public IcArbAccessControlledObjectProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IcAccessControlledType.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      projectIds(),
      ticketIds()
    );
  }

  private static Arbitrary<IcTicketID> ticketIds()
  {
    return Arbitraries.defaultFor(IcTicketID.class);
  }

  private static Arbitrary<IcProjectID> projectIds()
  {
    return Arbitraries.defaultFor(IcProjectID.class);
  }
}
