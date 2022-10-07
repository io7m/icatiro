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


package com.io7m.icatiro.server.internal;

import com.io7m.icatiro.database.api.IcDatabaseConnectionType;
import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.model.IcPermissionSet;
import com.io7m.icatiro.model.IcUser;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;

/**
 * Functions to update users.
 */

public final class IcUserUpdates
{
  private IcUserUpdates()
  {

  }

  /**
   * Merge the given user with the user in the database.
   *
   * @param database The database
   * @param user     The user
   *
   * @return The merged user
   *
   * @throws IcDatabaseException On errors
   */

  public static IcUser userMerge(
    final IcDatabaseType database,
    final IcUser user)
    throws IcDatabaseException
  {
    try (var connection = database.openConnection(ICATIRO)) {
      return userMerge(connection, user);
    }
  }

  /**
   * Merge the given user with the user in the database.
   *
   * @param connection The database connection
   * @param user       The user
   *
   * @return The merged user
   *
   * @throws IcDatabaseException On errors
   */

  public static IcUser userMerge(
    final IcDatabaseConnectionType connection,
    final IcUser user)
    throws IcDatabaseException
  {
    try (var transaction = connection.openTransaction()) {
      final var r = userMerge(transaction, user);
      transaction.commit();
      return r;
    }
  }

  /**
   * Merge the given user with the user in the database.
   *
   * @param transaction The database transaction
   * @param user        The user
   *
   * @return The merged user
   *
   * @throws IcDatabaseException On errors
   */

  public static IcUser userMerge(
    final IcDatabaseTransactionType transaction,
    final IcUser user)
    throws IcDatabaseException
  {
    final var users =
      transaction.queries(IcDatabaseUsersQueriesType.class);
    final var existingOpt =
      users.userGet(user.id());

    if (existingOpt.isPresent()) {
      final var existing = existingOpt.get();
      final var merged =
        new IcUser(
          user.id(),
          user.name(),
          user.emails(),
          existing.permissions()
        );
      users.userPut(merged);
      return merged;
    }

    final var merged =
      new IcUser(
        user.id(),
        user.name(),
        user.emails(),
        IcPermissionSet.empty()
      );
    users.userPut(merged);
    return merged;
  }
}
