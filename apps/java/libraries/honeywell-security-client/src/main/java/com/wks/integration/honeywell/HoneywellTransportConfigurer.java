package com.wks.integration.honeywell;

import org.apache.cxf.endpoint.Client;

/** Authentication/transport extension point. A future Windows/Negotiate provider belongs here. */
@FunctionalInterface
public interface HoneywellTransportConfigurer {
    void configure(Client client);
}
