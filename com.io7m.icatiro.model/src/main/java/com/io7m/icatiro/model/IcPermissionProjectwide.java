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

import java.util.Comparator;
import java.util.Objects;

/**
 * A projectwide applicable permission.
 *
 * @param projectId  The project ID
 * @param permission The permission
 */

public record IcPermissionProjectwide(
  IcProjectID projectId,
  IcPermission permission)
  implements IcPermissionScopedType, Comparable<IcPermissionProjectwide>
{
  /**
   * A projectwide applicable permission.
   *
   * @param projectId  The project ID
   * @param permission The permission
   */

  public IcPermissionProjectwide
  {
    Objects.requireNonNull(projectId, "projectId");
    Objects.requireNonNull(permission, "permission");
  }

  @Override
  public int compareTo(
    final IcPermissionProjectwide other)
  {
    return Comparator.comparing(IcPermissionProjectwide::projectId)
      .thenComparing(IcPermissionProjectwide::permission)
      .compare(this, other);
  }
}
