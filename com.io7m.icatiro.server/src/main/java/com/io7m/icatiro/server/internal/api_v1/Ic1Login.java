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
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.protocol.api.IcProtocolException;
import com.io7m.icatiro.protocol.api_v1.Ic1CommandLogin;
import com.io7m.icatiro.protocol.api_v1.Ic1Messages;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseLogin;
import com.io7m.icatiro.server.internal.IcHTTPErrorStatusException;
import com.io7m.icatiro.server.internal.IcRequestLimits;
import com.io7m.icatiro.server.internal.IcServerClock;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.HTTP_METHOD_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.SQL_ERROR;
import static com.io7m.icatiro.server.internal.IcServerRequestDecoration.requestIdFor;
import static com.io7m.icatiro.server.logging.IcServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

/**
 * A servlet that handles user logins.
 */

public final class Ic1Login extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(Ic1Login.class);

  private final IcDatabaseType database;
  private final Ic1Messages messages;
  private final IcServerStrings strings;
  private final IcServerClock clock;
  private final Ic1Sends errors;
  private final IcRequestLimits limits;

  /**
   * A servlet that handles user logins.
   *
   * @param inServices The service directory
   */

  public Ic1Login(
    final IcServiceDirectoryType inServices)
  {
    Objects.requireNonNull(inServices, "inServices");

    this.database =
      inServices.requireService(IcDatabaseType.class);
    this.messages =
      inServices.requireService(Ic1Messages.class);
    this.strings =
      inServices.requireService(IcServerStrings.class);
    this.clock =
      inServices.requireService(IcServerClock.class);
    this.errors =
      inServices.requireService(Ic1Sends.class);
    this.limits =
      inServices.requireService(IcRequestLimits.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try (var ignored0 = mdcForRequest(request)) {
      try {
        if (!Objects.equals(request.getMethod(), "POST")) {
          throw new IcHTTPErrorStatusException(
            METHOD_NOT_ALLOWED_405,
            HTTP_METHOD_ERROR,
            this.strings.format("methodNotAllowed")
          );
        }

        final var login =
          this.readLoginCommand(request);

        try (var connection = this.database.openConnection(ICATIRO)) {
          try (var transaction = connection.openTransaction()) {
            final var users =
              transaction.queries(IcDatabaseUsersQueriesType.class);
            this.tryLogin(request, response, users, login);
            transaction.commit();
          }
        }

      } catch (final IcHTTPErrorStatusException e) {
        this.errors.sendError(
          response,
          requestIdFor(request),
          e.statusCode(),
          e.errorCode(),
          e.getMessage()
        );
      } catch (final IcPasswordException e) {
        LOG.debug("password: ", e);
        this.errors.sendError(
          response,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          PASSWORD_ERROR,
          e.getMessage()
        );
      } catch (final IcDatabaseException e) {
        LOG.debug("database: ", e);
        this.errors.sendError(
          response,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          SQL_ERROR,
          e.getMessage()
        );
      }
    }
  }

  private void tryLogin(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final IcDatabaseUsersQueriesType users,
    final Ic1CommandLogin login)
    throws
    IcHTTPErrorStatusException,
    IcDatabaseException,
    IcPasswordException, IOException
  {
    final var userOpt =
      users.userGetForName(new IcUserDisplayName(login.userName()));

    if (userOpt.isEmpty()) {
      throw new IcHTTPErrorStatusException(
        UNAUTHORIZED_401,
        AUTHENTICATION_ERROR,
        this.strings.format("loginFailed")
      );
    }

    final var user =
      userOpt.get();
    final var ok =
      user.password().check(login.password());

    if (!ok) {
      throw new IcHTTPErrorStatusException(
        UNAUTHORIZED_401,
        AUTHENTICATION_ERROR,
        this.strings.format("loginFailed")
      );
    }

    LOG.info("user '{}' logged in", login.userName());
    final var session = request.getSession();
    session.setAttribute("UserID", user.id());
    response.setStatus(200);

    users.userLogin(user.id(), request.getRemoteAddr());

    this.sendLoginResponse(request, response, user);
  }

  private void sendLoginResponse(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final IcUser user)
    throws IOException
  {
    response.setStatus(200);
    response.setContentType(Ic1Messages.contentType());

    try {
      final var data =
        this.messages.serialize(
          new Ic1ResponseLogin(
            requestIdFor(request),
            user.lastLoginTime()
          )
        );
      response.setContentLength(data.length + 2);
      try (var output = response.getOutputStream()) {
        output.write(data);
        output.write('\r');
        output.write('\n');
      }
    } catch (final IcProtocolException e) {
      throw new IOException(e);
    }
  }

  private Ic1CommandLogin readLoginCommand(
    final HttpServletRequest request)
    throws IcHTTPErrorStatusException, IOException
  {
    try (var input = this.limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof Ic1CommandLogin login) {
        return login;
      }
    } catch (final IcProtocolException e) {
      throw new IcHTTPErrorStatusException(
        BAD_REQUEST_400,
        PROTOCOL_ERROR,
        e.getMessage(),
        e
      );
    }

    throw new IcHTTPErrorStatusException(
      BAD_REQUEST_400,
      PROTOCOL_ERROR,
      this.strings.format("expectedCommand", "CommandLogin")
    );
  }
}
