/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.collection.support;

import java.util.List;
import javax.management.ObjectName;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.jexl2.ReadonlyContext;
import org.apache.commons.jexl2.UnifiedJEXL;
import org.opennms.netmgt.collection.api.CollectionResource;
import org.opennms.netmgt.config.datacollection.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectNameStorageStrategy extends IndexStorageStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectNameStorageStrategy.class);
    private static final int DEFAULT_JEXLENGINE_CACHESIZE = 512;
    private static final String QUOTE = "\"";

    private static final String PARAM_INDEX_FORMAT = "index-format";
    private static final String PARAM_CLEAN_OUTPUT = "clean-output";
    private String m_indexFormat;
    private Boolean m_cleanOutput = false;

    private static final JexlEngine JEXL_ENGINE;
    private static final UnifiedJEXL EL;

    static {
        final int cacheSize = Integer.getInteger("org.opennms.netmgt.collection.support.ObjectNameStorageStrategy.cacheSize", DEFAULT_JEXLENGINE_CACHESIZE);
        JEXL_ENGINE = new JexlEngine();
        JEXL_ENGINE.setCache(cacheSize);
        JEXL_ENGINE.setLenient(false);
        JEXL_ENGINE.setStrict(true);

        EL = new UnifiedJEXL(JEXL_ENGINE);
    }

    public ObjectNameStorageStrategy() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public String getResourceNameFromIndex(CollectionResource resource) {
        String resourceName = null;
        try {
            ObjectName oname = new ObjectName(resource.getInstance());
            UnifiedJEXL.Expression expr = EL.parse( m_indexFormat );

            JexlContext context = new MapContext();
            context.set("ObjectName", oname);
            context.set("domain", oname.getDomain() == null ? "" : oname.getDomain());
            oname.getKeyPropertyList().entrySet().forEach((entry) -> {
                final String value = entry.getValue();
                if (value.startsWith(QUOTE) && value.endsWith(QUOTE)) {
                    context.set(entry.getKey(), ObjectName.unquote(entry.getValue()));
                } else {
                    context.set(entry.getKey(), entry.getValue());
                }
            });
            resourceName = (String) expr.evaluate(new ReadonlyContext(context));
        } catch (JexlException e) {
            LOG.error("getResourceNameFromIndex(): error evaluating index-format [{}] as a Jexl Expression", m_indexFormat, e);
        } catch (javax.management.MalformedObjectNameException e) {
            LOG.debug("getResourceNameFromIndex(): malformed object name: {}", resource.getInstance(), e);
        } finally {
            if (resourceName == null) {
                resourceName = resource.getInstance();
                resourceName = resourceName.replaceAll("\\s+", "_").replaceAll(":", "_").replaceAll("\\\\", "_").replaceAll("[\\[\\]]", "_").replaceAll("[|/]", "_").replaceAll("=", "").replaceAll("[_]+$", "").replaceAll("___", "_");
            }
        }
        if (m_cleanOutput && resourceName != null) {
            resourceName = resourceName.replaceAll("\\s+", "_").replaceAll(":", "_").replaceAll("\\\\", "_").replaceAll("[\\[\\]]", "_").replaceAll("[|/]", "_").replaceAll("=", "").replaceAll("[_]+$", "").replaceAll("___", "_");
        }

        LOG.debug("getResourceNameFromIndex(): {}", resourceName);
        return resourceName;
    }

    /** {@inheritDoc} */
    @Override
    public void setParameters(List<Parameter> parameterCollection) throws IllegalArgumentException {
        if (parameterCollection == null) {
            final String msg ="Got a null parameter list, but need one containing a '" + PARAM_INDEX_FORMAT + "' parameter.";
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }
        parameterCollection.forEach((param) -> {
            if (null == param.getKey()) {
                LOG.warn("Encountered unsupported parameter key=\"{}\". Can accept: {}, {}", param.getKey(), PARAM_INDEX_FORMAT, PARAM_CLEAN_OUTPUT);
            } else switch (param.getKey()) {
                case PARAM_INDEX_FORMAT:
                    m_indexFormat = param.getValue().trim();
                    break;
                case PARAM_CLEAN_OUTPUT:
                    m_cleanOutput = Boolean.parseBoolean(param.getValue().trim());
                    break;
                default:
                    LOG.warn("Encountered unsupported parameter key=\"{}\". Can accept: {}, {}", param.getKey(), PARAM_INDEX_FORMAT, PARAM_CLEAN_OUTPUT);
                    break;
            }
        });
        if (m_indexFormat == null) {
            throw new IllegalArgumentException("Missing index-format expression");
        }
    }
}
