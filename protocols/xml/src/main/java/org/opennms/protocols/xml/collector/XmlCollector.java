/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2014 The OpenNMS Group, Inc.
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

package org.opennms.protocols.xml.collector;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opennms.core.spring.BeanUtils;
import org.opennms.core.utils.ParameterMap;
import org.opennms.netmgt.collection.api.AbstractRemoteServiceCollector;
import org.opennms.netmgt.collection.api.CollectionAgent;
import org.opennms.netmgt.collection.api.CollectionException;
import org.opennms.netmgt.collection.api.CollectionInitializationException;
import org.opennms.netmgt.collection.api.CollectionSet;
import org.opennms.netmgt.rrd.RrdRepository;
import org.opennms.protocols.xml.config.XmlDataCollection;
import org.opennms.protocols.xml.config.XmlDataCollectionConfig;
import org.opennms.protocols.xml.dao.XmlDataCollectionConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * The Class XmlCollector.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class XmlCollector extends AbstractRemoteServiceCollector {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(XmlCollector.class);

    private static final String XML_DATACOLLECTION_KEY = "xmlDatacollection";

    private static final String RRD_REPOSITORY_PATH_KEY = "rddRepositoryPath";

    private static final Map<String, Class<?>> TYPE_MAP = Collections.unmodifiableMap(Stream.of(
            new SimpleEntry<>(XML_DATACOLLECTION_KEY, XmlDataCollection.class))
            .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

    /** The XML Data Collection DAO. */
    private XmlDataCollectionConfigDao m_xmlCollectionDao;

    private static final class XmlCollectionHandlerKey {
        private final String serviceName;
        private final String handlerClass;

        public XmlCollectionHandlerKey(String serviceName, String handlerClass) {
            this.serviceName = Objects.requireNonNull(serviceName);
            this.handlerClass = Objects.requireNonNull(handlerClass);
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getHandlerClass() {
            return handlerClass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceName, handlerClass);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof XmlCollectionHandlerKey)) {
                return false;
            }
            XmlCollectionHandlerKey other = (XmlCollectionHandlerKey) obj;
            return Objects.equals(this.serviceName, other.serviceName)
                    && Objects.equals(this.handlerClass, other.handlerClass);
        }
    }

    private final LoadingCache<XmlCollectionHandlerKey, XmlCollectionHandler> m_handlers = CacheBuilder.newBuilder()
            .build(
                new CacheLoader<XmlCollectionHandlerKey, XmlCollectionHandler>() {
                  public XmlCollectionHandler load(XmlCollectionHandlerKey key) throws Exception {
                      try {
                          LOG.debug("initialize: instantiating XML collection handler {}", key.getHandlerClass());
                          final Class<?> clazz = Class.forName(key.getHandlerClass());
                          final XmlCollectionHandler handler = (XmlCollectionHandler) clazz.newInstance();
                          handler.setServiceName(key.getServiceName());
                          return handler;
                      } catch (Exception e) {
                          throw new CollectionException("Unable to instantiate XML Collection Handler " + key.getHandlerClass() + " because: " + e.getMessage(), e);
                      }
                  }
                });

    public XmlCollector() {
        super(TYPE_MAP);
    }

    /**
     * Gets the XML Data Collection DAO.
     *
     * @return the XML Data Collection DAO
     */
    public XmlDataCollectionConfigDao getXmlCollectionDao() {
        return m_xmlCollectionDao;
    }

    /**
     * Sets the XML Data Collection DAO.
     *
     * @param xmlCollectionDao the new XML Data Collection DAO
     */
    public void setXmlCollectionDao(XmlDataCollectionConfigDao xmlCollectionDao) {
        m_xmlCollectionDao = xmlCollectionDao;
    }

    /**
     * Log.
     *
     * @return the thread category
     */

    /* (non-Javadoc)
     * @see org.opennms.netmgt.collectd.ServiceCollector#initialize(java.util.Map)
     */
    @Override
    public void initialize() throws CollectionInitializationException {
        LOG.debug("initialize: initializing XML collector");

        try {
            // This is not strictly required but added just in case (see NMS-7963)
            Charset.forName("UTF-8");
        } catch (UnsupportedCharsetException e) {
            throw new CollectionInitializationException("Can't initialize charset UTF-8: " + e.getMessage());
        }

        // Retrieve the DAO for our configuration file.
        if (m_xmlCollectionDao == null) {
            m_xmlCollectionDao = BeanUtils.getBean("daoContext", "xmlDataCollectionConfigDao", XmlDataCollectionConfigDao.class);
        }
    }

    @Override
    public Map<String, Object> getRuntimeAttributes(CollectionAgent agent, Map<String, Object> parameters) {
        final Map<String, Object> runtimeAttributes = new HashMap<>();

        // Getting XML Collection
        String collectionName = ParameterMap.getKeyedString(parameters, "collection", ParameterMap.getKeyedString(parameters, "xml-collection", null));
        if (collectionName == null) {
            throw new IllegalArgumentException("Parameter collection is required for the XML Collector!");
        }
        LOG.debug("collect: collecting XML data using collection {} for {}", collectionName, agent);
        XmlDataCollection collection = m_xmlCollectionDao.getDataCollectionByName(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("XML Collection " + collectionName +" does not exist.");
        }
        runtimeAttributes.put(XML_DATACOLLECTION_KEY, collection);
        runtimeAttributes.put(RRD_REPOSITORY_PATH_KEY, m_xmlCollectionDao.getConfig().getRrdRepository());

        return runtimeAttributes;
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.collectd.ServiceCollector#collect(org.opennms.netmgt.collectd.CollectionAgent, org.opennms.netmgt.model.events.EventProxy, java.util.Map)
     */
    @Override
    public CollectionSet collect(CollectionAgent agent, Map<String, Object> parameters) throws CollectionException {
        if (parameters == null) {
            throw new CollectionException("Null parameters is now allowed in XML Collector!");
        }

        LOG.debug("initialize: initializing XML collection handling using {} for collection agent {}", parameters, agent);
        String serviceName = ParameterMap.getKeyedString(parameters, "SERVICE", "XML");
        String handlerClass = ParameterMap.getKeyedString(parameters, "handler-class", "org.opennms.protocols.xml.collector.DefaultXmlCollectionHandler");

        try {
            final String rrdRepositoryPath = ParameterMap.getKeyedString(parameters, RRD_REPOSITORY_PATH_KEY, null);
            XmlDataCollection collection = (XmlDataCollection) parameters.get(XML_DATACOLLECTION_KEY);
            RrdRepository rrdRepository = XmlDataCollectionConfig.buildRrdRepository(rrdRepositoryPath, collection);

            // Filling XML CollectionSet
            XmlCollectionHandlerKey key = new XmlCollectionHandlerKey(serviceName, handlerClass);
            XmlCollectionHandler handler = m_handlers.get(key);
            handler.setRrdRepository(rrdRepository);
            return handler.collect(agent, collection, parameters);
        } catch (Exception e) {
            throw new CollectionException("Can't collect XML data because " + e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.collectd.ServiceCollector#getRrdRepository(java.lang.String)
     */
    @Override
    public RrdRepository getRrdRepository(String collectionName) {
        return m_xmlCollectionDao.getConfig().buildRrdRepository(collectionName);
    }

}
