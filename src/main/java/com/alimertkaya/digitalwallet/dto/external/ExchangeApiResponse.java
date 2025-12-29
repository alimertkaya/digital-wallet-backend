package com.alimertkaya.digitalwallet.dto.external;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ExchangeApiResponse {
    private String result;
    private String base_code;
    private Map<String, BigDecimal> rates;
}