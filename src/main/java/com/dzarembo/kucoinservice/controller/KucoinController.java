package com.dzarembo.kucoinservice.controller;

import com.dzarembo.kucoinservice.cache.FundingCache;
import com.dzarembo.kucoinservice.model.FundingRate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/kucoin")
@RequiredArgsConstructor
public class KucoinController {
    private final FundingCache cache;

    @GetMapping("/funding")
    public Collection<FundingRate> getFundingRates() {
        return cache.getAll();
    }
}
