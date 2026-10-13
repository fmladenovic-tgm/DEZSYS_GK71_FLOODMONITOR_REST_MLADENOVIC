package com.example.floodmonitor.config;

import com.example.floodmonitor.security.ApiKeyInterceptor;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Content Negotiation und Sicherheits-Interceptor.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;

    public WebConfig(ApiKeyInterceptor apiKeyInterceptor) {
        this.apiKeyInterceptor = apiKeyInterceptor;
    }

    /**
     * Das Format wird ausschliesslich ueber den Accept-Header bestimmt.
     * Ohne Accept-Header (oder mit *&#47;*) wird JSON geliefert.
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                // JSON bevorzugt; */* als Fallback, damit Endpunkte ohne JSON (z.B. /v3/api-docs.yaml) erreichbar bleiben
                .defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL)
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("xml", MediaType.APPLICATION_XML);
    }

    /** XML-Antworten beginnen mit &lt;?xml version='1.0' encoding='UTF-8'?&gt;. */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2XmlHttpMessageConverter xml
                    && xml.getObjectMapper() instanceof XmlMapper mapper) {
                mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
            }
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/api/**");
    }
}
