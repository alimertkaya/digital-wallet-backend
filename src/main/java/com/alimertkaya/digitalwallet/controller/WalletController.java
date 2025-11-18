package com.alimertkaya.digitalwallet.controller;

import com.alimertkaya.digitalwallet.dto.CreateWalletRequest;
import com.alimertkaya.digitalwallet.dto.WalletResponse;
import com.alimertkaya.digitalwallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return walletService.createWallet(request);
    }

    @GetMapping
    public Flux<WalletResponse> getCurrentUserWallets() {
        return walletService.getCurrentUserWallets();
    }

    @GetMapping("/{walletId}")
    public Mono<WalletResponse> getWalletById(@PathVariable Long walletId) {
        return walletService.getWalletById(walletId);
    }
}
