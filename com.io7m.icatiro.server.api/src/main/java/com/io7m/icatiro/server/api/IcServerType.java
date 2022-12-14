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

package com.io7m.icatiro.server.api;

import com.io7m.icatiro.database.api.IcDatabaseType;

import java.util.UUID;

/**
 * A server instance.
 */

public interface IcServerType extends AutoCloseable
{
  /**
   * Start the server instance.
   *
   * @throws IcServerException On errors
   */

  void start()
    throws IcServerException;

  /**
   * @return The server's database instance
   */

  IcDatabaseType database();

  @Override
  void close()
    throws IcServerException;

  /**
   * Set the given user as the initial user.
   *
   * @param userId The user ID
   *
   * @throws IcServerException On errors
   */

  void userInitialSet(UUID userId)
    throws IcServerException;

  /**
   * Unset the given user as the initial user.
   *
   * @param userId The user ID
   *
   * @throws IcServerException On errors
   */

  void userInitialUnset(UUID userId)
    throws IcServerException;
}
