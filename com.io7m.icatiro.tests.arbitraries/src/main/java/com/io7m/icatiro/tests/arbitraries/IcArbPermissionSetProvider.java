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

import com.io7m.icatiro.model.IcPermissionGlobal;
import com.io7m.icatiro.model.IcPermissionProjectwide;
import com.io7m.icatiro.model.IcPermissionScopedType;
import com.io7m.icatiro.model.IcPermissionSet;
import com.io7m.icatiro.model.IcPermissionTicketwide;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;

/**
 * A provider of permission set values.
 */

public final class IcArbPermissionSetProvider extends IcArbAbstractProvider
{
  /**
   * A provider of permission set values.
   */

  public IcArbPermissionSetProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IcPermissionSet.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      Arbitraries.defaultFor(IcPermissionScopedType.class)
        .set()
        .map(IcArbPermissionSetProvider::buildSet)
    );
  }

  private static IcPermissionSet buildSet(
    final Set<IcPermissionScopedType> permissions)
  {
    final var builder = IcPermissionSet.builder();
    for (final var p : permissions) {
      if (p instanceof IcPermissionGlobal global) {
        builder.addGlobal(global);
        continue;
      }
      if (p instanceof IcPermissionProjectwide pw) {
        builder.addProjectwide(pw);
        continue;
      }
      if (p instanceof IcPermissionTicketwide tw) {
        builder.addTicketwide(tw);
        continue;
      }
    }
    return builder.build();
  }
}
