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
import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.database.postgres.IcDatabases;
import com.io7m.icatiro.model.IcPassword;
import com.io7m.icatiro.model.IcPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcUserEmail;
import com.io7m.icatiro.server.IcServers;
import com.io7m.icatiro.server.api.IcServerConfiguration;
import com.io7m.icatiro.server.api.IcServerException;
import com.io7m.icatiro.server.api.IcServerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.icatiro.database.api.IcDatabaseCreate.CREATE_DATABASE;
import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.database.api.IcDatabaseUpgrade.UPGRADE_DATABASE;

@Testcontainers(disabledWithoutDocker = true)
public abstract class IcWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcWithServerContract.class);

  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private IcCapturingDatabases databases;
  private IcServerType server;
  private IcServers servers;
  private Path directory;
  private AtomicBoolean started;

  private static IcPassword createBadPassword()
    throws IcPasswordException
  {
    return IcPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  protected static OffsetDateTime timeNow()
  {
    return OffsetDateTime.now(Clock.systemUTC()).withNano(0);
  }

  protected final UUID serverCreateUser(
    final UUID admin,
    final String name)
    throws IcDatabaseException, IcPasswordException
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var connection = database.openConnection(ICATIRO)) {
      try (var transaction = connection.openTransaction()) {
        final var users =
          transaction.queries(IcDatabaseUsersQueriesType.class);
        transaction.userIdSet(admin);

        final var userId = UUID.randomUUID();
        users.userCreate(
          userId,
          new IcUserDisplayName(name),
          new IcUserEmail("%s@example.com".formatted(name)),
          timeNow(),
          createBadPassword()
        );
        transaction.commit();
        return userId;
      }
    }
  }

  public final PostgreSQLContainer<?> container()
  {
    return this.container;
  }

  public final IcServerType server()
  {
    return this.server;
  }

  @BeforeEach
  public final void serverSetup()
    throws Exception
  {
    LOG.debug("serverSetup");

    this.waitForDatabaseToStart();

    this.started =
      new AtomicBoolean(false);
    this.directory =
      IcTestDirectories.createTempDirectory();
    this.servers =
      new IcServers();
    this.databases =
      new IcCapturingDatabases(new IcDatabases());
    this.server =
      this.createServer();
  }

  private void waitForDatabaseToStart()
    throws InterruptedException, TimeoutException
  {
    LOG.debug("waiting for database to start");
    final var timeWait = Duration.ofSeconds(60L);
    final var timeThen = Instant.now();
    while (!this.container.isRunning()) {
      Thread.sleep(1L);
      final var timeNow = Instant.now();
      if (Duration.between(timeThen, timeNow).compareTo(timeWait) > 0) {
        LOG.error("timed out waiting for database to start");
        throw new TimeoutException("Timed out waiting for database to start");
      }
    }
    LOG.debug("database started");
  }

  @AfterEach
  public final void serverTearDown()
    throws Exception
  {
    LOG.debug("serverTearDown");

    this.server.close();
  }

  private IcServerType createServer()
  {
    LOG.debug("creating server");

    final var databaseConfiguration =
      new IcDatabaseConfiguration(
        "postgres",
        "12345678",
        this.container.getContainerIpAddress(),
        this.container.getFirstMappedPort().intValue(),
        "postgres",
        CREATE_DATABASE,
        UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    final var apiAddress =
      new InetSocketAddress("localhost", 40000);
    final var viewAddress =
      new InetSocketAddress("localhost", 40001);

    return this.servers.createServer(
      new IcServerConfiguration(
        Locale.getDefault(),
        Clock.systemUTC(),
        this.databases,
        databaseConfiguration,
        apiAddress,
        viewAddress,
        this.directory
      )
    );
  }

  protected final URI serverAPIURI()
  {
    return URI.create("http://localhost:40000/");
  }

  protected final UUID serverCreateUserInitial(
    final String user,
    final String pass)
    throws Exception
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var c = database.openConnection(ICATIRO)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IcDatabaseUsersQueriesType.class);

        final var password =
          IcPasswordAlgorithmPBKDF2HmacSHA256.create()
            .createHashed(pass);

        final var id = UUID.randomUUID();
        q.userCreateInitial(
          id,
          new IcUserDisplayName(user),
          new IcUserEmail(id + "@example.com"),
          OffsetDateTime.now(),
          password
        );
        t.commit();
        return id;
      }
    }
  }

  protected final void serverStartIfNecessary()
  {
    if (this.started.compareAndSet(false, true)) {
      try {
        this.server.start();
      } catch (final IcServerException e) {
        this.started.set(false);
        throw new IllegalStateException(e);
      }
    }
  }

  protected final IcUser userGet(
    final UUID id)
    throws IcDatabaseException
  {
    final var database = this.databases.mostRecent();
    try (var connection = database.openConnection(ICATIRO)) {
      try (var transaction = connection.openTransaction()) {
        final var users =
          transaction.queries(IcDatabaseUsersQueriesType.class);

        return users.userGetRequire(id);
      }
    }
  }
}
