package org.opennms.netmgt.syslogd.sink;

import org.opennms.core.ipc.sink.api.MessageConsumer;
import org.opennms.core.ipc.sink.api.MessageConsumerManager;
import org.opennms.core.ipc.sink.api.SinkModule;
import org.opennms.netmgt.config.SyslogdConfig;
import org.opennms.netmgt.syslogd.SyslogConnection;
import org.opennms.netmgt.syslogd.SyslogDTO;
import org.opennms.netmgt.syslogd.SyslogDTOToObjectProcessor;
import org.opennms.netmgt.syslogd.SyslogProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class SyslogConsumer implements MessageConsumer<SyslogDTO>, InitializingBean {

    private static final SyslogModule syslogModule = new SyslogModule();

    private final Timer handleTimer;
    private final Timer toEventTimer;
    private final Timer broadcastTimer;

    @Autowired
    private MessageConsumerManager messageConsumerManager;

    @Autowired
    private SyslogdConfig syslogdConfig;

    public SyslogConsumer(MetricRegistry registry) {
        handleTimer = registry.timer("handle");
        toEventTimer = registry.timer("handle.toevent");
        broadcastTimer = registry.timer("handle.broadcast");
    }

    @Override
    public SinkModule<SyslogDTO> getModule() {
        return syslogModule;
    }

    @Override
    public void handleMessage(SyslogDTO message) {
        try (Context handleCtx = handleTimer.time()) {
            final SyslogConnection connection = SyslogDTOToObjectProcessor.dto2object(message);
            connection.setConfig(syslogdConfig);

            SyslogProcessor processor = null;
            try (Context toEventCtx = toEventTimer.time()) {
                processor = connection.call();
            }

            if (processor != null) {
                try (Context broadCastCtx = broadcastTimer.time()) {
                    processor.call();
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Automatically register the consumer on initialization
        messageConsumerManager.registerConsumer(this);
    }

}
