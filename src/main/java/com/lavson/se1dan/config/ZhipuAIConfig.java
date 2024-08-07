package com.lavson.se1dan.config;

import com.zhipu.oapi.ClientV4;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "zhipu-ai")
@Data
public class ZhipuAIConfig {
    /**
     * apiKey
     */
    private String apiKey;

    /**
     * 获取智谱AI客户端
     *
     * @return
     */
    @Bean
    public ClientV4 getClientV4() {
        return new ClientV4.Builder(apiKey).build();
    }
}
