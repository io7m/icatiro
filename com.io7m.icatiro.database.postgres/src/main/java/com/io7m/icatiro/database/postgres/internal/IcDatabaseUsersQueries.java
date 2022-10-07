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
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.database.postgres.internal.tables.records.PermissionsRecord;
import com.io7m.icatiro.database.postgres.internal.tables.records.UsersRecord;
import com.io7m.icatiro.model.IcPermission;
import com.io7m.icatiro.model.IcPermissionGlobal;
import com.io7m.icatiro.model.IcPermissionProjectwide;
import com.io7m.icatiro.model.IcPermissionScopedType;
import com.io7m.icatiro.model.IcPermissionSet;
import com.io7m.icatiro.model.IcPermissionTicketwide;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcTicketID;
import com.io7m.icatiro.model.IcUser;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.io7m.icatiro.database.postgres.internal.IcDatabaseExceptions.handleDatabaseException;
import static com.io7m.icatiro.database.postgres.internal.Tables.EMAILS;
import static com.io7m.icatiro.database.postgres.internal.Tables.PERMISSIONS;
import static com.io7m.icatiro.database.postgres.internal.Tables.USERS;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_NONEXISTENT;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

final class IcDatabaseUsersQueries
  extends IcBaseQueries
  implements IcDatabaseUsersQueriesType
{
  static final Supplier<IcDatabaseException> USER_DOES_NOT_EXIST = () -> {
    return new IcDatabaseException(
      "User does not exist",
      USER_NONEXISTENT
    );
  };

  IcDatabaseUsersQueries(
    final IcDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public void userPut(
    final IcUser user)
    throws IcDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userPut");

    try {
      var userRec = context.fetchOne(USERS, USERS.ID.eq(user.id()));
      if (userRec == null) {
        userRec = context.newRecord(USERS);
        userRec.set(USERS.ID, user.id());
        userRec.set(USERS.INITIAL, FALSE);
      }
      userRec.setName(user.name().value());
      userRec.store();

      final var emailsThen =
        context.selectFrom(EMAILS)
          .where(EMAILS.USER_ID.eq(user.id()))
          .stream()
          .map(v -> new IdEmail(v.getEmailAddress()))
          .collect(Collectors.toUnmodifiableSet());

      final var emailsNow = new HashSet<>(user.emails());
      final var emailsToAdd = new HashSet<>(emailsNow);
      emailsToAdd.removeAll(emailsThen);
      final var emailsToRemove = new HashSet<>(emailsThen);
      emailsToRemove.removeAll(emailsNow);

      final var batches = new ArrayList<Query>();
      for (final var email : emailsToRemove) {
        final var condition =
          EMAILS.USER_ID.eq(user.id())
            .and(EMAILS.EMAIL_ADDRESS.eq(email.value()));
        batches.add(context.deleteFrom(EMAILS).where(condition));
      }
      for (final var email : emailsToAdd) {
        batches.add(
          context.insertInto(EMAILS)
            .set(EMAILS.USER_ID, user.id())
            .set(EMAILS.EMAIL_ADDRESS, email.value())
        );
      }

      batches.add(
        context.deleteFrom(PERMISSIONS)
          .where(PERMISSIONS.USER_ID.eq(user.id()))
      );

      for (final var permission : user.permissions().stream().toList()) {
        batches.add(permissionInsert(context, user.id(), permission));
      }

      context.batch(batches).execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  private static Query permissionInsert(
    final DSLContext context,
    final UUID userId,
    final IcPermissionScopedType permission)
  {
    final var icode = Integer.valueOf(permission.permission().value());
    if (permission instanceof IcPermissionGlobal global) {
      return context.insertInto(PERMISSIONS)
        .set(PERMISSIONS.USER_ID, userId)
        .set(PERMISSIONS.PERMISSION, icode)
        .set(PERMISSIONS.SCOPE_PROJECT, (Long) null)
        .set(PERMISSIONS.SCOPE_TICKET, (Long) null);
    }

    if (permission instanceof IcPermissionProjectwide projectwide) {
      final var projectId = projectwide.projectId();
      return context.insertInto(PERMISSIONS)
        .set(PERMISSIONS.USER_ID, userId)
        .set(PERMISSIONS.PERMISSION, icode)
        .set(PERMISSIONS.SCOPE_PROJECT, projectId.value())
        .set(PERMISSIONS.SCOPE_TICKET, (Long) null);
    }

    if (permission instanceof IcPermissionTicketwide ticketwide) {
      final var ticketId = ticketwide.ticketId();
      return context.insertInto(PERMISSIONS)
        .set(PERMISSIONS.USER_ID, userId)
        .set(PERMISSIONS.PERMISSION, icode)
        .set(PERMISSIONS.SCOPE_PROJECT, ticketId.project().value())
        .set(PERMISSIONS.SCOPE_TICKET, ticketId.value());
    }

    throw new IllegalStateException(
      "Unrecognized scoped permission: %s".formatted(permission)
    );
  }

  @Override
  public Optional<IcUser> userGet(
    final UUID id)
    throws IcDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userGet");

    try {
      final var userRec = context.fetchOne(USERS, USERS.ID.eq(id));
      if (userRec == null) {
        return Optional.empty();
      }

      final var emails =
        context.selectFrom(EMAILS)
          .where(EMAILS.USER_ID.eq(id))
          .stream()
          .map(v -> new IdEmail(v.getEmailAddress()))
          .toList();

      final var permissions =
        IcPermissionSet.of(
          context.selectFrom(PERMISSIONS)
            .where(PERMISSIONS.USER_ID.eq(id))
            .stream()
            .map(IcDatabaseUsersQueries::toPermissionScoped)
            .toList()
        );

      return Optional.of(
        new IcUser(
          id,
          new IdName(userRec.getName()),
          emails,
          permissions
        )
      );
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  private static IcPermissionScopedType toPermissionScoped(
    final PermissionsRecord p)
  {
    final var scopeProject =
      p.getScopeProject();
    final var scopeTicket =
      p.getScopeTicket();

    if (scopeProject != null) {
      if (scopeTicket != null) {
        return new IcPermissionTicketwide(
          new IcTicketID(
            new IcProjectID(scopeProject.longValue()),
            scopeTicket.longValue()
          ),
          toPermission(p.getPermission())
        );
      } else {
        return new IcPermissionProjectwide(
          new IcProjectID(scopeProject.longValue()),
          toPermission(p.getPermission())
        );
      }
    } else {
      return new IcPermissionGlobal(
        toPermission(p.getPermission())
      );
    }
  }

  private static IcPermission toPermission(
    final Integer permission)
  {
    return IcPermission.ofInteger(permission.intValue());
  }

  @Override
  public IcUser userGetRequire(
    final UUID id)
    throws IcDatabaseException
  {
    return this.userGet(id).orElseThrow(USER_DOES_NOT_EXIST);
  }

  @Override
  public void userInitialSet(
    final UUID id)
    throws IcDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userInitialSet");

    try {
      var userRec = context.fetchOne(USERS, USERS.ID.eq(id));
      if (userRec == null) {
        userRec = context.newRecord(USERS);
        userRec.set(USERS.ID, id);
        userRec.set(USERS.NAME, "Z");
      }
      userRec.set(USERS.INITIAL, TRUE);
      userRec.store();

      final var permissions =
        IcPermissionSet.of(
          Arrays.stream(IcPermission.values())
            .map(IcPermissionGlobal::new)
            .map(IcPermissionScopedType.class::cast)
            .toList()
        );

      final var batches = new ArrayList<Query>();
      batches.add(
        context.deleteFrom(PERMISSIONS).where(PERMISSIONS.USER_ID.eq(id))
      );

      for (final var permission : permissions.stream().toList()) {
        batches.add(permissionInsert(context, id, permission));
      }

      context.batch(batches).execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<UUID> userInitial()
    throws IcDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userInitial");

    try {
      return context.selectFrom(USERS)
        .where(USERS.INITIAL.eq(TRUE))
        .fetchOptional()
        .map(UsersRecord::getId);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void userInitialUnset(
    final UUID id)
    throws IcDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userInitialUnset");

    try {
      final var userRec = context.fetchOne(USERS, USERS.ID.eq(id));
      if (userRec == null) {
        return;
      }
      userRec.set(USERS.INITIAL, FALSE);
      userRec.store();

      context.deleteFrom(PERMISSIONS)
        .where(PERMISSIONS.USER_ID.eq(id))
        .execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }
}
