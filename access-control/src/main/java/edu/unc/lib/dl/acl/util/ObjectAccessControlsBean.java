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
package edu.unc.lib.dl.acl.util;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.dl.fedora.PID;

/**
 * Encapsulates the complete set of access controls that apply to a particular object.
 * @author count0
 *
 */
public class ObjectAccessControlsBean {
	PID object = null;
	Map<UserRole, Set<String>> role2groups = null;
	List<Date> activeEmbargoes = null;
	static DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Object Access Controls (").append(object.getPid()).append(")")
		.append("\nRoles granted to groups:");
		for(UserRole r : role2groups.keySet()) {
			result.append(r.getPredicate()).append("\n");
			for(String g : role2groups.get(r)) {
				result.append(g).append("\t");
			}
		}
		result.append("\nActive embargo dates:");
		for(Date d : activeEmbargoes) {
			result.append(format.format(d));
		}
		return result.toString();
	}
	
	public PID getObject() {
		return object;
	}
	
	/**
	 * Builds a set of all the user roles granted to the given groups.
	 * @param groups
	 * @return
	 */
	public Set<UserRole> getRoles(String [] groups) {
		Set<UserRole> result = new HashSet<UserRole>();
		for(String group : groups) { // get all user roles
			for(UserRole r : role2groups.keySet()) {
				if(role2groups.get(r).contains(group)) {
					result.add(r);
					break;
				}
			}
		}
		return result;
	}
	
	/**
	 * Determines a user permission, given a set of groups.
	 * @param groups user memberships
	 * @param permission the permission requested
	 * @return true if permitted
	 */
	public boolean hasPermission(String[] groups, Permission permission) {
		boolean result = false;
		Set<UserRole> roles = this.getRoles(groups);
		for(UserRole r : roles) {
			if(r.getPermissions().contains(permission)) return true;
		}
		return result;
	}
}
