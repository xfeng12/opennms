package org.opennms.netmgt.icmp.proxy;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.opennms.core.rpc.xml.AbstractXmlRpcModule;
import org.opennms.netmgt.icmp.EchoPacket;
import org.opennms.netmgt.icmp.PingResponseCallback;
import org.opennms.netmgt.icmp.PingerFactory;
import org.opennms.netmgt.model.discovery.IPAddrRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;

@Component
public class PingSweepRpcModule extends AbstractXmlRpcModule<PingSweepRequestDTO, PingSweepResponseDTO> {

    public static final String RPC_MODULE_ID = "PING-SWEEP";

    @Autowired
    private PingerFactory pingerFactory;

    public PingSweepRpcModule() {
        super(PingSweepRequestDTO.class, PingSweepResponseDTO.class);
    }

    @Override
    public CompletableFuture<PingSweepResponseDTO> execute(PingSweepRequestDTO request) {
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final PingSweepResultTracker tracker = new PingSweepResultTracker();
        IPAddrRange range = new IPAddrRange(request.getBegin(), request.getEnd());
        // Use a RateLimiter to limit the ping packets per second that we send
        RateLimiter limiter = RateLimiter.create(request.getPacketSize());

        return CompletableFuture.supplyAsync(() -> {
            range.forEach(address -> {
                try {
                    tracker.expectCallbackFor(address);
                    limiter.acquire();
                    pingerFactory.getInstance().ping(address, request.getTimeout(), request.getRetries(), request.getPacketSize(), 1,
                            tracker);
                } catch (Exception e) {
                    tracker.completeExceptionally(e);
                }
            });

            try {
                tracker.getLatch().await();
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }

            return tracker.getResponse();

        } , executor);

    }

    private static class PingSweepResultTracker extends CompletableFuture<PingSweepResponseDTO>
            implements PingResponseCallback {

        private final Set<InetAddress> waitingFor = Sets.newConcurrentHashSet();
        private final CountDownLatch m_doneSignal = new CountDownLatch(1);
        private final PingSweepResponseDTO responseDTO = new PingSweepResponseDTO();

        public void expectCallbackFor(InetAddress address) {
            waitingFor.add(address);
        }

        @Override
        public void handleResponse(InetAddress address, EchoPacket response) {
            if (response != null) {
                responseDTO.addPingerResult(address, response.elapsedTime(TimeUnit.MILLISECONDS));
            }
            afterHandled(address);
        }

        @Override
        public void handleTimeout(InetAddress address, EchoPacket request) {
            afterHandled(address);
        }

        @Override
        public void handleError(InetAddress address, EchoPacket request, Throwable t) {
            afterHandled(address);
        }

        private void afterHandled(InetAddress address) {
            waitingFor.remove(address);
            if (waitingFor.isEmpty()) {
                m_doneSignal.countDown();
            }
        }

        public CountDownLatch getLatch() {
            return m_doneSignal;
        }

        public PingSweepResponseDTO getResponse() {
            return responseDTO;
        }
    }

    @Override
    public String getId() {
        return RPC_MODULE_ID;
    }

    public PingerFactory getPingerFactory() {
        return pingerFactory;
    }

    public void setPingerFactory(PingerFactory pingerFactory) {
        this.pingerFactory = pingerFactory;
    }

  
}
