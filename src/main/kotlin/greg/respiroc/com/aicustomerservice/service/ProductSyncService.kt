package greg.respiroc.com.aicustomerservice.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import greg.respiroc.com.aicustomerservice.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Service
class ProductSyncService(
    private val productRepository: ProductRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(ProductSyncService::class.java)

    private val client: WebClient = WebClient.builder()
        .baseUrl("https://famme.no")
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { configurer ->
                    // increase in-memory buffer to 16 MB (adjust as needed)
                    configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                }
                .build()
        )
        .build()

    private val maxProducts = 50

    @Scheduled(initialDelayString = "0", fixedDelayString = "PT1H")
    fun syncProducts() {
        try {
            log.info("Starting product sync from famme.no")
            val root: JsonNode? = client.get()
                .uri("/products.json")
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .block()

            val productsNode = root?.get("products")
            if (productsNode == null || !productsNode.isArray) {
                log.warn("No 'products' array found in response")
                return
            }

            var count = 0
            productsNode.forEach { productNode ->
                if (count >= maxProducts) return@forEach
                val idNode = productNode.get("id") ?: return@forEach
                val id = idNode.asLong()
                val title = productNode.get("title")?.asText()
                val handle = productNode.get("handle")?.asText()
                val vendor = productNode.get("vendor")?.asText()
                val productType = productNode.get("product_type")?.asText()
                val createAt = productNode.get("created_at")?.asText()
                val publishedAt = productNode.get("published_at")?.asText()
                val updatedAt = productNode.get("updated_at")?.asText()
                productRepository.syncProduct(id, title, handle, vendor, productType, createAt, publishedAt, updatedAt)
                count++
            }

            log.info("Product sync completed: upserted {} products (max {})", count, maxProducts)
        } catch (ex: Exception) {
            log.error("Failed to sync products", ex)
        }
    }
}