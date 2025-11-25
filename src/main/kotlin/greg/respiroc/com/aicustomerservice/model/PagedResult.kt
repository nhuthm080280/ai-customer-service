package greg.respiroc.com.aicustomerservice.model

// A small paged result DTO
data class PagedResult<T>(
    val content: List<T>,
    val totalElements: Long,
    val page: Int,   // 0-based
    val size: Int
) {
    val totalPages: Int
        get() = if (size <= 0) 0 else ((totalElements + size - 1) / size).toInt()
}
