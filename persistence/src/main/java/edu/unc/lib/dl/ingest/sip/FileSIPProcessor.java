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
package edu.unc.lib.dl.ingest.sip;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.AIPImpl;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.aip.RDFAwareAIPImpl;
import edu.unc.lib.dl.pidgen.PIDGenerator;
import edu.unc.lib.dl.util.Checksum;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PathUtil;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil.ObjectProperty;

public abstract class FileSIPProcessor implements SIPProcessor {
	private static final Log log = LogFactory.getLog(FileSIPProcessor.class);
	protected PIDGenerator pidGenerator = null;
	protected TripleStoreQueryService tripleStoreQueryService = null;

	protected AIPImpl prepareIngestDirectory(FileSIP sip, DepositRecord record) throws IngestException {
		File batchPrepDir = null;
		try {
			batchPrepDir = FileUtils.createTempDirectory("ingest-prep");
		} catch (IOException e) {
			throw new IngestException("Unexpected UI error", e);
		}
		File sipDataSubDir = new File(batchPrepDir, "data");
		sipDataSubDir.mkdir();

		if (sip.getData() != null) {
			File relocatedData = new File(sipDataSubDir, sip.getData().getName());
			try {
				FileUtils.renameOrMoveTo(sip.getData(), relocatedData);
				sip.setDiscardFilesOnDestroy(false);
				sip.setData(relocatedData);
			} catch (IOException e1) {
				throw new IngestException("Unexpected IO exception", e1);
			}
		}
		return new AIPImpl(batchPrepDir, record);
	}

	protected void setDataFile(PID pid, FileSIP sip, Document foxml, RDFAwareAIPImpl rdfaip) throws IngestException {
		if (sip.getData() == null)
			return;
		Element locator = null;
		// TYPE="MD5" and DIGEST="aaaaaa" on datastreamVersion
		// verify checksum if one is present in SIP, then set it in FOXML
		log.debug("Setting data file for " + pid + " to " + sip.getData().getAbsolutePath());
		if (sip.getMd5checksum() != null && sip.getMd5checksum().trim().length() > 0) {
			Checksum checker = new Checksum();
			try {
				String sum = checker.getChecksum(sip.getData());
				if (!sum.equals(sip.getMd5checksum().toLowerCase())) {
					String msg = "Checksum failed for data file (SIP specified '" + sip.getMd5checksum()
							+ "', but ingest got '" + sum + "'.)";
					throw new IngestException(msg);
				}
				String msg = "Externally supplied checksum verified for data file.";
				rdfaip.getEventLogger().logEvent(PremisEventLogger.Type.VALIDATION, msg, pid, "DATA_FILE");
			} catch (IOException e) {
				throw new IngestException("Checksum processor failed to find data file.");
			}
			locator = FOXMLJDOMUtil.makeLocatorDatastream("DATA_FILE", "M", sip.getData().getName(), sip.getMimeType(),
					"URL", sip.getFileLabel(), true, sip.getMd5checksum());
		} else {
			locator = FOXMLJDOMUtil.makeLocatorDatastream("DATA_FILE", "M", sip.getData().getName(), sip.getMimeType(),
					"URL", sip.getFileLabel(), true, null);
		}

		// add the data file
		foxml.getRootElement().addContent(locator);
	}
	
	protected void assignFileTriples(PID pid, FileSIP sip, DepositRecord record, Document foxml, String label,
			RDFAwareAIPImpl rdfaip) throws IngestException {
		// set the label
		FOXMLJDOMUtil.setProperty(foxml, FOXMLJDOMUtil.ObjectProperty.label, label);

		// place the object within a container path
		Set<PID> topPIDs = new HashSet<PID>();
		topPIDs.add(pid);
		rdfaip.setTopPIDs(topPIDs);
		rdfaip.setContainerPlacement(sip.getContainerPID(), pid, null, null, label);

		// save FOXML to AIP
		rdfaip.saveFOXMLDocument(pid, foxml);

		// move over pre-ingest events
		if (sip.getPreIngestEventLogger().hasEvents()) {
			for (Element event : sip.getPreIngestEventLogger().getEvents(pid)) {
				rdfaip.getEventLogger().addEvent(pid, event);
			}
		}

		// set owner
		JRDFGraphUtil.addFedoraPIDRelationship(rdfaip.getGraph(), pid, ContentModelHelper.Relationship.owner, record
				.getOwner().getPID());

		// set content model
		JRDFGraphUtil.addFedoraProperty(rdfaip.getGraph(), pid, ContentModelHelper.FedoraProperty.hasModel,
				ContentModelHelper.Model.SIMPLE.getURI());

		// set slug using either default or suggested slug, detecting sibling slug conflicts and incrementing
		String slug = null;
		if (sip.getSuggestedSlug() == null) {
			slug = PathUtil.makeSlug(label);
		} else {
			slug = PathUtil.makeSlug(sip.getSuggestedSlug());
		}
		String containerPath = tripleStoreQueryService.lookupRepositoryPath(sip.getContainerPID());
		while (tripleStoreQueryService.fetchByRepositoryPath(containerPath + "/" + slug) != null) {
			slug = PathUtil.incrementSlug(slug);
		}
		JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.slug, slug);

		// Set object to be active or not depending on if it is in progress.
		if (sip.isInProgress()) {
			FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.state, "Inactive");
		} else {
			FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.state, "Active");
		}
		
		// setup the allowIndexing property
		if (sip.isAllowIndexing()) {
			JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.allowIndexing, "yes");
		} else {
			JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.allowIndexing, "no");
		}

		// set default web data to DATA_FILE datastream pid
		URI dsURI = null;
		try {
			dsURI = new URI(pid.getURI() + "/DATA_FILE");
		} catch (URISyntaxException e) {
			throw new Error("Unexpected exception creating URI for DATA_FILE datastream.", e);
		}

		// set sourceData datastream pointers
		JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.sourceData, dsURI);
		JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.defaultWebData, dsURI);

		// set indexText when appropriate
		if (sip.getMimeType() != null && sip.getMimeType().startsWith("text/")) {
			JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.indexText, dsURI);
		}
	}
	
	public PIDGenerator getPidGenerator() {
		return pidGenerator;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setPidGenerator(PIDGenerator pidGenerator) {
		this.pidGenerator = pidGenerator;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
