package org.opennms.netmgt.collection.api;

import java.util.Set;

public interface ServiceCollectorRegistry {

    ServiceCollector getCollectorByClassName(String className);

    Set<String> getCollectorClassNames();

}
