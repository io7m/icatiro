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
import com.io7m.icatiro.model.IcPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcUserEmail;
import com.io7m.icatiro.server.IcServers;
import com.io7m.icatiro.server.api.IcServerConfiguration;
import com.io7m.icatiro.server.api.IcServerType;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
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

    final var databaseConfiguration =
      new IcDatabaseConfiguration(
        "postgres",
        "12345678",
        "localhost",
        54321,
        "postgres",
        IcDatabaseCreate.CREATE_DATABASE,
        IcDatabaseUpgrade.UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    final var serverConfiguration =
      new IcServerConfiguration(
        Locale.getDefault(),
        Clock.systemUTC(),
        new IcDatabases(),
        databaseConfiguration,
        new InetSocketAddress("localhost", 40000),
        new InetSocketAddress("localhost", 40001),
        Files.createTempDirectory("icatiro")
      );

    final var servers = new IcServers();

    try (var server = servers.createServer(serverConfiguration)) {
      server.start();
      createInitialUser(server);

      while (true) {
        Thread.sleep(1_000L);
      }
    }
  }

  private static void createInitialUser(
    final IcServerType server)
  {
    try {
      final var db = server.database();
      try (var c = db.openConnection(ICATIRO)) {
        try (var t = c.openTransaction()) {
          final var q = t.queries(IcDatabaseUsersQueriesType.class);
          final var algo = IcPasswordAlgorithmPBKDF2HmacSHA256.create();
          final var password = algo.createHashed("12345678");
          q.userCreateInitial(
            UUID.randomUUID(),
            new IcUserDisplayName("someone"),
            new IcUserEmail("someone@example.com"),
            OffsetDateTime.now(),
            password
          );
          t.commit();
        }
      }
    } catch (final IcDatabaseException | IcPasswordException e) {
      // Don't care
    }
  }
}
