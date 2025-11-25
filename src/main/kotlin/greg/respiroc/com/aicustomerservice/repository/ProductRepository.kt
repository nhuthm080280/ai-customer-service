package greg.respiroc.com.aicustomerservice.repository

import greg.respiroc.com.aicustomerservice.model.PagedResult
import greg.respiroc.com.aicustomerservice.model.Product
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProductRepository(private val jdbcTemplate: JdbcTemplate) {

    private val upsertSql = """
        INSERT INTO products (id, title, handle)
        VALUES (?, ?, ?)
        ON CONFLICT (id) DO UPDATE
        SET title = EXCLUDED.title,
            handle = EXCLUDED.handle
    """.trimIndent()

    fun upsertProduct(id: Long, title: String?, handle: String?) {
        jdbcTemplate.update(upsertSql, id, title, handle)
    }

    fun findAll(limit: Int = 100): List<Product> {
        val sql = "SELECT id, title, handle FROM products ORDER BY created_on DESC LIMIT ?"
        return jdbcTemplate.query(sql, arrayOf(limit)) { rs, _ ->
            Product(
                id = rs.getLong("id"),
                title = rs.getString("title"),
                handle = rs.getString("handle")
            )
        }
    }

    // New: page-based query using LIMIT/OFFSET + count
    fun findPage(page: Int = 0, size: Int = 10): PagedResult<Product> {
        val sizeSafe = if (size <= 0) 10 else size
        val pageSafe = if (page < 0) 0 else page
        val offset = pageSafe.toLong() * sizeSafe.toLong()

        // Count total rows
        val countSql = "SELECT COUNT(*) FROM products"
        val totalElements = jdbcTemplate.queryForObject(countSql, Long::class.java) ?: 0L

        // Get page content
        val sql = """
            SELECT id, title, handle
            FROM products
            ORDER BY created_on DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        // Use proper parameter types: size then offset
        val content = jdbcTemplate.query(sql, arrayOf(sizeSafe, offset)) { rs, _ ->
            Product(
                id = rs.getLong("id"),
                title = rs.getString("title"),
                handle = rs.getString("handle")
            )
        }

        return PagedResult(content = content, totalElements = totalElements, page = pageSafe, size = sizeSafe)
    }

    // ---------------------------
    // New: findById
    // ---------------------------
    fun findById(id: Long): Product? {
        val sql = "SELECT id, title, handle FROM products WHERE id = ?"
        val list = jdbcTemplate.query(sql, arrayOf(id)) { rs, _ ->
            Product(
                id = rs.getLong("id"),
                title = rs.getString("title"),
                handle = rs.getString("handle")
            )
        }
        return list.firstOrNull()
    }

    fun searchByTitle(title: String?, limit: Int = 50): List<Product> {
        if (title.isNullOrBlank()) return emptyList()

        val sql = """
        SELECT id, title, handle
        FROM products
        WHERE title ILIKE ?
        ORDER BY created_on DESC
        LIMIT ?
    """.trimIndent()

        val pattern = "%${title.trim()}%"
        return jdbcTemplate.query(sql, arrayOf(pattern, limit)) { rs, _ ->
            Product(
                id = rs.getLong("id"),
                title = rs.getString("title"),
                handle = rs.getString("handle")
            )
        }
    }

    fun delete(id: Long) {
        val sql = "DELETE FROM products WHERE id = ?"
        jdbcTemplate.update(sql, id)
    }

}