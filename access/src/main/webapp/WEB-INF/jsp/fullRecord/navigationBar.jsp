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
<div class="onecol">
	<div class="contentarea">
		<div class="bottomline">
			<c:set var="recordNavigationState" value="${sessionScope.recordNavigationState}" />
			<p class="left"><a href="search?<c:out value='${recordNavigationState.searchStateUrl}' />">Back to Results</a></p>
			<p class="right">
				<c:catch var="previousException">
					<c:set var="previousId" value="${recordNavigationState.previousRecordId}"/>
				</c:catch>
				<c:choose>
					<c:when test="${previousException!=null}">
						<a href="recordNavigation?${searchSettings.searchStateParams['ACTIONS']}=${searchSettings.actions['PREVIOUS_PAGE']}&${searchSettings.searchStateParams['ID']}=${briefObject.id}">&lt; Previous</a>
					</c:when>
					<c:when test="${empty previousId}">
						&lt; Previous
					</c:when>
					<c:otherwise>
						<a href="record?id=${previousId}">&lt; Previous</a>
					</c:otherwise>
				</c:choose>
				&nbsp;
				<c:catch var="nextException">
					<c:set var="nextId" value="${recordNavigationState.nextRecordId}"/>
				</c:catch>
				<c:choose>
					<c:when test="${nextException!=null}">
						<a href="recordNavigation?${searchSettings.searchStateParams['ACTIONS']}=${searchSettings.actions['NEXT_PAGE']}&${searchSettings.searchStateParams['ID']}=${briefObject.id}">Next &gt;</a>
					</c:when>
					<c:when test="${empty nextId}">
						Next &gt;
					</c:when>
					<c:otherwise>
						<a href="record?id=${nextId}">Next &gt;</a>
					</c:otherwise>
				</c:choose>
			</p>
		</div>
	</div>
</div>