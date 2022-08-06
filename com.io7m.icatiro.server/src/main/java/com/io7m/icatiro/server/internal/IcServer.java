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
import com.io7m.icatiro.protocol.api_v1.Ic1Messages;
import com.io7m.icatiro.protocol.versions.IcVMessages;
import com.io7m.icatiro.server.api.IcServerConfiguration;
import com.io7m.icatiro.server.api.IcServerException;
import com.io7m.icatiro.server.api.IcServerType;
import com.io7m.icatiro.server.internal.api_v1.Ic1CommandServlet;
import com.io7m.icatiro.server.internal.api_v1.Ic1Login;
import com.io7m.icatiro.server.internal.api_v1.Ic1Sends;
import com.io7m.icatiro.server.internal.api_v1.Ic1Versions;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateService;
import com.io7m.icatiro.server.internal.views.IcViewLogin;
import com.io7m.icatiro.server.internal.views.IcViewLogout;
import com.io7m.icatiro.server.internal.views.IcViewMain;
import com.io7m.icatiro.server.internal.views.IcViewResource;
import com.io7m.icatiro.server.logging.IcServerRequestLog;
import com.io7m.icatiro.services.api.IcServiceDirectory;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main server implementation.
 */

public final class IcServer implements IcServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcServer.class);

  private final IcServerConfiguration configuration;
  private final CloseableCollectionType<IcServerException> resources;
  private final AtomicBoolean closed;
  private IcDatabaseType database;

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
      CloseableCollection.create(
        () -> {
          return new IcServerException(
            "Server creation failed.",
            "server-creation"
          );
        }
      );

    this.closed =
      new AtomicBoolean(false);
  }

  @Override
  public void start()
    throws IcServerException
  {
    if (this.closed.get()) {
      throw new IllegalStateException("Server is closed!");
    }

    try {
      this.database =
        this.resources.add(this.createDatabase());

      final var services =
        this.createServiceDirectory(this.database);
      this.resources.add(services);

      final var apiServer = this.createAPIServer(services);
      this.resources.add(apiServer::stop);

      final var viewServer = this.createViewServer(services);
      this.resources.add(viewServer::stop);

    } catch (final IcDatabaseException e) {
      try {
        this.close();
      } catch (final IcServerException ex) {
        e.addSuppressed(ex);
      }
      throw new IcServerException(e.getMessage(), e, "database");
    } catch (final Exception e) {
      try {
        this.close();
      } catch (final IcServerException ex) {
        e.addSuppressed(ex);
      }
      throw new IcServerException(e.getMessage(), e, "startup");
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

    services.register(
      IcDatabaseType.class,
      inDatabase
    );

    final var userControllers = new IcServerUserControllersService();
    services.register(IcServerUserControllersService.class, userControllers);

    final var versionMessages = new IcVMessages();
    services.register(IcVMessages.class, versionMessages);

    final var api1messages = new Ic1Messages();
    services.register(Ic1Messages.class, api1messages);

    final var clock = new IcServerClock(this.configuration.clock());
    services.register(IcServerClock.class, clock);

    final var config = new IcServerConfigurations(this.configuration);
    services.register(IcServerConfigurations.class, config);

    final var strings = new IcServerStrings(this.configuration.locale());
    services.register(IcServerStrings.class, strings);

    final var templates = IcFMTemplateService.create();
    services.register(IcFMTemplateService.class, templates);

    services.register(Ic1Sends.class, new Ic1Sends(api1messages));
    services.register(IcRequestLimits.class, new IcRequestLimits(strings));
    return services;
  }

  private Server createViewServer(
    final IcServiceDirectoryType services)
    throws Exception
  {
    final var address =
      this.configuration.viewAddress();
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
      services.requireService(IcServerUserControllersService.class)
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
      servletHolders.create(IcViewResource.class, IcViewResource::new),
      "/resource/*"
    );

    /*
     * Set up a session handler that allows for Servlets to have sessions
     * that can survive server restarts.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    final var sessionHandler = new SessionHandler();

    final var sessionStore = new FileSessionDataStore();
    sessionStore.setStoreDir(this.configuration.sessionDirectory().toFile());

    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Set up an MBean container so that the statistics handler can export
     * statistics to JMX.
     */

    final var mbeanContainer =
      new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbeanContainer);

    /*
     * Set up a statistics handler that wraps everything.
     */

    final var statsHandler = new StatisticsHandler();
    statsHandler.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new IcServerRequestDecoration(services))
    );

    server.setErrorHandler(new IcErrorHandler());
    server.setRequestLog(new IcServerRequestLog(services, "view"));
    server.setHandler(statsHandler);
    server.start();
    LOG.info("[{}] view server started", address);
    return server;
  }

  private Server createAPIServer(
    final IcServiceDirectoryType services)
    throws Exception
  {
    final var address =
      this.configuration.apiAddress();
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
      services.requireService(IcServerUserControllersService.class)
    );

    servlets.addServlet(
      servletHolders.create(Ic1Versions.class, Ic1Versions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(Ic1Login.class, Ic1Login::new),
      "/api/1/0/login"
    );
    servlets.addServlet(
      servletHolders.create(Ic1CommandServlet.class, Ic1CommandServlet::new),
      "/api/1/0/command"
    );

    /*
     * Set up a session handler that allows for Servlets to have sessions
     * that can survive server restarts.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    final var sessionHandler = new SessionHandler();

    final var sessionStore = new FileSessionDataStore();
    sessionStore.setStoreDir(this.configuration.sessionDirectory().toFile());

    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Set up an MBean container so that the statistics handler can export
     * statistics to JMX.
     */

    final var mbeanContainer =
      new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbeanContainer);

    /*
     * Set up a statistics handler that wraps everything.
     */

    final var statsHandler = new StatisticsHandler();
    statsHandler.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new IcServerRequestDecoration(services))
    );

    server.setErrorHandler(new IcErrorHandler());
    server.setRequestLog(new IcServerRequestLog(services, "public"));
    server.setHandler(statsHandler);
    server.start();
    LOG.info("[{}] API server started", address);
    return server;
  }

  private IcDatabaseType createDatabase()
    throws IcDatabaseException
  {
    return this.configuration.databases()
      .open(
        this.configuration.databaseConfiguration(),
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
}
