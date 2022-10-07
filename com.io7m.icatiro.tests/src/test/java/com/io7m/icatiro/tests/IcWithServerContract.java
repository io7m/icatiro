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
import com.io7m.icatiro.database.api.IcDatabaseUpgrade;
import com.io7m.icatiro.database.postgres.IcDatabases;
import com.io7m.icatiro.server.IcServers;
import com.io7m.icatiro.server.api.IcServerBrandingConfiguration;
import com.io7m.icatiro.server.api.IcServerConfiguration;
import com.io7m.icatiro.server.api.IcServerHTTPServiceConfiguration;
import com.io7m.icatiro.server.api.IcServerHistoryConfiguration;
import com.io7m.icatiro.server.api.IcServerIdstoreConfiguration;
import com.io7m.icatiro.server.api.IcServerMailConfiguration;
import com.io7m.icatiro.server.api.IcServerMailTransportSMTP;
import com.io7m.icatiro.server.api.IcServerRateLimitConfiguration;
import com.io7m.icatiro.server.api.IcServerType;
import com.io7m.idstore.admin_client.IdAClients;
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.IdServers;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.server.SMTPServer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

@Testcontainers(disabledWithoutDocker = true)
public abstract class IcWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcWithServerContract.class);

  @Container
  private final PostgreSQLContainer<?> icatiroContainer =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("icatiro")
      .withUsername("postgres")
      .withPassword("12345678");

  @Container
  private final PostgreSQLContainer<?> idstoreContainer =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("idstore")
      .withUsername("postgres")
      .withPassword("12345678");

  private IdServerType idstore;
  private ConcurrentLinkedQueue<MimeMessage> emailsReceived;
  private SMTPServer smtp;
  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private IcFakeClock clock;
  private IcServerType icatiro;
  private IdAClients idstoreAdmins;
  private IdAClientType idstoreAdmin;

  @BeforeEach
  public final void serverSetup()
    throws Exception
  {
    LOG.debug("serverSetup");

    this.clock = new IcFakeClock();
    this.resources = CloseableCollection.create();

    this.waitForDatabaseToStart();
    this.smtp = this.createMailServer();
    this.resources.add(() -> this.smtp.stop());

    this.idstoreAdmins =
      new IdAClients();
    this.idstoreAdmin =
      this.resources.add(this.idstoreAdmins.create(Locale.ROOT));

    this.idstore = this.resources.add(this.createIdstoreServer());
    this.idstore.setup(
      Optional.of(UUID.fromString("d6573475-96ab-498a-9794-64fb03beb6b0")),
      new IdName("admin"),
      new IdEmail("admin@example.com"),
      new IdRealName("Administrator"),
      "12345678"
    );
    this.idstore.start();

    this.icatiro = this.resources.add(this.createIcatiroServer());
    this.icatiro.start();
  }

  private IcServerType createIcatiroServer()
  {
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
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
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

    final var configuration =
      new IcServerConfiguration(
        Locale.ROOT,
        this.clock,
        new IcDatabases(),
        databaseConfiguration,
        mailService,
        userApiService,
        userViewService,
        branding,
        history,
        rateLimit,
        Optional.empty(),
        new IcServerIdstoreConfiguration(
          URI.create("http://localhost:50000"),
          URI.create("http://localhost:50001/password-reset")
        )
      );

    return new IcServers()
      .createServer(configuration);
  }

  private SMTPServer createMailServer()
  {
    this.emailsReceived =
      new ConcurrentLinkedQueue<>();
    final var smtp =
      SMTPServer.port(25000)
        .messageHandler((messageContext, source, destination, data) -> {
          LOG.debug(
            "received mail: {} {} {}",
            source,
            destination,
            Integer.valueOf(data.length)
          );

          try {
            final var message =
              new MimeMessage(
                Session.getDefaultInstance(new Properties()),
                new ByteArrayInputStream(data)
              );

            this.emailsReceived.add(message);
          } catch (final MessagingException e) {
            throw new IllegalStateException(e);
          }
        })
        .build();
    smtp.start();
    return smtp;
  }

  @AfterEach
  public final void serverTearDown()
    throws Exception
  {
    LOG.debug("serverTearDown");
    this.resources.close();
  }

  protected final UUID createIdstoreUser(
    final String name)
    throws Exception
  {
    this.idstoreAdmin.login(
      "admin",
      "12345678",
      URI.create("http://localhost:51000/")
    );

    final var user =
      this.idstoreAdmin.userCreate(
        Optional.empty(),
        new IdName(name),
        new IdRealName(name),
        new IdEmail("%s@example.com".formatted(name)),
        IdPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed("12345678")
      );

    return user.id();
  }

  private void waitForDatabaseToStart()
    throws InterruptedException, TimeoutException
  {
    LOG.debug("waiting for database to start");
    final var timeWait = Duration.ofSeconds(60L);
    final var timeThen = Instant.now();
    while (!this.icatiroContainer.isRunning()
           && !this.idstoreContainer.isRunning()) {
      Thread.sleep(1L);
      final var timeNow = Instant.now();
      if (Duration.between(timeThen, timeNow).compareTo(timeWait) > 0) {
        LOG.error("timed out waiting for database to start");
        throw new TimeoutException("Timed out waiting for database to start");
      }
    }
    LOG.debug("database started");
  }

  private IdServerType createIdstoreServer()
  {
    LOG.debug("creating server");

    final var databaseConfiguration =
      new IdDatabaseConfiguration(
        "postgres",
        "12345678",
        this.idstoreContainer.getHost(),
        this.idstoreContainer.getFirstMappedPort().intValue(),
        "postgres",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.UPGRADE_DATABASE,
        this.clock
      );

    final var mailService =
      new IdServerMailConfiguration(
        new IdServerMailTransportSMTP("localhost", 25000),
        Optional.empty(),
        "no-reply@example.com",
        Duration.ofDays(1L)
      );

    final var userApiService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        50000,
        URI.create("http://localhost:50000/"),
        Optional.empty()
      );
    final var userViewService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        50001,
        URI.create("http://localhost:50001/"),
        Optional.empty()
      );
    final var adminApiService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        51000,
        URI.create("http://localhost:51000/"),
        Optional.empty()
      );

    final var branding =
      new IdServerBrandingConfiguration(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      );

    final var history =
      new IdServerHistoryConfiguration(
        100,
        100
      );

    final var rateLimit =
      new IdServerRateLimitConfiguration(
        Duration.of(10L, ChronoUnit.MINUTES),
        Duration.of(10L, ChronoUnit.MINUTES)
      );

    return new IdServers().createServer(
      new IdServerConfiguration(
        Locale.getDefault(),
        this.clock,
        new IdDatabases(),
        databaseConfiguration,
        mailService,
        userApiService,
        userViewService,
        adminApiService,
        branding,
        history,
        rateLimit,
        Optional.empty()
      )
    );
  }

  protected static URI serverAPIBase()
  {
    return URI.create("http://localhost:40000/");
  }

  protected final IcServerType icatiro()
  {
    return this.icatiro;
  }
}
