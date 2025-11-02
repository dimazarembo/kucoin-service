package com.dzarembo.kucoinservice.client;

import com.dzarembo.kucoinservice.model.FundingRate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class KucoinApiClient {
    private final WebClient webClient;

    public KucoinApiClient() {
        // KuCoin может возвращать большой JSON → увеличиваем лимит буфера
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        this.webClient = WebClient.builder()
                .baseUrl("https://api-futures.kucoin.com/api/v1")
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * Получает все активные контракты и возвращает FundingRate только для USDT пар.
     */
    public Collection<FundingRate> fetchFundingRates() {
        try {
            KucoinResponse response = webClient.get()
                    .uri("/contracts/active")
                    .retrieve()
                    .bodyToMono(KucoinResponse.class)
                    .block();

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.warn("Empty response from KuCoin /contracts/active");
                return List.of();
            }

            return response.getData().stream()
                    // 1️⃣ оставляем только контракты с USDT
                    .filter(item -> item.getSymbol() != null && item.getSymbol().contains("USDT"))
                    // 2️⃣ убираем контракты без ставки фандинга
                    .filter(item -> item.getFundingFeeRate() != 0.0)
                    // 3️⃣ мапим в FundingRate
                    .map(this::mapToFundingRate)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("Failed to fetch funding rates from KuCoin", e);
            return List.of();
        }
    }

    private FundingRate mapToFundingRate(Item item) {
        try {
            double rate = item.getFundingFeeRate();
            long nextFundingTimeUtc = item.getNextFundingRateDateTime();
            int intervalHours = (int) (item.getFundingRateGranularity() / (1000 * 60 * 60)); // 28800000 → 8h

            String normalizedSymbol = normalizeSymbol(item.getSymbol());

            log.debug("KuCoin: {} rate={} nextFundingTime(UTC)={} interval={}h",
                    normalizedSymbol, rate, Instant.ofEpochMilli(nextFundingTimeUtc), intervalHours);

            return new FundingRate(
                    normalizedSymbol,
                    rate,
                    nextFundingTimeUtc,
                    intervalHours
            );
        } catch (Exception e) {
            log.warn("Failed to parse KuCoin item: {}", item, e);
            return null;
        }
    }

    private String normalizeSymbol(String symbol) {
        // KuCoin возвращает, например, "BTCUSDTM" → убираем финальную "M"
        if (symbol == null) return "";
        String s = symbol;
        if (s.endsWith("M")) s = s.substring(0, s.length() - 1);
        return s;
    }

    // ====================== DTO ======================

    @Data
    public static class KucoinResponse {
        private boolean success;
        private String code;
        private List<Item> data;
    }

    @Data
    public static class Item {
        private String symbol;
        private double fundingFeeRate;
        private Double predictedFundingFeeRate;
        private long nextFundingRateDateTime;
        private long fundingRateGranularity;
        private int period;
        private String rootSymbol;
        private String baseCurrency;
        private String quoteCurrency;
    }
}
