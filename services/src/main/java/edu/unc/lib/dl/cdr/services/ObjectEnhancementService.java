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
package edu.unc.lib.dl.cdr.services;

import java.util.List;

import org.jdom.Element;

import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementApplication;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author Gregory Jansen
 * 
 */
public interface ObjectEnhancementService {
	/**
	 * Returns a list of candidate objects to which this service may apply. This method is allowed to return some false
	 * positives. However, if the maxResults is equal or greater than the number of objects in the repository, then it
	 * must include all applicable objects.
	 * 
	 * @return
	 */
	public List<PID> findCandidateObjects(int maxResults) throws EnhancementException;

	/**
	 * Returns the total number of objects that would be returned by findCandidateObjects.
	 * 
	 * @return
	 * @throws EnhancementException
	 */
	public int countCandidateObjects() throws EnhancementException;

	/**
	 * Returns a list of candidate objects to which this service may apply to, including objects that it has applied to
	 * in the past but which are now stale.
	 * 
	 * @param maxResults
	 * @param priorToDate
	 * @return
	 * @throws EnhancementException
	 */
	public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) throws EnhancementException;

	/**
	 * Creates a task for running this service on the object in question.
	 * 
	 * @param pid
	 * @return an EnhancementTask
	 */
	public Enhancement<Element> getEnhancement(EnhancementMessage pid) throws EnhancementException;

	/**
	 * Does this service apply to this object?
	 * 
	 * @param pid
	 * @return true if the service is applicable
	 */
	public boolean isApplicable(EnhancementMessage pid) throws EnhancementException;

	/**
	 * Does this message apply to this service?
	 * 
	 * @param pid
	 * @return
	 * @throws EnhancementException
	 */
	public boolean prefilterMessage(EnhancementMessage pid) throws EnhancementException;

	/**
	 * Checks to see if the enhancement should be re-applied. Generally a comparison of timestamps or software agent
	 * strings. Returns true if this enhancement has never been run or will provide greater enhancement by running again.
	 * 
	 * @param pid
	 * @return true if the object is stale w/respect to this enhancement
	 */
	public boolean isStale(PID pid) throws EnhancementException;

	/**
	 * Determines the last date on which this service was applied to the object represented by pid.
	 * 
	 * @param pid
	 * @return the most recent date this service was applied to object pid, or null if it has never been applied.
	 * @throws EnhancementException
	 */
	public EnhancementApplication getLastApplied(PID pid) throws EnhancementException;

	/**
	 * @return true if this service is currently active
	 */
	public boolean isActive() throws EnhancementException;

	/**
	 * Returns the name of this service
	 * 
	 * @return
	 */
	public String getName();
}
