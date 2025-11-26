package com.alimertkaya.digitalwallet.controller;

import com.alimertkaya.digitalwallet.dto.CreateWalletRequest;
import com.alimertkaya.digitalwallet.dto.DepositRequest;
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

    @PostMapping("/{walletId}/deposit")
    @ResponseStatus(HttpStatus.ACCEPTED) // 202 Accepted -> talep alindi, arka plan isleniyor. / event-driven mimarisini icin OK 200 yerine
    public Mono<Void> depositToWallet(@PathVariable Long walletId, @Valid @RequestBody DepositRequest request) {
        return walletService.depositToWallet(walletId, request);
    }

    @PostMapping("/{walletId}/transfer")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> transferFunds(@PathVariable Long walletId, @Valid @RequestBody TransferRequest request) {
        return walletService.transferFunds(walletId, request);
    }
    @GetMapping("/{walletId}/transactions")
    public Flux<TransactionHistoryResponse> getWalletTransactionHistory(@PathVariable Long walletId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return walletService.getWalletTransactionHistory(walletId, page, size);
    }
}
