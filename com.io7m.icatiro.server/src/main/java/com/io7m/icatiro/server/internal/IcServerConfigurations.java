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


package com.io7m.icatiro.server.internal;

import com.io7m.icatiro.server.api.IcServerConfiguration;
import com.io7m.icatiro.services.api.IcServiceType;

import java.util.Objects;

/**
 * A service that exposes configuration information.
 */

public final class IcServerConfigurations implements IcServiceType
{
  private final IcServerConfiguration configuration;

  /**
   * A service that exposes configuration information.
   *
   * @param inConfiguration The configuration
   */

  public IcServerConfigurations(
    final IcServerConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  @Override
  public String description()
  {
    return "Server configurations.";
  }

  /**
   * @return The current configuration
   */

  public IcServerConfiguration configuration()
  {
    return this.configuration;
  }

  @Override
  public String toString()
  {
    return "[IcServerConfigurations 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
