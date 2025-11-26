package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.*;
import com.alimertkaya.digitalwallet.dto.enums.TransactionType;
import com.alimertkaya.digitalwallet.entity.User;
import com.alimertkaya.digitalwallet.entity.Wallet;
import com.alimertkaya.digitalwallet.repository.TransactionHistoryRepository;
import com.alimertkaya.digitalwallet.repository.WalletRepository;
import com.alimertkaya.digitalwallet.service.KafkaProducerService;
import com.alimertkaya.digitalwallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final KafkaProducerService kafkaProducerService;
    private final TransactionHistoryRepository transactionHistoryRepository;

    // mevcut user u alir
    public Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof User) {
                        return Mono.just((User) principal);
                    }
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Kullanıcı oturumu bulunamadı."));
                });
    }

    @Override
    public Mono<WalletResponse> createWallet(CreateWalletRequest request) {
        return getCurrentUser().flatMap(user -> {
            Wallet newWallet = Wallet.builder()
                    .userId(user.getId())
                    .name(request.getName())
                    .currencyCode(request.getCurrencyCode().toUpperCase())
                    .balance(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return walletRepository.save(newWallet)
                    .map(WalletResponse::fromEntity);
        });
    }

    @Override
    public Flux<WalletResponse> getCurrentUserWallets() {
        return getCurrentUser()
                .flatMapMany(user -> walletRepository.findByUserId(user.getId()))
                .map(WalletResponse::fromEntity);
    }

    @Override
    public Mono<WalletResponse> getWalletById(Long walletId) {
        return getCurrentUser()
                .flatMap(user -> walletRepository.findByIdAndUserId(walletId, user.getId()))
                .map(WalletResponse::fromEntity)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cüzdan bulunamadı veya bu cüzdana erişim yetkiniz yok.")));
    }

    @Override
    public Mono<Void> depositToWallet(Long walletId, DepositRequest request) {
        return getCurrentUser()
                .flatMap(user ->
                        // user un bu wallet e sahip oldugunu dogrulama
                        walletRepository.findByIdAndUserId(walletId, user.getId())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "Cüzdan bulunamadı veya bu cüzdana erişim yetkiniz yok.")))
                )
                .flatMap(wallet -> {
                    TransactionEvent event = TransactionEvent.builder()
                            .type(TransactionType.DEPOSIT)
                            .sourceWalletId(wallet.getId()) // para yatirilan wallet
                            .targetWalletId(null) // para yatirma isleminde hedef cuzdan yoktur
                            .amount(request.getAmount())
                            .currencyCode(wallet.getCurrencyCode())
                            .build();

                    log.info("Para yatırma talebi Kafka'ya gönderiliyor. Cüzdan ID: {}, Tutar: {}", wallet.getId(), request.getAmount());
                    return kafkaProducerService.sendTransactionEvent(event);
                });
    }

    @Override
    public Mono<Void> transferFunds(Long sourceWalletId, TransferRequest request) {
        return getCurrentUser()
                .flatMap(user -> walletRepository.findByIdAndUserId(sourceWalletId, user.getId())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Cüzdan bulunamadı veya bu cüzdana erişim yetkiniz yok.")))
                )
                .flatMap(sourceWallet -> {
                    // bakiye kontrol
                    if (sourceWallet.getBalance().compareTo(request.getAmount()) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,"Yetersiz bakiye! Mevcut: " + sourceWallet.getBalance()));
                    }

                    if (sourceWallet.getId().equals(request.getTargetWalletId())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,"Kendinize transfer yapamazsınız."));
                    }

                    return walletRepository.findById(request.getTargetWalletId())
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,"Hedef cüzdan bulunamadı.")))
                            .flatMap(targetWalllet -> {
                                TransactionEvent event = TransactionEvent.builder()
                                        .type(TransactionType.TRANSFER)
                                        .sourceWalletId(sourceWallet.getId())
                                        .targetWalletId(targetWalllet.getId())
                                        .amount(request.getAmount())
                                        .currencyCode(sourceWallet.getCurrencyCode())
                                        .build();

                                log.info("Transfer talebi Kafka'ya gönderiliyor. {} -> {}, Tutar: {}",
                                        sourceWallet.getId(), targetWalllet.getId(), request.getAmount());

                                return kafkaProducerService.sendTransactionEvent(event);
                            });
                });
    }

    @Override
    public Mono<Void> withdrawFromWallet(Long walletId, WithdrawRequest request) {
        return getCurrentUser()
                .flatMap(user -> walletRepository.findByIdAndUserId(walletId, user.getId())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Cüzdan bulunamadı veya bu cüzdana erişim yetkiniz yok.")))
                )
                .flatMap(wallet -> {
                    // bakiye kontrol
                    if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,"Yetersiz bakiye! Mevcut: " + wallet.getBalance()));
                    }

                    TransactionEvent event = TransactionEvent.builder()
                            .type(TransactionType.WITHDRAW)
                            .sourceWalletId(wallet.getId())
                            .targetWalletId(null)
                            .amount(request.getAmount())
                            .currencyCode(wallet.getCurrencyCode())
                            .build();

                    log.info("Para çekme talebi Kafka'ya gönderiliyor. Cüzdan ID: {}, Tutar: {}", wallet.getId(), request.getAmount());

                    return kafkaProducerService.sendTransactionEvent(event);
                });
    }

    @Override
    public Flux<TransactionHistoryResponse> getWalletTransactionHistory(Long walletId, int page, int size) {
        // size gelen kayit sayisi
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return getCurrentUser()
                .flatMapMany(user -> {
                    return walletRepository.findByIdAndUserId(walletId, user.getId())
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "Cüzdan bulunamadı veya bu cüzdana erişim yetkiniz yok.")))
                            .flatMapMany(wallet -> transactionHistoryRepository.findByWalletId(walletId, pageable));
                })
                .map(TransactionHistoryResponse::fromEntity);
    }
}