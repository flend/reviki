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
package net.hillsdon.reviki.di.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.hillsdon.reviki.configuration.DeploymentConfiguration;
import net.hillsdon.reviki.configuration.WikiConfiguration;
import net.hillsdon.reviki.di.WikiSession;
import net.hillsdon.reviki.search.impl.BasicAuthAwareSearchEngine;
import net.hillsdon.reviki.search.impl.ExternalCommitAwareSearchEngine;
import net.hillsdon.reviki.search.impl.LuceneSearcher;
import net.hillsdon.reviki.vc.PageInfo;
import net.hillsdon.reviki.vc.PageStore;
import net.hillsdon.reviki.vc.PageStoreException;
import net.hillsdon.reviki.vc.impl.AutoPropertiesApplier;
import net.hillsdon.reviki.vc.impl.AutoPropertiesApplierImpl;
import net.hillsdon.reviki.vc.impl.CachingPageStore;
import net.hillsdon.reviki.vc.impl.ChangeNotificationDispatcherImpl;
import net.hillsdon.reviki.vc.impl.ConfigPageCachingPageStore;
import net.hillsdon.reviki.vc.impl.DeletedRevisionTracker;
import net.hillsdon.reviki.vc.impl.FixedMimeIdentifier;
import net.hillsdon.reviki.vc.impl.InMemoryDeletedRevisionTracker;
import net.hillsdon.reviki.web.dispatching.ResourceHandler;
import net.hillsdon.reviki.web.dispatching.WikiHandler;
import net.hillsdon.reviki.web.dispatching.impl.ResourceHandlerImpl;
import net.hillsdon.reviki.web.dispatching.impl.WikiHandlerImpl;
import net.hillsdon.reviki.web.handlers.PageHandler;
import net.hillsdon.reviki.web.handlers.impl.PageHandlerImpl;
import net.hillsdon.reviki.web.pages.DiffGenerator;
import net.hillsdon.reviki.web.pages.PageSource;
import net.hillsdon.reviki.web.pages.SpecialPages;
import net.hillsdon.reviki.web.pages.impl.AllPages;
import net.hillsdon.reviki.web.pages.impl.DefaultPageImpl;
import net.hillsdon.reviki.web.pages.impl.DiffGeneratorImpl;
import net.hillsdon.reviki.web.pages.impl.FindPage;
import net.hillsdon.reviki.web.pages.impl.OrphanedPages;
import net.hillsdon.reviki.web.pages.impl.PageSourceImpl;
import net.hillsdon.reviki.web.pages.impl.RecentChanges;
import net.hillsdon.reviki.web.pages.impl.SpecialPagesImpl;
import net.hillsdon.reviki.web.urls.ApplicationUrls;
import net.hillsdon.reviki.web.urls.Configuration;
import net.hillsdon.reviki.web.urls.InternalLinker;
import net.hillsdon.reviki.web.urls.WikiUrls;
import net.hillsdon.reviki.web.urls.URLOutputFilter;
import net.hillsdon.reviki.web.urls.impl.PageStoreConfiguration;
import net.hillsdon.reviki.web.urls.impl.WikiUrlsImpl;
import net.hillsdon.reviki.web.vcintegration.AutoProperiesFromConfigPage;
import net.hillsdon.reviki.web.vcintegration.BasicAuthPassThroughBasicSVNOperationsFactory;
import net.hillsdon.reviki.web.vcintegration.PerRequestPageStoreFactory;
import net.hillsdon.reviki.web.vcintegration.RequestLifecycleAwareManager;
import net.hillsdon.reviki.web.vcintegration.RequestLifecycleAwareManagerImpl;
import net.hillsdon.reviki.web.vcintegration.RequestScopedPageStore;
import net.hillsdon.reviki.web.vcintegration.RequestScopedThreadLocalBasicSVNOperations;
import net.hillsdon.reviki.wiki.MarkupRenderer;
import net.hillsdon.reviki.wiki.feeds.AtomFeedWriter;
import net.hillsdon.reviki.wiki.feeds.FeedWriter;
import net.hillsdon.reviki.wiki.graph.WikiGraph;
import net.hillsdon.reviki.wiki.graph.WikiGraphImpl;
import net.hillsdon.reviki.wiki.macros.AttrMacro;
import net.hillsdon.reviki.wiki.macros.IncomingLinksMacro;
import net.hillsdon.reviki.wiki.macros.OutgoingLinksMacro;
import net.hillsdon.reviki.wiki.macros.SearchMacro;
import net.hillsdon.reviki.wiki.plugin.PluginsImpl;
import net.hillsdon.reviki.wiki.renderer.SvnWikiRenderer;
import net.hillsdon.reviki.wiki.renderer.creole.ast.ASTNode;
import net.hillsdon.reviki.wiki.renderer.macro.Macro;

import org.picocontainer.MutablePicoContainer;

import com.google.common.base.Supplier;

public class WikiSessionImpl extends AbstractSession implements WikiSession {

  private SvnWikiRenderer _renderer;
  private PluginsImpl _plugins;

  public WikiSessionImpl(final ApplicationSessionImpl parent, final WikiConfiguration configuration) {
    super(parent, configuration);
  }

  public WikiHandler getWikiHandler() {
    return getContainer().getComponent(WikiHandlerImpl.class);
  }

