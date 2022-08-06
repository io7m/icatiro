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
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;

import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.SQL_ERROR;

/**
 * The default postgres server database implementation.
 */

public final class IcDatabase implements IcDatabaseType
{
  private final Clock clock;
  private final HikariDataSource dataSource;
  private final Settings settings;
  private final IcDatabaseMetrics metrics;

  /**
   * The default postgres server database implementation.
   *
   * @param inClock                 The clock
   * @param inDataSource            A pooled data source
   * @param inMetrics               A metrics bean
   */

  public IcDatabase(
    final Clock inClock,
    final HikariDataSource inDataSource,
    final IcDatabaseMetrics inMetrics)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.dataSource =
      Objects.requireNonNull(inDataSource, "dataSource");
    this.metrics =
      Objects.requireNonNull(inMetrics, "metrics");
    this.settings =
      new Settings().withRenderNameCase(RenderNameCase.LOWER);
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
    try {
      final var conn = this.dataSource.getConnection();
      conn.setAutoCommit(false);
      return new IcDatabaseConnection(this, conn, role);
    } catch (final SQLException e) {
      throw new IcDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  /**
   * @return The database metrics
   */

  public IcDatabaseMetrics metrics()
  {
    return this.metrics;
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
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
