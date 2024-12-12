package com.smartchaindb.chain

import com.bigchaindb.builders.BigchainDbTransactionBuilder
import com.bigchaindb.constants.Operations
import com.bigchaindb.model.GenericCallback
import com.bigchaindb.smartchaindb.driver.Capabilities
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartchaindb.rfq.RFQModel
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import okhttp3.Response
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.KeyPair

@Service(value = "smartChainDbTransactor")
class Transactor {

    @Autowired
    private lateinit var keyPair: KeyPair

    @Autowired
    private lateinit var kafkaProducer: KafkaProducer<String, String>

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun sendRequestForQuote(model: RFQModel) {
        val assetData = emptyMap<String, String>()

        BigchainDbTransactionBuilder.init()
                .addAssets(assetData, Map::class.java)
                .addMetaData(model.metadata())
                .operation(Operations.REQUEST_FOR_QUOTE)
                .buildAndSign(keyPair.public as EdDSAPublicKey, keyPair.private as EdDSAPrivateKey)
                .sendTransaction(object : GenericCallback {
                    override fun pushedSuccessfully(response: Response?) {
                        logger.info("Transaction posted successfully")
                        processRequestForQuoteTransaction(model)
                    }

                    override fun transactionMalformed(response: Response?) {
                        logger.error("Transaction was malformed")
                        response.use { logger.debug("Response = $response") }
                    }

                    override fun otherError(response: Response?) {
                        logger.error("Transaction failed")
                        response.use { logger.debug("Response = $response") }
                    }

                })
    }

    private fun processRequestForQuoteTransaction(model: RFQModel) {
        val content = objectMapper.writeValueAsString(model)

        val topic = if ("PolyCarbonate".equals(model.material, ignoreCase = true)) {
            Capabilities.PRINTING_3D
        } else {
            Capabilities.MISC
        }

        logger.info("Pushing to topic=$topic")

        val producerRecord = ProducerRecord<String, String>(topic, content)

        val record = kafkaProducer.send(producerRecord).get()

        logger.info("Record pushed to kafka topic=$topic")

        logger.debug("Kafka Record = $record")

    }

}

