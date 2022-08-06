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

import com.io7m.icatiro.server.internal.freemarker.IcFMLoginData;
import com.io7m.icatiro.server.internal.freemarker.IcFMTemplateService;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class IcFMTemplateServiceTest
{
  @Test
  public void testGetLogin()
    throws IOException, TemplateException
  {
    final var service =
      IcFMTemplateService.create();

    final var template =
      service.loginTemplate();

    final var writer =
      new BufferedWriter(new OutputStreamWriter(System.out, UTF_8));

    template.process(new IcFMLoginData(
      "icatiro: Login",
      "Login",
      Optional.of("Error!")
    ), writer);

    writer.flush();
  }
}
