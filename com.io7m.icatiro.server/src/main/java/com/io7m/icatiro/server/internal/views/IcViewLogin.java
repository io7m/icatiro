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
import com.io7m.icatiro.model.IcPermissionSet;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.model.IcValidityException;
import com.io7m.icatiro.server.internal.IcIdentityClients;
import com.io7m.icatiro.server.internal.IcServerBrandingService;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.server.internal.IcUserSessionService;
import com.io7m.icatiro.server.internal.IcUserUpdates;
import com.io7m.icatiro.server.internal.common.IcCommonInstrumentedServlet;
import com.io7m.icatiro.server.internal.freemarker.IcFMLoginData;
import com.io7m.icatiro.server.internal.freemarker.IcFMLoginErrorMessage;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateService;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateType;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.jvindicator.core.Vindication;
import freemarker.template.TemplateException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.icatiro.server.internal.IcServerRequestDecoration.requestIdFor;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHostProxied;

/**
 * The login form.
 */

public final class IcViewLogin extends IcCommonInstrumentedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcViewLogin.class);

  private final IcFMTemplateType<IcFMLoginData> template;
  private final IcDatabaseType database;
  private final IcServerStrings strings;
  private final IcIdentityClients idClients;
  private final IcUserSessionService userSessions;
  private final IcServerBrandingService branding;

  /**
   * The login form.
   *
   * @param inServices The service directory
   */

  public IcViewLogin(
    final IcServiceDirectoryType inServices)
  {
    super(inServices);

    this.database =
      inServices.requireService(IcDatabaseType.class);
    this.strings =
      inServices.requireService(IcServerStrings.class);
    this.idClients =
      inServices.requireService(IcIdentityClients.class);
    this.branding =
      inServices.requireService(IcServerBrandingService.class);
    this.userSessions =
      inServices.requireService(IcUserSessionService.class);

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
    final var session = request.getSession(true);

    try {
      final var v =
        Vindication.startWithExceptions(IcValidityException::new);
      final var userNameParameter =
        v.addRequiredParameter("username", x -> x);
      final var passwordParameter =
        v.addRequiredParameter("password", x -> x);

      v.check(request.getParameterMap());

      this.tryLogin(
        request,
        servletResponse,
        session,
        userNameParameter.get(),
        passwordParameter.get()
      );
    } catch (final IcValidityException e) {
      Span.current().recordException(e);
      servletResponse.setStatus(400);
      this.showForm(servletResponse, session);
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
          this.branding.htmlTitle("Login"),
          this.branding.title(),
          true,
          this.idClients.passwordResetURI(),
          Optional.empty(),
          Optional.ofNullable((IcFMLoginErrorMessage) session.getAttribute(
            "ErrorMessage")),
          this.branding.loginExtraText()
        ),
        writer
      );
    } catch (final TemplateException e) {
      Span.current().recordException(e);
      throw new IOException(e);
    }
  }

  private void tryLogin(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final HttpSession session,
    final String username,
    final String password)
    throws IOException
  {
    final IdUser user;

    final var span =
      this.tracer()
        .spanBuilder("LoginToIdstore")
        .setStartTimestamp(Instant.now())
        .setSpanKind(SpanKind.CLIENT)
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      final var client = this.idClients.createClient();
      user = client.login(
        username,
        password,
        this.idClients.baseURI(),
        Map.ofEntries(
          Map.entry(remoteHostProxied(), request.getRemoteAddr())
        )
      );

      final var resultUser =
        IcUserUpdates.userMerge(
          this.database,
          new IcUser(
            user.id(),
            user.idName(),
            user.emails().toList(),
            IcPermissionSet.empty())
        );

      this.userSessions.create(resultUser, session, client);
    } catch (final IdUClientException e) {
      span.recordException(e);
      this.failWithClientError(request, response, session, e);
      return;
    } catch (final Exception e) {
      span.recordException(e);
      this.fail(
        response,
        session,
        new IcFMLoginErrorMessage(
          e.getMessage(),
          Optional.of(this.strings.format("errorReported")),
          Optional.of(requestIdFor(request))
        )
      );
      return;
    } finally {
      span.end();
    }

    LOG.info("user '{}' logged in", username);
    session.setAttribute("UserID", user.id());
    response.sendRedirect("/");
  }

  private void failWithClientError(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final HttpSession session,
    final IdUClientException e)
    throws IOException
  {
    final var code = e.errorCode();
    if (Objects.equals(code, AUTHENTICATION_ERROR)) {
      session.setAttribute(
        "ErrorMessage",
        new IcFMLoginErrorMessage(
          this.strings.format("errorInvalidUsernamePassword"),
          Optional.empty(),
          Optional.empty()
        )
      );
      response.setStatus(401);
    } else if (Objects.equals(code, IO_ERROR)) {
      session.setAttribute(
        "ErrorMessage",
        new IcFMLoginErrorMessage(
          this.strings.format("errorIdentityServerConnect"),
          Optional.of(this.strings.format("errorReported")),
          Optional.of(requestIdFor(request))
        )
      );
      response.setStatus(500);
    } else {
      session.setAttribute("ErrorMessage", e.getMessage());
      response.setStatus(400);
    }

    this.showForm(response, session);
  }

  private void fail(
    final HttpServletResponse response,
    final HttpSession session,
    final IcFMLoginErrorMessage message)
    throws IOException
  {
    session.setAttribute("ErrorMessage", message);
    response.setStatus(500);
    this.showForm(response, session);
  }
}
