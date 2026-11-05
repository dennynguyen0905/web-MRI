package com.vrris.pacs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc dung ObjectMapper rieng (Swagger-core) de sinh schema trong /v3/api-docs, KHONG tu lay
 * theo spring.jackson.property-naming-strategy (application.yml) - neu khong co bean nay, Swagger UI
 * se hien field camelCase (parentId, departmentId, bodyPart...) sai voi JSON response thuc te va sai
 * voi doc/api/openapi.yaml (quy dinh snake_case). Dang ky ModelResolver dung ObjectMapper cua app
 * de hai ben khop nhau.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }
}
