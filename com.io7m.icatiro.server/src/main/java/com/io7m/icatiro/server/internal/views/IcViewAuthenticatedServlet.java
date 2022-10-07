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

import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.server.internal.IcServerBrandingService;
import com.io7m.icatiro.server.internal.IcServerClock;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.server.internal.IcUserSession;
import com.io7m.icatiro.server.internal.IcUserSessionService;
import com.io7m.icatiro.server.internal.common.IcCommonInstrumentedServlet;
import com.io7m.icatiro.server.internal.freemarker.IcFMMessageData;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateService;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateType;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.UUID;

import static com.io7m.icatiro.server.internal.IcServerRequestDecoration.requestIdFor;
import static com.io7m.icatiro.server.logging.IcServerMDCRequestProcessor.mdcForRequest;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class IcViewAuthenticatedServlet
  extends IcCommonInstrumentedServlet
{
  private final IcServerClock clock;
  private final IcServerStrings strings;
  private final IcDatabaseType database;
  private final IcUserSessionService userSessions;
  private final IcFMTemplateType<IcFMMessageData> messageTemplate;
  private final IcServerBrandingService branding;
  private final IcServiceDirectoryType services;
  private IcUser user;
  private IcUserSession userSession;

  /**
   * A servlet that checks that a user is authenticated before delegating
   * execution to a subclass.
   *
   * @param inServices The service directory
   */

  protected IcViewAuthenticatedServlet(
    final IcServiceDirectoryType inServices)
  {
    super(inServices);

    this.services =
      inServices;
    this.strings =
      inServices.requireService(IcServerStrings.class);
    this.clock =
      inServices.requireService(IcServerClock.class);
    this.database =
      inServices.requireService(IcDatabaseType.class);
    this.userSessions =
      inServices.requireService(IcUserSessionService.class);
    this.branding =
      inServices.requireService(IcServerBrandingService.class);
    this.messageTemplate =
      inServices.requireService(IcFMTemplateService.class)
        .pageMessage();
  }

  /**
   * @return The authenticated user
   */

  protected final IcUser user()
  {
    return this.user;
  }

  protected final UUID userId()
  {
    return this.user().id();
  }

  protected final IcServerClock clock()
  {
    return this.clock;
  }

  protected final IcServerStrings strings()
  {
    return this.strings;
  }

  protected abstract Logger logger();

  protected abstract void serviceAuthenticated(
    HttpServletRequest request,
    HttpServletResponse servletResponse,
    HttpSession session)
    throws Exception;

  @Override
  protected final void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    try (var ignored0 = mdcForRequest((Request) request)) {
      try {
        final var session = request.getSession(true);
        final var userId = (UUID) session.getAttribute("UserID");

        if (userId != null) {
          final var userSessionOpt =
            this.userSessions.find(userId, session.getId());

          if (userSessionOpt.isPresent()) {
            this.userSession = userSessionOpt.get();
            this.serviceAuthenticated(request, servletResponse, session);
            return;
          }
        }
      } catch (final Exception e) {
        this.showError(request, servletResponse, e.getMessage(), true);
      }

      servletResponse.setStatus(401);
      new IcViewLogin(this.services).service(request, servletResponse);
    }
  }

  private void showError(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final String message,
    final boolean isServerError)
    throws IOException
  {
    try (var writer = servletResponse.getWriter()) {
      if (isServerError) {
        servletResponse.setStatus(500);
      } else {
        servletResponse.setStatus(400);
      }

      this.messageTemplate.process(
        new IcFMMessageData(
          this.branding.htmlTitle(this.strings.format("error")),
          this.branding.title(),
          requestIdFor(request),
          true,
          isServerError,
          this.strings.format("error"),
          message,
          "/"
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  protected final IcDatabaseType database()
  {
    return this.database;
  }
}
