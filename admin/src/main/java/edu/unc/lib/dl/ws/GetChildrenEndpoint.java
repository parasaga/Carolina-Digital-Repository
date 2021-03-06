/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ws;

import org.apache.log4j.Logger;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import edu.unc.lib.dl.schema.BasicQueryRequest;
import edu.unc.lib.dl.schema.BasicQueryResponseList;
import edu.unc.lib.dl.schema.GetChildrenRequest;
import edu.unc.lib.dl.schema.GetChildrenResponse;
import edu.unc.lib.dl.service.SearchService;
import edu.unc.lib.dl.util.Constants;

@Endpoint
public class GetChildrenEndpoint {
    private final Logger logger = Logger.getLogger(GetChildrenEndpoint.class);

    private SearchService searchService;

    @PayloadRoot(localPart = Constants.GET_CHILDREN_REQUEST, namespace = Constants.NAMESPACE)
    public GetChildrenResponse getSearchResult(
	    GetChildrenRequest getChildrenRequest) {

	return searchService.getChildren(getChildrenRequest);
    }

    public void setSearchService(SearchService searchService) {
	this.searchService = searchService;
    }
}
