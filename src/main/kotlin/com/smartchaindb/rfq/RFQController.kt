package com.smartchaindb.rfq

import com.complexible.stardog.rdf4j.StardogRepository
import com.smartchaindb.chain.Transactor
import org.eclipse.rdf4j.query.QueryLanguage.SPARQL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RFQController {

    private val PREFIX = "http://www.manunetwork.com/manuservice/v1"
    private val RDF_PROPS = listOf("Product", "CostSpecification", "Material")

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    lateinit var repository: StardogRepository

    @Autowired
    @Qualifier("smartChainDbTransactor")
    lateinit var transactor: Transactor

    @RequestMapping(path = ["/rfq/form/fields"], method = [GET])
    fun getFormFields(): List<String> {
        return repository.connection?.use { conn ->
            RDF_PROPS.flatMap { prop ->
                val query = conn.prepareTupleQuery(SPARQL, """
                    SELECT ?o
                    WHERE {
                        <$PREFIX#$prop> ?p ?o .
                        FILTER(?p = rdfs:label) .
                    }
                """.trimIndent())
                query?.evaluate()?.use { queryResult ->
                    val results = ArrayList<String>()
                    while (queryResult.hasNext()) {
                        val resultSet = queryResult.next()
                        results.add(resultSet.getValue("o").stringValue())
                    }
                    results
                }.orEmpty()
            }
        }.orEmpty()
    }

    @RequestMapping(path = ["/rfq/form/values"], method = [GET])
    fun getFormFieldPermittedValues(@RequestParam("field", required = true) field: String): List<String> {
        return repository.connection?.use {
            val query = it.prepareTupleQuery(SPARQL, """
                SELECT DISTINCT ?s
                WHERE {
                    ?s rdf:type <$PREFIX#$field> .
                    ?s ?p ?o .
                }
            """.trimIndent())
            query?.evaluate()?.use { queryResult ->
                val results = ArrayList<String>()
                while (queryResult.hasNext()) {
                    val resultSet = queryResult.next()
                    results.add(resultSet.getValue("s").stringValue())
                }
                results
            }
        }.orEmpty()
    }

    @RequestMapping(path = ["/rfq"], method = [POST])
    fun submitRFQ(@RequestBody(required = true) rfq: RFQModel) {
        logger.debug("rfq = $rfq")
        transactor.sendRequestForQuote(rfq)
    }

}
