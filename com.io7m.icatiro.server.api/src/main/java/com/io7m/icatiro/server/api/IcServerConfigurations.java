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

import com.io7m.icatiro.database.api.IcDatabaseConfiguration;
import com.io7m.icatiro.database.api.IcDatabaseFactoryType;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static com.io7m.icatiro.database.api.IcDatabaseCreate.CREATE_DATABASE;
import static com.io7m.icatiro.database.api.IcDatabaseCreate.DO_NOT_CREATE_DATABASE;
import static com.io7m.icatiro.database.api.IcDatabaseUpgrade.DO_NOT_UPGRADE_DATABASE;
import static com.io7m.icatiro.database.api.IcDatabaseUpgrade.UPGRADE_DATABASE;

/**
 * Functions to produce server configurations.
 */

public final class IcServerConfigurations
{
  private IcServerConfigurations()
  {

  }

  /**
   * Read a server configuration from the given file.
   *
   * @param locale The locale
   * @param clock  The clock
   * @param file   The file
   *
   * @return A server configuration
   *
   * @throws IOException On errors
   */

  public static IcServerConfiguration ofFile(
    final Locale locale,
    final Clock clock,
    final Path file)
    throws IOException
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(file, "file");

    return ofFile(
      locale,
      clock,
      new IcServerConfigurationFiles().parse(file)
    );
  }

  /**
   * Read a server configuration from the given file.
   *
   * @param locale        The locale
   * @param clock         The clock
   * @param file          The file
   *
   * @return A server configuration
   */

  public static IcServerConfiguration ofFile(
    final Locale locale,
    final Clock clock,
    final IcServerConfigurationFile file)
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(file, "file");

    final var fileDbConfig =
      file.databaseConfiguration();

    final var databaseConfiguration =
      new IcDatabaseConfiguration(
        fileDbConfig.user(),
        fileDbConfig.password(),
        fileDbConfig.address(),
        fileDbConfig.port(),
        fileDbConfig.databaseName(),
        fileDbConfig.create() ? CREATE_DATABASE : DO_NOT_CREATE_DATABASE,
        fileDbConfig.upgrade() ? UPGRADE_DATABASE : DO_NOT_UPGRADE_DATABASE,
        clock
      );

    final var databaseFactories =
      ServiceLoader.load(IcDatabaseFactoryType.class)
        .iterator();

    final var database =
      findDatabase(databaseFactories, fileDbConfig.kind());

    return new IcServerConfiguration(
      locale,
      clock,
      database,
      databaseConfiguration,
      file.mailConfiguration(),
      file.httpConfiguration().userAPIService(),
      file.httpConfiguration().userViewService(),
      file.brandingConfiguration(),
      file.historyConfiguration(),
      file.rateLimit(),
      file.openTelemetry(),
      file.idstore()
    );
  }

  private static IcDatabaseFactoryType findDatabase(
    final Iterator<IcDatabaseFactoryType> databaseFactories,
    final IcServerDatabaseKind kind)
  {
    if (!databaseFactories.hasNext()) {
      throw new ServiceConfigurationError(
        "No available implementations of type %s"
          .formatted(IcDatabaseFactoryType.class)
      );
    }

    final var kinds = new ArrayList<String>();
    while (databaseFactories.hasNext()) {
      final var database = databaseFactories.next();
      kinds.add(database.kind());
      if (Objects.equals(database.kind(), kind.name())) {
        return database;
      }
    }

    throw new ServiceConfigurationError(
      "No available databases of kind %s (Available databases include: %s)"
        .formatted(IcDatabaseFactoryType.class, kinds)
    );
  }
}
