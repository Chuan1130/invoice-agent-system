package invoice_agent_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("invoice_agent_backend.mapper")
public class InvoiceAgentBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvoiceAgentBackendApplication.class, args);
	}
}