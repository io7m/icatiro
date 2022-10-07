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

import java.util.Objects;

/**
 * The unique ID of a ticket.
 *
 * @param project The project ID
 * @param value   The value
 */

public record IcTicketID(
  IcProjectID project,
  long value)
  implements Comparable<IcTicketID>, IcAccessControlledType
{
  /**
   * The unique ID of a ticket.
   *
   * @param project The project ID
   * @param value   The value
   */

  public IcTicketID
  {
    Objects.requireNonNull(project, "project");
  }

  @Override
  public String toString()
  {
    return "%s-%s".formatted(
      this.project.toString(),
      Long.toUnsignedString(this.value)
    );
  }

  @Override
  public int compareTo(
    final IcTicketID other)
  {
    final var pcmp = this.project.compareTo(other.project);
    if (pcmp == 0) {
      return Long.compareUnsigned(this.value, other.value);
    }
    return pcmp;
  }

  @Override
  public String objectType()
  {
    return "ticket";
  }
}
