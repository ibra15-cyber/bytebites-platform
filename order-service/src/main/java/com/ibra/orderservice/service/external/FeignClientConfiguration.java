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

    // Define the headers you want to propagate
    private static final String[] HEADERS_TO_PROPAGATE = {
            "X-User-Id",
            "X-User-Email", // If restaurant service also uses email for logging/context
            "X-User-Role",
            "Authorization" // Propagate the original Authorization header as well, if needed by downstream services
            // (though X-User-Id/Role should be sufficient after Gateway processing)
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
