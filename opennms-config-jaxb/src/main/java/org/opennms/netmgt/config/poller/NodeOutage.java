/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
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

/*
 * This class was converted to JAXB from Castor.
 */

package org.opennms.netmgt.config.poller;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Configuration of node-outage
 *  functionality
 */

@XmlRootElement(name="node-outage")
@XmlAccessorType(XmlAccessType.NONE)
public class NodeOutage implements Serializable {
    private static final long serialVersionUID = 1005268629840127148L;

    /**
     * Enable/disable node outage processing
     */
    @XmlAttribute(name="status")
    private String m_status;

    /**
     * Defines behavior of node outage processing when a service has changed
     * status to DOWN and a critical service is not defined. If "true", all
     * remaining services on the interface are polled.
     */
    @XmlAttribute(name="pollAllIfNoCriticalServiceDefined")
    private String m_pollAllIfNoCriticalServiceDefined = "true";

    /**
     * Critical service. Defining a critical service greatly reduces the
     * traffic generated by the poller when an interface is DOWN. When an
     * interface is DOWN only the critical service is polled. If and when the
     * critical service comes back UP then the interface's other services are
     * polled to determine their status. When an interface is UP all services
     * are polled as expected. If the critical service goes DOWN, all services
     * are considered to be DOWN and therefore the interface is also
     * considered DOWN.
     */
    @XmlElement(name="critical-service")
    private CriticalService m_criticalService;

    public NodeOutage() {
        super();
    }

    /**
     * Enable/disable node outage processing
     */
    public String getStatus() {
        return m_status;
    }

    public void setStatus(final String status) {
        m_status = status;
    }

    /**
     * Defines behavior of node outage processing when a service has changed
     * status to DOWN and a critical service is not defined. If "true", all
     * remaining services on the interface are polled.
     */
    public String getPollAllIfNoCriticalServiceDefined() {
        return m_pollAllIfNoCriticalServiceDefined == null? "true" : m_pollAllIfNoCriticalServiceDefined;
    }

    public void setPollAllIfNoCriticalServiceDefined(final String pollAllIfNoCriticalServiceDefined) {
        m_pollAllIfNoCriticalServiceDefined = pollAllIfNoCriticalServiceDefined;
    }

    /**
     * Critical service. Defining a critical service greatly reduces the
     * traffic generated by the poller when an interface is DOWN. When an
     * interface is DOWN only the critical service is polled. If and when the
     * critical service comes back UP then the interface's other services are
     * polled to determine their status. When an interface is UP all services
     * are polled as expected. If the critical service goes DOWN, all services
     * are considered to be DOWN and therefore the interface is also
     * considered DOWN.
     */
    public CriticalService getCriticalService() {
        return m_criticalService;
    }

    public void setCriticalService(final CriticalService criticalService) {
        m_criticalService = criticalService;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_criticalService == null) ? 0 : m_criticalService.hashCode());
        result = prime * result + ((m_pollAllIfNoCriticalServiceDefined == null) ? 0 : m_pollAllIfNoCriticalServiceDefined.hashCode());
        result = prime * result + ((m_status == null) ? 0 : m_status.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NodeOutage)) {
            return false;
        }
        final NodeOutage other = (NodeOutage) obj;
        if (m_criticalService == null) {
            if (other.m_criticalService != null) {
                return false;
            }
        } else if (!m_criticalService.equals(other.m_criticalService)) {
            return false;
        }
        if (m_pollAllIfNoCriticalServiceDefined == null) {
            if (other.m_pollAllIfNoCriticalServiceDefined != null) {
                return false;
            }
        } else if (!m_pollAllIfNoCriticalServiceDefined.equals(other.m_pollAllIfNoCriticalServiceDefined)) {
            return false;
        }
        if (m_status == null) {
            if (other.m_status != null) {
                return false;
            }
        } else if (!m_status.equals(other.m_status)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NodeOutage[status=" + m_status + ",pollAllIfNoCriticalServiceDefined=" + m_pollAllIfNoCriticalServiceDefined + ",criticalService=" + m_criticalService + "]";
    }
}