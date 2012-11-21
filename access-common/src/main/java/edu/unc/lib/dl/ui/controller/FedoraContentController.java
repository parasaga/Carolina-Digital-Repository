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
package edu.unc.lib.dl.ui.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.unc.lib.dl.security.access.AccessGroupConstants;
import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.service.FedoraContentService;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

/**
 * Controller which handles requests for specific content datastreams from Fedora and streams the results back as the
 * response.
 * 
 * @author bbpennel
 */
@Controller
public class FedoraContentController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(FedoraContentController.class);
	@Autowired
	private FedoraContentService fedoraContentService;
	@Autowired
	private SearchSettings searchSettings;

	@RequestMapping("/indexablecontent")
	public void handleIndexableRequest(Model model, HttpServletRequest request, HttpServletResponse response) {
		handleRequest(model, request, response);
	}

	@RequestMapping("/content")
	public void handleRequest(Model model, HttpServletRequest request, HttpServletResponse response) {
		AccessGroupSet accessGroups = getUserAccessGroups(request);

		boolean download = false;
		try {
			download = Boolean.parseBoolean(request.getParameter("dl"));
		} catch (Exception ignored) {
		}
		String datastream = request.getParameter("ds");
		Datastream datastreamClass;
		// Defaults to data_file if no datastream specified
		if (datastream == null) {
			datastreamClass = Datastream.DATA_FILE;
		} else {
			datastreamClass = Datastream.getDatastream(datastream);
		}

		String accessField = null;
		// Determine which permission applies to accessing this datastream.

		switch (datastreamClass.getCategory()) {
			case Original:
				accessField = SearchFieldKeys.FILE_ACCESS;
				break;
			case Derivative:
				accessField = SearchFieldKeys.SURROGATE_ACCESS;
				break;
			case Administrative:
				if (!accessGroups.contains(AccessGroupConstants.ADMIN_GROUP))
					throw new InvalidRecordRequestException();
				accessField = null;
				break;
			case Metadata:
				accessField = null;
				break;
			default:
				throw new InvalidRecordRequestException();
		}

		// Use solr to check if the user is allowed to view this item.
		String id = request.getParameter(searchSettings.searchStateParam(SearchFieldKeys.ID));

		// Get the content type of the object if its accessible
		List<String> resultFields = new ArrayList<String>();
		resultFields.add(SearchFieldKeys.ID);
		resultFields.add(SearchFieldKeys.FILESIZE);
		resultFields.add(SearchFieldKeys.DATASTREAM);
		SimpleIdRequest idRequest = new SimpleIdRequest(id, resultFields, accessGroups, accessField);

		BriefObjectMetadataBean briefObject = queryLayer.getObjectById(idRequest);
		// If the record isn't accessible then invalid record exception.
		if (briefObject == null) {
			throw new InvalidRecordRequestException();
		}

		try {
			String fileExtension = null;
			edu.unc.lib.dl.search.solr.model.Datastream datastreamResult = briefObject.getDatastream(datastreamClass
					.name());
			if (datastreamResult != null) {
				fileExtension = datastreamResult.getExtension();
				response.setContentLength(datastreamResult.getFilesize().intValue());
			}

			fedoraContentService.streamData(id, datastreamClass.name(), response.getOutputStream(), response,
					fileExtension, download);
		} catch (Exception e) {
			LOG.error("Failed to retrieve content for " + id + " datastream: " + datastream, e);
			throw new ResourceNotFoundException();
		}
	}

	@ResponseStatus(value = HttpStatus.FORBIDDEN)
	@ExceptionHandler(InvalidRecordRequestException.class)
	public String handleInvalidRecordRequest(HttpServletRequest request) {
		request.setAttribute("pageSubtitle", "Invalid content");
		return "error/invalidRecord";
	}
}