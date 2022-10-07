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

package com.io7m.icatiro.tests;

import com.io7m.icatiro.database.api.IcDatabaseConfiguration;
import com.io7m.icatiro.database.api.IcDatabaseCreate;
import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseUpgrade;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.database.postgres.IcDatabases;
import com.io7m.icatiro.server.IcServers;
import com.io7m.icatiro.server.api.IcServerBrandingConfiguration;
import com.io7m.icatiro.server.api.IcServerConfiguration;
import com.io7m.icatiro.server.api.IcServerHTTPServiceConfiguration;
import com.io7m.icatiro.server.api.IcServerHistoryConfiguration;
import com.io7m.icatiro.server.api.IcServerIdstoreConfiguration;
import com.io7m.icatiro.server.api.IcServerMailConfiguration;
import com.io7m.icatiro.server.api.IcServerMailTransportSMTP;
import com.io7m.icatiro.server.api.IcServerOpenTelemetryConfiguration;
import com.io7m.icatiro.server.api.IcServerRateLimitConfiguration;
import com.io7m.icatiro.server.api.IcServerType;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;

public final class IcServerDemo
{
  private IcServerDemo()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    System.setProperty("org.jooq.no-tips", "true");
    System.setProperty("org.jooq.no-logo", "true");

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    final var tmpDirectory =
      IcTestDirectories.createTempDirectory();

    final var loginExtraText =
      IcTestDirectories.resourceOf(
        IcServerDemo.class,
        tmpDirectory,
        "loginExtra.xhtml"
      );

    final var openTelemetry =
      new IcServerOpenTelemetryConfiguration(
        "icatiro",
        URI.create("http://127.0.0.1:4317")
      );

    final var databaseConfiguration =
      new IcDatabaseConfiguration(
        "postgres",
        "12345678",
        "localhost",
        54323,
        "postgres",
        IcDatabaseCreate.CREATE_DATABASE,
        IcDatabaseUpgrade.UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    final var mailService =
      new IcServerMailConfiguration(
        new IcServerMailTransportSMTP("localhost", 25000),
        Optional.empty(),
        "no-reply@example.com",
        Duration.ofDays(1L)
      );

    final var userApiService =
      new IcServerHTTPServiceConfiguration(
        "localhost",
        40000,
        URI.create("http://localhost:40000/")
      );
    final var userViewService =
      new IcServerHTTPServiceConfiguration(
        "localhost",
        40001,
        URI.create("http://localhost:40001/")
      );

    final var branding =
      new IcServerBrandingConfiguration(
        Optional.of("Icatiro"),
        Optional.empty(),
        Optional.of(loginExtraText),
        Optional.empty()
      );

    final var history =
      new IcServerHistoryConfiguration(
        100,
        100
      );

    final var rateLimit =
      new IcServerRateLimitConfiguration(
        Duration.of(10L, ChronoUnit.MINUTES),
        Duration.of(10L, ChronoUnit.MINUTES)
      );

    final var idStore =
      new IcServerIdstoreConfiguration(
        URI.create("http://localhost:50000/"),
        URI.create("http://localhost:50001/password-reset")
      );

    final var serverConfiguration =
      new IcServerConfiguration(
        Locale.getDefault(),
        Clock.systemUTC(),
        new IcDatabases(),
        databaseConfiguration,
        mailService,
        userApiService,
        userViewService,
        branding,
        history,
        rateLimit,
        Optional.of(openTelemetry),
        idStore
      );

    final var servers = new IcServers();

    try (var server = servers.createServer(serverConfiguration)) {
      server.start();

      while (true) {
        Thread.sleep(1_000L);
      }
    }
  }
}
