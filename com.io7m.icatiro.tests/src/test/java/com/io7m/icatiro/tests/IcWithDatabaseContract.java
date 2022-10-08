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
import com.io7m.icatiro.database.api.IcDatabaseRole;
import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.database.api.IcDatabaseUpgrade;
import com.io7m.icatiro.database.postgres.IcDatabases;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

@Testcontainers(disabledWithoutDocker = true)
public abstract class IcWithDatabaseContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcWithDatabaseContract.class);

  @Container
  private final PostgreSQLContainer<?> icatiroContainer =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("icatiro")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private IcFakeClock clock;
  private IcDatabaseType database;

  @BeforeEach
  public final void serverSetup()
    throws Exception
  {
    LOG.debug("serverSetup");

    this.clock = new IcFakeClock();
    this.resources = CloseableCollection.create();
    this.waitForDatabaseToStart();

    final var databaseConfiguration =
      new IcDatabaseConfiguration(
        "postgres",
        "12345678",
        this.icatiroContainer.getHost(),
        this.icatiroContainer.getFirstMappedPort().intValue(),
        "icatiro",
        IcDatabaseCreate.CREATE_DATABASE,
        IcDatabaseUpgrade.UPGRADE_DATABASE,
        this.clock
      );

    final var databases = new IcDatabases();
    this.database =
      this.resources.add(
        databases.open(databaseConfiguration, OpenTelemetry.noop(), s -> {

        }));
  }

  @AfterEach
  public final void serverTearDown()
    throws Exception
  {
    LOG.debug("serverTearDown");
    this.resources.close();
  }

  public interface WithTransactionType<T, E extends Exception>
  {
    T execute(IcDatabaseTransactionType transaction)
      throws E, IcDatabaseException;
  }

  protected final <T, E extends Exception> T withTransaction(
    final WithTransactionType<T, E> f)
    throws IcDatabaseException, E
  {
    try (var c = this.database.openConnection(IcDatabaseRole.ICATIRO)) {
      try (var t = c.openTransaction()) {
        return f.execute(t);
      }
    }
  }

  private void waitForDatabaseToStart()
    throws InterruptedException, TimeoutException
  {
    LOG.debug("waiting for database to start");
    final var timeWait = Duration.ofSeconds(60L);
    final var timeThen = Instant.now();
    while (!this.icatiroContainer.isRunning()) {
      Thread.sleep(1L);
      final var timeNow = Instant.now();
      if (Duration.between(timeThen, timeNow).compareTo(timeWait) > 0) {
        LOG.error("timed out waiting for database to start");
        throw new TimeoutException("Timed out waiting for database to start");
      }
    }
    LOG.debug("database started");
  }
}
