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
package edu.unc.lib.dl.search.solr.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;

/**
 * Response object for a search request.  Contains the list of results from the selected 
 * page, the list of hierarchical and nonhierarchical facets, and the count of the total
 * number of results the query found.
 * @author bbpennel
 */
public class SearchResultResponse {
	private List<BriefObjectMetadata> resultList;
	private FacetFieldList facetFields;
	private long resultCount;
	private SearchState searchState;
	private SolrQuery generatedQuery;
	
	public SearchResultResponse(){
	}
	
	public List<BriefObjectMetadata> getResultList() {
		return resultList;
	}

	public void setResultList(List<BriefObjectMetadata> resultList) {
		this.resultList = resultList;
	}

	public FacetFieldList getFacetFields() {
		return facetFields;
	}

	public void setFacetFields(FacetFieldList facetFields) {
		this.facetFields = facetFields;
	}

	public long getResultCount() {
		return resultCount;
	}

	public void setResultCount(long resultCount) {
		this.resultCount = resultCount;
	}

	public SearchState getSearchState() {
		return searchState;
	}

	public void setSearchState(SearchState searchState) {
		this.searchState = searchState;
	}
	
	public SolrQuery getGeneratedQuery() {
		return generatedQuery;
	}

	public void setGeneratedQuery(SolrQuery generatedQuery) {
		this.generatedQuery = generatedQuery;
	}

	public List<String> getIdList(){
		if (this.resultList == null)
			return null;
		List<String> ids = new ArrayList<String>();
		for (BriefObjectMetadata brief: this.resultList){
			ids.add(brief.getId());
		}
		return ids;
	}
}
