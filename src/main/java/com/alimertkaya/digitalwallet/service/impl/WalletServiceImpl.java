package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.enums.TransactionType;
import com.alimertkaya.digitalwallet.dto.wallet.*;
import com.alimertkaya.digitalwallet.entity.User;
import com.alimertkaya.digitalwallet.entity.Wallet;
import com.alimertkaya.digitalwallet.repository.TransactionHistoryRepository;
import com.alimertkaya.digitalwallet.repository.WalletRepository;
import com.alimertkaya.digitalwallet.service.ExchangeRateService;
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
    private final ExchangeRateService exchangeRateService;

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
                            .sourceAmount(request.getAmount())
                            .targetAmount(request.getAmount())
                            .sourceCurrency(wallet.getCurrencyCode())
                            .targetCurrency(wallet.getCurrencyCode())
                            .build();

                    log.info("Para yatırma talebi Kafka'ya gönderiliyor. Cüzdan ID: {}, Tutar: {}", wallet.getId(), request.getAmount());
                    return kafkaProducerService.sendTransactionEvent(event);
                });
    }

    @Override
    public Mono<Void> transferFunds(Long sourceWalletId, TransferRequest request) {
        Mono<Wallet> sourceWalletMono = getCurrentUser()
                .flatMap(user -> walletRepository.findByIdAndUserId(sourceWalletId, user.getId())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Kaynak cüzdan bulunamadı veya yetkiniz yok."))));

        Mono<Wallet> targetWalletMono = walletRepository.findById(request.getTargetWalletId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Hedef cüzdan bulunamadı.")));

        return Mono.zip(sourceWalletMono, targetWalletMono)
                .flatMap(tuple -> {
                    Wallet sourceWallet = tuple.getT1();
                    Wallet targetWallet = tuple.getT2();

                    if (sourceWallet.getBalance().compareTo(request.getAmount()) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yetersiz bakiye!"));
                    }
                    if (sourceWallet.getId().equals(targetWallet.getId())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kendinize transfer yapamazsınız."));
                    }

                    return exchangeRateService.convertCurrency(
                            request.getAmount(),
                            sourceWallet.getCurrencyCode(),
                            targetWallet.getCurrencyCode()
                    ).flatMap(convertedAmount -> {
                        TransactionEvent event = TransactionEvent.builder()
                                .type(TransactionType.TRANSFER)
                                .sourceWalletId(sourceWallet.getId())
                                .targetWalletId(targetWallet.getId())
                                .sourceAmount(request.getAmount())
                                .targetAmount(convertedAmount)
                                .sourceCurrency(sourceWallet.getCurrencyCode())
                                .targetCurrency(targetWallet.getCurrencyCode())
                                .description(request.getDescription())
                                .build();

                        log.info("Transfer talebi Kafka'ya gönderiliyor. {} -> {}, Tutar: {} {}, Hedef Tutar: {} {}",
                                sourceWallet.getId(), targetWallet.getId(), request.getAmount(),
                                sourceWallet.getCurrencyCode(), convertedAmount, targetWallet.getCurrencyCode());

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
                            .sourceAmount(request.getAmount())
                            .targetAmount(request.getAmount())
                            .sourceCurrency(wallet.getCurrencyCode())
                            .targetCurrency(wallet.getCurrencyCode())
                            .description(request.getDescription())
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

    @Override
    public Mono<Void> deleteWallet(Long walletId) {
        return getCurrentUser()
                .flatMap(user -> walletRepository.findByIdAndUserId(walletId, user.getId())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Cüzdan bulunamadı veya bu cüzdana erişim yetkiniz yok."))))
                .flatMap(wallet -> {
                    if (wallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Bakiyesi olan cüzdan silinemez. Önce bakiyeyi çekin veya transfer edin."));
                    }
                    log.info("Cüzdan siliniyor. ID: {}, Kullanıcı ID: {}", wallet.getId(),
                            wallet.getUserId());
                    return walletRepository.delete(wallet);
                });
    }
}