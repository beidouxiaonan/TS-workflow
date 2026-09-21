package com.zhixing.ticket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI knowledgeTicketOpenAPI() {
        return new OpenAPI().info(new Info()
            .title("知识工单系统 REST API")
            .description("知识工单申请、审批、知识上传、验收及附件 ID 持久化接口")
            .version("1.0.11")
            .contact(new Contact().name("知识工单系统维护团队")));
    }
}
