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
package edu.unc.lib.dl.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fcrepo.server.errors.ObjectNotFoundException;
import org.jdom.Content;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.ParentBond;

public class AccessControlUtils {
	private PID collectionsPid;
	private static final Logger LOG = LoggerFactory
			.getLogger(AccessControlUtils.class);
	private edu.unc.lib.dl.util.TripleStoreQueryService tripleStoreQueryService = null;
	private AncestorFactory ancestorFactory = null;
	private GroupRolesFactory groupRolesFactory = null;
	private EmbargoFactory embargoFactory = null;
	private String adminGroup;

	public EmbargoFactory getEmbargoFactory() {
		return embargoFactory;
	}

	public void setEmbargoFactory(EmbargoFactory embargoFactory) {
		this.embargoFactory = embargoFactory;
	}

	public AncestorFactory getAncestorFactory() {
		return ancestorFactory;
	}

	public void setAncestorFactory(AncestorFactory ancestorFactory) {
		this.ancestorFactory = ancestorFactory;
	}

	public GroupRolesFactory getGroupRolesFactory() {
		return groupRolesFactory;
	}

	public void setGroupRolesFactory(GroupRolesFactory groupRolesFactory) {
		this.groupRolesFactory = groupRolesFactory;
	}

	public edu.unc.lib.dl.util.TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			edu.unc.lib.dl.util.TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public String getAdminGroup() {
		return adminGroup;
	}

	public void setAdminGroup(String adminGroup) {
		this.adminGroup = adminGroup;
	}

	public void init() {
		collectionsPid = tripleStoreQueryService
				.fetchByRepositoryPath("/Collections");
		if (collectionsPid == null)
			throw new Error("Cannot find collections pid, failing.");
	}

	public AccessControlUtils() {
	}

	/**
	 * Retrieves list of active embargoes for the specified item, computed from
	 * it and its ancestors.
	 * 
	 * @param pid
	 *            identifier of the object being queried
	 * @return a list of embargo dates for this object
	 */
	public List<String> getEmbargoes(PID pid) {
		try {
			List<PID> ancestors = this.ancestorFactory.getInheritanceList(pid);
			List<String> activeEmbargo = new ArrayList<String>();

			// get embargo dates
			Set<PID> embargoPids = new HashSet<PID>();
			embargoPids.add(pid);
			embargoPids.addAll(ancestors);
			activeEmbargo = getEmbargoFactory().getEmbargoDates(embargoPids);
			return activeEmbargo;
		} catch (ObjectNotFoundException e) {
			LOG.error("Cannot find object in question", e);
		}
		return null;
	}

