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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:choose>
	<c:when test="${param.currentSortOrder == searchSettings.sortReverse}">
		<c:set var="currentSortOrder" value="reverse"/>
	</c:when>
	<c:otherwise>
		<c:set var="currentSortOrder" value="normal"/>
	</c:otherwise>
</c:choose>
<c:set var="currentSortKey" value="${param.currentSort},${currentSortOrder}"/>
<form action="basicSearch" id="result_sort_form" class="navigation_sort_form right">
	<select id="sort_select" name="${searchSettings.searchStateParams['ACTIONS']}">
		<option>sort by</option>
		<c:forEach var="sortEntry" items="${searchSettings.sortDisplayOrder}">
			<c:choose>
				<c:when test="${sortEntry == currentSortKey}">
					<c:set var="selected" value="selected"/>
				</c:when>
				<c:otherwise>
					<c:set var="selected" value=""/>
				</c:otherwise>
			</c:choose>
			<option value="${searchSettings.actions['SET_SORT']}:${sortEntry}" ${selected}>${searchSettings.sortDisplayNames[sortEntry]}</option>
		</c:forEach>
	</select>
	<noscript>
		<input type="submit" value="Go"/>
	</noscript>
	<c:set var="searchStateParameters" value='${fn:replace(searchStateUrl, "\\\"", "%22")}'/>
	<input type="hidden" name="${searchSettings.searchStateParams['SEARCH_WITHIN']}" value="${searchStateParameters}">
</form>