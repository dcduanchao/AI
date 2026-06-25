package com.dc.ai.provider;

import com.dc.ai.domain.AdapterType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderClientRouter {

    private final Map<AdapterType, AiProviderClient> clients;

    public ProviderClientRouter(List<AiProviderClient> clientList) {
        this.clients = new EnumMap<>(AdapterType.class);
        for (AiProviderClient client : clientList) {
            clients.put(client.adapterType(), client);
        }
    }

    public AiProviderClient get(AdapterType adapterType) {
        AiProviderClient client = clients.get(adapterType);
        if (client == null) {
            throw new IllegalArgumentException("Unsupported adapter type: " + adapterType);
        }
        return client;
    }
}
