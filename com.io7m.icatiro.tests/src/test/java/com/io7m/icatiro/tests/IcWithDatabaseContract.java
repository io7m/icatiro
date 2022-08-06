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
import com.io7m.icatiro.database.api.IcDatabaseConnectionType;
import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseRole;
import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.database.postgres.IcDatabases;
import com.io7m.icatiro.model.IcPassword;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.io7m.icatiro.database.api.IcDatabaseCreate.CREATE_DATABASE;
import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.database.api.IcDatabaseUpgrade.UPGRADE_DATABASE;
import static java.time.OffsetDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
public abstract class IcWithDatabaseContract
{
  private static final IcDatabases DATABASES =
    new IcDatabases();

  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>(DockerImageName.parse("postgres").withTag("14.4"))
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private IcDatabaseType database;

  @BeforeEach
  public final void withDatabaseSetup()
  {
    this.resources = CloseableCollection.create();
  }

  @AfterEach
  public final void withDatabaseTearDown()
    throws ClosingResourceFailedException
  {
    this.resources.close();
  }

  protected static void checkAuditLog(
    final IcDatabaseTransactionType transaction,
    final ExpectedEvent... expectedEvents)
    throws IcDatabaseException
  {
    final var audit =
      transaction.queries(IcDatabaseAuditQueriesType.class);
    final var events =
      audit.auditEvents(
        timeNow().minusYears(1L),
        timeNow().plusYears(1L),
        new IcSubsetMatch<>("", ""),
        new IcSubsetMatch<>("", ""),
        new IcSubsetMatch<>("", "")
      );

    for (var index = 0; index < expectedEvents.length; ++index) {
      final var event =
        events.get(index);
      final var expect =
        expectedEvents[index];

      assertEquals(
        expect.type,
        event.type(),
        String.format(
          "Event [%d] %s type must be %s",
          Integer.valueOf(index),
          event,
          expect.type)
      );

      if (expect.message != null) {
        assertEquals(
          expect.message,
          event.message(),
          String.format(
            "Event [%d] %s message must be %s",
            Integer.valueOf(index),
            event,
            expect.message)
        );
      }
    }
  }

  protected static OffsetDateTime timeNow()
  {
    /*
     * Postgres doesn't store times at as high a resolution as the JVM,
     * so trim the nanoseconds off in order to ensure we can correctly
     * compare results returned from the database.
     */

    return now().withNano(0);
  }

  protected static IcPassword databaseGenerateBadPassword()
    throws IcPasswordException
  {
    return IcPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  protected final boolean containerIsRunning()
  {
    return this.container.isRunning();
  }

  private IcDatabaseType databaseOf(
    final PostgreSQLContainer<?> container)
    throws IcDatabaseException
  {
    return this.resources.add(
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
      ));
  }

  protected final IcDatabaseTransactionType transactionOf(
    final IcDatabaseRole role)
    throws IcDatabaseException
  {
    final var connection =
      this.connectionOf(role);
    return this.resources.add(connection.openTransaction());
  }

  protected final IcDatabaseConnectionType connectionOf(
    final IcDatabaseRole role)
    throws IcDatabaseException
  {
    if (this.database == null) {
      this.database = this.databaseOf(this.container);
    }

    final var connection =
      this.resources.add(this.database.openConnection(role));
    return connection;
  }

  protected final UUID databaseCreateUserInitial(
    final String user,
    final String pass)
    throws Exception
  {
    try (var t = this.transactionOf(ICATIRO)) {
      final var q =
        t.queries(IcDatabaseUsersQueriesType.class);

      final var password =
        IcPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed(pass);

      final var id = UUID.randomUUID();
      q.userCreateInitial(
        id,
        new IcUserDisplayName(user),
        new IcUserEmail(UUID.randomUUID() + "@example.com"),
        now(),
        password
      );
      t.commit();
      return id;
    }
  }

  protected record ExpectedEvent(
    String type,
    String message)
  {

  }
}
