package com.zincoid.nullbot.console.configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * 控制台接口的 OpenAPI 安全方案。
 * <p>OpenAPI security scheme of the console API.</p>
 */
@Configuration
@SecurityScheme(
        name = "console-token",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "token",
        description = "Static console token")
public class ConsoleOpenApiSecurityConfiguration {
}
