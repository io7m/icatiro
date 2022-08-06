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
import com.io7m.icatiro.database.api.IcDatabasePermissionsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseProjectsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseTicketsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseUpgrade;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.database.postgres.IcDatabases;
import com.io7m.icatiro.model.IcPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcPermissionScopeType;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketTitle;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcUserEmail;
import com.io7m.icatiro.server.api.IcServerType;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;

public final class IcCreateLotsOfTicketsDemo
{
  private IcCreateLotsOfTicketsDemo()
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

    final var databases = new IcDatabases();
    try (var database = databases.open(databaseConfiguration, s -> {
    })) {
      try (var c = database.openConnection(ICATIRO)) {
        try (var t = c.openTransaction()) {
          final var users =
            t.queries(IcDatabaseUsersQueriesType.class);
          final var projects =
            t.queries(IcDatabaseProjectsQueriesType.class);

          final var user =
            users.userGetForNameRequire(new IcUserDisplayName("someone"));

          t.userIdSet(user.id());

          try {
            projects.projectCreate(
              new IcProjectTitle("Project 0"),
              new IcProjectShortName("PROJ")
            );
            t.commit();
          } catch (final IcDatabaseException e) {
            // Whatever
            t.rollback();
          }

          final var tickets =
            t.queries(IcDatabaseTicketsQueriesType.class);

          for (int index = 0; index < 100; ++index) {
            tickets.ticketCreate(
              new IcTicketCreation(
                new IcProjectID(1L),
                new IcTicketTitle("Please fix %d"
                                    .formatted(Instant.now().toEpochMilli()))
              )
            );
          }

          t.commit();
        }
      }
    }
  }
}
