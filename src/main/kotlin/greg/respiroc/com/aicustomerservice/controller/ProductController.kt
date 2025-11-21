package greg.respiroc.com.aicustomerservice.controller

import greg.respiroc.com.aicustomerservice.model.Product
import greg.respiroc.com.aicustomerservice.repository.ProductRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException

@Controller
class ProductController(private val productRepository: ProductRepository) {

    @GetMapping("/products")
    fun productsPage(model: Model): String {
        // initial page load - table empty until user clicks "Load products"
//        model.addAttribute("products", emptyList<Any>())
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

    // Renders the search page (contains input + HTMX target)
    @GetMapping("/products/search")
    fun searchPage(): String {
        return "products_search"   // new template: templates/products_search.html
    }

    // HTMX endpoint that returns the same table fragment (products :: table)
    @GetMapping("/products/search/results")
    fun searchResults(@RequestParam name: String?, model: Model): String {
        val results = productRepository.searchByTitle(name, 100)
        model.addAttribute("products", results)
        // return fragment "products :: table" — our table only renders if products != null && not empty
        return "products :: table"
    }
}
