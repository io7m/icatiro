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

import com.io7m.icatiro.model.IcPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.icatiro.model.IcPasswordAlgorithms;
import com.io7m.icatiro.model.IcPasswordException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

public final class IcPasswordAlgorithmsTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcPasswordAlgorithmsTest.class);

  @Test
  public void testPBKDF2()
    throws Exception
  {
    Assertions.assertInstanceOf(
      IcPasswordAlgorithmPBKDF2HmacSHA256.class,
      IcPasswordAlgorithms.parse("PBKDF2WithHmacSHA256:10000:256")
    );
  }

  @Test
  public void testPBKDF2Execute()
    throws Exception
  {
    final var algorithm =
      IcPasswordAlgorithmPBKDF2HmacSHA256.create(10000, 256);

    final byte[] salt = new byte[4];
    salt[0] = (byte) 0x10;
    salt[1] = (byte) 0x20;
    salt[2] = (byte) 0x30;
    salt[3] = (byte) 0x40;

    final var password =
      algorithm.createHashed("12345678", salt);

    LOG.debug("hash: {}", password.hash());
    LOG.debug("salt: {}", password.salt());
  }

  @TestFactory
  public Stream<DynamicTest> testUnparseable()
  {
    return Stream.of(
      "",
      "PBKDF2WithHmacSHA256",
      "PBKDF2WithHmacSHA256:10000",
      "PBKDF2WithHmacSHA256:10000:x",
      "PBKDF2WithHmacSHA256:y:245"
    ).map(IcPasswordAlgorithmsTest::testUnparseableOf);
  }

  private static DynamicTest testUnparseableOf(
    final String text)
  {
    return DynamicTest.dynamicTest(
      "testUnparseable_" + text,
      () -> {
        Assertions.assertThrows(IcPasswordException.class, () -> {
          IcPasswordAlgorithms.parse(text);
        });
      }
    );
  }
}
