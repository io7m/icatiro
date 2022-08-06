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

import com.io7m.icatiro.database.api.IcDatabaseAuditQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseConfiguration;
import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseRole;
import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.database.postgres.IcDatabases;
import com.io7m.icatiro.model.IcPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcSubsetMatch;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcUserEmail;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.util.UUID;

import static com.io7m.icatiro.database.api.IcDatabaseCreate.CREATE_DATABASE;
import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.database.api.IcDatabaseUpgrade.UPGRADE_DATABASE;
import static java.time.OffsetDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class IcDatabaseAuditTest
{
  private static final UUID ADMIN_UUID =
    UUID.randomUUID();
  private static final IcDatabases DATABASES =
    new IcDatabases();

  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>(DockerImageName.parse("postgres").withTag("14.4"))
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> resources;

  private IcDatabaseType databaseOf(
    final PostgreSQLContainer<?> container)
    throws IcDatabaseException
  {
    final var database =
      DATABASES.open(
        new IcDatabaseConfiguration(
          "postgres",
          "12345678",
          container.getContainerIpAddress(),
          container.getFirstMappedPort().intValue(),
          "postgres",
          CREATE_DATABASE,
          UPGRADE_DATABASE,
          Clock.systemUTC()
        ),
        message -> {

        }
      );

    try (var c = database.openConnection(ICATIRO)) {
      try (var t = c.openTransaction()) {
        final var a =
          t.queries(IcDatabaseUsersQueriesType.class);

        try {
          final var password =
            IcPasswordAlgorithmPBKDF2HmacSHA256.create()
              .createHashed("12345678");

          a.userCreateInitial(
            ADMIN_UUID,
            new IcUserDisplayName("someone"),
            new IcUserEmail("someone@example.com"),
            now(),
            password
          );
        } catch (final IcPasswordException e) {
          throw new IllegalStateException(e);
        }

        t.commit();
      }
    }

    return this.resources.add(database);
  }

  private IcDatabaseTransactionType transactionOf(
    final IcDatabaseRole role)
    throws IcDatabaseException
  {
    final var database =
      this.databaseOf(this.container);
    final var connection =
      this.resources.add(database.openConnection(role));
    return this.resources.add(connection.openTransaction());
  }

  @BeforeEach
  public void setup()
  {
    this.resources = CloseableCollection.create();
  }

  @AfterEach
  public void tearDown()
    throws ClosingResourceFailedException
  {
    this.resources.close();
  }

  @Test
  public void testAuditQuery0()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(ICATIRO);
    final var audit =
      transaction.queries(IcDatabaseAuditQueriesType.class);

    final var then = now();
    audit.auditPut(ADMIN_UUID, then.plusSeconds(1), "ET_0", "E0", false);
    audit.auditPut(ADMIN_UUID, then.plusSeconds(2), "ET_0", "E1", false);
    audit.auditPut(ADMIN_UUID, then.plusSeconds(3), "ET_0", "E2", false);
    audit.auditPut(ADMIN_UUID, then.plusSeconds(4), "ET_1", "F3", false);
    audit.auditPut(ADMIN_UUID, then.plusSeconds(5), "ET_1", "F4", false);
    audit.auditPut(ADMIN_UUID, then.plusSeconds(6), "ET_1", "F5", false);
    audit.auditPut(ADMIN_UUID, then.plusSeconds(7), "ET_2", "G6", false);
    audit.auditPut(ADMIN_UUID, then.plusSeconds(8), "ET_2", "G7", false);
    audit.auditPut(ADMIN_UUID, then.plusSeconds(9), "ET_2", "G8", false);

    transaction.commit();

    {
      final var events =
        audit.auditEvents(
          then,
          then.plusDays(1L),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("", "")
        );
      assertEquals(9, events.size());
      assertEquals("E0", events.get(0).message());
      assertEquals("E1", events.get(1).message());
      assertEquals("E2", events.get(2).message());
      assertEquals("F3", events.get(3).message());
      assertEquals("F4", events.get(4).message());
      assertEquals("F5", events.get(5).message());
      assertEquals("G6", events.get(6).message());
      assertEquals("G7", events.get(7).message());
      assertEquals("G8", events.get(8).message());
    }

    {
      final var events =
        audit.auditEvents(
          then,
          then.plusDays(1L),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("", "ET_1"),
          new IcSubsetMatch<>("", "")
        );
      assertEquals(6, events.size());
      assertEquals("E0", events.get(0).message());
      assertEquals("E1", events.get(1).message());
      assertEquals("E2", events.get(2).message());
      assertEquals("G6", events.get(3).message());
      assertEquals("G7", events.get(4).message());
      assertEquals("G8", events.get(5).message());
    }

    {
      final var events =
        audit.auditEvents(
          then,
          then.plusDays(1L),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("ET_0", ""),
          new IcSubsetMatch<>("", "")
        );
      assertEquals(3, events.size());
      assertEquals("E0", events.get(0).message());
      assertEquals("E1", events.get(1).message());
      assertEquals("E2", events.get(2).message());
    }

    {
      final var events =
        audit.auditEvents(
          then,
          then.plusDays(1L),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("", "F")
        );
      assertEquals(6, events.size());
      assertEquals("E0", events.get(0).message());
      assertEquals("E1", events.get(1).message());
      assertEquals("E2", events.get(2).message());
      assertEquals("G6", events.get(3).message());
      assertEquals("G7", events.get(4).message());
      assertEquals("G8", events.get(5).message());
    }

    {
      final var events =
        audit.auditEvents(
          then,
          then.plusDays(1L),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("G", "")
        );
      assertEquals(3, events.size());
      assertEquals("G6", events.get(0).message());
      assertEquals("G7", events.get(1).message());
      assertEquals("G8", events.get(2).message());
    }

    {
      final var events =
        audit.auditEvents(
          then,
          then.plusDays(1L),
          new IcSubsetMatch<>(ADMIN_UUID.toString(), ""),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("", "")
        );
      assertEquals(9, events.size());
      assertEquals("E0", events.get(0).message());
      assertEquals("E1", events.get(1).message());
      assertEquals("E2", events.get(2).message());
      assertEquals("F3", events.get(3).message());
      assertEquals("F4", events.get(4).message());
      assertEquals("F5", events.get(5).message());
      assertEquals("G6", events.get(6).message());
      assertEquals("G7", events.get(7).message());
      assertEquals("G8", events.get(8).message());
    }

    {
      final var events =
        audit.auditEvents(
          then,
          then.plusDays(1L),
          new IcSubsetMatch<>("", ADMIN_UUID.toString()),
          new IcSubsetMatch<>("", ""),
          new IcSubsetMatch<>("", "")
        );
      assertEquals(0, events.size());
    }
  }
}
