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

package com.io7m.icatiro.server.internal;

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.protocol.tickets.cb.IcT1Messages;
import com.io7m.icatiro.server.api.IcServerConfiguration;
import com.io7m.icatiro.server.api.IcServerException;
import com.io7m.icatiro.server.api.IcServerType;
import com.io7m.icatiro.server.internal.common.IcCommonCSSServlet;
import com.io7m.icatiro.server.internal.common.IcCommonLogoServlet;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateService;
import com.io7m.icatiro.server.internal.tickets_v1.IcT1CommandServlet;
import com.io7m.icatiro.server.internal.tickets_v1.IcT1Login;
import com.io7m.icatiro.server.internal.tickets_v1.IcT1Sends;
import com.io7m.icatiro.server.internal.tickets_v1.IcT1Versions;
import com.io7m.icatiro.server.internal.views.IcViewLogin;
import com.io7m.icatiro.server.internal.views.IcViewLogout;
import com.io7m.icatiro.server.internal.views.IcViewMain;
import com.io7m.icatiro.server.logging.IcServerRequestLog;
import com.io7m.icatiro.services.api.IcServiceDirectory;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;

/**
 * The main server implementation.
 */

public final class IcServer implements IcServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcServer.class);

  private final IcServerConfiguration configuration;
  private CloseableCollectionType<IcServerException> resources;
  private final AtomicBoolean closed;
  private IcDatabaseType database;
  private IcServerTelemetryService telemetry;

  /**
   * The main server implementation.
   *
   * @param inConfiguration The server configuration
   */

  public IcServer(
    final IcServerConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.resources =
      createResourceCollection();
    this.closed =
      new AtomicBoolean(false);
  }

  private static CloseableCollectionType<IcServerException> createResourceCollection()
  {
    return CloseableCollection.create(
      () -> {
        return new IcServerException(
          new IcErrorCode("server-creation"),
          "Server creation failed."
        );
      }
    );
  }
  
  @Override
  public void start()
    throws IcServerException
  {
    this.closed.set(false);
    this.resources = createResourceCollection();

    this.telemetry =
      IcServerTelemetryService.create(this.configuration);

    final var startupSpan =
      this.telemetry.tracer()
        .spanBuilder("IcServer.start")
        .setSpanKind(SpanKind.INTERNAL)
        .startSpan();

    try {
      this.database =
        this.resources.add(this.createDatabase(this.telemetry.openTelemetry()));

      final var services = this.createServiceDirectory(this.database);
      this.resources.add(services);

      final var userAPIServer = this.createAPIServer(services);
      this.resources.add(userAPIServer::stop);

      final var userViewServer = this.createViewServer(services);
      this.resources.add(userViewServer::stop);
    } catch (final IcDatabaseException e) {
      startupSpan.recordException(e);

      try {
        this.close();
      } catch (final IcServerException ex) {
        e.addSuppressed(ex);
      }
      throw new IcServerException(
        new IcErrorCode("database"),
        e.getMessage()
      );
    } catch (final Exception e) {
      startupSpan.recordException(e);

      try {
        this.close();
      } catch (final IcServerException ex) {
        e.addSuppressed(ex);
      }
      throw new IcServerException(
        new IcErrorCode("startup"),
        e.getMessage(),
        e
      );
    } finally {
      startupSpan.end();
    }
  }

  @Override
  public IcDatabaseType database()
  {
    return Optional.ofNullable(this.database)
      .orElseThrow(() -> {
        return new IllegalStateException("Server is not started.");
      });
  }

  private IcServiceDirectory createServiceDirectory(
    final IcDatabaseType inDatabase)
    throws IOException
  {
    final var services = new IcServiceDirectory();
    services.register(IcServerTelemetryService.class, this.telemetry);
    services.register(IcDatabaseType.class, inDatabase);

    final var userSessions = new IcUserSessionService(this.telemetry);
    services.register(IcUserSessionService.class, userSessions);

    final var idClients =
      IcIdentityClients.create(
        this.configuration.locale(),
        this.configuration.idstore()
      );
    services.register(IcIdentityClients.class, idClients);

    final var clock = new IcServerClock(this.configuration.clock());
    services.register(IcServerClock.class, clock);

    final var strings = new IcServerStrings(this.configuration.locale());
    services.register(IcServerStrings.class, strings);

    final var templates = IcFMTemplateService.create();
    services.register(IcFMTemplateService.class, templates);

    final var branding =
      IcServerBrandingService.create(strings, templates, this.configuration.branding());
    services.register(IcServerBrandingService.class, branding);

    services.register(IcRequestLimits.class, new IcRequestLimits(strings));
    services.register(IcVerdantMessages.class, new IcVerdantMessages());
    services.register(IcT1Messages.class, new IcT1Messages());
    services.register(
      IcT1Sends.class,
      new IcT1Sends(services.requireService(IcT1Messages.class))
    );
    return services;
  }

  private Server createViewServer(
    final IcServiceDirectoryType services)
    throws Exception
  {
    final var httpConfig =
      this.configuration.userViewAddress();
    final var address =
      InetSocketAddress.createUnresolved(
        httpConfig.listenAddress(),
        httpConfig.listenPort()
      );

    final var server =
      new Server(address);

    /*
     * Configure all the servlets.
     */

    final var servletHolders =
      new IcServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    servlets.addEventListener(
      services.requireService(IcUserSessionService.class)
    );

    servlets.addServlet(
      servletHolders.create(IcViewMain.class, IcViewMain::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(IcViewLogout.class, IcViewLogout::new),
      "/logout"
    );
    servlets.addServlet(
      servletHolders.create(IcViewLogin.class, IcViewLogin::new),
      "/login"
    );
    servlets.addServlet(
      servletHolders.create(IcViewLogin.class, IcViewLogin::new),
      "/login/*"
    );

    servlets.addServlet(
      servletHolders.create(IcCommonCSSServlet.class, IcCommonCSSServlet::new),
      "/css/*"
    );
    servlets.addServlet(
      servletHolders.create(IcCommonCSSServlet.class, IcCommonCSSServlet::new),
      "/css"
    );
    servlets.addServlet(
      servletHolders.create(
        IcCommonLogoServlet.class,
        IcCommonLogoServlet::new),
      "/logo/*"
    );
    servlets.addServlet(
      servletHolders.create(
        IcCommonLogoServlet.class,
        IcCommonLogoServlet::new),
      "/logo"
    );

    servlets.addServlet(
      servletHolders.create(
        IcCommonLogoServlet.class,
        IcCommonLogoServlet::new),
      "/favicon.ico"
    );

    /*
     * Set up a session handler.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    server.setSessionIdManager(sessionIds);

    final var sessionHandler = new SessionHandler();
    sessionHandler.setSessionCookie("ICATIRO_VIEW_SESSION");

    final var sessionStore = new NullSessionDataStore();
    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Enable gzip.
     */

    final var gzip = new GzipHandler();
    gzip.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new IcServerRequestDecoration(services))
    );

    server.setErrorHandler(new IcErrorHandler());
    server.setRequestLog(new IcServerRequestLog(services, "view"));
    server.setHandler(gzip);
    server.start();
    LOG.info("[{}] view server started", address);
    return server;
  }

  private Server createAPIServer(
    final IcServiceDirectoryType services)
    throws Exception
  {
    final var httpConfig =
      this.configuration.userApiAddress();
    final var address =
      InetSocketAddress.createUnresolved(
        httpConfig.listenAddress(),
        httpConfig.listenPort()
      );

    final var server =
      new Server(address);

    /*
     * Configure all the servlets.
     */

    final var servletHolders =
      new IcServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    servlets.addServlet(
      servletHolders.create(IcT1Versions.class, IcT1Versions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(IcT1Login.class, IcT1Login::new),
      "/tickets/1/0/login"
    );
    servlets.addServlet(
      servletHolders.create(IcT1CommandServlet.class, IcT1CommandServlet::new),
      "/tickets/1/0/command"
    );

    servlets.addEventListener(
      services.requireService(IcUserSessionService.class)
    );

    /*
     * Set up a session handler.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    server.setSessionIdManager(sessionIds);

    final var sessionHandler = new SessionHandler();
    sessionHandler.setSessionCookie("ICATIRO_API_SESSION");

    final var sessionStore = new NullSessionDataStore();
    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Enable gzip.
     */

    final var gzip = new GzipHandler();
    gzip.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new IcServerRequestDecoration(services))
    );

    server.setErrorHandler(new IcErrorHandler());
    server.setRequestLog(new IcServerRequestLog(services, "public"));
    server.setHandler(gzip);
    server.start();
    LOG.info("[{}] API server started", address);
    return server;
  }

  private IcDatabaseType createDatabase(
    final OpenTelemetry openTelemetry)
    throws IcDatabaseException
  {
    return this.configuration.databases()
      .open(
        this.configuration.databaseConfiguration(),
        openTelemetry,
        event -> {

        });
  }

  @Override
  public void close()
    throws IcServerException
  {
    if (this.closed.compareAndSet(false, true)) {
      this.resources.close();
    }
  }

  @Override
  public void userInitialSet(
    final UUID userId)
    throws IcServerException
  {
    try (var c = this.database.openConnection(ICATIRO)) {
      try (var t = c.openTransaction()) {
        final var users =
          t.queries(IcDatabaseUsersQueriesType.class);
        users.userInitialSet(userId);
        t.commit();
      }
    } catch (final IcDatabaseException e) {
      throw new IcServerException(e.errorCode(), e.getMessage(), e);
    }
  }

  @Override
  public void userInitialUnset(
    final UUID userId)
    throws IcServerException
  {
    try (var c = this.database.openConnection(ICATIRO)) {
      try (var t = c.openTransaction()) {
        final var users =
          t.queries(IcDatabaseUsersQueriesType.class);
        users.userInitialUnset(userId);
        t.commit();
      }
    } catch (final IcDatabaseException e) {
      throw new IcServerException(e.errorCode(), e.getMessage(), e);
    }
  }
}
