package com.alimertkaya.digitalwallet.repository;

import com.alimertkaya.digitalwallet.entity.TransactionHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface TransactionHistoryRepository extends R2dbcRepository<TransactionHistory, Long> {
    // islem gecmisini sayfali aliriz, sort bilgisi pageable nesnesinden gelecek
    Flux<TransactionHistory> findByWalletId(Long walletId, Pageable pageable);
    // birden fazla wallet icin
    Flux<TransactionHistory> findByWalletIdIn(List<Long> walletIds);
    // aylik analiz
    Flux<TransactionHistory> findByWalletIdInAndCreatedAtAfter(List<Long> walletIds, LocalDateTime date);
}