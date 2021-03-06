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
package net.hillsdon.reviki.web.dispatching.impl;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.hillsdon.reviki.vc.ChangeNotificationDispatcher;
import net.hillsdon.reviki.vc.PageStoreAuthenticationException;
import net.hillsdon.reviki.vc.PageStoreInvalidException;
import net.hillsdon.reviki.vc.impl.CachingPageStore;
import net.hillsdon.reviki.web.common.ConsumedPath;
import net.hillsdon.reviki.web.common.JspView;
import net.hillsdon.reviki.web.common.RequestAttributes;
import net.hillsdon.reviki.web.common.ComplementaryPageRenderer;
import net.hillsdon.reviki.web.common.RequestHandler;
import net.hillsdon.reviki.web.common.View;
import net.hillsdon.reviki.web.dispatching.ResourceHandler;
import net.hillsdon.reviki.web.dispatching.WikiHandler;
import net.hillsdon.reviki.web.handlers.PageHandler;
import net.hillsdon.reviki.web.urls.Configuration;
import net.hillsdon.reviki.web.urls.InternalLinker;
import net.hillsdon.reviki.web.urls.WikiUrls;
import net.hillsdon.reviki.web.vcintegration.BuiltInPageReferences;
import net.hillsdon.reviki.web.vcintegration.RequestLifecycleAwareManager;
import net.hillsdon.reviki.wiki.MarkupRenderer;
import net.hillsdon.reviki.wiki.renderer.SvnWikiRenderer;
import net.hillsdon.reviki.wiki.renderer.creole.LinkResolutionContext;

/**
 * A particular wiki (sub-wiki, whatever).
 *
 * @author mth
 */
public class WikiHandlerImpl implements WikiHandler {

  private static final class RequestAuthenticationView implements View {
    public void render(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
      response.setHeader("WWW-Authenticate", "Basic realm=\"Wiki login\"");
      response.setStatus(401);
      String message = "Authentication required to access this page.";
      request.setAttribute("customMessage", message);
      request.setAttribute(ATTRIBUTE_WIKI_IS_VALID, false);
      request.getRequestDispatcher("/WEB-INF/templates/Error.jsp").forward(request, response);
    }
  }

  public static final String ATTRIBUTE_WIKI_IS_VALID = "wikiIsValid";

  private final RequestLifecycleAwareManager _requestLifecycleAwareManager;
  private final MarkupRenderer<String> _renderer;
  private final CachingPageStore _cachingPageStore;
  private final InternalLinker _internalLinker;
  private final ChangeNotificationDispatcher _syncUpdater;
  private final WikiUrls _wikiUrls;
  private final ResourceHandler _resources;
  private final PageHandler _pageHandler;

  private final Configuration _configuration;

  public WikiHandlerImpl(CachingPageStore cachingPageStore, SvnWikiRenderer renderer, InternalLinker internalLinker, ChangeNotificationDispatcher syncUpdater, RequestLifecycleAwareManager requestLifecycleAwareManager, ResourceHandler resources, PageHandler handler, WikiUrls wikiUrls, Configuration configuration) {
    _cachingPageStore = cachingPageStore;
    _renderer = renderer;
    _internalLinker = internalLinker;
    _syncUpdater = syncUpdater;
    _requestLifecycleAwareManager = requestLifecycleAwareManager;
    _resources = resources;
    _pageHandler = handler;
    _wikiUrls = wikiUrls;
    _configuration = configuration;
  }

  public View test(HttpServletRequest request, HttpServletResponse response) throws PageStoreInvalidException, Exception {
    return handleInternal(new ConsumedPath(Collections.<String>emptyList()), request, response, new PageHandler() {
      public View handle(ConsumedPath path, HttpServletRequest request, HttpServletResponse response) throws Exception {
        _cachingPageStore.assertValid();
        return null;
      }
    });
  }

  public View handle(final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    return handleInternal(path, request, response, new RequestHandler() {
      public View handle(ConsumedPath path, HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setAttribute(WikiUrls.KEY, _wikiUrls);
        request.setAttribute(JspView.ATTR_CSS_URL, _internalLinker.uri(BuiltInPageReferences.CONFIG_CSS.getPath(), "ctype=raw").toASCIIString());

        // Used for resolving links in the context of this request
        request.setAttribute("internalLinker", _internalLinker);
        request.setAttribute("configuration", _configuration);
        request.setAttribute("pageStore", _cachingPageStore);
        request.setAttribute(RequestAttributes.LINK_RESOLUTION_CONTEXT, new LinkResolutionContext(_internalLinker, _configuration.getInterWikiLinker(), _configuration, _cachingPageStore));

        if ("resources".equals(path.peek())) {
          return _resources.handle(path.consume(), request, response);
        }

        _syncUpdater.sync();
        request.setAttribute("complementaryContent", new ComplementaryPageRenderer(request, response, _renderer, _cachingPageStore));
        return _pageHandler.handle(path, request, response);
      }
    });
  }

  private View handleInternal(final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response, final RequestHandler delegate) throws Exception {
    try {
      _requestLifecycleAwareManager.requestStarted(request);
      return delegate.handle(path, request, response);
    }
    catch (PageStoreAuthenticationException ex) {
      return new RequestAuthenticationView();
    }
    catch (Exception ex) {
      // Rather horrible, needed at the moment for auth failures during rendering (linking).
      if (ex.getCause() instanceof PageStoreAuthenticationException) {
        return new RequestAuthenticationView();
      }
      else {
        // Don't try to show wiki header/footer.
        request.setAttribute(ATTRIBUTE_WIKI_IS_VALID, false);
        throw ex;
      }
    }
  }

}
