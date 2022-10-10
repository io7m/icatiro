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


package com.io7m.icatiro.client.internal;

import com.io7m.icatiro.client.api.IcClientException;
import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.model.IcPage;
import com.io7m.icatiro.model.IcPermissionScopedType;
import com.io7m.icatiro.model.IcProject;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicket;
import com.io7m.icatiro.model.IcTicketComment;
import com.io7m.icatiro.model.IcTicketCommentCreation;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketID;
import com.io7m.icatiro.model.IcTicketSearch;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.protocol.IcProtocolException;
import com.io7m.icatiro.protocol.tickets.IcTCommandLogin;
import com.io7m.icatiro.protocol.tickets.IcTCommandPermissionGrant;
import com.io7m.icatiro.protocol.tickets.IcTCommandProjectCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketCommentCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketGet;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchBegin;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchNext;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchPrevious;
import com.io7m.icatiro.protocol.tickets.IcTCommandType;
import com.io7m.icatiro.protocol.tickets.IcTResponseError;
import com.io7m.icatiro.protocol.tickets.IcTResponseLogin;
import com.io7m.icatiro.protocol.tickets.IcTResponsePermissionGrant;
import com.io7m.icatiro.protocol.tickets.IcTResponseProjectCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketCommentCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketGet;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchBegin;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchNext;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchPrevious;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;
import com.io7m.icatiro.protocol.tickets.cb.IcT1Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.icatiro.client.internal.IcCompression.decompressResponse;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.IO_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static java.net.http.HttpResponse.BodyHandlers;

/**
 * The version 1 protocol handler.
 */

