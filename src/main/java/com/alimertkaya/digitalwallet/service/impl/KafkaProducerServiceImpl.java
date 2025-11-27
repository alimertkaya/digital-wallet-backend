package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.config.KafkaTopicConfig;
import com.alimertkaya.digitalwallet.dto.TransactionEvent;
import com.alimertkaya.digitalwallet.service.KafkaProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerServiceImpl implements KafkaProducerService {

    // spring otomatik kafka sablonunu yapilandirir
    private final KafkaTemplate<String, String> kafkaTemplate;

    // DTO'yu JSON'a cevirmek icin
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> sendTransactionEvent(TransactionEvent event) {
        // kafkaya gonderme islemi blocking olmaması icin ayri bir thread pool yaratiriz
        return Mono.fromCallable(() -> {
            try {
                // TransactionEvent DTO'sunu JSON'a cevirir
                return objectMapper.writeValueAsString(event);
            } catch (Exception e) {
                log.error("Event DTO'su JSON'a çevrilirken hata oluştu: {}", event, e);
                throw new RuntimeException("Event serialization failed", e);
            }
        })
                .flatMap(jsonEvent -> {
                    // JSON string ini kafka ya gonderir
                    log.info("Kafka'ya even gönderiliyor. EventId: {}", event.getEventId());
                    String key = String.valueOf(event.getSourceWalletId());

                    return Mono.fromFuture(kafkaTemplate.send(
                            KafkaTopicConfig.WALLET_TRANSACTIONS_TOPIC,
                            key, // message key
                            jsonEvent // message value
                    ));
                })
                .subscribeOn(Schedulers.boundedElastic()) // IO/blocking engellemek icin thread pool olusturuyoruz
                .doOnSuccess(sendResult -> log.info(
                        "Event Kafka'ya başarıyla gönderildi. EventId: {}, Topic: {}, Partition: {} ",
                        event.getEventId(),
                        sendResult.getRecordMetadata().topic(),
                        sendResult.getRecordMetadata().partition()
                ))
                .doOnError(ex -> log.error("Event Kafka'ya gönderilirken hata oluştu. EventId: {}", event.getEventId(), ex))
                .then();
    }
}