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
package net.hillsdon.reviki.configuration;

import java.io.File;
import java.util.List;

import org.tmatesoft.svn.core.SVNURL;

import com.google.common.base.Function;

/**
 * Configuration details for a particular wiki.
 *
 * @author mth
 */
public interface WikiConfiguration {

  Function<WikiConfiguration, String> TO_NAME = new Function<WikiConfiguration, String>() {
    public String apply(final WikiConfiguration wiki) {
      return wiki.getWikiName();
    }
  };

  Function<WikiConfiguration, File> TO_SEARCH_INDEX_DIR = new Function<WikiConfiguration, File>() {
    public File apply(final WikiConfiguration wiki) {
      return wiki.getSearchIndexDirectory();
    }
  };

  /**
   * @return The name of the wiki this is the configuration for.
   */
  String getWikiName();

  /**
   * @return The fixed base URL, if any.  Otherwise the base URL should be derived
   *         from the request.
   */
  String getFixedBaseUrl();

  /**
   * @return The fixed base URL, if any, for the named wiki.
   */
  String getFixedBaseUrl(String wikiName);

  /**
   * @return The SVN URL for our data store.
   */
  SVNURL getUrl();

  /**
   * @return The default username to access the data store.
   */
  String getSVNUser();

  /**
   * @return The default password to access the data store.
   */
  String getSVNPassword();

  /**
   * @param url The URL.
   * @throws IllegalArgumentException If the URI is not a valid SVNURL.
   */
  void setUrl(String url) throws IllegalArgumentException;

  /**
   * @param user The SVN user.
   */
  void setSVNUser(String user);

  /**
   * @param pass The SVN password.
   */
  void setSVNPassword(String pass);

  /**
   * @return The directory to store the search engine index in or null if not possible.
   */
  File getSearchIndexDirectory();

  /**
   * @return An array of directories to the other wikis use to store the search engine indices in.
   */
  List<File> getOtherSearchIndexDirectories();

  /**
   * @return true if the configuration is OK.
   */
  boolean isComplete();

  /**
   * Save the wiki configuration.
   */
  void save();

  /**
   * @return true if changes can be persisted.
   *              (note it is possible for this to change over time).
   */
  boolean isEditable();

}
