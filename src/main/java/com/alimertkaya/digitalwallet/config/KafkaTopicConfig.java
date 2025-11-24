package com.alimertkaya.digitalwallet.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    // kafkaya gidecek tum wallet (deposit, transfer) islemleri bu topic uzerinden olacak
    public static final String WALLET_TRANSACTIONS_TOPIC = "wallet_transactions";

    @Bean
    public NewTopic walletTransactionsTopic() {
        return TopicBuilder.name(WALLET_TRANSACTIONS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}