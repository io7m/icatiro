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

import com.io7m.icatiro.model.IcPermission;
import com.io7m.icatiro.model.IcPermissionGlobal;
import com.io7m.icatiro.model.IcPermissionProjectwide;
import com.io7m.icatiro.model.IcPermissionScopedType;
import com.io7m.icatiro.model.IcPermissionTicketwide;
import com.io7m.icatiro.model.IcTicketID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;

/**
 * A provider of scoped permission values.
 */

public final class IcArbPermissionScopedProvider extends IcArbAbstractProvider
{
  /**
   * A provider of scoped permission values.
   */

  public IcArbPermissionScopedProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IcPermissionScopedType.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      permissionsGlobal(),
      permissionsProjectwide(),
      permissionsTicketwide()
    );
  }

  private static Arbitrary<IcPermissionScopedType> permissionsTicketwide()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(IcTicketID.class),
      Arbitraries.defaultFor(IcPermission.class)
    ).as(IcPermissionTicketwide::new);
  }

  private static Arbitrary<IcPermissionScopedType> permissionsProjectwide()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(IcTicketID.class),
      Arbitraries.defaultFor(IcPermission.class)
    ).as((ticketId, permission) -> {
      return new IcPermissionProjectwide(ticketId.project(), permission);
    });
  }

  private static Arbitrary<IcPermissionScopedType> permissionsGlobal()
  {
    return Arbitraries.defaultFor(IcPermission.class)
      .map(IcPermissionGlobal::new);
  }
}
