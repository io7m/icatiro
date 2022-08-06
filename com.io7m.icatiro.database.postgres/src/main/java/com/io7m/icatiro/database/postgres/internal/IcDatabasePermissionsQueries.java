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

package com.io7m.icatiro.database.postgres.internal;

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabasePermissionsQueriesType;
import com.io7m.icatiro.model.IcPermission;
import com.io7m.icatiro.model.IcPermissionScopeType;
import com.io7m.icatiro.model.IcPermissionScopeType.Global;
import org.jooq.exception.DataAccessException;

import java.util.Objects;
import java.util.UUID;

import static com.io7m.icatiro.database.postgres.internal.IcDatabaseExceptions.handleDatabaseException;
import static com.io7m.icatiro.database.postgres.internal.IcDatabaseUsersQueries.USER_DOES_NOT_EXIST;
import static com.io7m.icatiro.database.postgres.internal.Tables.PERMISSIONS;
import static com.io7m.icatiro.database.postgres.internal.Tables.USERS;
import static com.io7m.icatiro.model.IcPermissionScopeType.Projectwide;
import static com.io7m.icatiro.model.IcPermissionScopeType.Ticketwide;
import static java.lang.Long.valueOf;

final class IcDatabasePermissionsQueries
  extends IcBaseQueries
  implements IcDatabasePermissionsQueriesType
{
  protected IcDatabasePermissionsQueries(
    final IcDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public void permissionSet(
    final UUID user,
    final IcPermissionScopeType scope,
    final IcPermission permission)
    throws IcDatabaseException
  {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(permission, "permission");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var userId = transaction.userId();

    try {
      context.fetchOptional(USERS, USERS.ID.eq(user))
        .orElseThrow(USER_DOES_NOT_EXIST);

      if (scope instanceof Global) {
        context.insertInto(PERMISSIONS)
          .set(PERMISSIONS.SCOPE_PROJECT, (Long) null)
          .set(PERMISSIONS.SCOPE_TICKET, (Long) null)
          .set(PERMISSIONS.USER_ID, user)
          .set(PERMISSIONS.PERMISSION, Integer.valueOf(permission.value()))
          .execute();
        return;
      }

      if (scope instanceof Projectwide projectwide) {
        context.insertInto(PERMISSIONS)
          .set(PERMISSIONS.SCOPE_PROJECT, valueOf(projectwide.id().value()))
          .set(PERMISSIONS.SCOPE_TICKET, (Long) null)
          .set(PERMISSIONS.USER_ID, user)
          .set(PERMISSIONS.PERMISSION, Integer.valueOf(permission.value()))
          .execute();
        return;
      }

      if (scope instanceof Ticketwide ticketwide) {
        context.insertInto(PERMISSIONS)
          .set(PERMISSIONS.SCOPE_PROJECT, (Long) null)
          .set(PERMISSIONS.SCOPE_TICKET, valueOf(ticketwide.id().value()))
          .set(PERMISSIONS.USER_ID, user)
          .set(PERMISSIONS.PERMISSION, Integer.valueOf(permission.value()))
          .execute();
        return;
      }

      throw new IllegalStateException();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public void permissionUnset(
    final UUID user,
    final IcPermissionScopeType scope,
    final IcPermission permission)
    throws IcDatabaseException
  {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(permission, "permission");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var userId = transaction.userId();

    try {
      context.fetchOptional(USERS, USERS.ID.eq(user))
        .orElseThrow(USER_DOES_NOT_EXIST);

      if (scope instanceof Global) {
        context.deleteFrom(PERMISSIONS)
          .where(
            PERMISSIONS.USER_ID.eq(user)
              .and(PERMISSIONS.PERMISSION.eq(
                Integer.valueOf(permission.value()))))
          .execute();
        return;
      }

      if (scope instanceof Projectwide projectwide) {
        context.deleteFrom(PERMISSIONS)
          .where(
            PERMISSIONS.USER_ID.eq(user)
              .and(PERMISSIONS.SCOPE_PROJECT.eq(valueOf(projectwide.id().value())))
              .and(PERMISSIONS.PERMISSION.eq(Integer.valueOf(permission.value()))))
          .execute();
        return;
      }

      if (scope instanceof Ticketwide ticketwide) {
        context.deleteFrom(PERMISSIONS)
          .where(
            PERMISSIONS.USER_ID.eq(user)
              .and(PERMISSIONS.SCOPE_TICKET.eq(valueOf(ticketwide.id().value())))
              .and(PERMISSIONS.PERMISSION.eq(Integer.valueOf(permission.value()))))
          .execute();
        return;
      }

      throw new IllegalStateException();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }
}
