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
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.database.postgres.IcDatabases;
import com.io7m.icatiro.model.IcPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcUserEmail;
import com.io7m.jmulticlose.core.CloseableCollection;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static com.io7m.icatiro.database.api.IcDatabaseCreate.CREATE_DATABASE;
import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.database.api.IcDatabaseUpgrade.UPGRADE_DATABASE;

public final class IcDatabaseExample
{
  private IcDatabaseExample()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var databases = new IcDatabases();

    final var configuration =
      new IcDatabaseConfiguration(
        "postgres",
        "12345678",
        "localhost",
        5432,
        "postgres",
        CREATE_DATABASE,
        UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    final var rng =
      SecureRandom.getInstanceStrong();
    final var salt =
      new byte[16];

    rng.nextBytes(salt);

    try (var resources = CloseableCollection.create()) {
      final var database =
        resources.add(databases.open(configuration, message -> {

        }));
      final var c =
        resources.add(database.openConnection(ICATIRO));
      final var t =
        resources.add(c.openTransaction());
      final var q =
        t.queries(IcDatabaseUsersQueriesType.class);

      final var password =
        IcPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed("12345678");

      final var id =
        q.userCreate(
          UUID.randomUUID(),
          new IcUserDisplayName("user"),
          new IcUserEmail("someone@example.com"),
          OffsetDateTime.now(ZoneId.of("UTC")),
          password
        );

      t.commit();
    }
  }
}
