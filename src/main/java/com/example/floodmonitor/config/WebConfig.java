package com.example.floodmonitor.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableScheduling
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;

    public WebConfig(ApiKeyInterceptor apiKeyInterceptor) {
        this.apiKeyInterceptor = apiKeyInterceptor;
    }

    // Uhr als Bean, damit man in den Tests eine fixe Zeit verwenden kann
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    // Format nur ueber Accept-Header, Standard ist JSON
    // (*/* zusaetzlich, sonst geht /v3/api-docs.yaml nicht mehr)
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorParameter(false)
                .defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
    }

    // XML soll mit <?xml version...?> anfangen
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> c : converters) {
            if (c instanceof MappingJackson2XmlHttpMessageConverter xml) {
                ((XmlMapper) xml.getObjectMapper()).configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
            }
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/api/**");
    }
}
