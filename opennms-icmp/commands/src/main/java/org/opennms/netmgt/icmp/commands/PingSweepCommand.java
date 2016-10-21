package org.opennms.netmgt.icmp.commands;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opennms.netmgt.icmp.proxy.LocationAwarePingSweepClient;
import org.opennms.netmgt.icmp.proxy.PingSweepSummary;

@Command(scope = "ping", name = "sweep", description = "Ping-Sweep")
public class PingSweepCommand extends OsgiCommandSupport {

    private LocationAwarePingSweepClient client;

    private InetAddress begin;

    private InetAddress end;

    @Option(name = "-l", aliases = "--location", description = "Location", required = false, multiValued = false)
    String m_location = "localhost";

    @Option(name = "-r", aliases = "--retries", description = "number of retries")
    int m_retries;

    @Option(name = "-t", aliases = "--timeout", description = "timeout in msec")
    int m_timeout;

    @Option(name = "-p", aliases = "--packetsize", description = "packet size")
    int m_packetsize;

    @Argument(index = 0, name = "range", description = "IP Range specified in form startIP-endIP", required = true, multiValued = false)
    String m_range;

    @Override
    protected Object doExecute() throws Exception {
        parseRange(m_range);

        System.out.printf("ping:sweep %s  begin=%s   end=%s \n", m_location != null ? "-l " + m_location : "", begin,
                end);

        final CompletableFuture<PingSweepSummary> future = client.ping(begin, end).withLocation(m_location)
                .withRetries(m_retries).withTimeout(m_timeout, TimeUnit.MILLISECONDS).withPacketSize(m_packetsize)
                .execute();

        while (true) {
            try {
                try {
                    PingSweepSummary summary = future.get(1, TimeUnit.SECONDS);
                    if (summary.getResponses().isEmpty()) {
                        System.out.printf("Not able to ping any IPs in given range %s", m_range);
                    }

                    summary.getResponses().forEach((address, rtt) -> {
                        System.out.printf(" %s : %.3f ms  \n", address, rtt);
                    });
                } catch (InterruptedException e) {
                    System.out.println("\nInterrupted.");
                } catch (ExecutionException e) {
                    System.out.printf("\n Ping Sweep failed with: %s\n", e);
                }
                break;
            } catch (TimeoutException e) {
                // pass
            }
            System.out.print(".");
        }
        return null;
    }

    private void parseRange(String range) {
        if (StringUtils.isNotBlank(range)) {
            String[] rangeSplitter = range.split("-", 2);
            try {
                begin = InetAddress.getByName(rangeSplitter[0]);
                if ((rangeSplitter.length > 1) && StringUtils.isNotBlank(rangeSplitter[1])) {
                    end = InetAddress.getByName(rangeSplitter[1]);
                } else {
                    end = begin;
                }
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Error in parsing the range, Unknown Host");
            }

        }
    }

    public void setClient(LocationAwarePingSweepClient client) {
        this.client = client;
    }

}
