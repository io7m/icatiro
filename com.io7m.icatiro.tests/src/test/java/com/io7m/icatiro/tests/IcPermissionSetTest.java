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

import com.io7m.icatiro.model.IcAccessControlledType;
import com.io7m.icatiro.model.IcPermission;
import com.io7m.icatiro.model.IcPermissionGlobal;
import com.io7m.icatiro.model.IcPermissionProjectwide;
import com.io7m.icatiro.model.IcPermissionSet;
import com.io7m.icatiro.model.IcPermissionTicketwide;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcTicketID;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public final class IcPermissionSetTest
{
  @Property
  public void testBuilderIdentity(
    final @ForAll IcPermissionSet permissions)
  {
    final var builder = IcPermissionSet.builder();
    for (final var p : permissions.stream().toList()) {
      builder.add(p);
    }
    final var rebuilt = builder.build();
    assertEquals(permissions, rebuilt);
    assertEquals(permissions.toString(), rebuilt.toString());
    assertEquals(permissions.hashCode(), rebuilt.hashCode());
  }

  @Property
  public void testImpliesSelf(
    final @ForAll IcPermissionSet permissions)
  {
    assertAll(
      permissions.stream()
        .map(p -> () -> {
          assertTrue(permissions.impliesScoped(p));
        })
    );
  }

  @Property
  public void testEmptyImpliesNothing(
    final @ForAll IcAccessControlledType object,
    final @ForAll IcPermission permission)
  {
    assertFalse(
      IcPermissionSet.empty().implies(object, permission)
    );
  }

  @Property
  public void testGlobalImpliesAlways(
    final @ForAll IcAccessControlledType object,
    final @ForAll IcPermission permission)
  {
    assertTrue(
      IcPermissionSet.builder()
        .add(new IcPermissionGlobal(permission))
        .build()
        .implies(object, permission)
    );
  }

  @Property
  public void testProjectwideImpliesTicket(
    final @ForAll IcTicketID ticket,
    final @ForAll IcPermission permission)
  {
    assertTrue(
      IcPermissionSet.builder()
        .add(new IcPermissionProjectwide(ticket.project(), permission))
        .build()
        .implies(ticket, permission)
    );
  }

  @Property
  public void testProjectwideDoesNotImplyTicket(
    final @ForAll IcTicketID ticket,
    final @ForAll IcPermission permission)
  {
    final var project = new IcProjectID(0L);
    assumeFalse(project.equals(ticket.project()));

    assertFalse(
      IcPermissionSet.builder()
        .add(new IcPermissionProjectwide(project, permission))
        .build()
        .implies(ticket, permission)
    );
  }

  @Property
  public void testTicketwideImpliesTicket(
    final @ForAll IcTicketID ticket,
    final @ForAll IcPermission permission)
  {
    assertTrue(
      IcPermissionSet.builder()
        .add(new IcPermissionTicketwide(ticket, permission))
        .build()
        .implies(ticket, permission)
    );
  }

  @Property
  public void testTicketwideDoesNotImplyTicket(
    final @ForAll IcTicketID ticket,
    final @ForAll IcPermission permission)
  {
    final var ticketOther = new IcTicketID(ticket.project(), 0L);
    assumeFalse(ticketOther.equals(ticket));

    assertFalse(
      IcPermissionSet.builder()
        .add(new IcPermissionTicketwide(ticketOther, permission))
        .build()
        .implies(ticket, permission)
    );
  }
}
