package com.smartchaindb.config

import com.complexible.stardog.api.ConnectionConfiguration
import com.complexible.stardog.rdf4j.StardogRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class Config {

    @Bean
    open fun stardogRepository(@Qualifier("stardogConnectionConfiguration")
                               configuration: ConnectionConfiguration): StardogRepository {
        val repo = StardogRepository(configuration)
        repo.initialize()
        return repo
    }

    @Bean
    open fun stardogConnectionConfiguration(
            @Value("\${stardog.host}") host: String,
            @Value("\${stardog.username}") username: String,
            @Value("\${stardog.password}") password: String,
            @Value("\${stardog.port}") port: Int,
            @Value("\${stardog.database}") database: String): ConnectionConfiguration {
        return ConnectionConfiguration.from("http://$host:$port/$database")
                .credentials(username, password)
    }

    @Bean
    open fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**").allowCredentials(true).allowedOrigins("*")
            }
        }
    }

}
