package org.opennms.netmgt.icmp.proxy;

import java.net.InetAddress;
import javax.annotation.PostConstruct;

import org.opennms.core.rpc.api.RpcClient;
import org.opennms.core.rpc.api.RpcClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "locationAwarePingSweepClient")
public class LocationAwarePingSweepClientImpl implements LocationAwarePingSweepClient {

    @Autowired
    private RpcClientFactory rpcClientFactory;

    @Autowired
    private PingSweepRpcModule pingSweepRpcModule;

    private RpcClient<PingSweepRequestDTO, PingSweepResponseDTO> delegate;

    @PostConstruct
    public void init() {
        delegate = rpcClientFactory.getClient(pingSweepRpcModule);
    }

    public RpcClient<PingSweepRequestDTO, PingSweepResponseDTO> getDelegate() {
        return delegate;
    }

    @Override
    public PingSweepRequestBuilder ping(InetAddress start, InetAddress end) {
        return new PingSweepRequestBuilderImpl(delegate).withRange(start, end);
    }

}
