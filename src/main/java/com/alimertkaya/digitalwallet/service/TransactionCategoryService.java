package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.enums.TransactionCategory;
import com.alimertkaya.digitalwallet.dto.enums.TransactionType;

public interface TransactionCategoryService {
    TransactionCategory categorize(TransactionType type, String description);
}