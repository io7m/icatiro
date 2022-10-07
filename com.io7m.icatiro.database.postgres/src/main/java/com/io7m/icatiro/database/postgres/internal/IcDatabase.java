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

import com.io7m.icatiro.database.api.IcDatabaseConnectionType;
import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseRole;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.error_codes.IcStandardErrorCodes;
import com.zaxxer.hikari.HikariDataSource;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DbSystemValues.POSTGRESQL;

/**
 * The default postgres server database implementation.
 */

public final class IcDatabase implements IcDatabaseType
{
  private final OpenTelemetry telemetry;
  private final Clock clock;
  private final HikariDataSource dataSource;
  private final Settings settings;
  private final Tracer tracer;

  /**
   * The default postgres server database implementation.
   *
   * @param inOpenTelemetry A telemetry interface
   * @param inClock         The clock
   * @param inDataSource    A pooled data source
   */

  public IcDatabase(
    final OpenTelemetry inOpenTelemetry,
    final Clock inClock,
    final HikariDataSource inDataSource)
  {
    this.telemetry =
      Objects.requireNonNull(inOpenTelemetry, "inOpenTelemetry");
    this.tracer =
      this.telemetry.getTracer("com.io7m.icatiro.database.postgres", version());
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.dataSource =
      Objects.requireNonNull(inDataSource, "dataSource");
    this.settings =
      new Settings().withRenderNameCase(RenderNameCase.LOWER);
  }

  /**
   * @return The OpenTelemetry tracer
   */

  public Tracer tracer()
  {
    return this.tracer;
  }

  @Override
  public void close()
  {
    this.dataSource.close();
  }

  @Override
  public IcDatabaseConnectionType openConnection(
    final IcDatabaseRole role)
    throws IcDatabaseException
  {
    final var span =
      this.tracer
        .spanBuilder("IcDatabaseConnection")
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(DB_SYSTEM, POSTGRESQL)
        .startSpan();

    try {
      final var conn = this.dataSource.getConnection();
      conn.setAutoCommit(false);
      return new IcDatabaseConnection(this, conn, role, span);
    } catch (final SQLException e) {
      span.recordException(e);
      span.end();
      throw new IcDatabaseException(e.getMessage(), e, IcStandardErrorCodes.SQL_ERROR);
    }
  }

  /**
   * @return The jooq SQL settings
   */

  public Settings settings()
  {
    return this.settings;
  }

  /**
   * @return The clock used for time-related queries
   */

  public Clock clock()
  {
    return this.clock;
  }

  @Override
  public String description()
  {
    return "Server database service.";
  }

  @Override
  public String toString()
  {
    return "[IcDatabase 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  private static String version()
  {
    final var p =
      IcDatabase.class.getPackage();
    final var v =
      p.getImplementationVersion();

    if (v == null) {
      return "0.0.0";
    }
    return v;
  }
}
