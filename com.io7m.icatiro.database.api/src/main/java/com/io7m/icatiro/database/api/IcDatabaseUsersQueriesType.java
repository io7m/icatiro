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

package com.io7m.icatiro.database.api;

import com.io7m.icatiro.model.IcUser;

import java.util.Optional;
import java.util.UUID;

/**
 * The database queries involving users.
 */

public non-sealed interface IcDatabaseUsersQueriesType
  extends IcDatabaseQueriesType
{
  /**
   * Update the given user.
   *
   * @param user The user
   *
   * @throws IcDatabaseException On errors
   */

  void userPut(IcUser user)
    throws IcDatabaseException;

  /**
   * @param id The user ID
   *
   * @return The user
   *
   * @throws IcDatabaseException On errors
   */

  Optional<IcUser> userGet(UUID id)
    throws IcDatabaseException;

  /**
   * @param id The user ID
   *
   * @return The user
   *
   * @throws IcDatabaseException On errors
   */

  IcUser userGetRequire(UUID id)
    throws IcDatabaseException;

  /**
   * Set the user as the initial user. Only one user can be the initial user.
   *
   * @param id The user ID
   *
   * @throws IcDatabaseException On errors
   */

  void userInitialSet(UUID id)
    throws IcDatabaseException;

  /**
   * @return The initial user
   *
   * @throws IcDatabaseException On errors
   */

  Optional<UUID> userInitial()
    throws IcDatabaseException;

  /**
   * Unset the user as the initial user. Only one user can be the initial user.
   *
   * @param id The user ID
   *
   * @throws IcDatabaseException On errors
   */

  void userInitialUnset(UUID id)
    throws IcDatabaseException;
}
