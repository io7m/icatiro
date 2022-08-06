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

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabasePermissionsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseProjectsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseTicketListPaging;
import com.io7m.icatiro.database.api.IcDatabaseTicketsQueriesType;
import com.io7m.icatiro.model.IcPermissionScopeType.Global;
import com.io7m.icatiro.model.IcPermissionScopeType.Ticketwide;
import com.io7m.icatiro.model.IcProject;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketColumnOrdering;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketListParameters;
import com.io7m.icatiro.model.IcTicketOrdering;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcTicketTitle;
import com.io7m.icatiro.model.IcTimeRange;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROJECT_NONEXISTENT;
import static com.io7m.icatiro.model.IcPermission.TICKET_READ;
import static com.io7m.icatiro.model.IcTicketColumn.BY_ID;
import static com.io7m.icatiro.model.IcTicketColumn.BY_TIME_CREATED;
import static com.io7m.icatiro.model.IcTicketColumn.BY_TIME_UPDATED;
import static com.io7m.icatiro.model.IcTicketColumn.BY_TITLE;
import static com.io7m.icatiro.model.IcTicketOrdering.noOrdering;
import static java.lang.Thread.sleep;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class IcDatabaseTicketsTest extends IcWithDatabaseContract
{
  private static ArrayList<IcTicketSummary> createSampleTickets(
    final IcDatabaseTicketsQueriesType tickets,
    final IcProject project)
    throws IcDatabaseException
  {
    final var ticketsCreated = new ArrayList<IcTicketSummary>();
    for (int index = 0; index < 100; ++index) {
      ticketsCreated.add(
        tickets.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle("Please fix %d".formatted(index))
          )
        ));
    }
    return ticketsCreated;
  }

  /**
   * Tickets cannot be created for nonexistent projects.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketCreateProjectNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);

    final var ex =
      assertThrows(IcDatabaseException.class, () -> {
        tickets.ticketCreate(
          new IcTicketCreation(
            new IcProjectID(23L),
            new IcTicketTitle("Some ticket.")
          )
        );
      });

    assertEquals(PROJECT_NONEXISTENT, ex.errorCode());
  }

  /**
   * Tickets can be created for existing projects.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketCreate()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);

    final var project =
      projects.projectCreate(
        new IcProjectTitle("A project"),
        new IcProjectShortName("PROJECT")
      );

    final var ticketsCreated = new ArrayList<>();
    for (int index = 0; index < 100; ++index) {
      ticketsCreated.add(
        tickets.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle("Please fix %d".formatted(index))
          )
        ));
    }

    assertEquals(100, ticketsCreated.size());
  }

  /**
   * Tickets can only be seen with the correct permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketListWithPermissions0()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);
    final var permissions =
      transaction.queries(IcDatabasePermissionsQueriesType.class);

    final var project =
      projects.projectCreate(
        new IcProjectTitle("A project"),
        new IcProjectShortName("PROJECT")
      );

    final var ticketsCreated =
      createSampleTickets(tickets, project);

    {
      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          noOrdering(),
          999,
          empty()
        );
      assertEquals(0, page.size());
    }
  }

  /**
   * Tickets can only be seen with the correct permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketListWithPermissions1()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);
    final var permissions =
      transaction.queries(IcDatabasePermissionsQueriesType.class);

    final var project =
      projects.projectCreate(
        new IcProjectTitle("A project"),
        new IcProjectShortName("PROJECT")
      );

    final var ticketsCreated =
      createSampleTickets(tickets, project);

    {
      permissions.permissionSet(
        user,
        new Ticketwide(ticketsCreated.get(0).ticketId()),
        TICKET_READ
      );

      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          noOrdering(),
          999,
          empty()
        );
      assertEquals(1, page.size());
    }

    {
      permissions.permissionUnset(
        user,
        new Ticketwide(ticketsCreated.get(0).ticketId()),
        TICKET_READ
      );

      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          noOrdering(),
          999,
          empty()
        );
      assertEquals(0, page.size());
    }
  }

  /**
   * Tickets can only be seen with the correct permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketListWithPermissions60()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);
    final var permissions =
      transaction.queries(IcDatabasePermissionsQueriesType.class);

    final var project =
      projects.projectCreate(
        new IcProjectTitle("A project"),
        new IcProjectShortName("PROJECT")
      );

    final var ticketsCreated =
      createSampleTickets(tickets, project);

    {
      for (final var ticket : ticketsCreated.subList(20, 80)) {
        permissions.permissionSet(
          user,
          new Ticketwide(ticket.ticketId()),
          TICKET_READ
        );
      }

      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          noOrdering(),
          999,
          empty()
        );
      assertEquals(60, page.size());
    }

    {
      for (final var ticket : ticketsCreated.subList(20, 80)) {
        permissions.permissionUnset(
          user,
          new Ticketwide(ticket.ticketId()),
          TICKET_READ
        );
      }

      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          noOrdering(),
          999,
          empty()
        );
      assertEquals(0, page.size());
    }
  }

  /**
   * Ticket ordering works as expected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketOrderingByID()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);
    final var permissions =
      transaction.queries(IcDatabasePermissionsQueriesType.class);

    final var project =
      projects.projectCreate(
        new IcProjectTitle("A project"),
        new IcProjectShortName("PROJECT")
      );

    final var ticketsCreated = new ArrayList<>();
    for (int index = 0; index < 100; ++index) {
      ticketsCreated.add(
        tickets.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle("Please fix %d".formatted(index))
          )
        ));
    }

    permissions.permissionSet(user, new Global(), TICKET_READ);

    {
      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          new IcTicketOrdering(List.of(
            new IcTicketColumnOrdering(BY_ID, true)
          )),
          999,
          empty()
        );

      assertEquals(1L, page.get(0).ticketId().value());
      assertEquals(100L, page.get(99).ticketId().value());
      assertEquals(100, page.size());
    }

    {
      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          new IcTicketOrdering(List.of(
            new IcTicketColumnOrdering(BY_ID, false)
          )),
          999,
          empty()
        );

      assertEquals(100L, page.get(0).ticketId().value());
      assertEquals(1L, page.get(99).ticketId().value());
      assertEquals(100, page.size());
    }
  }

  /**
   * Ticket ordering works as expected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketOrderingByTimeCreated()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);
    final var permissions =
      transaction.queries(IcDatabasePermissionsQueriesType.class);

    final var project =
      projects.projectCreate(
        new IcProjectTitle("A project"),
        new IcProjectShortName("PROJECT")
      );

    final var ticketsCreated = new ArrayList<>();
    for (int index = 0; index < 3; ++index) {
      ticketsCreated.add(
        tickets.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle("Please fix %d".formatted(index))
          )
        ));
      sleep(1_500L);
    }

    permissions.permissionSet(user, new Global(), TICKET_READ);

    {
      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          new IcTicketOrdering(List.of(
            new IcTicketColumnOrdering(BY_TIME_CREATED, true)
          )),
          999,
          empty()
        );

      assertEquals(1L, page.get(0).ticketId().value());
      assertEquals(3L, page.get(2).ticketId().value());
      assertEquals(3, page.size());
    }

    {
      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          new IcTicketOrdering(List.of(
            new IcTicketColumnOrdering(BY_TIME_CREATED, false)
          )),
          999,
          empty()
        );

      assertEquals(3L, page.get(0).ticketId().value());
      assertEquals(1L, page.get(2).ticketId().value());
      assertEquals(3, page.size());
    }
  }

  /**
   * Ticket ordering works as expected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketOrderingByTimeUpdated()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);
    final var permissions =
      transaction.queries(IcDatabasePermissionsQueriesType.class);

    final var project =
      projects.projectCreate(
        new IcProjectTitle("A project"),
        new IcProjectShortName("PROJECT")
      );

    final var ticketsCreated = new ArrayList<>();
    for (int index = 0; index < 3; ++index) {
      ticketsCreated.add(
        tickets.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle("Please fix %d".formatted(index))
          )
        ));
      sleep(1_500L);
    }

    permissions.permissionSet(user, new Global(), TICKET_READ);

    {
      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          new IcTicketOrdering(List.of(
            new IcTicketColumnOrdering(BY_TIME_UPDATED, true)
          )),
          999,
          empty()
        );

      assertEquals(1L, page.get(0).ticketId().value());
      assertEquals(3L, page.get(2).ticketId().value());
      assertEquals(3, page.size());
    }

    {
      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          new IcTicketOrdering(List.of(
            new IcTicketColumnOrdering(BY_TIME_UPDATED, false)
          )),
          999,
          empty()
        );

      assertEquals(3L, page.get(0).ticketId().value());
      assertEquals(1L, page.get(2).ticketId().value());
      assertEquals(3, page.size());
    }
  }

  /**
   * Ticket ordering works as expected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketOrderingByTitle()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);
    final var permissions =
      transaction.queries(IcDatabasePermissionsQueriesType.class);

    final var project =
      projects.projectCreate(
        new IcProjectTitle("A project"),
        new IcProjectShortName("PROJECT")
      );

    final var ticketsCreated = new ArrayList<>();
    for (int index = 0; index < 3; ++index) {
      ticketsCreated.add(
        tickets.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle("Please fix %d".formatted(index))
          )
        ));
      sleep(1_500L);
    }

    permissions.permissionSet(user, new Global(), TICKET_READ);

    {
      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          new IcTicketOrdering(List.of(
            new IcTicketColumnOrdering(BY_TITLE, true)
          )),
          999,
          empty()
        );

      assertEquals(1L, page.get(0).ticketId().value());
      assertEquals(3L, page.get(2).ticketId().value());
      assertEquals(3, page.size());
    }

    {
      final var page =
        tickets.ticketListWithPermissions(
          user,
          anyTimeRange(),
          anyTimeRange(),
          new IcTicketOrdering(List.of(
            new IcTicketColumnOrdering(BY_TITLE, false)
          )),
          999,
          empty()
        );

      assertEquals(3L, page.get(0).ticketId().value());
      assertEquals(1L, page.get(2).ticketId().value());
      assertEquals(3, page.size());
    }
  }

  /**
   * Ticket paging works as expected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketPaging()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);
    final var permissions =
      transaction.queries(IcDatabasePermissionsQueriesType.class);

    final var project =
      projects.projectCreate(
        new IcProjectTitle("A project"),
        new IcProjectShortName("PROJECT")
      );

    final var ticketsCreated = new ArrayList<>();
    for (int index = 0; index < 350; ++index) {
      ticketsCreated.add(
        tickets.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle("Please fix %d".formatted(index))
          )
        ));
    }

    permissions.permissionSet(user, new Global(), TICKET_READ);

    final var ordering = new IcTicketOrdering(List.of(
      new IcTicketColumnOrdering(BY_ID, true)
    ));

    final var parameters =
      new IcTicketListParameters(
        anyTimeRange(),
        anyTimeRange(),
        ordering,
        100
      );

    final var paging =
      IcDatabaseTicketListPaging.create(user, parameters);

    var page = paging.pageCurrent(tickets);
    assertEquals(1L, first(page).ticketId().value());
    assertEquals(100L, last(page).ticketId().value());
    assertEquals(0, paging.pageNumber());
    assertTrue(paging.pageNextAvailable());
    assertFalse(paging.pagePreviousAvailable());

    page = paging.pageNext(tickets);
    assertEquals(101L, first(page).ticketId().value());
    assertEquals(200L, last(page).ticketId().value());
    assertEquals(1, paging.pageNumber());
    assertTrue(paging.pageNextAvailable());
    assertTrue(paging.pagePreviousAvailable());

    page = paging.pageNext(tickets);
    assertEquals(201L, first(page).ticketId().value());
    assertEquals(300L, last(page).ticketId().value());
    assertEquals(2, paging.pageNumber());
    assertTrue(paging.pageNextAvailable());
    assertTrue(paging.pagePreviousAvailable());

    page = paging.pageNext(tickets);
    assertEquals(301L, first(page).ticketId().value());
    assertEquals(350L, last(page).ticketId().value());
    assertEquals(3, paging.pageNumber());
    assertFalse(paging.pageNextAvailable());
    assertTrue(paging.pagePreviousAvailable());

    page = paging.pagePrevious(tickets);
    assertEquals(201L, first(page).ticketId().value());
    assertEquals(300L, last(page).ticketId().value());
    assertEquals(2, paging.pageNumber());
    assertTrue(paging.pageNextAvailable());
    assertTrue(paging.pagePreviousAvailable());

    page = paging.pagePrevious(tickets);
    assertEquals(101L, first(page).ticketId().value());
    assertEquals(200L, last(page).ticketId().value());
    assertEquals(1, paging.pageNumber());
    assertTrue(paging.pageNextAvailable());
    assertTrue(paging.pagePreviousAvailable());

    page = paging.pagePrevious(tickets);
    assertEquals(1L, first(page).ticketId().value());
    assertEquals(100L, last(page).ticketId().value());
    assertEquals(0, paging.pageNumber());
    assertTrue(paging.pageNextAvailable());
    assertFalse(paging.pagePreviousAvailable());
  }

  private static <T> T first(
    final List<T> xs)
  {
    return xs.get(0);
  }

  private static <T> T last(
    final List<T> xs)
  {
    return xs.get(xs.size() - 1);
  }

  private static IcTimeRange anyTimeRange()
  {
    final var now =
      OffsetDateTime.now().plusDays(1L);
    final var then =
      now.minusYears(1L);

    return new IcTimeRange(then, now);
  }
}
