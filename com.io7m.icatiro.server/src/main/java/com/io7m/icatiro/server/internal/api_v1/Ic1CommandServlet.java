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

import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.protocol.api.IcProtocolException;
import com.io7m.icatiro.protocol.api_v1.Ic1CommandType;
import com.io7m.icatiro.protocol.api_v1.Ic1Messages;
import com.io7m.icatiro.server.internal.IcHTTPErrorStatusException;
import com.io7m.icatiro.server.internal.IcRequestLimits;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.icatiro.server.internal.IcServerRequestDecoration.requestIdFor;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;

/**
 * A servlet for executing a single command.
 */

public final class Ic1CommandServlet extends Ic1AuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(Ic1CommandServlet.class);

  private final IcDatabaseType database;
  private final IcRequestLimits limits;
  private final Ic1Messages messages;
  private final Ic1CommandExecutor executor;
  private final IcServiceDirectoryType services;

  /**
   * A servlet for executing a single command.
   *
   * @param inServices The service directory
   */

  public Ic1CommandServlet(
    final IcServiceDirectoryType inServices)
  {
    super(inServices);

    this.services =
      Objects.requireNonNull(inServices, "inServices");
    this.database =
      inServices.requireService(IcDatabaseType.class);
    this.limits =
      inServices.requireService(IcRequestLimits.class);
    this.messages =
      inServices.requireService(Ic1Messages.class);
    this.executor =
      new Ic1CommandExecutor();
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws Exception
  {
    final var requestId =
      requestIdFor(request);

    try (var input = this.limits.boundedMaximumInput(request, 1048576)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof Ic1CommandType command) {
        this.executeCommand(servletResponse, requestId, command);
        return;
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
      this.strings().format("expectedCommand", "IcSA1CommandType")
    );
  }

  private void executeCommand(
    final HttpServletResponse servletResponse,
    final UUID requestId,
    final Ic1CommandType<?> command)
    throws Exception
  {
    try (var connection = this.database.openConnection(ICATIRO)) {
      try (var transaction = connection.openTransaction()) {
        this.executeCommandInTransaction(
          servletResponse,
          requestId,
          command,
          transaction
        );
      }
    }
  }

  private void executeCommandInTransaction(
    final HttpServletResponse servletResponse,
    final UUID requestId,
    final Ic1CommandType<?> command,
    final IcDatabaseTransactionType transaction)
    throws Exception
  {
    final var context =
      new Ic1CommandContext(
        this.services,
        this.strings(),
        requestId,
        transaction,
        this.clock(),
        this.user()
      );

    final var result =
      this.executor.execute(context, command);

    this.sends()
      .send(servletResponse, result.httpStatus(), result.response());

    transaction.commit();
  }
}
