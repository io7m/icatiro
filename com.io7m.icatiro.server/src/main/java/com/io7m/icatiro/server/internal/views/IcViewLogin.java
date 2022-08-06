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
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcValidityException;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.server.internal.freemarker.IcFMLoginData;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateService;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateType;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_NONEXISTENT;

/**
 * The login form.
 */

public final class IcViewLogin extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcViewLogin.class);

  private final IcFMTemplateType<IcFMLoginData> template;
  private final IcDatabaseType database;
  private final IcServerStrings strings;

  /**
   * The login form.
   *
   * @param inServices The service directory
   */

  public IcViewLogin(
    final IcServiceDirectoryType inServices)
  {
    Objects.requireNonNull(inServices, "inServices");

    this.database =
      inServices.requireService(IcDatabaseType.class);
    this.strings =
      inServices.requireService(IcServerStrings.class);

    this.template =
      inServices.requireService(IcFMTemplateService.class)
        .loginTemplate();
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    final var username =
      request.getParameter("username");
    final var password =
      request.getParameter("password");
    final var session =
      request.getSession(true);

    if (username == null || password == null) {
      this.showForm(servletResponse, session);
      return;
    }

    try (var connection =
           this.database.openConnection(ICATIRO)) {
      try (var transaction =
             connection.openTransaction()) {
        final var users =
          transaction.queries(IcDatabaseUsersQueriesType.class);
        this.tryLogin(
          request,
          servletResponse,
          session,
          users,
          username,
          password
        );
      }
    } catch (final Exception e) {
      throw new ServletException(e);
    }
  }

  private void showForm(
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws IOException
  {
    servletResponse.setContentType("application/xhtml+xml");

    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IcFMLoginData(
          "Login",
          "Login",
          Optional.ofNullable((String) session.getAttribute("ErrorMessage"))
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private void tryLogin(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final HttpSession session,
    final IcDatabaseUsersQueriesType users,
    final String username,
    final String password)
    throws
    IOException,
    IcPasswordException,
    IcDatabaseException
  {
    if (username == null) {
      this.fail(response, session);
      return;
    }

    if (password == null) {
      this.fail(response, session);
      return;
    }

    final IcUser user;

    try {
      user =
        users.userGetForNameRequire(new IcUserDisplayName(username));
      final var ok =
        user.password().check(password);

      if (!ok) {
        this.fail(response, session);
        return;
      }
    } catch (final IcDatabaseException e) {
      if (Objects.equals(e.errorCode(), USER_NONEXISTENT)) {
        this.fail(response, session);
        return;
      }
      throw e;
    } catch (final IcValidityException e) {
      this.fail(response, session);
      return;
    }

    LOG.info("user '{}' logged in", username);
    session.setAttribute("UserID", user.id());

    users.userLogin(user.id(), request.getRemoteAddr());
    response.sendRedirect("/");
  }

  private void fail(
    final HttpServletResponse response,
    final HttpSession session)
    throws IOException
  {
    session.setAttribute(
      "ErrorMessage",
      this.strings.format("errorInvalidUsernamePassword")
    );

    response.setStatus(401);
    this.showForm(response, session);
  }
}
