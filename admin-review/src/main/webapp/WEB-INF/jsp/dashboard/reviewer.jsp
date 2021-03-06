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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="contentarea">
	<div class="description_text descriptivearea">
		<p>
			Welcome, <c:out value="${pageContext.request.remoteUser}"/>. You are viewing the Carolina Digital Repository's administrative interface.
		</p>
		<p>
			All collections you have administrative access to are listed below.  To begin reviewing unpublished objects, click the "Review unpublished items" link to the right of the collection.  
			To View or modify any of the objects within the collection, including previously published items, click the title of the collection.
		</p>
		<p>
			If you have any questions about the interface or the Carolina Digital Repository in general, please do not hesitate to <a href="https://${pageContext.request.serverName}/external?page=contact">contact us</a>.
			  
		</p>
	</div>
	<div class="collection_list">
		<c:import url="search/collectionList.jsp" />
	</div>
</div>