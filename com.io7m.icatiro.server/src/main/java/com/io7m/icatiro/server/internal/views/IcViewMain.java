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

package com.io7m.icatiro.server.internal.views;

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseTicketListPagingType;
import com.io7m.icatiro.database.api.IcDatabaseTicketsQueriesType;
import com.io7m.icatiro.model.IcTicketListParameters;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcTimeRange;
import com.io7m.icatiro.server.internal.IcServerUserController;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateService;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateType;
import com.io7m.icatiro.server.internal.freemarker.IcFMTicketListData;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;

/**
 * The main view.
 */

public final class IcViewMain extends IcViewAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcViewMain.class);

  private final IcFMTemplateType<IcFMTicketListData> template;

  /**
   * The main view.
   *
   * @param inServices The service directory
   */

  public IcViewMain(
    final IcServiceDirectoryType inServices)
  {
    super(inServices);

    this.template =
      inServices.requireService(IcFMTemplateService.class)
        .ticketListTemplate();
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws Exception
  {
    final var controller =
      this.userController();
    final var ticketParameters =
      loadTicketParameters(controller, request);
    final var ticketPaging =
      controller.ticketPaging();
    final var tickets =
      this.loadTickets(ticketPaging);

    servletResponse.setContentType("application/xhtml+xml");

    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IcFMTicketListData("Main", tickets),
        writer
      );
    }
  }

  private List<IcTicketSummary> loadTickets(
    final IcDatabaseTicketListPagingType ticketPaging)
    throws IcDatabaseException
  {
    try (var c = this.database().openConnection(ICATIRO)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IcDatabaseTicketsQueriesType.class);
        return ticketPaging.pageCurrent(q);
      }
    }
  }

  private static IcTicketListParameters loadTicketParameters(
    final IcServerUserController controller,
    final HttpServletRequest request)
  {
    var ticketParameters =
      controller.ticketListParameters();

    final var timeCreatedLowP = request.getParameter("timeCreatedLow");
    if (timeCreatedLowP != null) {
      try {
        final var newTime =
          OffsetDateTime.parse(timeCreatedLowP);
        final var originalTime =
          ticketParameters.timeCreatedRange();

        ticketParameters = new IcTicketListParameters(
          new IcTimeRange(newTime, originalTime.timeUpper()),
          ticketParameters.timeUpdatedRange(),
          ticketParameters.ordering(),
          ticketParameters.limit()
        );
      } catch (final Exception e) {
        // Oh well
      }
    }

    final var timeCreatedHighP = request.getParameter("timeCreatedHigh");
    if (timeCreatedHighP != null) {
      try {
        final var newTime =
          OffsetDateTime.parse(timeCreatedHighP);
        final var originalTime =
          ticketParameters.timeCreatedRange();

        ticketParameters = new IcTicketListParameters(
          new IcTimeRange(originalTime.timeLower(), newTime),
          ticketParameters.timeUpdatedRange(),
          ticketParameters.ordering(),
          ticketParameters.limit()
        );
      } catch (final Exception e) {
        // Oh well
      }
    }

    controller.setTicketListParameters(ticketParameters);
    return ticketParameters;
  }
}
