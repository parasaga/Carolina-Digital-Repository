<%--

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="edu.unc.lib.dl.search.solr.model.SearchState" %>
<%@ page import="edu.unc.lib.dl.search.solr.util.SearchSettings" %>

<c:set var="pageEndCount" value="${resultCount}"/>

<%
{
	//Calculating total number of pages and current page since jstl handles casting division to ints very poorly
	SearchSettings searchSettings = (SearchSettings)request.getAttribute("searchSettings");
	Long resultCount = (Long)request.getAttribute("resultCount");
	SearchState searchState = (SearchState)request.getAttribute("searchState");
	if (resultCount == null || searchState == null)
		return;
	long totalPages = resultCount / searchState.getRowsPerPage();
	if (resultCount % searchState.getRowsPerPage() > 0)
		totalPages++;
	long currentPage = searchState.getStartRow() / searchState.getRowsPerPage() + 1;
	
	long sideGap = searchSettings.pagesToDisplay / 2;
	long left = currentPage - sideGap;
	long right = currentPage + sideGap;
	
	if (left < 1){
		right -= left;
		left = 1;
	}
	if (right > totalPages){
		left -= (right - totalPages);
		if (left < 1)
			left = 1;
		right = totalPages;
	}
	
	pageContext.setAttribute("left", left);
	pageContext.setAttribute("right", right);
	pageContext.setAttribute("currentPage", currentPage);
	pageContext.setAttribute("totalPages", totalPages);
}
%>

<c:if test="${pageEndCount > searchState.rowsPerPage + searchState.startRow}">
	<c:set var="pageEndCount" value="${searchState.rowsPerPage + searchState.startRow}"/>
</c:if>
<p class="left navigation_bar">
	Showing 
	<c:if test="${resultCount > 0}">
		<span class="bold">${searchState.startRow+1}-${pageEndCount}</span>
		of
	</c:if>
	<span class="bold">${resultCount}</span> results
	<c:choose>
		<c:when test="${currentPage == 1}">
			&lt; previous
		</c:when>
		<c:otherwise>
			<c:url var="previousPageUrl" scope="page" value='search?${searchStateUrl}'>
				<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["PREVIOUS_PAGE"]}'/>
			</c:url>
			<a href="<c:out value="${previousPageUrl}"/>">&lt; previous</a>
		</c:otherwise>
	</c:choose>
	<c:if test="${left != 1}">
		...
	</c:if>
	<c:forEach var="pageNumber" begin="${left}" end="${right}" step="1" varStatus ="status">
		<c:choose>
			<c:when test="${pageNumber == currentPage}">
				<span class="bold">${pageNumber}</span>
			</c:when>
			<c:otherwise>
				<c:url var="pageJumpUrl" scope="page" value='search?${searchStateUrl}'>
					<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_START_ROW"]}:${(pageNumber - 1) * searchState.rowsPerPage}'/>
				</c:url>
				<a href="<c:out value="${pageJumpUrl}"/>"><c:out value="${pageNumber}" /></a>
			</c:otherwise>
		</c:choose>
	</c:forEach>
	<c:if test="${right != totalPages}">
		...
	</c:if>
	<c:choose>
		<c:when test="${right == '0' || currentPage == right}">
			next &gt;
		</c:when>
		<c:otherwise>
			<c:url var="nextPageUrl" scope="page" value='search?${searchStateUrl}'>
				<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["NEXT_PAGE"]}'/>
			</c:url>
			<a href="<c:out value="${nextPageUrl}"/>">next &gt;</a>
		</c:otherwise>
	</c:choose>
</p>