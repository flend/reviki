/**
 * Copyright 2008 Matthew Hillsdon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hillsdon.reviki.web.urls;

import static java.lang.String.format;
import static net.hillsdon.reviki.text.WikiWordUtils.isAcronym;
import net.hillsdon.fij.text.Escape;
import net.hillsdon.reviki.vc.PageStore;
import net.hillsdon.reviki.vc.PageStoreException;
import net.hillsdon.reviki.vc.impl.CachingPageStore;
import net.hillsdon.reviki.vc.impl.PageReferenceImpl;

public class InternalLinker {

  private final PageStore _store;
  private final WikiUrls _wikiUrls;

  public InternalLinker(final WikiUrls wikiUrls, final CachingPageStore store) {
    _wikiUrls = wikiUrls;
    _store = store;
  }

  private boolean exists(final String name) {
    try {
      return _store.exists(new PageReferenceImpl(name));
    }
    catch (PageStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public String url(final String pageName, final String extraPath, final String query, final String fragment, final URLOutputFilter urlOutputFilter) {
    return _wikiUrls.page(null, pageName, extraPath, query, fragment, urlOutputFilter);
  }

  public String url(final String wikiName, final String pageName, final String extraPath, final String query, final String fragment, final URLOutputFilter urlOutputFilter) {
    return _wikiUrls.page(wikiName, pageName, extraPath, query, fragment, urlOutputFilter);
  }

  public String aHref(final String pageName, final String linkText, final String extraPath, final String query, final String fragment, final URLOutputFilter urlOutputFilter) {
    String x = aHref(null, pageName, linkText, extraPath, query, fragment, urlOutputFilter);
    return x;
  }

  /**
   * Generate a hyperlink.
   * @param wikiName If not-null it will generate a foreign-wiki link (which will always be deemed to exist)
   * @param pageName The name of the page on wikiName
   * @param linkText The text to display
   * @return The html
   */
  public String aHref(final String wikiName, final String pageName, final String linkText, final String extraPath, final String query, final String fragment, final URLOutputFilter urlOutputFilter) {
    // Special case: only link acronyms with real pages or on foreign wikis.
    final boolean exists = wikiName!=null || exists(pageName);
    if (!exists && isAcronym(pageName)) {
      return Escape.html(pageName);
    }

    final String otherAttrs = exists ? "" : "rel='nofollow' ";
    final String cssClass = exists ? "existing-page" : "new-page";
    return format("<a %sclass='%s' href='%s'>%s</a>",
      otherAttrs,
      cssClass,
      Escape.html(url(wikiName, pageName, query, extraPath, fragment, urlOutputFilter)),
      Escape.html(linkText)
    );
  }

}
