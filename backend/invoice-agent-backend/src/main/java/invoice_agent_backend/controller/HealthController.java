package invoice_agent_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/* 当访问 http://localhost:8080/health，http会发送get请求，
** 从应用层->传输层->网络层->链路层->物理层,再反过来一步步到达后端的应用层。
** 此时请求进入 Spring Boot 后，Spring 会根据注解找到这里。
** 返回OK （暂时的）*/
@RestController
public class HealthController {

    // 这个接口不查数据库，也不调用 Service。它只是测试后端程序是否真的启动成功
    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}