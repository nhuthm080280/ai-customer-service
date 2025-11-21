package greg.respiroc.com.aicustomerservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class AiCustomerServiceApplication

fun main(args: Array<String>) {
	runApplication<AiCustomerServiceApplication>(*args)
}