  public void configure(final MutablePicoContainer container) {
    container.addComponent(WikiUrls.class, WikiUrlsImpl.class);

    // This is cheating!
    // Some of this is a bit circular.  It needs fixing before we can use the di container.
    final WikiConfiguration configuration = container.getComponent(WikiConfiguration.class);
    final ApplicationUrls applicationUrls = getParentContainer().getComponent(ApplicationUrls.class);

    File primarySearchDir = configuration.getSearchIndexDirectory();
    List<File> otherSearchDirs = configuration.getOtherSearchIndexDirectories();

    // Wrapping to get around _renderer not being initialised yet
    MarkupRenderer<String> renderer = new MarkupRenderer<String>() {
      @Override
      public ASTNode parse(PageInfo page) throws IOException, PageStoreException {
        return _renderer.parse(page);
      }

      @Override
      public String render(ASTNode ast, URLOutputFilter urlOutputFilter) throws IOException, PageStoreException {
        return _renderer.render(ast,  urlOutputFilter);
      }
    };

    InternalLinker internalLinker = new InternalLinker(container.getComponent(WikiUrls.class));
    AutoProperiesFromConfigPage autoProperties = new AutoProperiesFromConfigPage();
    AutoPropertiesApplier autoPropertiesApplier = new AutoPropertiesApplierImpl(autoProperties);
    RequestScopedThreadLocalBasicSVNOperations operations = new RequestScopedThreadLocalBasicSVNOperations(new BasicAuthPassThroughBasicSVNOperationsFactory(configuration.getUrl(), autoPropertiesApplier));
    BasicAuthAwareSearchEngine authSearch = new BasicAuthAwareSearchEngine(new LuceneSearcher(configuration.getWikiName(), primarySearchDir, otherSearchDirs, renderer), getParentContainer().getComponent(DeploymentConfiguration.class));
    final ExternalCommitAwareSearchEngine searchEngine = new ExternalCommitAwareSearchEngine(authSearch);
    DeletedRevisionTracker tracker = new InMemoryDeletedRevisionTracker();
    Supplier<PageStore> pageStoreFactory = new PerRequestPageStoreFactory(configuration.getWikiName(), searchEngine, tracker, operations, autoPropertiesApplier, new FixedMimeIdentifier());
    final RequestScopedPageStore pageStore = new RequestScopedPageStore(pageStoreFactory);
    ConfigPageCachingPageStore cachingPageStore = new ConfigPageCachingPageStore(pageStore);
    PageStoreConfiguration pageStoreConfiguration = new PageStoreConfiguration(cachingPageStore, applicationUrls);
    final WikiGraph wikiGraph = new WikiGraphImpl(cachingPageStore, searchEngine);
    _renderer = new SvnWikiRenderer(pageStoreConfiguration, pageStore, internalLinker, new Supplier<List<Macro>>() {
      public List<Macro> get() {
        List<Macro> macros = new ArrayList<Macro>(Arrays.<Macro>asList(new IncomingLinksMacro(wikiGraph), new OutgoingLinksMacro(wikiGraph), new SearchMacro(searchEngine), new AttrMacro(pageStore)));
        macros.addAll(_plugins.getImplementations(Macro.class));
        return macros;
      }
    });

    _plugins = new PluginsImpl(pageStore);
    searchEngine.setPageStore(pageStore);
    autoProperties.setPageStore(cachingPageStore);

    container.addComponent(authSearch.getRequestLifecycleAware()); // This needs adding so that RequestLifecycleAwareness works, but it shouldn't show up as the search engine
    container.addComponent(tracker);
    container.addComponent(operations);
    container.addComponent(PageStore.class, pageStore);
    container.addComponent(CachingPageStore.class, cachingPageStore);
    container.addComponent(RequestLifecycleAwareManager.class, RequestLifecycleAwareManagerImpl.class);

    container.addComponent(wikiGraph);
    container.addComponent(Configuration.class, pageStoreConfiguration);
    container.addComponent(internalLinker);
    container.addComponent(_plugins);
    container.addComponent(_renderer);
    container.addComponent(_renderer.getRenderers());
    container.addComponent(searchEngine);
    container.addComponent(DiffGenerator.class, DiffGeneratorImpl.class);
    container.addComponent(FeedWriter.class, AtomFeedWriter.class);

    // Special pages
    container.addComponent(SpecialPages.class, SpecialPagesImpl.class);
    container.addComponent(FindPage.class);
    container.addComponent(OrphanedPages.class);
    container.addComponent(AllPages.class);
    container.addComponent(RecentChanges.class);

    // Page handling
    container.addComponent(DefaultPageImpl.class, DefaultPageImpl.class);
    container.addComponent(PageSource.class, PageSourceImpl.class);
    container.addComponent(PageHandler.class, PageHandlerImpl.class);

    // Allow plugin classes to depend on the core wiki API.
    _plugins.addPluginAccessibleComponent(pageStore);
    _plugins.addPluginAccessibleComponent(wikiGraph);
    _plugins.addPluginAccessibleComponent(searchEngine);

    container.addComponent(ChangeNotificationDispatcherImpl.class);

    container.addComponent(WikiSession.class, this);
    container.addComponent(WikiHandlerImpl.class, WikiHandlerImpl.class);
    container.addComponent(ResourceHandler.class, ResourceHandlerImpl.class);
  }

}
