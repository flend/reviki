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
package net.hillsdon.reviki.wiki.renderer.creole;

import java.util.Arrays;

public class LinkParts {
  private final String _text;
  private final String _wiki;
  private final String _refd;
  private final String _fragment;
  private final String _pagePath;

  public LinkParts(final String text, final String wiki, final String refd) {
    _text = text;
    _wiki = wiki;
    _refd = refd == null ? "" : refd;
    if (!isURL()) {
      final int indexOfHash = _refd.lastIndexOf('#');
      if (indexOfHash != -1) {
        _fragment = _refd.substring(indexOfHash + 1);
        _pagePath = _refd.substring(0, indexOfHash);
      }
      else {
        _fragment = null;
        _pagePath = _refd;
      }
    }
    else {
      _fragment = null;
      _pagePath = null;
    }
  }
  public String getText() {
    return _text;
  }
  public String getWiki() {
    return _wiki;
  }
  public String getRefd() {
    return _refd;
  }
  public String getPagePath() {
    return _pagePath;
  }
  public String getFragment() {
    return _fragment;
  }
  public boolean isURL() {
    return _wiki == null && getRefd().matches("\\p{L}+?:.*");
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + Arrays.asList(_text, _wiki, _refd).toString();
  }

  // Eclipse generated, yes it is obsene but it's easy and correct.

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_refd == null) ? 0 : _refd.hashCode());
    result = prime * result + ((_text == null) ? 0 : _text.hashCode());
    result = prime * result + ((_wiki == null) ? 0 : _wiki.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final LinkParts other = (LinkParts) obj;
    if (_refd == null) {
      if (other._refd != null)
        return false;
    }
    else if (!_refd.equals(other._refd))
      return false;
    if (_text == null) {
      if (other._text != null)
        return false;
    }
    else if (!_text.equals(other._text))
      return false;
    if (_wiki == null) {
      if (other._wiki != null)
        return false;
    }
    else if (!_wiki.equals(other._wiki))
      return false;
    return true;
  }

}