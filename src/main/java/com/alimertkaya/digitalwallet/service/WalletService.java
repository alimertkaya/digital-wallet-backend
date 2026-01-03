package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.wallet.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WalletService {

    // oturum acan user icin wallet olusturur
    Mono<WalletResponse> createWallet(CreateWalletRequest request);

    // mevcut wallet leri listeler
    Flux<WalletResponse> getCurrentUserWallets();

    // user in id ye gore wallet ini dondurur
    Mono<WalletResponse> getWalletById(Long walletId);

    // para yatirma talebi, kafka event i olarak gonderir
    Mono<Void> depositToWallet(Long walletId, DepositRequest request);

    // wallet ler arasi transfer talebi olusturur
    Mono<Void> transferFunds(Long sourceWalletId, TransferRequest request);

    Mono<Void> withdrawFromWallet(Long walletId, WithdrawRequest request);

    Flux<TransactionHistoryResponse> getWalletTransactionHistory(Long walletId, int page, int size);

    Mono<Void> deleteWallet(Long walletId);
}
