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

package com.io7m.icatiro.client.api;

import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.error_codes.IcException;

import java.util.Objects;

/**
 * The type of client exceptions.
 */

public final class IcClientException extends IcException
{
  /**
   * Construct an exception.
   *
   * @param errorCode The error code
   * @param message   The message
   */

  public IcClientException(
    final IcErrorCode errorCode,
    final String message)
  {
    super(
      Objects.requireNonNull(errorCode, "errorCode"),
      Objects.requireNonNull(message, "message")
    );
  }

  /**
   * Construct an exception.
   *
   * @param errorCode The error code
   * @param message   The message
   * @param cause     The cause
   */

  public IcClientException(
    final IcErrorCode errorCode,
    final String message,
    final Throwable cause)
  {
    super(
      Objects.requireNonNull(errorCode, "errorCode"),
      Objects.requireNonNull(message, "message"),
      Objects.requireNonNull(cause, "cause")
    );
  }

  /**
   * Construct an exception.
   *
   * @param errorCode The error code
   * @param cause     The cause
   */

  public IcClientException(
    final IcErrorCode errorCode,
    final Throwable cause)
  {
    super(
      Objects.requireNonNull(errorCode, "errorCode"),
      Objects.requireNonNull(cause, "cause")
    );
  }
}
