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


package com.io7m.icatiro.protocol.tickets.cb;

import com.io7m.cedarbridge.runtime.api.CBProtocolMessageVersionedSerializerType;
import com.io7m.cedarbridge.runtime.bssio.CBSerializationContextBSSIO;
import com.io7m.icatiro.protocol.IcProtocolException;
import com.io7m.icatiro.protocol.IcProtocolMessagesType;
import com.io7m.icatiro.protocol.tickets.IcTMessageType;
import com.io7m.icatiro.services.api.IcServiceType;
import com.io7m.jbssio.api.BSSReaderProviderType;
import com.io7m.jbssio.api.BSSWriterProviderType;
import com.io7m.jbssio.vanilla.BSSReaders;
import com.io7m.jbssio.vanilla.BSSWriters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.IO_ERROR;

/**
 * The protocol messages for Admin v1 Cedarbridge.
 */

public final class IcT1Messages
  implements IcProtocolMessagesType<IcTMessageType>, IcServiceType
{
  private static final ProtocolTickets PROTOCOL = new ProtocolTickets();

  /**
   * The content type for the protocol.
   */

  public static final String CONTENT_TYPE =
    "application/icatiro_admin+cedarbridge";

  private final BSSReaderProviderType readers;
  private final BSSWriterProviderType writers;
  private final IcT1Validation validator;
  private final CBProtocolMessageVersionedSerializerType<ProtocolTicketsType> serializer;

  /**
   * The protocol messages for Admin v1 Cedarbridge.
   *
   * @param inReaders The readers
   * @param inWriters The writers
   */

  public IcT1Messages(
    final BSSReaderProviderType inReaders,
    final BSSWriterProviderType inWriters)
  {
    this.readers =
      Objects.requireNonNull(inReaders, "readers");
    this.writers =
      Objects.requireNonNull(inWriters, "writers");

    this.validator = new IcT1Validation();
    this.serializer =
      PROTOCOL.serializerForProtocolVersion(1L)
        .orElseThrow(() -> {
          return new IllegalStateException("No support for version 1");
        });
  }

  /**
   * The protocol messages for Admin v1 Cedarbridge.
   */

  public IcT1Messages()
  {
    this(new BSSReaders(), new BSSWriters());
  }

  /**
   * @return The content type
   */

  public static String contentType()
  {
    return CONTENT_TYPE;
  }

  /**
   * @return The protocol identifier
   */

  public static UUID protocolId()
  {
    return PROTOCOL.protocolId();
  }

  @Override
  public IcTMessageType parse(
    final byte[] data)
    throws IcProtocolException
  {
    final var context =
      CBSerializationContextBSSIO.createFromByteArray(this.readers, data);

    try {
      return this.validator.convertFromWire(
        (ProtocolTicketsv1Type) this.serializer.deserialize(context)
      );
    } catch (final IOException e) {
      throw new IcProtocolException(IO_ERROR, e.getMessage(), e);
    }
  }

  @Override
  public byte[] serialize(
    final IcTMessageType message)
    throws IcProtocolException
  {
    try (var output = new ByteArrayOutputStream()) {
      final var context =
        CBSerializationContextBSSIO.createFromOutputStream(
          this.writers,
          output);
      this.serializer.serialize(context, this.validator.convertToWire(message));
      return output.toByteArray();
    } catch (final IOException e) {
      throw new IcProtocolException(IO_ERROR, e.getMessage(), e);
    }
  }

  @Override
  public String description()
  {
    return "Tickets Cedarbridge message service.";
  }

  @Override
  public String toString()
  {
    return "[IcT1Messages 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}
