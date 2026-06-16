package com.adem.attijari_compass.service.card;

import com.adem.attijari_compass.config.CardProviderProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CardDataProviderFactory {

    private final Map<String, CardDataProvider> providers;
    private final CardProviderProperties properties;

    public CardDataProvider getActiveProvider() {
        String providerKey = properties.getProvider() == null
                ? "local"
                : properties.getProvider().trim().toLowerCase(Locale.ROOT);

        CardDataProvider provider = providers.get(providerKey);
        if (provider == null) {
            Set<String> availableProviders = providers.keySet();
            throw new IllegalStateException(
                    "Unsupported card data provider configured: " + providerKey + ". Available providers: " + availableProviders
            );
        }

        return provider;
    }
}
