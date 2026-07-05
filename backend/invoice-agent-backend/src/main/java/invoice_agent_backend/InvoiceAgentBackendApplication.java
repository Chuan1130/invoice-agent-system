package invoice_agent_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/* @SpringBootApplication 告诉 Spring Boot：从这个包开始扫描 Controller、Service
** 等组件。
** @MapperScan("invoice_agent_backend.mapper") 告诉 MyBatis：去
** invoice_agent_backend.mapper 包下面找 Mapper 接口，比如 AuditTaskMapper 和
** InvoiceInfoMapper。否则后面 Service 想调用数据库时，会找不到 Mapper。
**/
@SpringBootApplication
@MapperScan("invoice_agent_backend.mapper")
public class InvoiceAgentBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvoiceAgentBackendApplication.class, args);
	}
}