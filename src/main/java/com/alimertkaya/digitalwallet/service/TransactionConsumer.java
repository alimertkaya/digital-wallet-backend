package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.config.KafkaTopicConfig;
import com.alimertkaya.digitalwallet.dto.TransactionEvent;
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
        log.info("Para yatırma işlemi başlatılıyor. Cüzdan ID: {}, Tutar: {}", event.getSourceWalletId(), event.getAmount());

        // @KafkaListener blocking thread uzerinde calisir
        // repository reaktif oldugu icin reaktif zincirin sonuna .subscribe ekledik

        walletRepository.findById(event.getSourceWalletId())
                .flatMap(wallet -> {
                    BigDecimal currentBalance = wallet.getBalance();
                    BigDecimal newBalance = currentBalance.add(event.getAmount());
                    wallet.setBalance(newBalance);

                    return walletRepository.save(wallet)
                            .flatMap(savedWallet -> saveHistory(
                                    savedWallet,
                                    TransactionType.DEPOSIT,
                                    HistoryDirection.IN,
                                    event.getAmount(),
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
        log.info("Transfer işlemi başlatılıyor. {} -> {}, Tutar: {}", event.getSourceWalletId(), event.getTargetWalletId() ,event.getAmount());

        walletRepository.findById(event.getSourceWalletId())
                .flatMap(sourceWallet -> {
                    // bakiyeyi dus
                    BigDecimal currentBalance = sourceWallet.getBalance();
                    sourceWallet.setBalance(sourceWallet.getBalance().subtract(event.getAmount()));
                    return walletRepository.save(sourceWallet)
                            .flatMap(savedSource -> saveHistory(
                                    savedSource,
                                    TransactionType.TRANSFER,
                                    HistoryDirection.OUT,
                                    event.getAmount(),
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
                                targetWallet.setBalance(targetWallet.getBalance().add(event.getAmount()));
                                return walletRepository.save(targetWallet)
                                        .flatMap(savedTarget -> saveHistory(
                                                savedTarget,
                                                TransactionType.TRANSFER,
                                                HistoryDirection.IN,
                                                event.getAmount(),
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
        log.info("Para çekme işlemi başlatılıyor. Cüzdan ID: {}, Tutar: {}", event.getSourceWalletId(), event.getAmount());

        walletRepository.findById(event.getSourceWalletId())
                .flatMap(wallet -> {
                    BigDecimal currentBalance = wallet.getBalance();
                    wallet.setBalance(wallet.getBalance().subtract(event.getAmount()));
                    return walletRepository.save(wallet)
                            .flatMap(savedWallet -> saveHistory(
                                    savedWallet,
                                    TransactionType.WITHDRAW,
                                    HistoryDirection.OUT,
                                    event.getAmount(),
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
    private Mono<TransactionHistory> saveHistory(Wallet wallet, TransactionType type, HistoryDirection direction, BigDecimal amount,
                                                 BigDecimal balanceBefore, Long relatedWalletId, String description) {
        TransactionHistory history = TransactionHistory.builder()
                .walletId(wallet.getId())
                .relatedWalletId(relatedWalletId)
                .type(type)
                .amount(amount)
                .currencyCode(wallet.getCurrencyCode())
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
