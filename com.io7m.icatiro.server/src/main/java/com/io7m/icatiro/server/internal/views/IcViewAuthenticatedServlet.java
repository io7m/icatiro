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

package com.io7m.icatiro.server.internal.views;

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.protocol.api_v1.Ic1Messages;
import com.io7m.icatiro.server.internal.IcHTTPErrorStatusException;
import com.io7m.icatiro.server.internal.IcServerClock;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.server.internal.IcServerUserController;
import com.io7m.icatiro.server.internal.IcServerUserControllersService;
import com.io7m.icatiro.server.internal.api_v1.Ic1Sends;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.SQL_ERROR;
import static com.io7m.icatiro.server.internal.IcServerRequestDecoration.requestIdFor;
import static com.io7m.icatiro.server.logging.IcServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class IcViewAuthenticatedServlet extends HttpServlet
{
  private final Ic1Sends sends;
  private final IcServerClock clock;
  private final IcServerStrings strings;
  private final Ic1Messages messages;
  private final IcDatabaseType database;
  private final IcServiceDirectoryType services;
  private final IcServerUserControllersService controllers;
  private IcUser user;
  private IcServerUserController userController;

  /**
   * A servlet that checks that a user is authenticated before delegating
   * execution to a subclass.
   *
   * @param inServices The service directory
   */

  protected IcViewAuthenticatedServlet(
    final IcServiceDirectoryType inServices)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");

    this.messages =
      inServices.requireService(Ic1Messages.class);
    this.strings =
      inServices.requireService(IcServerStrings.class);
    this.clock =
      inServices.requireService(IcServerClock.class);
    this.sends =
      inServices.requireService(Ic1Sends.class);
    this.database =
      inServices.requireService(IcDatabaseType.class);
    this.controllers =
      inServices.requireService(IcServerUserControllersService.class);
  }

  protected final IcServerUserController userController()
  {
    return this.userController;
  }

  /**
   * @return The authenticated user
   */

  protected final IcUser user()
  {
    return this.user;
  }

  protected final UUID userId()
  {
    return this.user().id();
  }

  protected final Ic1Sends sends()
  {
    return this.sends;
  }

  protected final IcServerClock clock()
  {
    return this.clock;
  }

  protected final IcServerStrings strings()
  {
    return this.strings;
  }

  protected final Ic1Messages messages()
  {
    return this.messages;
  }

  protected abstract Logger logger();

  protected abstract void serviceAuthenticated(
    HttpServletRequest request,
    HttpServletResponse servletResponse,
    HttpSession session)
    throws Exception;

  @Override
  protected final void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    try (var ignored0 = mdcForRequest((Request) request)) {
      try {
        final var session = request.getSession(true);
        final var userId = (UUID) session.getAttribute("UserID");
        if (userId != null) {
          this.user = this.userGet(userId);
          this.userController =
            this.controllers.createOrGet(userId, session.getId());
          this.serviceAuthenticated(request, servletResponse, session);
          return;
        }

        servletResponse.setStatus(401);
        new IcViewLogin(this.services).service(request, servletResponse);
      } catch (final IcHTTPErrorStatusException e) {
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          e.statusCode(),
          e.errorCode(),
          e.getMessage()
        );
      } catch (final IcPasswordException e) {
        this.logger().debug("password: ", e);
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          PASSWORD_ERROR,
          e.getMessage()
        );
      } catch (final IcDatabaseException e) {
        this.logger().debug("database: ", e);
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          SQL_ERROR,
          e.getMessage()
        );
      } catch (final Exception e) {
        this.logger().trace("exception: ", e);
        throw new IOException(e);
      }
    }
  }

  private IcUser userGet(final UUID id)
    throws IcDatabaseException
  {
    try (var c = this.database.openConnection(ICATIRO)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IcDatabaseUsersQueriesType.class);
        return q.userGetRequire(id);
      }
    }
  }

  protected final IcDatabaseType database()
  {
    return this.database;
  }
}
