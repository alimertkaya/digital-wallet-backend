package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.config.KafkaTopicConfig;
import com.alimertkaya.digitalwallet.dto.TransactionEvent;
import com.alimertkaya.digitalwallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper; // JSON -> object

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
                case WITHDRAW -> log.info("Para çekme işlemi henüz eklenmedi.");
                case TRANSFER -> log.info("Transfer işlemi henüz eklenmedi.");
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

                    return walletRepository.save(wallet);
                })
                .doOnSuccess(updatedWallet -> log.info("Para yatırma başarılı. Yeni Bakiye: {}", updatedWallet.getBalance()))
                .doOnError(error -> log.error("Para yatırma sırasında veritabını hatası", error))
                .subscribe();
    }
}
