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

import com.io7m.icatiro.tests.arbitraries.IcArbAuditEventProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbHashProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbIcVMessagesProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbIcVProtocolSupportedProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbOffsetDateTimeProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbPasswordProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbSubsetMatchProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbTokenProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbURIProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbUUIDProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbUserDisplayNameProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbUserEmailProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbUserProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbUserSummaryProvider;
import net.jqwik.api.providers.ArbitraryProvider;

/**
 * Minimalist issue tracker (Arbitrary instances)
 */

module com.io7m.icatiro.tests.arbitraries
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires transitive com.io7m.icatiro.protocol.versions;
  requires transitive com.io7m.icatiro.model;
  requires transitive net.jqwik.api;

  provides ArbitraryProvider
    with
      IcArbIcVMessagesProvider,
      IcArbIcVProtocolSupportedProvider,
      IcArbAuditEventProvider,
      IcArbHashProvider,
      IcArbOffsetDateTimeProvider,
      IcArbPasswordProvider,
      IcArbSubsetMatchProvider,
      IcArbTokenProvider,
      IcArbURIProvider,
      IcArbUUIDProvider,
      IcArbUserDisplayNameProvider,
      IcArbUserEmailProvider,
      IcArbUserProvider,
      IcArbUserSummaryProvider
    ;

  exports com.io7m.icatiro.tests.arbitraries;
}
