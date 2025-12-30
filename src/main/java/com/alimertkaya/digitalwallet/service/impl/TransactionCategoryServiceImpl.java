package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.enums.TransactionCategory;
import com.alimertkaya.digitalwallet.dto.enums.TransactionType;
import com.alimertkaya.digitalwallet.service.TransactionCategoryService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service

public class TransactionCategoryServiceImpl implements TransactionCategoryService {

    private static final Map<String, TransactionCategory> KEYWORD_RULES = new HashMap<>();

    static {
        // Market / Alışveriş
        addRule(TransactionCategory.SHOPPING, "MARKET", "MIGROS", "BIM", "A101", "SOK", "CARREFOUR", "AMAZON", "TRENDYOL");

        // Yeme & İçme
        addRule(TransactionCategory.FOOD_BEVERAGE, "RESTORAN", "CAFE", "STARBUCKS", "BURGER", "PIZZA", "YEMEK");

        // Faturalar
        addRule(TransactionCategory.BILLS, "FATURA", "ELEKTRIK", "SU", "DOGALGAZ", "TELEKOM", "TURKCELL", "VODAFONE");

        // Eğlence & Abonelik
        addRule(TransactionCategory.ENTERTAINMENT, "NETFLIX", "SPOTIFY", "YOUTUBE", "APPLE", "STEAM", "PLAYSTATION");

        // Ulaşım
        addRule(TransactionCategory.TRANSPORTATION, "TAKSI", "UBER", "OTOBUS", "UCAK", "METRO", "BENZIN", "OPET", "SHELL");
    }

    private static void addRule(TransactionCategory category, String... keywords) {
        for (String keyword : keywords) {
            KEYWORD_RULES.put(keyword, category);
        }
    }

    @Override
    public TransactionCategory categorize(TransactionType type, String description) {
        if (type == TransactionType.DEPOSIT) return TransactionCategory.DEPOSIT;

        if (description == null || description.isEmpty()) {
            return TransactionCategory.OTHER;
        }

        String descUpper = description.toUpperCase(Locale.ENGLISH);

        return KEYWORD_RULES.entrySet().stream()
                .filter(entry -> descUpper.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(TransactionCategory.OTHER);
    }
}
