package org.opennms.netmgt.syslogd.sink;

import org.opennms.core.ipc.sink.api.MessageProducer;
import org.opennms.core.ipc.sink.api.MessageProducerFactory;
import org.opennms.netmgt.syslogd.SyslogConnection;
import org.opennms.netmgt.syslogd.SyslogConnectionHandler;
import org.opennms.netmgt.syslogd.SyslogDTO;
import org.opennms.netmgt.syslogd.SyslogObjectToDTOProcessor;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class SyslogProducer implements SyslogConnectionHandler {

    private static final MetricRegistry METRICS = new MetricRegistry();

    private final Timer processTimer = METRICS.timer(MetricRegistry.name(getClass(), "send"));

    private final boolean includeRawMessage;
    private final MessageProducer<SyslogDTO> delegate;

    public SyslogProducer(boolean includeRawMessage, MessageProducerFactory messageProducerFactory) {
        this.includeRawMessage = includeRawMessage;
        delegate = messageProducerFactory.getProducer(new SyslogModule());

        final JmxReporter reporter = JmxReporter.forRegistry(METRICS).inDomain("test").build();
        reporter.start();
    }

    @Override
    public void handleSyslogConnection(SyslogConnection message) {
        try (Context ctx = processTimer.time()) {
            delegate.send(SyslogObjectToDTOProcessor.object2dto(message, includeRawMessage));
        }
    }

}
