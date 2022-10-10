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
import com.io7m.icatiro.model.IcPermissionGlobal;
import com.io7m.icatiro.model.IcPermissionTicketwide;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketColumnOrdering;
import com.io7m.icatiro.model.IcTicketCommentCreation;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketID;
import com.io7m.icatiro.model.IcTicketSearch;
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
import java.util.Optional;
import java.util.OptionalLong;

import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.NOT_LOGGED_IN;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.TICKET_COMMENT_NONEXISTENT;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.TICKET_NONEXISTENT;
import static com.io7m.icatiro.model.IcPermission.TICKET_CREATE;
import static com.io7m.icatiro.model.IcPermission.TICKET_READ;
import static com.io7m.icatiro.model.IcPermission.TICKET_WRITE;
import static com.io7m.icatiro.model.IcTicketColumn.BY_ID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        new IcTicketCreation(
          project.id(),
          new IcTicketTitle("A ticket."),
          "A ticket description"
        )
      );

    assertEquals(project.id(), ticket.ticketId().project());
    assertEquals("A ticket.", ticket.ticketTitle().value());

    final var page =
      this.client.ticketSearchBegin(
        new IcTicketSearch(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketColumnOrdering(BY_ID, true),
          1000,
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );

    assertEquals(1, page.items().size());
    assertEquals(ticket, page.items().get(0));
    assertEquals(1, page.pageIndex());
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
            new IcTicketTitle(Integer.toUnsignedString(index)),
            "Ticket description %d".formatted(index)
          )
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
        new IcTicketSearch(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketColumnOrdering(BY_ID, true),
          30,
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );

    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(1, page.pageIndex());
    assertEquals(0L, page.pageFirstOffset());
    checkItemsById(1L, page.items());

    page = this.client.ticketSearchNext();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(2, page.pageIndex());
    assertEquals(30L, page.pageFirstOffset());
    checkItemsById(31L, page.items());

    page = this.client.ticketSearchNext();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(3, page.pageIndex());
    assertEquals(60L, page.pageFirstOffset());
    checkItemsById(61L, page.items());

    page = this.client.ticketSearchNext();
    assertEquals(11, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(4, page.pageIndex());
    assertEquals(90L, page.pageFirstOffset());
    checkItemsById(91L, page.items());

    page = this.client.ticketSearchNext();
    assertEquals(11, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(4, page.pageIndex());
    assertEquals(90L, page.pageFirstOffset());
    checkItemsById(91L, page.items());

    page = this.client.ticketSearchPrevious();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(3, page.pageIndex());
    assertEquals(60L, page.pageFirstOffset());
    checkItemsById(61L, page.items());

    page = this.client.ticketSearchPrevious();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(2, page.pageIndex());
    assertEquals(30L, page.pageFirstOffset());
    checkItemsById(31L, page.items());

    page = this.client.ticketSearchPrevious();
    assertEquals(30, page.items().size());
    assertEquals(4, page.pageCount());
    assertEquals(1, page.pageIndex());
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
        this.client.permissionGrant(
          user0,
          new IcPermissionGlobal(TICKET_CREATE));
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
   * A user can see ticketSearch after they receive the correct permissions.
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
     * Create a pile of ticketSearch with a user that has permissions.
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
            new IcTicketTitle(Integer.toUnsignedString(index)),
            "Ticket description %d".formatted(index)
          )
        );
      tickets.add(ticket);
    }

    /*
     * Switch to another user and check that we're unable to see ticketSearch.
     */

    var user1 =
      this.client.login("someone-else", "12345678", serverAPIBase());

    var page =
      this.client.ticketSearchBegin(
        new IcTicketSearch(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketColumnOrdering(BY_ID, true),
          30,
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );

    assertEquals(0, page.items().size());
    assertEquals(1, page.pageCount());
    assertEquals(1, page.pageIndex());
    assertEquals(0L, page.pageFirstOffset());

    /*
     * Switch to back to the first user and grant permissions to the other
     * user.
     */

    user0 =
      this.client.login("someone", "12345678", serverAPIBase());
    this.client.permissionGrant(
      user1.id(),
      new IcPermissionGlobal(TICKET_READ));

    /*
     * Switch to back to the second user and check that we can now see ticketSearch.
     */

    user1 =
      this.client.login("someone-else", "12345678", serverAPIBase());

    page =
      this.client.ticketSearchBegin(
        new IcTicketSearch(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketColumnOrdering(BY_ID, true),
          30,
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );

    assertEquals(10, page.items().size());
    assertEquals(1, page.pageCount());
    assertEquals(1, page.pageIndex());
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
            this.client.ticketSearchBegin(
              new IcTicketSearch(
                IcTimeRange.largest(),
                IcTimeRange.largest(),
                new IcTicketColumnOrdering(BY_ID, true),
                30,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
              )
            );
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

  /**
   * Ticket searches must be started before pages can be retrieved.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketSearchWrong()
    throws Exception
  {
    final var user = this.createIdstoreUser("someone");
    this.icatiro().userInitialSet(user);

    this.client.login("someone", "12345678", serverAPIBase());

    {
      final var ex =
        assertThrows(IcClientException.class, () -> {
          this.client.ticketSearchNext();
        });
      assertEquals(PROTOCOL_ERROR, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IcClientException.class, () -> {
          this.client.ticketSearchPrevious();
        });
      assertEquals(PROTOCOL_ERROR, ex.errorCode());
    }
  }

  /**
   * Ticket creation grants read access to the ticket.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketCreateGrantsReadWriteAccess()
    throws Exception
  {
    final var user0Id =
      this.createIdstoreUser("someone");
    final var user1Id =
      this.createIdstoreUser("someone-else");

    this.icatiro().userInitialSet(user0Id);

    this.client.login("someone-else", "12345678", serverAPIBase());

    /*
     * Grant access to someone-else to allow them to create tickets.
     */

    this.client.login("someone", "12345678", serverAPIBase());
    final var createTicketsGlobally = new IcPermissionGlobal(TICKET_CREATE);
    this.client.permissionGrant(user1Id, createTicketsGlobally);

    final var project =
      this.client.projectCreate(
        new IcProjectShortName("PROJECT"),
        new IcProjectTitle("Example project.")
      );

    /*
     * Log in as someone-else.
     */

    final var user1 =
      this.client.login("someone-else", "12345678", serverAPIBase());

    assertTrue(user1.permissions().impliesScoped(createTicketsGlobally));

    /*
     * Creating a ticket grants read and write access if the user does not
     * already have it.
     */

    final var ticket =
      this.client.ticketCreate(
        new IcTicketCreation(
          project.id(),
          new IcTicketTitle("Title"),
          "Description."
        )
      );

    final var user1After =
      this.client.login("someone-else", "12345678", serverAPIBase());

    assertTrue(
      user1After.permissions()
        .impliesScoped(
          new IcPermissionTicketwide(ticket.ticketId(), TICKET_WRITE))
    );
    assertTrue(
      user1After.permissions()
        .impliesScoped(
          new IcPermissionTicketwide(ticket.ticketId(), TICKET_READ))
    );
  }

  /**
   * Retrieving a ticket with comments works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketCreateGet()
    throws Exception
  {
    final var user0Id = this.createIdstoreUser("someone");
    this.icatiro().userInitialSet(user0Id);
    this.client.login("someone", "12345678", serverAPIBase());

    final var project =
      this.client.projectCreate(
        new IcProjectShortName("PROJECT"),
        new IcProjectTitle("Example project.")
      );

    final var ticketSummary =
      this.client.ticketCreate(
        new IcTicketCreation(
          project.id(),
          new IcTicketTitle("Title"),
          "Description."
        )
      );

    var lastComment = OptionalLong.empty();
    for (int index = 0; index < 20; ++index) {
      final var comment =
        this.client.ticketCommentCreate(
          new IcTicketCommentCreation(
            ticketSummary.ticketId(),
            lastComment,
            "Comment %d".formatted(index)
          )
        );
      lastComment = OptionalLong.of(comment.commentId());
    }

    final var ticket = this.client.ticketGet(ticketSummary.ticketId());
    assertEquals(20, ticket.comments().size());
  }

  /**
   * It's not possible to specify that a comment was a reply to a comment on
   * another ticket.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketReplyWrongTicket()
    throws Exception
  {
    final var user0Id = this.createIdstoreUser("someone");
    this.icatiro().userInitialSet(user0Id);
    this.client.login("someone", "12345678", serverAPIBase());

    final var project =
      this.client.projectCreate(
        new IcProjectShortName("PROJECT"),
        new IcProjectTitle("Example project.")
      );

    final var ticket0 =
      this.client.ticketCreate(
        new IcTicketCreation(
          project.id(),
          new IcTicketTitle("Title"),
          "Description."
        )
      );

    final var ticket1 =
      this.client.ticketCreate(
        new IcTicketCreation(
          project.id(),
          new IcTicketTitle("Title"),
          "Description."
        )
      );

    final var comment0 =
      this.client.ticketCommentCreate(
        new IcTicketCommentCreation(
          ticket0.ticketId(),
          OptionalLong.empty(),
          "Comment on ticket 0"
        )
      );

    final var ex =
      assertThrows(IcClientException.class, () -> {
        this.client.ticketCommentCreate(
          new IcTicketCommentCreation(
            ticket1.ticketId(),
            OptionalLong.of(comment0.commentId()),
            "Comment on ticket 1"
          )
        );
      });

    assertEquals(TICKET_COMMENT_NONEXISTENT, ex.errorCode());
  }

  /**
   * It's not possible to specify that a comment was a reply to a nonexistent
   * comment.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketReplyNonexistentComment()
    throws Exception
  {
    final var user0Id = this.createIdstoreUser("someone");
    this.icatiro().userInitialSet(user0Id);
    this.client.login("someone", "12345678", serverAPIBase());

    final var project =
      this.client.projectCreate(
        new IcProjectShortName("PROJECT"),
        new IcProjectTitle("Example project.")
      );

    final var ticket0 =
      this.client.ticketCreate(
        new IcTicketCreation(
          project.id(),
          new IcTicketTitle("Title"),
          "Description."
        )
      );

    final var ex =
      assertThrows(IcClientException.class, () -> {
        this.client.ticketCommentCreate(
          new IcTicketCommentCreation(
            ticket0.ticketId(),
            OptionalLong.of(239919L),
            "Comment on ticket 0"
          )
        );
      });

    assertEquals(TICKET_COMMENT_NONEXISTENT, ex.errorCode());
  }

  /**
   * It's not possible to create a comment on a nonexistent ticket.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketCommentNonexistentTicket()
    throws Exception
  {
    final var user0Id = this.createIdstoreUser("someone");
    this.icatiro().userInitialSet(user0Id);
    this.client.login("someone", "12345678", serverAPIBase());

    final var project =
      this.client.projectCreate(
        new IcProjectShortName("PROJECT"),
        new IcProjectTitle("Example project.")
      );

    final var ex =
      assertThrows(IcClientException.class, () -> {
        this.client.ticketCommentCreate(
          new IcTicketCommentCreation(
            new IcTicketID(project.id(), 23L),
            OptionalLong.empty(),
            "Comment on ticket 0"
          )
        );
      });

    assertEquals(TICKET_NONEXISTENT, ex.errorCode());
  }
}
