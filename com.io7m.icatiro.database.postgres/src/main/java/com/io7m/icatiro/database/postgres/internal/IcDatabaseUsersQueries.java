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
import com.io7m.icatiro.database.postgres.internal.tables.records.UsersRecord;
import com.io7m.icatiro.model.IcPassword;
import com.io7m.icatiro.model.IcPasswordAlgorithms;
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcUserEmail;
import com.io7m.icatiro.model.IcUserSummary;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.io7m.icatiro.database.postgres.internal.IcDatabaseExceptions.handleDatabaseException;
import static com.io7m.icatiro.database.postgres.internal.Tables.AUDIT;
import static com.io7m.icatiro.database.postgres.internal.Tables.USERS;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_DUPLICATE_EMAIL;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_DUPLICATE_ID;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_DUPLICATE_NAME;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_NOT_INITIAL;
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

  private static IcUser userRecordToUser(
    final UsersRecord userRecord)
    throws IcPasswordException
  {
    return new IcUser(
      userRecord.getId(),
      new IcUserDisplayName(userRecord.getName()),
      new IcUserEmail(userRecord.getEmail()),
      userRecord.getCreated(),
      userRecord.getLastLoginTime(),
      new IcPassword(
        IcPasswordAlgorithms.parse(userRecord.getPasswordAlgo()),
        userRecord.getPasswordHash().toUpperCase(Locale.ROOT),
        userRecord.getPasswordSalt().toUpperCase(Locale.ROOT)
      )
    );
  }

  private static Optional<IcUser> userMap(
    final DSLContext context,
    final Optional<UsersRecord> recordOpt)
    throws IcPasswordException
  {
    if (recordOpt.isPresent()) {
      final var userRecord = recordOpt.get();
      return Optional.of(userRecordToUser(userRecord));
    }

    return Optional.empty();
  }

  private static IcDatabaseException handlePasswordException(
    final IcPasswordException exception)
  {
    return new IcDatabaseException(
      exception.getMessage(),
      exception,
      PASSWORD_ERROR
    );
  }

  @Override
  public IcUser userCreateInitial(
    final UUID id,
    final IcUserDisplayName userName,
    final IcUserEmail email,
    final OffsetDateTime created,
    final IcPassword password)
    throws IcDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userName, "userName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(password, "password");

    final var transaction = this.transaction();
    final var context = transaction.createContext();

    try {
      final var existing = context.fetchOne(USERS);
      if (existing != null) {
        throw new IcDatabaseException(
          "User already exists",
          USER_NOT_INITIAL
        );
      }

      final var userCreate =
        context.insertInto(USERS)
          .set(USERS.ID, id)
          .set(USERS.NAME, userName.value())
          .set(USERS.EMAIL, email.value())
          .set(USERS.CREATED, created)
          .set(USERS.LAST_LOGIN_TIME, created)
          .set(USERS.PASSWORD_ALGO, password.algorithm().identifier())
          .set(USERS.PASSWORD_HASH, password.hash())
          .set(USERS.PASSWORD_SALT, password.salt());

      userCreate.execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_CREATED")
          .set(AUDIT.USER_ID, id)
          .set(AUDIT.MESSAGE, id.toString())
          .set(AUDIT.CONFIDENTIAL, Boolean.FALSE);

      audit.execute();
      return this.userGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public IcUser userCreate(
    final UUID id,
    final IcUserDisplayName userName,
    final IcUserEmail email,
    final OffsetDateTime created,
    final IcPassword password)
    throws IcDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userName, "userName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(password, "password");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var owner = transaction.userId();

    try {
      {
        final var existing =
          context.fetchOptional(USERS, USERS.ID.eq(id));
        if (existing.isPresent()) {
          throw new IcDatabaseException(
            "User ID already exists",
            USER_DUPLICATE_ID
          );
        }
      }

      {
        final var existing =
          context.fetchOptional(USERS, USERS.NAME.eq(userName.value()));
        if (existing.isPresent()) {
          throw new IcDatabaseException(
            "User name already exists",
            USER_DUPLICATE_NAME
          );
        }
      }

      {
        final var existing =
          context.fetchOptional(USERS, USERS.EMAIL.eq(email.value()));
        if (existing.isPresent()) {
          throw new IcDatabaseException(
            "Email already exists",
            USER_DUPLICATE_EMAIL
          );
        }
      }

      final var userCreate =
        context.insertInto(USERS)
          .set(USERS.ID, id)
          .set(USERS.NAME, userName.value())
          .set(USERS.EMAIL, email.value())
          .set(USERS.CREATED, created)
          .set(USERS.LAST_LOGIN_TIME, created)
          .set(USERS.PASSWORD_ALGO, password.algorithm().identifier())
          .set(USERS.PASSWORD_HASH, password.hash())
          .set(USERS.PASSWORD_SALT, password.salt());

      userCreate.execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_CREATED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.toString())
          .set(AUDIT.CONFIDENTIAL, Boolean.FALSE);

      audit.execute();
      return this.userGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public Optional<IcUser> userGet(
    final UUID id)
    throws IcDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var context = this.transaction().createContext();
    try {
      final var record =
        context.fetchOptional(USERS, USERS.ID.eq(id));

      return userMap(context, record);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final IcPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public IcUser userGetRequire(
    final UUID id)
    throws IcDatabaseException
  {
    return this.userGet(id).orElseThrow(USER_DOES_NOT_EXIST);
  }

  @Override
  public Optional<IcUser> userGetForName(
    final IcUserDisplayName name)
    throws IcDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var context = this.transaction().createContext();
    try {
      final var record =
        context.fetchOptional(USERS, USERS.NAME.eq(name.value()));

      return userMap(context, record);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final IcPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public IcUser userGetForNameRequire(
    final IcUserDisplayName name)
    throws IcDatabaseException
  {
    return this.userGetForName(name).orElseThrow(USER_DOES_NOT_EXIST);
  }

  @Override
  public Optional<IcUser> userGetForEmail(
    final IcUserEmail email)
    throws IcDatabaseException
  {
    Objects.requireNonNull(email, "email");

    final var context = this.transaction().createContext();
    try {
      final var record =
        context.fetchOptional(USERS, USERS.EMAIL.eq(email.value()));

      return userMap(context, record);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final IcPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public IcUser userGetForEmailRequire(
    final IcUserEmail email)
    throws IcDatabaseException
  {
    return this.userGetForEmail(email).orElseThrow(USER_DOES_NOT_EXIST);
  }

  @Override
  public void userLogin(
    final UUID id,
    final String host)
    throws IcDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(host, "host");

    final var context =
      this.transaction().createContext();

    try {
      final var time = this.currentTime();

      final var existingOpt =
        context.fetchOptional(USERS, USERS.ID.eq(id));
      if (!existingOpt.isPresent()) {
        throw new IcDatabaseException(
          "User does not exist",
          USER_NONEXISTENT
        );
      }

      final var existing = existingOpt.get();
      existing.setLastLoginTime(time);
      existing.store();

      /*
       * The audit event is considered confidential because IP addresses
       * are tentatively considered confidential.
       */

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, "USER_LOGGED_IN")
          .set(AUDIT.USER_ID, id)
          .set(AUDIT.MESSAGE, host)
          .set(AUDIT.CONFIDENTIAL, TRUE);

      audit.execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public List<IcUserSummary> userSearch(
    final String query)
    throws IcDatabaseException
  {
    Objects.requireNonNull(query, "query");

    final var context =
      this.transaction().createContext();

    try {
      final var wildcardQuery =
        "%%%s%%".formatted(query);

      final var records =
        context.selectFrom(USERS)
          .where(USERS.NAME.likeIgnoreCase(wildcardQuery))
          .or(USERS.EMAIL.likeIgnoreCase(wildcardQuery))
          .or(USERS.ID.likeIgnoreCase(wildcardQuery))
          .orderBy(USERS.NAME)
          .fetch();

      final var summaries = new ArrayList<IcUserSummary>(records.size());
      for (final var record : records) {
        summaries.add(
          new IcUserSummary(
            record.get(USERS.ID),
            new IcUserDisplayName(record.get(USERS.NAME)),
            new IcUserEmail(record.get(USERS.EMAIL))
          )
        );
      }
      return List.copyOf(summaries);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public void userUpdate(
    final UUID id,
    final Optional<IcUserDisplayName> withDisplayName,
    final Optional<IcUserEmail> withEmail,
    final Optional<IcPassword> withPassword)
    throws IcDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var owner = transaction.userId();

    try {
      final var record = context.fetchOne(USERS, USERS.ID.eq(id));
      if (record == null) {
        throw USER_DOES_NOT_EXIST.get();
      }

      if (withDisplayName.isPresent()) {
        final var name = withDisplayName.get();
        record.setName(name.value());

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_CHANGED_DISPLAY_NAME")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, "%s|%s".formatted(id.toString(), name.value()))
          .set(AUDIT.CONFIDENTIAL, Boolean.FALSE)
          .execute();
      }

      if (withEmail.isPresent()) {
        final var email = withEmail.get();
        record.setEmail(email.value());

        /*
         * Email addresses are confidential.
         */

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_CHANGED_EMAIL")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, "%s|%s".formatted(id.toString(), email.value()))
          .set(AUDIT.CONFIDENTIAL, TRUE)
          .execute();
      }

      if (withPassword.isPresent()) {
        final var pass = withPassword.get();
        record.setPasswordAlgo(pass.algorithm().identifier());
        record.setPasswordHash(pass.hash());
        record.setPasswordSalt(pass.salt());

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_CHANGED_PASSWORD")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.toString())
          .set(AUDIT.CONFIDENTIAL, Boolean.FALSE)
          .execute();
      }

      record.store();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }
}
