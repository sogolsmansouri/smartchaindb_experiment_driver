package com.smartchaindb

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@EnableWebMvc
@SpringBootApplication
open class SmartChainDBDriver

fun main(args: Array<String>) {
    SpringApplication.run(SmartChainDBDriver::class.java, *args)
}
