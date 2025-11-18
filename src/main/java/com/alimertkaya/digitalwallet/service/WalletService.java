package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.CreateWalletRequest;
import com.alimertkaya.digitalwallet.dto.WalletResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WalletService {

    // oturum acan user icin wallet olusturur
    Mono<WalletResponse> createWallet(CreateWalletRequest request);

    // mevcut wallet leri listeler
    Flux<WalletResponse> getCurrentUserWallets();

    // user in id ye gore wallet ini dondurur
    Mono<WalletResponse> getWalletById(Long walletId);
}