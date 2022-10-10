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

/**
 * A permission to perform an operation.
 */

public enum IcPermission
{
  /**
   * A ticket can be read.
   */

  TICKET_READ(1),

  /**
   * A ticket can be created.
   */

  TICKET_CREATE(2),

  /**
   * A ticket can be written.
   */

  TICKET_WRITE(3),

  /**
   * A ticket can be commented on.
   */

  TICKET_COMMENT(4),

  /**
   * A project can be created.
   */

  PROJECT_CREATE(5);

  private final int value;

  IcPermission(
    final int inValue)
  {
    this.value = inValue;
  }

  /**
   * @param x The integer
   *
   * @return The permission value
   */

  public static IcPermission ofInteger(
    final int x)
  {
    for (final var value : values()) {
      if (value.value == x) {
        return value;
      }
    }

    throw new IllegalArgumentException(
      "Unrecognized permission integer: %d".formatted(x)
    );
  }

  /**
   * @return The integer value of the enumeration
   */

  public int value()
  {
    return this.value;
  }
}
