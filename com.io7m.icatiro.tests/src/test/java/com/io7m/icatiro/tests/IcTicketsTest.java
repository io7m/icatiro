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

import com.io7m.icatiro.client.IcClients;
import com.io7m.icatiro.client.api.IcClientException;
import com.io7m.icatiro.client.api.IcClientType;
import com.io7m.icatiro.error_codes.IcStandardErrorCodes;
import com.io7m.icatiro.model.IcPermission;
import com.io7m.icatiro.model.IcPermissionGlobal;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketColumnOrdering;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketListParameters;
import com.io7m.icatiro.model.IcTicketOrdering;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcTicketTitle;
import com.io7m.icatiro.model.IcTimeRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.NOT_LOGGED_IN;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.icatiro.model.IcPermission.TICKET_CREATE;
import static com.io7m.icatiro.model.IcPermission.TICKET_READ;
import static com.io7m.icatiro.model.IcTicketColumn.BY_ID;
import static com.io7m.icatiro.model.IcTicketListParameters.defaults;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class IcTicketsTest extends IcWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcTicketsTest.class);

  private IcClients clients;
  private IcClientType client;

  @BeforeEach
  public void setup()
  {
    this.clients = new IcClients();
    this.client = this.clients.create(Locale.ROOT);
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    this.client.close();
  }

  /**
   * Unrecognized users cannot log in.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailure()
    throws Exception
  {
    final var ex =
      assertThrows(IcClientException.class, () -> {
        this.client.login("unknown", "12345678", serverAPIBase());
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());
  }

  /**
   * Users can log in.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginOK()
    throws Exception
  {
    this.createIdstoreUser("someone");
    this.client.login("someone", "12345678", serverAPIBase());
  }

  /**
   * Project creation works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProjectCreate()
    throws Exception
  {
    final var user = this.createIdstoreUser("someone");
    this.icatiro().userInitialSet(user);

    this.client.login("someone", "12345678", serverAPIBase());

    final var project =
      this.client.projectCreate(
        new IcProjectShortName("PROJECT"),
        new IcProjectTitle("Example project.")
      );

    assertEquals("PROJECT", project.shortName().value());
    assertEquals("Example project.", project.title().value());
  }

  /**
   * Ticket creation works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketCreate()
    throws Exception
  {
    final var user = this.createIdstoreUser("someone");
    this.icatiro().userInitialSet(user);

    this.client.login("someone", "12345678", serverAPIBase());

    final var project =
      this.client.projectCreate(
        new IcProjectShortName("PROJECT"),
        new IcProjectTitle("Example project.")
      );

    assertEquals("PROJECT", project.shortName().value());
    assertEquals("Example project.", project.title().value());

    final var ticket =
      this.client.ticketCreate(
        new IcTicketCreation(project.id(), new IcTicketTitle("A ticket."))
      );

    assertEquals(project.id(), ticket.ticketId().project());
    assertEquals("A ticket.", ticket.ticketTitle().value());

    final var page =
      this.client.ticketSearchBegin(
        new IcTicketListParameters(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          IcTicketOrdering.noOrdering(),
          1000
        )
      );

    assertEquals(1, page.items().size());
    assertEquals(ticket, page.items().get(0));
    assertEquals(0, page.pageIndex());
    assertEquals(1, page.pageCount());
    assertEquals(0L, page.pageFirstOffset());
  }

  /**
   * Ticket creation works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketCreateBulk()
    throws Exception
  {
    final var user = this.createIdstoreUser("someone");
    this.icatiro().userInitialSet(user);

    this.client.login("someone", "12345678", serverAPIBase());

    final var project =
      this.client.projectCreate(
        new IcProjectShortName("PROJECT"),
        new IcProjectTitle("Example project.")
      );

    assertEquals("PROJECT", project.shortName().value());
    assertEquals("Example project.", project.title().value());

    final var tickets = new ArrayList<IcTicketSummary>();
    for (int index = 0; index < 101; ++index) {
      final var ticket =
        this.client.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle(Integer.toUnsignedString(index)))
        );

      assertEquals(project.id(), ticket.ticketId().project());
      assertEquals(
        Integer.toUnsignedString(index),
        ticket.ticketTitle().value()
      );
      tickets.add(ticket);
    }

    var page =
      this.client.ticketSearchBegin(
        new IcTicketListParameters(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketOrdering(
            List.of(new IcTicketColumnOrdering(BY_ID, true))
          ),
          30
        )
      );

    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(0, page.pageIndex());
    assertEquals(0L, page.pageFirstOffset());
    checkItemsById(1L, page.items());

    page = this.client.ticketSearchNext();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(1, page.pageIndex());
    assertEquals(30L, page.pageFirstOffset());
    checkItemsById(31L, page.items());

    page = this.client.ticketSearchNext();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(2, page.pageIndex());
    assertEquals(60L, page.pageFirstOffset());
    checkItemsById(61L, page.items());

    page = this.client.ticketSearchNext();
    assertEquals(11, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(3, page.pageIndex());
    assertEquals(90L, page.pageFirstOffset());
    checkItemsById(91L, page.items());

    page = this.client.ticketSearchNext();
    assertEquals(11, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(3, page.pageIndex());
    assertEquals(90L, page.pageFirstOffset());
    checkItemsById(91L, page.items());

    page = this.client.ticketSearchPrevious();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(2, page.pageIndex());
    assertEquals(60L, page.pageFirstOffset());
    checkItemsById(61L, page.items());

    page = this.client.ticketSearchPrevious();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(1, page.pageIndex());
    assertEquals(30L, page.pageFirstOffset());
    checkItemsById(31L, page.items());

    page = this.client.ticketSearchPrevious();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(0, page.pageIndex());
    assertEquals(0L, page.pageFirstOffset());
    checkItemsById(1L, page.items());
  }

  private static void checkItemsById(
    final long idStart,
    final List<IcTicketSummary> items)
  {
    for (int index = 0; index < items.size(); ++index) {
      final var ticket = items.get(index);
      final var id = ticket.ticketId().value();
      LOG.debug("ticket [{}]: {}", index, id);
      assertEquals(idStart + index, id);
    }
  }

  /**
   * It's not possible to grant a permission one does not have.
   *
   * @throws Exception On errors
   */

  @Test
  public void testPermissionGrantNotHeld()
    throws Exception
  {
    final var user0 = this.createIdstoreUser("someone");
    final var user1 = this.createIdstoreUser("someone-else");

    this.icatiro().userInitialSet(user0);

    this.client.login("someone-else", "12345678", serverAPIBase());

    final var ex =
      assertThrows(IcClientException.class, () -> {
        this.client.permissionGrant(user0, new IcPermissionGlobal(TICKET_CREATE));
      });

    assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
  }

  /**
   * It's possible to grant a permission one has.
   *
   * @throws Exception On errors
   */

  @Test
  public void testPermissionGrantHeld()
    throws Exception
  {
    final var user0 = this.createIdstoreUser("someone");
    final var user1 = this.createIdstoreUser("someone-else");

    this.icatiro().userInitialSet(user0);

    this.client.login("someone-else", "12345678", serverAPIBase());
    this.client.login("someone", "12345678", serverAPIBase());
    this.client.permissionGrant(user1, new IcPermissionGlobal(TICKET_CREATE));
  }

  /**
   * A user can see tickets after they receive the correct permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketPermissionsMissing()
    throws Exception
  {
    final var userId0 = this.createIdstoreUser("someone");
    final var userId1 = this.createIdstoreUser("someone-else");
    this.icatiro().userInitialSet(userId0);

    /*
     * Create a pile of tickets with a user that has permissions.
     */

    var user0 =
      this.client.login("someone", "12345678", serverAPIBase());

    final var project =
      this.client.projectCreate(
        new IcProjectShortName("PROJECT"),
        new IcProjectTitle("Example project.")
      );

    assertEquals("PROJECT", project.shortName().value());
    assertEquals("Example project.", project.title().value());

    final var tickets = new ArrayList<IcTicketSummary>();
    for (int index = 0; index < 10; ++index) {
      final var ticket =
        this.client.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle(Integer.toUnsignedString(index)))
        );
      tickets.add(ticket);
    }

    /*
     * Switch to another user and check that we're unable to see tickets.
     */

    var user1 =
      this.client.login("someone-else", "12345678", serverAPIBase());

    var page =
      this.client.ticketSearchBegin(
        new IcTicketListParameters(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketOrdering(
            List.of(new IcTicketColumnOrdering(BY_ID, true))
          ),
          30
        )
      );

    assertEquals(0, page.items().size());
    assertEquals(1, page.pageCount());
    assertEquals(0, page.pageIndex());
    assertEquals(0L, page.pageFirstOffset());

    /*
     * Switch to back to the first user and grant permissions to the other
     * user.
     */

    user0 =
      this.client.login("someone", "12345678", serverAPIBase());
    this.client.permissionGrant(user1.id(), new IcPermissionGlobal(TICKET_READ));

    /*
     * Switch to back to the second user and check that we can now see tickets.
     */

    user1 =
      this.client.login("someone-else", "12345678", serverAPIBase());

    page =
      this.client.ticketSearchBegin(
        new IcTicketListParameters(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketOrdering(
            List.of(new IcTicketColumnOrdering(BY_ID, true))
          ),
          30
        )
      );

    assertEquals(10, page.items().size());
    assertEquals(1, page.pageCount());
    assertEquals(0, page.pageIndex());
    assertEquals(0L, page.pageFirstOffset());
  }

  /**
   * Commands require authentication.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnauthenticated()
    throws Exception
  {
    assertAll(
      () -> {
        assertEquals(
          NOT_LOGGED_IN,
          assertThrows(IcClientException.class, () -> {
            this.client.ticketSearchBegin(defaults());
          }).errorCode()
        );
      },
      () -> {
        assertEquals(
          NOT_LOGGED_IN,
          assertThrows(IcClientException.class, () -> {
            this.client.ticketSearchNext();
          }).errorCode()
        );
      },
      () -> {
        assertEquals(
          NOT_LOGGED_IN,
          assertThrows(IcClientException.class, () -> {
            this.client.ticketSearchPrevious();
          }).errorCode()
        );
      });
  }
}
