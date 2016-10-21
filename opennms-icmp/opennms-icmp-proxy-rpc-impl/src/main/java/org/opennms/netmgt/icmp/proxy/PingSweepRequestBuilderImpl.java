/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.icmp.proxy;

import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.opennms.core.rpc.api.RpcClient;
import org.opennms.netmgt.icmp.PingConstants;

public class PingSweepRequestBuilderImpl implements PingSweepRequestBuilder {

    protected final RpcClient<PingSweepRequestDTO, PingSweepResponseDTO> client;
    protected long timeout = PingConstants.DEFAULT_TIMEOUT;
    protected int packetSize = PingConstants.DEFAULT_PACKET_SIZE;
    protected int retries = PingConstants.DEFAULT_RETRIES;
    protected String location;
    protected InetAddress begin;
    protected InetAddress end;
    private int numberOfRequests = 1;

    public PingSweepRequestBuilderImpl(RpcClient<PingSweepRequestDTO, PingSweepResponseDTO> client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public PingSweepRequestBuilder withTimeout(long timeout, TimeUnit unit) {
        Objects.requireNonNull(unit);
        this.timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
        return this;
    }

    @Override
    public PingSweepRequestBuilder withPacketSize(int packetSize) {
        this.packetSize = (packetSize > 0 ? packetSize : this.packetSize);
        return this;
    }

    @Override
    public PingSweepRequestBuilder withRetries(int retries) {
        this.retries = (retries > 0 ? retries : this.retries);
        return this;
    }

    @Override
    public PingSweepRequestBuilder withLocation(String location) {
        this.location = Objects.requireNonNull(location);
        return this;
    }

    @Override
    public PingSweepRequestBuilder withNumberOfRequests(int numberOfRequests) {
        this.numberOfRequests = (numberOfRequests > 0 ? numberOfRequests : this.numberOfRequests);
        return this;
    }

    @Override
    public CompletableFuture<PingSweepSummary> execute() {
        final PingSweepRequestDTO request = new PingSweepRequestDTO();
        request.setBegin(begin);
        request.setEnd(end);
        request.setLocation(location);
        request.setRetries(retries);
        request.setPacketSize(packetSize);
        request.setTimeout(timeout);
        return client.execute(request).thenApply(response -> {
            PingSweepSummary summary = new PingSweepSummary();
            summary.addResponse(response.getPingerResult());
            return summary;
        });
    }

    @Override
    public PingSweepRequestBuilder withRange(InetAddress begin, InetAddress end) {
        this.begin = begin;
        this.end = end;
        return this;
    }

    public RpcClient<PingSweepRequestDTO, PingSweepResponseDTO> getClient() {
        return client;
    }

}
