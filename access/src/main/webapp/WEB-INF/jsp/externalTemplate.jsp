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
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>

<c:set var="contentPath" value="${contentUrl}" scope="page"/>
<c:if test="${not empty pageContext.request.queryString}">
	<c:set var="contentPath" value="${contentUrl}?${pageContext.request.queryString}"/>
</c:if>

<c:choose>
	<c:when test="${pageContext.request.method == 'POST'}">
		${cdr:postImport(pageContext.request, contentUrl)}
	</c:when>
	<c:otherwise>
		<c:import url="${contentPath}"/>
	</c:otherwise>
</c:choose>
