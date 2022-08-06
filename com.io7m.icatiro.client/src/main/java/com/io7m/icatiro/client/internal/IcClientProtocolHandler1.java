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
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.protocol.api.IcProtocolException;
import com.io7m.icatiro.protocol.api_v1.Ic1CommandLogin;
import com.io7m.icatiro.protocol.api_v1.Ic1CommandType;
import com.io7m.icatiro.protocol.api_v1.Ic1CommandUserSelf;
import com.io7m.icatiro.protocol.api_v1.Ic1Messages;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseError;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseLogin;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseType;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseUserSelf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.Optional;

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
  private final URI transactionURI;
  private final Ic1Messages messages;
  private final URI loginURI;

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
      new Ic1Messages();

    this.loginURI =
      inBase.resolve("login")
        .normalize();
    this.commandURI =
      inBase.resolve("command")
        .normalize();
    this.transactionURI =
      inBase.resolve("transaction")
        .normalize();
  }

  private static <A, B, E extends Exception> Optional<B> mapPartial(
    final Optional<A> o,
    final FunctionType<A, B, E> f)
    throws E
  {
    if (o.isPresent()) {
      return Optional.of(f.apply(o.get()));
    }
    return Optional.empty();
  }

  @Override
  public IcClientProtocolHandlerType login(
    final String user,
    final String password,
    final URI base)
    throws IcClientException, InterruptedException
  {
    this.sendLogin(new Ic1CommandLogin(user, password));
    return this;
  }

  private Ic1ResponseLogin sendLogin(
    final Ic1CommandLogin message)
    throws InterruptedException, IcClientException
  {
    return this.send(this.loginURI, Ic1ResponseLogin.class, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends Ic1ResponseType> T sendCommand(
    final Class<T> responseClass,
    final Ic1CommandType<T> message)
    throws InterruptedException, IcClientException
  {
    return this.send(this.commandURI, responseClass, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends Ic1ResponseType> Optional<T> sendCommandOptional(
    final Class<T> responseClass,
    final Ic1CommandType<T> message)
    throws InterruptedException, IcClientException
  {
    return this.send(this.commandURI, responseClass, message, true);
  }

  private <T extends Ic1ResponseType> Optional<T> send(
    final URI uri,
    final Class<T> responseClass,
    final Ic1CommandType<T> message,
    final boolean allowNotFound)
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

      if (response.statusCode() == 404 && allowNotFound) {
        return Optional.empty();
      }

      final var responseHeaders =
        response.headers();

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      if (!contentType.equals(Ic1Messages.contentType())) {
        throw new IcClientException(
          this.strings()
            .format(
              "errorContentType",
              commandType,
              Ic1Messages.contentType(),
              contentType)
        );
      }

      final var responseMessage =
        this.messages.parse(response.body());

      if (!(responseMessage instanceof Ic1ResponseType)) {
        throw new IcClientException(
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              commandType,
              Ic1ResponseType.class,
              responseMessage.getClass())
        );
      }

      final var responseActual = (Ic1ResponseType) responseMessage;
      if (responseActual instanceof Ic1ResponseError error) {
        throw new IcClientException(
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
          this.strings()
            .format(
              "errorResponseType",
              responseActual.requestId(),
              commandType,
              responseClass,
              responseMessage.getClass())
        );
      }

      return Optional.of(responseClass.cast(responseMessage));
    } catch (final IcProtocolException | IOException e) {
      throw new IcClientException(e);
    }
  }

  @Override
  public IcUser userSelf()
    throws IcClientException, InterruptedException
  {
    try {
      final var response =
        this.sendCommand(
          Ic1ResponseUserSelf.class,
          new Ic1CommandUserSelf()
        );

      return response.user().toUser();
    } catch (final IcPasswordException e) {
      throw new IcClientException(e);
    }
  }

  interface FunctionType<A, B, E extends Exception>
  {
    B apply(A x)
      throws E;
  }

  private static final class NotFoundException extends Exception
  {
    NotFoundException()
    {

    }
  }
}
