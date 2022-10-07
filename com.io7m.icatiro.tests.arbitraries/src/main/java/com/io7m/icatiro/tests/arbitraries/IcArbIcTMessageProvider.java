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

package com.io7m.icatiro.tests.arbitraries;

import com.io7m.icatiro.model.IcHash;
import com.io7m.icatiro.model.IcPage;
import com.io7m.icatiro.model.IcPermissionScopedType;
import com.io7m.icatiro.model.IcProject;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketListParameters;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.protocol.tickets.IcTCommandLogin;
import com.io7m.icatiro.protocol.tickets.IcTCommandPermissionGrant;
import com.io7m.icatiro.protocol.tickets.IcTCommandProjectCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchBegin;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchNext;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchPrevious;
import com.io7m.icatiro.protocol.tickets.IcTMessageType;
import com.io7m.icatiro.protocol.tickets.IcTResponseError;
import com.io7m.icatiro.protocol.tickets.IcTResponseLogin;
import com.io7m.icatiro.protocol.tickets.IcTResponsePermissionGrant;
import com.io7m.icatiro.protocol.tickets.IcTResponseProjectCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchBegin;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchNext;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchPrevious;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link IcHash} values.
 */

public final class IcArbIcTMessageProvider extends IcArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IcArbIcTMessageProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IcTMessageType.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      commandLogin(),
      commandPermissionGrant(),
      commandProjectCreate(),
      commandTicketCreate(),
      commandTicketSearchBegin(),
      commandTicketSearchNext(),
      commandTicketSearchPrevious(),
      responseError(),
      responseLogin(),
      responsePermissionGrant(),
      responseProjectCreate(),
      responseTicketCreate(),
      responseTicketSearchBegin(),
      responseTicketSearchNext(),
      responseTicketSearchPrevious()
    );
  }

  private static Arbitrary<IcTResponsePermissionGrant> responsePermissionGrant()
  {
    return Arbitraries.defaultFor(UUID.class)
      .map(IcTResponsePermissionGrant::new);
  }

  private static Arbitrary<IcTCommandPermissionGrant> commandPermissionGrant()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      Arbitraries.defaultFor(IcPermissionScopedType.class)
    ).as(IcTCommandPermissionGrant::new);
  }

  private static Arbitrary<IcTResponseProjectCreate> responseProjectCreate()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      Arbitraries.defaultFor(IcProject.class)
    ).as(IcTResponseProjectCreate::new);
  }

  private static Arbitrary<IcTResponseTicketCreate> responseTicketCreate()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      Arbitraries.defaultFor(IcTicketSummary.class)
    ).as(IcTResponseTicketCreate::new);
  }

  private static Arbitrary<IcTCommandProjectCreate> commandProjectCreate()
  {
    final var s0 =
      Arbitraries.defaultFor(IcProjectShortName.class);
    final var s1 =
      Arbitraries.defaultFor(IcProjectTitle.class);

    return Combinators.combine(s0, s1).as(IcTCommandProjectCreate::new);
  }

  private static Arbitrary<IcTCommandTicketCreate> commandTicketCreate()
  {
    return Arbitraries.defaultFor(IcTicketCreation.class)
      .map(IcTCommandTicketCreate::new);
  }

  private static Arbitrary<IcTCommandTicketSearchPrevious> commandTicketSearchPrevious()
  {
    return Arbitraries.of(new IcTCommandTicketSearchPrevious());
  }

  private static Arbitrary<IcTCommandTicketSearchNext> commandTicketSearchNext()
  {
    return Arbitraries.of(new IcTCommandTicketSearchNext());
  }

  private static Arbitrary<IcTCommandTicketSearchBegin> commandTicketSearchBegin()
  {
    return Arbitraries.defaultFor(IcTicketListParameters.class)
      .map(IcTCommandTicketSearchBegin::new);
  }

  private static Arbitrary<IcTCommandLogin> commandLogin()
  {
    return Combinators.combine(
      Arbitraries.strings(),
      Arbitraries.strings()
    ).as(IcTCommandLogin::new);
  }

  private static Arbitrary<IcTResponseTicketSearchPrevious> responseTicketSearchPrevious()
  {
    final var a0 =
      Arbitraries.defaultFor(UUID.class);
    final var a1 =
      Arbitraries.defaultFor(IcTicketSummary.class)
        .list();
    final var a2 =
      Arbitraries.integers();

    return Combinators.combine(a0, a1, a2)
      .as((id, summaries, n) -> {
        return new IcTResponseTicketSearchPrevious(
          id,
          new IcPage<>(summaries, n.intValue(), n.intValue(), n.longValue())
        );
      });
  }

  private static Arbitrary<IcTResponseTicketSearchNext> responseTicketSearchNext()
  {
    final var a0 =
      Arbitraries.defaultFor(UUID.class);
    final var a1 =
      Arbitraries.defaultFor(IcTicketSummary.class)
        .list();
    final var a2 =
      Arbitraries.integers();

    return Combinators.combine(a0, a1, a2)
      .as((id, summaries, n) -> {
        return new IcTResponseTicketSearchNext(
          id,
          new IcPage<>(summaries, n.intValue(), n.intValue(), n.longValue())
        );
      });
  }

  private static Arbitrary<IcTResponseTicketSearchBegin> responseTicketSearchBegin()
  {
    final var a0 =
      Arbitraries.defaultFor(UUID.class);
    final var a1 =
      Arbitraries.defaultFor(IcTicketSummary.class)
        .list();
    final var a2 =
      Arbitraries.integers();

    return Combinators.combine(a0, a1, a2)
      .as((id, summaries, n) -> {
        return new IcTResponseTicketSearchBegin(
          id,
          new IcPage<>(summaries, n.intValue(), n.intValue(), n.longValue())
        );
      });
  }

  private static Arbitrary<IcTResponseLogin> responseLogin()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      Arbitraries.defaultFor(IcUser.class)
    ).as(IcTResponseLogin::new);
  }

  private static Arbitrary<IcTResponseError> responseError()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      Arbitraries.strings(),
      Arbitraries.strings()
    ).as(IcTResponseError::new);
  }
}