	/**
	 * Given a PID, return a map containing the group permissions organized by
	 * rights type and active embargoes
	 * 
	 * @param pid
	 * @return
	 */
	public Map<String, Object> getAllCdrAccessControls(PID pid) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("roles", this.getRoles(pid));
		result.put("embargoes", this.getEmbargoes(pid));
		return result;
	}

	/**
	 * Given a PID, return the group permissions organized by rights type in an
	 * XML structure: <permissions><roles><originalsRead>rla</originalsRead>
	 * <originalsRead>abc</originalsRead></roles></permissions>
	 * 
	 * @param inputPid
	 * @return XML representation of the access control for this PID.
	 */
	public Content getAllCdrAccessControlsXML(PID pid) {
		Map<String, Object> stuff = this.getAllCdrAccessControls(pid);

		@SuppressWarnings("unchecked")
		Map<String, Set<String>> summary = (Map<String, Set<String>>) stuff
				.get("roles");
		@SuppressWarnings("unchecked")
		List<String> activeEmbargo = (List<String>) stuff.get("embargoes");

		// serialize JDOM
		Element permsEl = new Element("permissions");
		Element rolesEl = new Element("roles");
		permsEl.addContent(rolesEl);
		for (String role : summary.keySet()) {
			Element roleEl = new Element("role");
			rolesEl.addContent(roleEl);
			roleEl.setAttribute("roleId", role);
			for (String groupName : summary.get(role)) {
				Element groupEl = new Element("group");
				roleEl.addContent(groupEl);
				groupEl.setText(groupName);
			}
		}
		Element embargoesEl = new Element("embargoes");
		permsEl.addContent(embargoesEl);
		for (String date : activeEmbargo) {
			Element embargoEl = new Element("embargo");
			embargoesEl.addContent(embargoEl);
			embargoEl.setText(date);
		}
		return permsEl;
	}

	public List<String> getAllEmbargoes(PID pid) {
		List<String> result = Collections.EMPTY_LIST;
		try {
			Set<PID> embargoPIDs = new HashSet<PID>();
			embargoPIDs.addAll(this.getAncestorFactory()
					.getInheritanceList(pid));
			embargoPIDs.add(pid);
			result = this.getEmbargoFactory().getEmbargoDates(embargoPIDs);
		} catch (ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			LOG.error("Cannot find object to look up ancestors", e);
		}
		return result;
	}

	/**
	 * Retrieves a map representing the roles and groups assigned to those roles
	 * for the specified object, computed from it and its ancestors.
	 * 
	 * @param pid
	 *            identifier of the object being queried
	 * @return
	 */
	public Map<String, Set<String>> getRoles(PID pid) {
		Map<String, Set<String>> summary = new HashMap<String, Set<String>>();
		try {
			Map<String, Set<String>> local = groupRolesFactory
					.getAllRolesAndGroups(pid);
			summary.putAll(local);

			List<PID> ancestors = this.ancestorFactory.getInheritanceList(pid);
			for (PID ancestor : ancestors) {
				Map<String, Set<String>> inherited = groupRolesFactory
						.getAllRolesAndGroups(ancestor);
				if (inherited != null && !inherited.isEmpty()) {
					for (String role : inherited.keySet()) {
						if (summary.containsKey(role)) {
							summary.get(role).addAll(inherited.get(role));
						} else {
							summary.put(role, inherited.get(role));
						}
					}
				}
			}

			// special list inheritance
			// if my bond with parent is non-inheriting,
			// then all groups with roles (inherited or not) on parent have the
			// list role
			ParentBond bond = this.ancestorFactory.getParentBond(pid);
			if (bond != null && !bond.inheritsRoles) {
				Set<String> listGroups = new HashSet<String>();
				Map<String, Set<String>> rolesMap = groupRolesFactory
						.getAllRolesAndGroups(bond.parentPid);
				for (Set<String> rgroups : rolesMap.values()) {
					listGroups.addAll(rgroups);
				}
				List<PID> parentAncestors = this.ancestorFactory
						.getInheritanceList(bond.parentPid);
				for (PID ancestor : parentAncestors) {
					Map<String, Set<String>> inherited = groupRolesFactory
							.getAllRolesAndGroups(ancestor);
					for (Set<String> rgroups : inherited.values()) {
						listGroups.addAll(rgroups);
					}
				}
				if (listGroups.size() > 0) {
					summary.put(UserRole.list.getURI().toString(), listGroups);
				}
			}

			// Add the admin group into the results
			if (this.adminGroup != null) {
				Set<String> adminGroups = summary.get(UserRole.administrator
						.getURI().toString());
				if (adminGroups == null) {
					adminGroups = new HashSet<String>();
				}
				adminGroups.add(this.adminGroup);
			}
		} catch (ObjectNotFoundException e) {
			LOG.error("Cannot find object in question", e);
		}
		return summary;
	}

	public Set<String> getRolesForGroups(Collection<String> groups, PID pid) {
		Set<String> result = new HashSet<String>();
		LOG.debug("getRolesForGroups: " + pid + " " + groups);

		try {
			if(groups.contains(this.getAdminGroup())) {
				result.add(UserRole.administrator.getURI().toString());
			}
			
			Map<String, Set<String>> roles2Groups = groupRolesFactory
					.getAllRolesAndGroups(pid);
			if (roles2Groups != null) {
				for (String role : roles2Groups.keySet()) {
					if (!Collections.disjoint(roles2Groups.get(role), groups))
						result.add(role);
				}
			}

			// special list inheritance logic
			// if my bond with parent is non-inheriting,
			// then all groups with roles (local or inherited) on parent have
			// list
			ParentBond bond = this.ancestorFactory.getParentBond(pid);
			if (bond == null) {
				LOG.debug("got no parent bond, returning");
				return result;
			}
			if (!bond.inheritsRoles) {
				boolean foundList = false;
				Map<String, Set<String>> rolesMap = groupRolesFactory
						.getAllRolesAndGroups(bond.parentPid);
				for (Set<String> rgroups : rolesMap.values()) {
					if (!Collections.disjoint(groups, rgroups)) {
						result.add(UserRole.list.getURI().toString());
						foundList = true;
						break;
					}
				}
				if (!foundList) {
					List<PID> parentAncestors = this.ancestorFactory
							.getInheritanceList(bond.parentPid);
					foo: for (PID ancestor : parentAncestors) {
						Map<String, Set<String>> inheritedRoles = groupRolesFactory
								.getAllRolesAndGroups(ancestor);
						for (Set<String> rgroups : inheritedRoles.values()) {
							if (!Collections.disjoint(groups, rgroups)) {
								result.add(UserRole.list.getURI().toString());
								break foo;
							}
						}
					}
				}
			} else {
				// list of ancestors from which this pid inherits access
				// controls
				List<PID> ancestors = this.ancestorFactory
						.getInheritanceList(pid);
				for (PID ancestor : ancestors) {
					Map<String, Set<String>> aroles2Groups = groupRolesFactory
							.getAllRolesAndGroups(ancestor);
					if (aroles2Groups != null) {
						for (String role : aroles2Groups.keySet()) {
							if (!Collections.disjoint(aroles2Groups.get(role),
									groups))
								result.add(role);
						}
					}
				}
			}
		} catch (ObjectNotFoundException e) {
			LOG.error("Cannot find object in question", e);
		}
		LOG.debug("found roles: " + result);
		return result;
	}

	/**
	 * Retrieves the set of groups in the requested role, including the
	 * parent-implied list role.
	 */
	public Set<String> getGroupsInRole(PID pid, String role) {
		Set<String> groups = new HashSet<String>();
		LOG.debug("getGroupsInRole: " + pid + " " + role);

		try {
			Set<String> local = groupRolesFactory.getGroupsInRole(pid, role);
			if (local != null) {
				groups.addAll(local);
			}

			if (UserRole.list.getURI().equals(role)) {
				LOG.debug("looking for list role");
				// special list inheritance logic
				// if my bond with parent is non-inheriting,
				// then all groups with roles on parent have list
				ParentBond bond = this.ancestorFactory.getParentBond(pid);
				if (!bond.inheritsRoles) {
					Map<String, Set<String>> rolesMap = groupRolesFactory
							.getAllRolesAndGroups(bond.parentPid);
					for (Set<String> rgroups : rolesMap.values()) {
						groups.addAll(rgroups);
					}
				}
			} else {
				// list of ancestors from which this pid inherits access
				// controls
				List<PID> ancestors = this.ancestorFactory
						.getInheritanceList(pid);
				for (PID ancestor : ancestors) {
					LOG.debug("checking for roles on ancestor: " + ancestor);
					Set<String> additions = groupRolesFactory.getGroupsInRole(
							ancestor, role);
					if (additions != null) {
						groups.addAll(additions);
					}
				}
			}
		} catch (ObjectNotFoundException e) {
			LOG.error("Cannot find object in question", e);
		}
		LOG.debug("getGroupsInRole: found the following groups in role " + role
				+ " on pid " + pid);
		if (LOG.isDebugEnabled()) {
			StringBuilder b = new StringBuilder();
			for (String s : groups) {
				b.append(s).append("\t");
			}
			LOG.debug(b.toString());
		}
		return groups;
	}

	public List<String> getDatastreamCategories(String datastreamId) {
		List<String> result = new ArrayList<String>();
		for(Datastream ds : ContentModelHelper.Datastream.values()) {
			if(ds.getName().equals(datastreamId)) {
				result.add(ds.getCategory().name());
				break;
			}
		}
		return result;
	}
}