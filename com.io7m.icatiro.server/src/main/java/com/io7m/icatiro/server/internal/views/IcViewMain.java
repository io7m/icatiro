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

import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateService;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateType;
import com.io7m.icatiro.server.internal.freemarker.IcFMTicketListData;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
    servletResponse.setContentType("application/xhtml+xml");

    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IcFMTicketListData(
          "Main",
          "Main",
          List.of()),
        writer
      );
    }
  }
}
