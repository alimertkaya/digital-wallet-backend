package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.wallet.TransactionEvent;
import reactor.core.publisher.Mono;

public interface KafkaProducerService {

    /* TransactionEvent i Kafka ya asenkron olarak gonderir
    * @param event gonderilecek islem
    * @return islemin tamamlandigini haber veren bir Mono<Void>
    */
    Mono<Void> sendTransactionEvent(TransactionEvent event);
}
