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

package com.io7m.icatiro.tests;

import com.io7m.icatiro.database.api.IcDatabaseConfiguration;
import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseFactoryType;
import com.io7m.icatiro.database.api.IcDatabaseType;
import io.opentelemetry.api.OpenTelemetry;

import java.util.Objects;
import java.util.function.Consumer;

final class IcCapturingDatabases
  implements IcDatabaseFactoryType
{
  private final IcDatabaseFactoryType delegate;
  private IcDatabaseType mostRecent;

  IcCapturingDatabases(
    final IcDatabaseFactoryType inDelegate)
  {
    this.delegate =
      Objects.requireNonNull(inDelegate, "delegate");
  }

  @Override
  public String kind()
  {
    return this.delegate.kind();
  }

  @Override
  public IcDatabaseType open(
    final IcDatabaseConfiguration configuration,
    final OpenTelemetry openTelemetry,
    final Consumer<String> startupMessages)
    throws IcDatabaseException
  {
    final var database =
      this.delegate.open(configuration, openTelemetry, startupMessages);
    this.mostRecent = database;
    return database;
  }

  public IcDatabaseType mostRecent()
  {
    return this.mostRecent;
  }
}
