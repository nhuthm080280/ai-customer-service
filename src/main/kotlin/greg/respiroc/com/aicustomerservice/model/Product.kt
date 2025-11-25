package greg.respiroc.com.aicustomerservice.model

import java.time.LocalDateTime

data class Product(
    val id: Long?,
    val title: String?,
    val handle: String?,

    val vendor: String?,
    val productType: String?,

    val publishedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)
