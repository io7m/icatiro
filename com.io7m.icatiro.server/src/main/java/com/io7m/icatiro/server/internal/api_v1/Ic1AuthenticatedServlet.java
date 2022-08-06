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

package com.io7m.icatiro.server.internal.api_v1;

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.protocol.api_v1.Ic1Messages;
import com.io7m.icatiro.server.internal.IcHTTPErrorStatusException;
import com.io7m.icatiro.server.internal.IcServerClock;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.SQL_ERROR;
import static com.io7m.icatiro.server.internal.IcServerRequestDecoration.requestIdFor;
import static com.io7m.icatiro.server.logging.IcServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class Ic1AuthenticatedServlet extends HttpServlet
{
  private final Ic1Sends sends;
  private final IcServerClock clock;
  private final IcServerStrings strings;
  private final Ic1Messages messages;
  private final IcDatabaseType database;
  private IcUser user;

  /**
   * A servlet that checks that a user is authenticated before delegating
   * execution to a subclass.
   *
   * @param services The service directory
   */

  protected Ic1AuthenticatedServlet(
    final IcServiceDirectoryType services)
  {
    Objects.requireNonNull(services, "services");

    this.messages =
      services.requireService(Ic1Messages.class);
    this.strings =
      services.requireService(IcServerStrings.class);
    this.clock =
      services.requireService(IcServerClock.class);
    this.sends =
      services.requireService(Ic1Sends.class);
    this.database =
      services.requireService(IcDatabaseType.class);
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
        final var session = request.getSession(false);
        if (session != null) {
          final var userId = (UUID) session.getAttribute("UserID");
          this.user = this.userGet(userId);
          this.serviceAuthenticated(request, servletResponse, session);
          return;
        }

        this.logger().debug("unauthenticated!");
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          HttpStatus.UNAUTHORIZED_401,
          AUTHENTICATION_ERROR,
          this.strings.format("unauthorized")
        );
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
}
