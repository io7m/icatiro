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


package com.io7m.icatiro.server.internal.tickets_v1;

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.error_codes.IcStandardErrorCodes;
import com.io7m.icatiro.model.IcPermissionSet;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.protocol.IcProtocolException;
import com.io7m.icatiro.protocol.tickets.IcTCommandLogin;
import com.io7m.icatiro.protocol.tickets.IcTResponseLogin;
import com.io7m.icatiro.protocol.tickets.cb.IcT1Messages;
import com.io7m.icatiro.server.internal.IcHTTPErrorStatusException;
import com.io7m.icatiro.server.internal.IcIdentityClients;
import com.io7m.icatiro.server.internal.IcRequestLimits;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.server.internal.IcUserSessionService;
import com.io7m.icatiro.server.internal.IcUserUpdates;
import com.io7m.icatiro.server.internal.common.IcCommonInstrumentedServlet;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import com.io7m.idstore.model.IdLoginMetadataStandard;
import com.io7m.idstore.user_client.api.IdUClientException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.HTTP_METHOD_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.icatiro.server.internal.IcServerRequestDecoration.requestIdFor;
import static com.io7m.icatiro.server.logging.IcServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;

/**
 * A servlet that handles user logins.
 */

public final class IcT1Login extends IcCommonInstrumentedServlet
{
  private final IcT1Messages messages;
  private final IcServerStrings strings;
  private final IcT1Sends errors;
  private final IcRequestLimits limits;
  private final IcIdentityClients idClients;
  private final IcUserSessionService sessions;
  private final IcDatabaseType database;

  /**
   * A servlet that handles user logins.
   *
   * @param inServices The service directory
   */

  public IcT1Login(
    final IcServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.messages =
      inServices.requireService(IcT1Messages.class);
    this.idClients =
      inServices.requireService(IcIdentityClients.class);
    this.strings =
      inServices.requireService(IcServerStrings.class);
    this.errors =
      inServices.requireService(IcT1Sends.class);
    this.limits =
      inServices.requireService(IcRequestLimits.class);
    this.sessions =
      inServices.requireService(IcUserSessionService.class);
    this.database =
      inServices.requireService(IcDatabaseType.class);
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

        final var client =
          this.idClients.createClient();

        final var user =
          client.login(
            login.userName(),
            login.password(),
            this.idClients.baseURI(),
            Map.ofEntries(
              Map.entry(
                IdLoginMetadataStandard.remoteHostProxied(),
                request.getRemoteAddr()
              )
            )
          );

        var icUser =
          new IcUser(
            user.id(),
            user.idName(),
            user.emails().toList(),
            IcPermissionSet.empty()
          );

        icUser = IcUserUpdates.userMerge(this.database, icUser);
        final var httpSession = request.getSession(true);
        this.sessions.create(icUser, httpSession, client);
        httpSession.setAttribute("UserID", user.id());
        this.sendLoginResponse(request, response, icUser);

      } catch (final IcHTTPErrorStatusException e) {
        this.errors.sendError(
          response,
          requestIdFor(request),
          e.statusCode(),
          e.errorCode(),
          e.getMessage()
        );
      } catch (final IdUClientException e) {
        this.errors.sendError(
          response,
          requestIdFor(request),
          401,
          new IcErrorCode(e.errorCode().id()),
          e.getMessage()
        );
      } catch (final InterruptedException e) {
        this.errors.sendError(
          response,
          requestIdFor(request),
          500,
          IcStandardErrorCodes.IO_ERROR,
          e.getMessage()
        );
      } catch (final IcDatabaseException e) {
        this.errors.sendError(
          response,
          requestIdFor(request),
          500,
          e.errorCode(),
          e.getMessage()
        );
      }
    }
  }

  private void sendLoginResponse(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final IcUser user)
    throws IOException
  {
    response.setStatus(200);
    response.setContentType(IcT1Messages.contentType());

    try {
      final var data =
        this.messages.serialize(
          new IcTResponseLogin(requestIdFor(request), user)
        );
      response.setContentLength(data.length);
      try (var output = response.getOutputStream()) {
        output.write(data);
      }
    } catch (final IcProtocolException e) {
      throw new IOException(e);
    }
  }

  private IcTCommandLogin readLoginCommand(
    final HttpServletRequest request)
    throws IcHTTPErrorStatusException, IOException
  {
    try (var input = this.limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof IcTCommandLogin login) {
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
