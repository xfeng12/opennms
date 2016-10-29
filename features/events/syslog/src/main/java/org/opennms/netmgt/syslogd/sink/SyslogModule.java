package org.opennms.netmgt.syslogd.sink;

import org.opennms.core.ipc.sink.xml.AbstractXmlSinkModule;
import org.opennms.netmgt.syslogd.SyslogDTO;

public class SyslogModule extends AbstractXmlSinkModule<SyslogDTO> {

    public static final String MODULE_ID = "Syslog";

    public SyslogModule() {
        super(SyslogDTO.class);
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

}
