package greg.respiroc.com.aicustomerservice.controller

import greg.respiroc.com.aicustomerservice.model.Product
import greg.respiroc.com.aicustomerservice.repository.ProductRepository
import greg.respiroc.com.aicustomerservice.service.ProductSyncService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException

@Controller
class ProductController(private val productRepository: ProductRepository) {
    private val logger = LoggerFactory.getLogger(ProductController::class.java)

    @GetMapping("/products")
    fun productsPage(model: Model): String {
        return "products"
    }

    @GetMapping("/products/list")
    fun productsList(model: Model): String {
        val products = productRepository.findAll(10) // limit shown to 50
        model.addAttribute("products", products)
        // return Thymeleaf fragment named "table" from products.html
        return "products :: table"
    }


    // HTMX form posts here (hx-post="/products")
    @PostMapping("/products")
    fun createProduct(@ModelAttribute("newProduct") form: Product, model: Model): String {
        val id = (1..15)
            .map { ('0'..'9').random() }
            .joinToString("")
            .toLong()
        productRepository.upsertProduct(
            id,
            form.title,
            form.handle
        )                   // save to DB
        model.addAttribute("products", productRepository.findAll(10))
        // return the fragment that contains ONLY the table (so HTMX swaps it in)
        return "products :: table"
    }

    @GetMapping("/products/search")
    fun searchPage(): String {
        return "products_search"
    }

    @GetMapping("/products/search/results")
    fun searchResults(@RequestParam title: String?, model: Model): String {
        logger.info("Searching for products with title: {}", title)
        val results = productRepository.searchByTitle(title, 50)
        logger.info("Found {} products", results.size)
        model.addAttribute("products", results)
        return "products_search :: searchResults"
    }

    @GetMapping("/products/{id}")
    fun productDetail(@PathVariable id: Long, model: Model): String {
        val product = productRepository.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")

        model.addAttribute("product", product)
        return "product_detail"
    }

    @PutMapping("/products/{id}")
    fun updateProduct(
        @PathVariable id: Long,
        @ModelAttribute product: Product
    ): String {
        logger.info("Updating product with id: {}", id)
        productRepository.upsertProduct(id, product.title, product.handle)
        logger.info("Product id {} updated successfully", id)
        return "redirect:/products"
    }

    @DeleteMapping("/products/{id}")
    fun deleteProduct(
        @PathVariable id: Long,
        request: HttpServletRequest
    ): Any {
        // delete from DB
        logger.info("Deleting product with id: {}", id)
        productRepository.delete(id)
        logger.info("Product id {} deleted successfully", id)
        // If this is an HTMX request, return an empty 200 body so hx-swap="outerHTML" removes the row
        if (request.getHeader("HX-Request") != null) {
            return ResponseEntity.ok("")
        }

        // Non-HTMX fallback: redirect to the list page
        return "redirect:/products"
    }
}
