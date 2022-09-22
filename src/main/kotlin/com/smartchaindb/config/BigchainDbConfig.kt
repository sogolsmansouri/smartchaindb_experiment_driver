package com.smartchaindb.config

import com.bigchaindb.builders.BigchainDbConfigBuilder
import net.i2p.crypto.eddsa.KeyPairGenerator
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import java.security.KeyPair
import java.util.*

@Configuration
open class BigchainDbConfig {

    @Bean(name = ["edDsaKpg"])
    open fun keyPairGenerator(): KeyPairGenerator {
        return KeyPairGenerator()
    }

    @Bean
    open fun keyPair(@Qualifier("edDsaKpg") edDsaKpg: KeyPairGenerator): KeyPair {
        return edDsaKpg.generateKeyPair()
    }

    @EventListener(ApplicationReadyEvent::class)
    open fun bigchainDbConfig(event: ApplicationReadyEvent) {
        val host = event.applicationContext.environment.getProperty("bigchaindb.host")
        val port = event.applicationContext.environment.getProperty("bigchaindb.port")
        BigchainDbConfigBuilder.baseUrl("http://$host:$port/").setup()
    }

    @Bean
    open fun kafkaProducer(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
                           @Value("\${spring.kafka.client-id") clientId: String): KafkaProducer<String, String> {
        val properties = Properties()
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        properties.setProperty(ProducerConfig.CLIENT_ID_CONFIG, clientId)
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.qualifiedName)
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.qualifiedName)
        return KafkaProducer(properties)
    }

}
