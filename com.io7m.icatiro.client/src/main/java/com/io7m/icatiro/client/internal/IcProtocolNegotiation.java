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
import com.io7m.icatiro.protocol.api.IcProtocolException;
import com.io7m.icatiro.protocol.versions.IcVMessageType;
import com.io7m.icatiro.protocol.versions.IcVMessages;
import com.io7m.icatiro.protocol.versions.IcVProtocols;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;
import static java.util.function.Function.identity;

/**
 * Functions to negotiate protocols.
 */

public final class IcProtocolNegotiation
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcProtocolNegotiation.class);

  private IcProtocolNegotiation()
  {

  }

  private static IcClientException noProtocolsInCommon(
    final Map<BigInteger, IcClientProtocolHandlerFactoryType> handlers,
    final IcStrings strings,
    final IcVProtocols protocols)
  {
    final var lineSeparator = System.lineSeparator();
    final var text = new StringBuilder(128);
    text.append(strings.format("noSupportedVersions"));
    text.append(lineSeparator);
    text.append("  ");
    text.append(strings.format("serverSupports"));
    text.append(lineSeparator);

    for (final var candidate : protocols.protocols()) {
      text.append("    ");
      text.append(candidate.id());
      text.append(" ");
      text.append(candidate.versionMajor());
      text.append(".");
      text.append(candidate.versionMinor());
      text.append(" ");
      text.append(candidate.endpointPath());
      text.append(lineSeparator);
    }

    text.append(strings.format("clientSupports"));
    text.append(lineSeparator);

    for (final var handler : handlers.values()) {
      text.append("    ");
      text.append(handler.id());
      text.append(" ");
      text.append(handler.versionMajor());
      text.append(".*");
      text.append(lineSeparator);
    }

    return new IcClientException(text.toString());
  }

  private static IcVProtocols fetchSupportedVersions(
    final URI base,
    final HttpClient httpClient,
    final IcStrings strings)
    throws InterruptedException, IcClientException
  {
    LOG.debug("retrieving supported server protocols");

    final var vMessages =
      new IcVMessages();

    final var request =
      HttpRequest.newBuilder(base)
        .GET()
        .build();

    final HttpResponse<byte[]> response;
    try {
      response = httpClient.send(request, ofByteArray());
    } catch (final IOException e) {
      throw new IcClientException(e);
    }

    LOG.debug("server: status {}", response.statusCode());

    if (response.statusCode() >= 400) {
      throw new IcClientException(
        strings.format("httpError", Integer.valueOf(response.statusCode()))
      );
    }

    final IcVMessageType message;
    try {
      message = vMessages.parse(response.body());
    } catch (final IcProtocolException e) {
      throw new IcClientException(e);
    }

    if (message instanceof IcVProtocols protocols) {
      return protocols;
    }

    throw new IcClientException(
      strings.format(
        "unexpectedMessage", "IcVProtocols", message.getClass())
    );
  }

  /**
   * Negotiate a protocol handler.
   *
   * @param httpClient The HTTP client
   * @param strings    The string resources
   * @param user       The user
   * @param password   The password
   * @param base       The base URI
   *
   * @return The protocol handler
   *
   * @throws IcClientException   On errors
   * @throws InterruptedException On interruption
   */

  public static IcClientProtocolHandlerType negotiateProtocolHandler(
    final HttpClient httpClient,
    final IcStrings strings,
    final String user,
    final String password,
    final URI base)
    throws IcClientException, InterruptedException
  {
    Objects.requireNonNull(httpClient, "httpClient");
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(base, "base");

    final var handlerFactories =
      Stream.<IcClientProtocolHandlerFactoryType>of(new IcClientProtocolHandlers1())
        .collect(Collectors.toMap(
          IcClientProtocolHandlerFactoryType::versionMajor,
          identity())
        );

    final var protocols =
      fetchSupportedVersions(base, httpClient, strings);

    LOG.debug("server supports {} protocols", protocols.protocols().size());

    final var candidates =
      protocols.protocols()
        .stream()
        .sorted(Comparator.reverseOrder())
        .toList();

    for (final var candidate : candidates) {
      final var handlerFactory =
        handlerFactories.get(candidate.versionMajor());

      LOG.debug(
        "checking if protocol {} {}.{} is supported",
        candidate.id(),
        candidate.versionMajor(),
        candidate.versionMinor()
      );

      if (handlerFactory != null) {
        final var target =
          base.resolve(candidate.endpointPath())
            .normalize();

        LOG.debug(
          "using protocol {} {}.{} at endpoint {}",
          candidate.id(),
          candidate.versionMajor(),
          candidate.versionMinor(),
          target
        );

        return handlerFactory.createHandler(
          httpClient,
          strings,
          target
        );
      }
    }

    throw noProtocolsInCommon(handlerFactories, strings, protocols);
  }
}
