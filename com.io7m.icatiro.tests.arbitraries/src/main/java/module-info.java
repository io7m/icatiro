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

import com.io7m.icatiro.tests.arbitraries.IcArbAccessControlledObjectProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbAuditEventProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbHashProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbIcTMessageProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbOffsetDateTimeProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbPermissionScopedProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbPermissionSetProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbProjectIdProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbProjectProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbProjectShortNameProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbProjectTitleProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbTicketColumnOrderingProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbTicketCreationProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbTicketIdProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbTicketListParametersProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbTicketSummaryProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbTicketTitleProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbTimeRangeProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbTokenProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbURIProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbUUIDProvider;
import com.io7m.icatiro.tests.arbitraries.IcArbUserProvider;
import net.jqwik.api.providers.ArbitraryProvider;

/**
 * Help desk (Arbitrary instances)
 */

module com.io7m.icatiro.tests.arbitraries
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires transitive com.io7m.icatiro.model;
  requires transitive com.io7m.icatiro.protocol.tickets;
  requires transitive net.jqwik.api;

  uses ArbitraryProvider;

  provides ArbitraryProvider
    with
      IcArbAccessControlledObjectProvider,
      IcArbAuditEventProvider,
      IcArbHashProvider,
      IcArbIcTMessageProvider,
      IcArbOffsetDateTimeProvider,
      IcArbPermissionScopedProvider,
      IcArbPermissionSetProvider,
      IcArbProjectIdProvider,
      IcArbProjectProvider,
      IcArbProjectShortNameProvider,
      IcArbProjectTitleProvider,
      IcArbTicketColumnOrderingProvider,
      IcArbTicketCreationProvider,
      IcArbTicketIdProvider,
      IcArbTicketListParametersProvider,
      IcArbTicketSummaryProvider,
      IcArbTicketTitleProvider,
      IcArbTimeRangeProvider,
      IcArbTokenProvider,
      IcArbURIProvider,
      IcArbUUIDProvider,
      IcArbUserProvider
    ;

  exports com.io7m.icatiro.tests.arbitraries;
}
