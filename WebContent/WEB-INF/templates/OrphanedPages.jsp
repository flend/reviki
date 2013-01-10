<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.hillsdon.net/ns/reviki/tags" prefix="sw" %>

<tiles:insertTemplate template="SiteTemplate.jsp">
  <tiles:putAttribute name="title">Orphaned Pages</tiles:putAttribute>
  <tiles:putAttribute name="heading">Orphaned Pages</tiles:putAttribute>
  <tiles:putAttribute name="content">
    <p>The following pages are not referenced from any other page.</p>
    <ul>
      <c:forEach var="path" items="${pageList}">
        <li><sw:wikiPage page="${path}"/></li>
      </c:forEach>
    </ul>
  </tiles:putAttribute>
</tiles:insertTemplate>
