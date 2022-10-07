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

import com.io7m.icatiro.database.api.IcDatabaseAuditQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseProjectsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseRole;
import com.io7m.icatiro.database.api.IcDatabaseTicketsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.icatiro.database.postgres.internal.Tables.USERS;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.SQL_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.SQL_ERROR_UNSUPPORTED_QUERY_CLASS;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_UNSET;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DbSystemValues.POSTGRESQL;
import static org.jooq.SQLDialect.POSTGRES;

final class IcDatabaseTransaction
  implements IcDatabaseTransactionType
{
  private final IcDatabaseConnection connection;
  private final Span transactionSpan;
  private UUID currentUserId;
  private UUID currentAdminId;

  IcDatabaseTransaction(
    final IcDatabaseConnection inConnection,
    final Span inTransactionScope)
  {
    this.connection =
      Objects.requireNonNull(inConnection, "connection");
    this.transactionSpan =
      Objects.requireNonNull(inTransactionScope, "inMetricsScope");
  }

  /**
   * @return The transaction span for metrics
   */

  public Span span()
  {
    return this.transactionSpan;
  }

  /**
   * Create a new query span for measuring query times.
   *
   * @param name The query name
   *
   * @return The query span
   */

  public Span createQuerySpan(
    final String name)
  {
    return this.tracer()
      .spanBuilder(name)
      .setParent(Context.current().with(this.transactionSpan))
      .setAttribute(DB_SYSTEM, POSTGRESQL)
      .setSpanKind(INTERNAL)
      .startSpan();
  }

  void setRole(
    final IcDatabaseRole role)
    throws SQLException
  {
    switch (role) {
      case ADMIN -> {

      }
      case ICATIRO -> {
        try (var st =
               this.connection.connection()
                 .prepareStatement("set role icatiro")) {
          st.execute();
        }
      }
      case NONE -> {
        try (var st =
               this.connection.connection()
                 .prepareStatement("set role icatiro_none")) {
          st.execute();
        }
      }
    }
  }

  @Override
  public <T extends IcDatabaseQueriesType> T queries(
    final Class<T> qClass)
    throws IcDatabaseException
  {
    if (Objects.equals(qClass, IcDatabaseUsersQueriesType.class)) {
      return qClass.cast(new IcDatabaseUsersQueries(this));
    }
    if (Objects.equals(qClass, IcDatabaseAuditQueriesType.class)) {
      return qClass.cast(new IcDatabaseAuditQueries(this));
    }
    if (Objects.equals(qClass, IcDatabaseTicketsQueriesType.class)) {
      return qClass.cast(new IcDatabaseTicketsQueries(this));
    }
    if (Objects.equals(qClass, IcDatabaseProjectsQueriesType.class)) {
      return qClass.cast(new IcDatabaseProjectsQueries(this));
    }

    throw new IcDatabaseException(
      "Unsupported query type: %s".formatted(qClass),
      SQL_ERROR_UNSUPPORTED_QUERY_CLASS
    );
  }

  @Override
  public void rollback()
    throws IcDatabaseException
  {
    try {
      this.connection.connection().rollback();
    } catch (final SQLException e) {
      throw new IcDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  public DSLContext createContext()
  {
    final var sqlConnection =
      this.connection.connection();
    final var settings =
      this.connection.database().settings();
    return DSL.using(sqlConnection, POSTGRES, settings);
  }

  public Clock clock()
  {
    return this.connection.database().clock();
  }

  @Override
  public void commit()
    throws IcDatabaseException
  {
    try {
      this.connection.connection().commit();
    } catch (final SQLException e) {
      throw new IcDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  @Override
  public void close()
    throws IcDatabaseException
  {
    try {
      this.rollback();
    } catch (final Exception e) {
      this.transactionSpan.recordException(e);
      throw e;
    } finally {
      this.transactionSpan.end();
    }
  }

  @Override
  public void userIdSet(
    final UUID userId)
    throws IcDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    final var context = this.createContext();

    try {
      final var userOpt =
        context.select(USERS.ID)
          .from(USERS)
          .where(USERS.ID.eq(userId))
          .fetchOptional()
          .map(r -> r.getValue(USERS.ID));

      if (userOpt.isEmpty()) {
        throw new IcDatabaseException(
          "No such user: %s".formatted(userId),
          USER_NONEXISTENT
        );
      }

      this.currentUserId = userId;
    } catch (final DataAccessException e) {
      throw new IcDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  @Override
  public UUID userId()
    throws IcDatabaseException
  {
    return Optional.ofNullable(this.currentUserId).orElseThrow(() -> {
      return new IcDatabaseException(
        "A user must be set before calling this method.",
        USER_UNSET
      );
    });
  }

  /**
   * @return The metrics tracer
   */

  public Tracer tracer()
  {
    return this.connection.database().tracer();
  }
}
