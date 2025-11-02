package com.dzarembo.kucoinservice.updater;

import com.dzarembo.kucoinservice.cache.FundingCache;
import com.dzarembo.kucoinservice.client.KucoinApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KucoinUpdater {
    private final FundingCache cache;
    private final KucoinApiClient apiClient;

    @Scheduled(fixedRate = 1 * 60 * 1000) // обновление каждые 1 минут
    public void updateFundingRates() {
        log.info("Updating Kucoin funding cache...");
        cache.putAll(apiClient.fetchFundingRates());
        log.info("Kucoin funding cache updated: {} entries", cache.getAll().size());
    }
}
