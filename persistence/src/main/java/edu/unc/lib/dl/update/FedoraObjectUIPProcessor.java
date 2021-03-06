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
package edu.unc.lib.dl.update;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class FedoraObjectUIPProcessor implements UIPProcessor {
	private static Logger log = Logger.getLogger(FedoraObjectUIPProcessor.class);

	private DigitalObjectManager digitalObjectManager;
	private UIPUpdatePipeline pipeline;
	private AccessClient accessClient;
	
	@Override
	public void process(UpdateInformationPackage uip) throws UpdateException, UIPException {
		if (!(uip instanceof FedoraObjectUIP)){
			throw new UIPException("Incorrect UIP class, found " + uip.getClass().getName() + ", expected " + FedoraObjectUIP.class.getName());
		}
		log.debug("Preparing to process Fedora Object UIP for operation " + uip.getOperation() + " on " + uip.getPID().getPid());
		
		FedoraObjectUIP fuip = (FedoraObjectUIP)uip;
		
		fuip.storeOriginalDatastreams(accessClient);
		
		uip = pipeline.processUIP(uip);
		Map<String,File> modifiedFiles = uip.getModifiedFiles();
		if (modifiedFiles != null){
			for (Entry<String,File> modifiedFile: modifiedFiles.entrySet()){
				Datastream datastream = Datastream.getDatastream(modifiedFile.getKey());
				if (datastream != null && modifiedFile.getValue() != null){
					log.debug("Adding/replacing datastream " + datastream.getName() + " on " + uip.getPID().getPid());
					digitalObjectManager.addOrReplaceDatastream(uip.getPID(), datastream, modifiedFile.getValue(),
							uip.getMimetype(modifiedFile.getKey()), (Agent) uip.getUser(), uip.getMessage());
				}
			}
		}
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public UIPUpdatePipeline getPipeline() {
		return pipeline;
	}

	public void setPipeline(UIPUpdatePipeline pipeline) {
		this.pipeline = pipeline;
	}

	public AccessClient getAccessClient() {
		return accessClient;
	}

	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}
}
