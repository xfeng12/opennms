/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.topology.app.internal.support;

import org.opennms.netmgt.dao.api.IpInterfaceDao;

/**
 * This class supports creation of <IpLikeHopCriteria>.
 * 
 * @author <a href=mailto:thedesloge@opennms.org>Donald Desloge</a>
 * @author <a href=mailto:seth@opennms.org>Seth Leger</a>
 * @author <a href=mailto:david@opennms.org>David Hustace</a>
 *
 */
public class IpLikeHopCriteriaFactory {

	private final IpInterfaceDao m_ipInterfaceDao;

	public IpLikeHopCriteriaFactory(IpInterfaceDao dao) {
		m_ipInterfaceDao = dao;
	}
	
	/**
	 * The ipQuery value is a string representing an IP address or a valid IPLIKE query string
	 * passed from the UI as a user selection of a <SearchResult>.
	 * 
	 * FIXME these should be static operations.
	 * 
	 * @param ipQuery
	 * @return
	 */
	public IpLikeHopCriteria createCriteria(String ipQuery) {
		IpLikeHopCriteria retval = new IpLikeHopCriteria(ipQuery, m_ipInterfaceDao);
		return retval;
	}
}