public final class IcClientProtocolHandler1
  extends IcClientProtocolHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcClientProtocolHandler1.class);

  private final URI commandURI;
  private final IcT1Messages messages;
  private final URI loginURI;
  private IcTCommandLogin mostRecentLogin;

  /**
   * The version 1 protocol handler.
   *
   * @param inHttpClient The HTTP client
   * @param inStrings    The string resources
   * @param inBase       The base URI
   */

  public IcClientProtocolHandler1(
    final HttpClient inHttpClient,
    final IcStrings inStrings,
    final URI inBase)
  {
    super(inHttpClient, inStrings, inBase);

    this.messages =
      new IcT1Messages();

    this.loginURI =
      inBase.resolve("login")
        .normalize();
    this.commandURI =
      inBase.resolve("command")
        .normalize();
  }

  @Override
  public IcNewHandler login(
    final String user,
    final String password,
    final URI base)
    throws IcClientException, InterruptedException
  {
    this.mostRecentLogin = new IcTCommandLogin(user, password);
    final var result = this.sendLogin(this.mostRecentLogin).user();
    return new IcNewHandler(result, this);
  }

  private IcTResponseLogin sendLogin(
    final IcTCommandLogin message)
    throws InterruptedException, IcClientException
  {
    return this.send(1, this.loginURI, IcTResponseLogin.class, true, message);
  }

  private <T extends IcTResponseType> T sendCommand(
    final Class<T> responseClass,
    final IcTCommandType<T> message)
    throws InterruptedException, IcClientException
  {
    return this.send(1, this.commandURI, responseClass, false, message);
  }

  private <T extends IcTResponseType> T send(
    final int attempt,
    final URI uri,
    final Class<T> responseClass,
    final boolean isLoggingIn,
    final IcTCommandType<T> message)
    throws InterruptedException, IcClientException
  {
    try {
      final var commandType = message.getClass().getSimpleName();
      LOG.debug("sending {} to {}", commandType, uri);

      final var sendBytes =
        this.messages.serialize(message);

      final var request =
        HttpRequest.newBuilder(uri)
          .POST(HttpRequest.BodyPublishers.ofByteArray(sendBytes))
          .build();

      final var response =
        this.httpClient()
          .send(request, BodyHandlers.ofByteArray());

      LOG.debug("server: status {}", response.statusCode());

      final var responseHeaders =
        response.headers();

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      if (!contentType.equals(IcT1Messages.contentType())) {
        throw new IcClientException(
          PROTOCOL_ERROR,
          this.strings()
            .format(
              "errorContentType",
              commandType,
              IcT1Messages.contentType(),
              contentType)
        );
      }

      final var responseMessage =
        this.messages.parse(decompressResponse(response, responseHeaders));

      if (!(responseMessage instanceof IcTResponseType)) {
        throw new IcClientException(
          PROTOCOL_ERROR,
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              commandType,
              IcTResponseType.class,
              responseMessage.getClass())
        );
      }

      final var responseActual = (IcTResponseType) responseMessage;
      if (responseActual instanceof IcTResponseError error) {
        if (attempt < 3) {
          if (isAuthenticationError(error) && !isLoggingIn) {
            LOG.debug("attempting re-login");
            this.sendLogin(this.mostRecentLogin);
            return this.send(
              attempt + 1,
              uri,
              responseClass,
              false,
              message
            );
          }
        }

        throw new IcClientException(
          new IcErrorCode(error.errorCode()),
          this.strings()
            .format(
              "errorResponse",
              error.requestId(),
              commandType,
              Integer.valueOf(response.statusCode()),
              error.errorCode(),
              error.message())
        );
      }

      if (!Objects.equals(responseActual.getClass(), responseClass)) {
        throw new IcClientException(
          PROTOCOL_ERROR,
          this.strings()
            .format(
              "errorResponseType",
              responseActual.requestId(),
              commandType,
              responseClass,
              responseMessage.getClass())
        );
      }

      return responseClass.cast(responseMessage);
    } catch (final IcProtocolException e) {
      throw new IcClientException(PROTOCOL_ERROR, e);
    } catch (final IOException e) {
      throw new IcClientException(IO_ERROR, e);
    }
  }

  private static boolean isAuthenticationError(
    final IcTResponseError error)
  {
    return Objects.equals(error.errorCode(), AUTHENTICATION_ERROR.id());
  }

  @Override
  public IcTicketSummary ticketCreate(
    final IcTicketCreation create)
    throws IcClientException, InterruptedException
  {
    return this.sendCommand(
      IcTResponseTicketCreate.class,
      new IcTCommandTicketCreate(create)
    ).ticket();
  }

  @Override
  public IcPage<IcTicketSummary> ticketSearchBegin(
    final IcTicketSearch parameters)
    throws IcClientException, InterruptedException
  {
    return this.sendCommand(
      IcTResponseTicketSearchBegin.class,
      new IcTCommandTicketSearchBegin(parameters)
    ).tickets();
  }

  @Override
  public IcPage<IcTicketSummary> ticketSearchNext()
    throws IcClientException, InterruptedException
  {
    return this.sendCommand(
      IcTResponseTicketSearchNext.class,
      new IcTCommandTicketSearchNext()
    ).tickets();
  }

  @Override
  public IcPage<IcTicketSummary> ticketSearchPrevious()
    throws IcClientException, InterruptedException
  {
    return this.sendCommand(
      IcTResponseTicketSearchPrevious.class,
      new IcTCommandTicketSearchPrevious()
    ).tickets();
  }

  @Override
  public IcTicketComment ticketCommentCreate(
    final IcTicketCommentCreation create)
    throws IcClientException, InterruptedException
  {
    return this.sendCommand(
      IcTResponseTicketCommentCreate.class,
      new IcTCommandTicketCommentCreate(create)
    ).comment();
  }

  @Override
  public IcTicket ticketGet(
    final IcTicketID id)
    throws IcClientException, InterruptedException
  {
    return this.sendCommand(
      IcTResponseTicketGet.class,
      new IcTCommandTicketGet(id)
    ).ticket();
  }

  @Override
  public IcProject projectCreate(
    final IcProjectShortName shortName,
    final IcProjectTitle title)
    throws IcClientException, InterruptedException
  {
    return this.sendCommand(
      IcTResponseProjectCreate.class,
      new IcTCommandProjectCreate(shortName, title)
    ).project();
  }

  @Override
  public void permissionGrant(
    final UUID targetUser,
    final IcPermissionScopedType permission)
    throws IcClientException, InterruptedException
  {
    this.sendCommand(
      IcTResponsePermissionGrant.class,
      new IcTCommandPermissionGrant(targetUser, permission)
    );
  }
}
