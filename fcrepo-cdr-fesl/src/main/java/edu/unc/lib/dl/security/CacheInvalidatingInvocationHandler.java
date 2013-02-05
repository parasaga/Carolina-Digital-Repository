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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fcrepo.server.messaging.FedoraMethod;
import org.fcrepo.server.proxy.AbstractInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;

public class CacheInvalidatingInvocationHandler extends
		AbstractInvocationHandler {
	Logger log = LoggerFactory
			.getLogger(CacheInvalidatingInvocationHandler.class);
	private final ExecutorService exec = Executors.newCachedThreadPool();

	EmbargoFactory embargoFactory = null;
	GroupRolesFactory groupRolesFactory = null;
	AncestorFactory ancestorFactory = null;

	private void init() {
		this.setAncestorFactory((AncestorFactory) SpringApplicationContext
				.getBean("ancestorFactory"));
		this.setEmbargoFactory((EmbargoFactory) SpringApplicationContext
				.getBean("embargoFactory"));
		this.setGroupRolesFactory((GroupRolesFactory) SpringApplicationContext
				.getBean("groupRolesFactory"));
	}

	public EmbargoFactory getEmbargoFactory() {
		return embargoFactory;
	}

	public void setEmbargoFactory(EmbargoFactory embargoFactory) {
		this.embargoFactory = embargoFactory;
	}

	public GroupRolesFactory getGroupRolesFactory() {
		return groupRolesFactory;
	}

	public void setGroupRolesFactory(GroupRolesFactory groupRolesFactory) {
		this.groupRolesFactory = groupRolesFactory;
	}

	public AncestorFactory getAncestorFactory() {
		return ancestorFactory;
	}

	public void setAncestorFactory(AncestorFactory ancestorFactory) {
		this.ancestorFactory = ancestorFactory;
	}

	/**
	 * Note: Setting of <code>messaging</code> does not take place in this
	 * constructor because the construction of the Management proxy chain (of
	 * which this class is intended to be a part) takes place in
	 * ManagementModule.postInit(), i.e., prior to completion of Server
	 * initialization.
	 */
	public CacheInvalidatingInvocationHandler() {
	}

	/**
	 * {@inheritDoc}
	 */
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		Object returnValue = null;

		try {
			returnValue = method.invoke(target, args);
		} catch (InvocationTargetException ite) {
			throw ite.getTargetException();
		}

		if (!exec.isShutdown()) {
			try {
				exec.submit(new CacheInvalidator(method, args, returnValue)).get();
			} catch(RuntimeException e) {
				log.error("Error while clearing FRACAS caches", e);
			} catch(Exception e) {
				log.error("Error while clearing FRACAS caches", e);				
			}
		}

		return returnValue;
	}

	@Override
	public void close() {
		exec.shutdown();
	}

	class CacheInvalidator implements Callable<Void> {
		private final Method method;
		private final Object[] args;
		private final Object returnValue;

		public CacheInvalidator(Method method, Object[] args, Object returnValue) {
			this.method = method;
			this.args = args;
			this.returnValue = returnValue;
		}

		public Void call() throws Exception {
			if (ancestorFactory == null || embargoFactory == null
					|| groupRolesFactory == null) {
				init(); // load spring beans
			}
			FedoraMethod fm = new FedoraMethod(method, args, returnValue);
			String methodName = method.getName();
			try {
				fm.getPID();
			} catch(NullPointerException e) {
				return null;
			}
			PID pid = new PID(fm.getPID().getObjectId());
			
			if("purgeObject".equals(methodName)) {
				// does not invalidate active embargoes
				getAncestorFactory().invalidateBondsToChildren(pid);
				getGroupRolesFactory().invalidate(pid);
			} else if(methodName.startsWith("modifyDatastream")) {
				String dsId = (String)fm.getParameters()[2];
				if("RELS-EXT".equals(dsId)) {
					getAncestorFactory().invalidateBondsToChildren(pid);
					getAncestorFactory().invalidateBondToParent(pid);
					getGroupRolesFactory().invalidate(pid);
					getEmbargoFactory().invalidate();
				}
			} else if("addRelationship".equals(methodName)) {
				// cdr:contains - new triples not cached
				// cdr:embargo - new triples not cached
				
				String relationship = (String)fm.getParameters()[2];
				if(ContentModelHelper.CDRProperty.inheritPermissions.getURI().toString().equals(relationship)) {
					// cdr:inherit - new settings will impact ancestry
					getAncestorFactory().invalidateBondToParent(pid);
				} else if(ContentModelHelper.Relationship.contains.getURI().toString().equals(relationship)) {
					// adding a contains relation invalidates just the old child-parent bond
					String childPID = (String)fm.getParameters()[3];
					getAncestorFactory().invalidateBondToParent(new PID(childPID)); // this is parent invalidation
				} else if(UserRole.matchesMemberURI(relationship)) {
					// cdr:<role> - adding a role invalidates cached roles
					getGroupRolesFactory().invalidate(pid);
				}
			} else if("purgeRelationship".equals(methodName)) {
				String relationship = (String)fm.getParameters()[2];
				if(ContentModelHelper.CDRProperty.inheritPermissions.getURI().toString().equals(relationship)) {
					// removing inheritance triple invalidates ancestor cache
					getAncestorFactory().invalidateBondToParent(pid);
				} else if(UserRole.matchesMemberURI(relationship)) {
					// removing a cdr:<role> invalidates cached roles
					getGroupRolesFactory().invalidate(pid);
				} else if(ContentModelHelper.Relationship.contains.getURI().toString().equals(relationship)) {
					// removing a contains relation invalidates just that one child-parent bond
					String childPID = (String)fm.getParameters()[3];
					getAncestorFactory().invalidateBondToParent(new PID(childPID)); // this is parent invalidation
				} else if(ContentModelHelper.CDRProperty.embargo.getURI().toString().equals(relationship)) {
					getEmbargoFactory().invalidate();
				}
			}
			return null;
		}
	}
}
