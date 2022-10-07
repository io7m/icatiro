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


package com.io7m.icatiro.server.internal.command_exec;

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.model.IcAccessControlledType;
import com.io7m.icatiro.model.IcPermission;
import com.io7m.icatiro.model.IcValidityException;
import com.io7m.icatiro.protocol.IcProtocolException;
import com.io7m.icatiro.protocol.IcProtocolMessageType;
import com.io7m.icatiro.server.internal.IcSecurityException;
import com.io7m.icatiro.server.internal.IcServerClock;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.server.internal.IcServerTelemetryService;
import com.io7m.icatiro.server.internal.IcUserSession;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import com.io7m.idstore.model.IdEmail;
import io.opentelemetry.api.trace.Tracer;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.HTTP_PARAMETER_INVALID;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.MAIL_SYSTEM_FAILURE;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROTOCOL_ERROR;

/**
 * The context for execution of a command (or set of commands in a
 * transaction).
 *
 * @param <E> The type of error messages
 */

public abstract class IcCommandContext<E extends IcProtocolMessageType>
{
  private final IcServiceDirectoryType services;
  private final UUID requestId;
  private final IcDatabaseTransactionType transaction;
  private final IcServerClock clock;
  private final IcServerStrings strings;
  private final IcUserSession userSession;
  private final String remoteHost;
  private final String remoteUserAgent;
  private final Tracer tracer;

  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices        The service directory
   * @param inStrings         The string resources
   * @param inRequestIc       The request ID
   * @param inTransaction     The transaction
   * @param inClock           The clock
   * @param inSession         The user session
   * @param inRemoteHost      The remote host
   * @param inRemoteUserAgent The remote user agent
   */

  public IcCommandContext(
    final IcServiceDirectoryType inServices,
    final IcServerStrings inStrings,
    final UUID inRequestIc,
    final IcDatabaseTransactionType inTransaction,
    final IcServerClock inClock,
    final IcUserSession inSession,
    final String inRemoteHost,
    final String inRemoteUserAgent)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.requestId =
      Objects.requireNonNull(inRequestIc, "requestId");
    this.transaction =
      Objects.requireNonNull(inTransaction, "transaction");
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.userSession =
      Objects.requireNonNull(inSession, "inSession");
    this.remoteHost =
      Objects.requireNonNull(inRemoteHost, "remoteHost");
    this.remoteUserAgent =
      Objects.requireNonNull(inRemoteUserAgent, "remoteUserAgent");
    this.tracer =
      inServices.requireService(IcServerTelemetryService.class)
        .tracer();
  }

  /**
   * @return The user session
   */

  public final IcUserSession userSession()
  {
    return this.userSession;
  }

  /**
   * @return The remote host
   */

  public final String remoteHost()
  {
    return this.remoteHost;
  }

  /**
   * @return The remote user agent
   */

  public final String remoteUserAgent()
  {
    return this.remoteUserAgent;
  }

  /**
   * @return The service directory used during execution
   */

  public final IcServiceDirectoryType services()
  {
    return this.services;
  }

  /**
   * @return The ID of the incoming request
   */

  public final UUID requestId()
  {
    return this.requestId;
  }

  /**
   * @return The database transaction
   */

  public final IcDatabaseTransactionType transaction()
  {
    return this.transaction;
  }

  /**
   * @return The OpenTelemetry tracer
   */

  public final Tracer tracer()
  {
    return this.tracer;
  }

  /**
   * @return The current time
   */

  public final OffsetDateTime now()
  {
    return this.clock.now();
  }

  /**
   * Produce an exception indicating an error, with a formatted error message.
   *
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param messageId  The string resource message ID
   * @param args       The string resource format arguments
   *
   * @return An execution failure
   */

  public final IcCommandExecutionFailure failFormatted(
    final int statusCode,
    final IcErrorCode errorCode,
    final String messageId,
    final Object... args)
  {
    return this.fail(
      statusCode,
      errorCode,
      this.strings.format(messageId, args)
    );
  }

  /**
   * Produce an exception indicating an error, with a string constant message.
   *
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param message    The string message
   *
   * @return An execution failure
   */

  public final IcCommandExecutionFailure fail(
    final int statusCode,
    final IcErrorCode errorCode,
    final String message)
  {
    return new IcCommandExecutionFailure(
      message,
      this.requestId,
      statusCode,
      errorCode
    );
  }

  protected abstract E error(
    UUID id,
    IcErrorCode errorCode,
    String message
  );

  /**
   * Produce an exception indicating a database error.
   *
   * @param e The database exception
   *
   * @return An execution failure
   */

  public final IcCommandExecutionFailure failDatabase(
    final IcDatabaseException e)
  {
    return new IcCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      500,
      e.errorCode()
    );
  }

  /**
   * Produce an exception indicating a mail system error.
   *
   * @param email The email address
   * @param e     The mail system exception
   *
   * @return An execution failure
   */

  public final IcCommandExecutionFailure failMail(
    final IdEmail email,
    final Exception e)
  {
    return new IcCommandExecutionFailure(
      this.strings.format(
        "mailSystemFailure",
        email,
        e.getMessage()
      ),
      e,
      this.requestId,
      500,
      MAIL_SYSTEM_FAILURE
    );
  }

  /**
   * Produce an exception indicating a validation error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public IcCommandExecutionFailure failValidity(
    final IcValidityException e)
  {
    return new IcCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      400,
      HTTP_PARAMETER_INVALID
    );
  }

  /**
   * Produce an exception indicating a protocol error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public IcCommandExecutionFailure failProtocol(
    final IcProtocolException e)
  {
    return new IcCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      400,
      PROTOCOL_ERROR
    );
  }

  /**
   * Check that the current user has the required permission.
   *
   * @param object     The object
   * @param permission The permission
   *
   * @throws IcSecurityException If the check failed
   */

  public void permissionCheck(
    final IcAccessControlledType object,
    final IcPermission permission)
    throws IcSecurityException
  {
    final var user = this.userSession().user();
    if (!user.permissions().implies(object, permission)) {
      throw new IcSecurityException(
        this.strings.format(
          "errorPermissionsRequired",
          permission,
          object.objectType(),
          object
        ),
        OPERATION_NOT_PERMITTED
      );
    }
  }

  /**
   * Produce an exception indicating a security error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public IcCommandExecutionFailure failSecurity(
    final IcSecurityException e)
  {
    return new IcCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      403,
      e.errorCode()
    );
  }
}
