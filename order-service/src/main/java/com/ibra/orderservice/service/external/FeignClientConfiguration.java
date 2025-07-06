package com.ibra.orderservice.service.external;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;

@Configuration
public class FeignClientConfiguration {

    private static final String[] HEADERS_TO_PROPAGATE = {
            "X-User-Id",
            "X-User-Email",
            "X-User-Role",
            "Authorization"
    };

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Get the current HttpServletRequest from the RequestContextHolder
                // This ensures we're getting headers from the original incoming request
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    Enumeration<String> headerNames = request.getHeaderNames();
                    if (headerNames != null) {
                        while (headerNames.hasMoreElements()) {
                            String name = headerNames.nextElement();
                            // Only propagate the headers we explicitly want
                            for (String headerToPropagate : HEADERS_TO_PROPAGATE) {
                                if (name.equalsIgnoreCase(headerToPropagate)) {
                                    String value = request.getHeader(name);
                                    // Add the header to the Feign request template
                                    template.header(name, value);
                                    break; // Move to the next incoming header
                                }
                            }
                        }
                    }
                }
            }
        };
    }
}
