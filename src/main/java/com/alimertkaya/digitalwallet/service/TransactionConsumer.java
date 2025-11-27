package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.config.KafkaTopicConfig;
import com.alimertkaya.digitalwallet.dto.TransactionEvent;
import com.alimertkaya.digitalwallet.dto.enums.HistoryDirection;
import com.alimertkaya.digitalwallet.dto.enums.TransactionType;
import com.alimertkaya.digitalwallet.entity.TransactionHistory;
import com.alimertkaya.digitalwallet.entity.Wallet;
import com.alimertkaya.digitalwallet.repository.TransactionHistoryRepository;
import com.alimertkaya.digitalwallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper; // JSON -> object
    private final TransactionHistoryRepository transactionHistoryRepository;

    // yeni mesaj gelince auto tetiklenir
    @KafkaListener(topics = KafkaTopicConfig.WALLET_TRANSACTIONS_TOPIC, groupId = "digital-wallet-group")
    public void consumeTransactionEvent(String message) {
        log.info("Kafka'dan mesaj alındı: {}", message);

        try {
            // gelen JSON string msg event object cevrilir
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);

            // islem tipine gore
            switch (event.getType()) {
                case DEPOSIT -> processDeposit(event);
                case WITHDRAW -> processWithdraw(event);
                case TRANSFER -> processTransfer(event);
                default -> log.warn("Bilinmeten işlem tipi: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Mesaj işlenirken hata oluştu: {}", message, e);
        }
    }

    private void processDeposit(TransactionEvent event) {
        log.info("Para yatırma işlemi başlatılıyor. Cüzdan ID: {}, Tutar: {} {}", event.getSourceWalletId(), event.getSourceAmount(), event.getSourceCurrency());

        // @KafkaListener blocking thread uzerinde calisir
        // repository reaktif oldugu icin reaktif zincirin sonuna .subscribe ekledik
        walletRepository.findById(event.getSourceWalletId())
                .flatMap(wallet -> {
                    BigDecimal currentBalance = wallet.getBalance();
                    wallet.setBalance(wallet.getBalance().add(event.getTargetAmount()));

                    return walletRepository.save(wallet)
                            .flatMap(savedWallet -> saveHistory(
                                    savedWallet,
                                    TransactionType.DEPOSIT,
                                    HistoryDirection.IN,
                                    event.getSourceAmount(),
                                    event.getTargetAmount(),
                                    event.getSourceCurrency(),
                                    event.getTargetCurrency(),
                                    currentBalance,
                                    null,
                                    "Para Yatırma"
                            ));
                })
                .doOnSuccess(updatedWallet -> log.info("Para yatırma başarılı. Yeni Bakiye: {}", updatedWallet.getBalanceAfter()))
                .doOnError(error -> log.error("Para yatırma sırasında veritabını hatası", error))
                .subscribe();
    }

    public void processTransfer(TransactionEvent event) {
        log.info("Transfer işlemi başlatılıyor. {} -> {}, Tutar: {} {}", event.getSourceWalletId(), event.getTargetWalletId(), event.getSourceAmount(), event.getSourceCurrency());

        walletRepository.findById(event.getSourceWalletId())
                .flatMap(sourceWallet -> {
                    // bakiyeyi dus
                    BigDecimal currentBalance = sourceWallet.getBalance();
                    sourceWallet.setBalance(sourceWallet.getBalance().subtract(event.getSourceAmount()));
                    return walletRepository.save(sourceWallet)
                            .flatMap(savedSource -> saveHistory(
                                    savedSource,
                                    TransactionType.TRANSFER,
                                    HistoryDirection.OUT,
                                    event.getSourceAmount(),
                                    event.getTargetAmount(),
                                    event.getSourceCurrency(),
                                    event.getTargetCurrency(),
                                    currentBalance,
                                    event.getTargetWalletId(),
                                    "Transfer Gönderimi"
                            ));
                })
                .flatMap(updatedSource -> {
                    // wallet guncelle, para yatir
                    return walletRepository.findById(event.getTargetWalletId())
                            .flatMap(targetWallet -> {
                                BigDecimal currentBalance = targetWallet.getBalance();
                                targetWallet.setBalance(targetWallet.getBalance().add(event.getTargetAmount()));
                                return walletRepository.save(targetWallet)
                                        .flatMap(savedTarget -> saveHistory(
                                                savedTarget,
                                                TransactionType.TRANSFER,
                                                HistoryDirection.IN,
                                                event.getSourceAmount(),
                                                event.getTargetAmount(),
                                                event.getSourceCurrency(),
                                                event.getTargetCurrency(),
                                                currentBalance,
                                                event.getSourceWalletId(),
                                                "Transfer Alımı"
                                        ));
                            });
                })
                .doOnSuccess(v -> log.info("Transfer başarıyla tamamlandı."))
                .doOnError(e -> log.error("Transfer sırasında hata oluştu!", e))
                .subscribe();
    }

    private void processWithdraw(TransactionEvent event) {
        log.info("Para çekme işlemi başlatılıyor. Cüzdan ID: {}, Tutar: {} {}", event.getSourceWalletId(), event.getSourceAmount(), event.getSourceCurrency());

        walletRepository.findById(event.getSourceWalletId())
                .flatMap(wallet -> {
                    BigDecimal currentBalance = wallet.getBalance();
                    wallet.setBalance(wallet.getBalance().subtract(event.getSourceAmount()));
                    return walletRepository.save(wallet)
                            .flatMap(savedWallet -> saveHistory(
                                    savedWallet,
                                    TransactionType.WITHDRAW,
                                    HistoryDirection.OUT,
                                    event.getSourceAmount(),
                                    event.getTargetAmount(),
                                    event.getSourceCurrency(),
                                    event.getTargetCurrency(),
                                    currentBalance,
                                    null,
                                    "Para Çekme"
                            ));
                })
                .doOnSuccess(updatedWallet -> log.info("Para çekme başarılı. Yeni Bakiye: {}", updatedWallet.getBalanceAfter()))
                .doOnError(error -> log.error("Para çekme sırasında veritabını hatası", error))
                .subscribe();
    }

    // helper method
    private Mono<TransactionHistory> saveHistory(Wallet wallet, TransactionType type, HistoryDirection direction, BigDecimal sourceAmount, BigDecimal targetAmount,
                                                 String sourceCurrency, String targetCurrency, BigDecimal balanceBefore, Long relatedWalletId, String description) {
        BigDecimal amountToSave = (direction == HistoryDirection.OUT) ? sourceAmount : targetAmount;
        String currencyToSave = (direction == HistoryDirection.OUT) ? sourceCurrency : targetCurrency;

        TransactionHistory history = TransactionHistory.builder()
                .walletId(wallet.getId())
                .relatedWalletId(relatedWalletId)
                .type(type)
                .amount(amountToSave)
                .currencyCode(currencyToSave)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .direction(direction)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionHistoryRepository.save(history)
                .doOnSuccess(h -> log.info("İşlem geçmişi kaydedildi. Cüzdan: {}, Tip: {}, Para Birimi: {}", wallet.getId(), type, wallet.getCurrencyCode()));
    }
}
