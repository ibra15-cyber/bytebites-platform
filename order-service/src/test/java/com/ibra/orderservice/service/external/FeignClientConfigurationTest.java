package com.ibra.orderservice.service.external;

import static org.junit.jupiter.api.Assertions.*;


import com.ibra.orderservice.dto.ApiResponse;
import com.ibra.orderservice.dto.MenuItemDTO;
import com.ibra.orderservice.dto.RestaurantDTO;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeignClientConfigurationTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private ServletRequestAttributes mockAttributes;

    @Mock
    private RequestTemplate mockTemplate;

    private FeignClientConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new FeignClientConfiguration();
    }

    @Test
    void testRequestInterceptorCreation() {
        // Act
        RequestInterceptor interceptor = configuration.requestInterceptor();

        // Assert
        assertNotNull(interceptor);
    }


    @Test
    void testRequestInterceptor_CaseInsensitiveHeaderMatching() {
        // Arrange
        RequestInterceptor interceptor = configuration.requestInterceptor();

        Vector<String> headerNames = new Vector<>();
        headerNames.add("x-user-id"); // lowercase
        headerNames.add("X-USER-EMAIL"); // uppercase
        headerNames.add("X-User-Role"); // mixed case

        when(mockAttributes.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getHeaderNames()).thenReturn(headerNames.elements());
        when(mockRequest.getHeader("x-user-id")).thenReturn("12345");
        when(mockRequest.getHeader("X-USER-EMAIL")).thenReturn("test@example.com");
        when(mockRequest.getHeader("X-User-Role")).thenReturn("CUSTOMER");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(mockAttributes);

            // Act
            interceptor.apply(mockTemplate);

            // Assert
            verify(mockTemplate).header("x-user-id", "12345");
            verify(mockTemplate).header("X-USER-EMAIL", "test@example.com");
            verify(mockTemplate).header("X-User-Role", "CUSTOMER");
        }
    }

    @Test
    void testRequestInterceptor_WithNullRequestAttributes() {
        // Arrange
        RequestInterceptor interceptor = configuration.requestInterceptor();

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> interceptor.apply(mockTemplate));

            // Verify no headers were added
            verify(mockTemplate, never()).header(anyString(), anyString());
        }
    }

    @Test
    void testRequestInterceptor_WithNullHeaderNames() {
        // Arrange
        RequestInterceptor interceptor = configuration.requestInterceptor();

        when(mockAttributes.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getHeaderNames()).thenReturn(null);

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(mockAttributes);

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> interceptor.apply(mockTemplate));

            // Verify no headers were added
            verify(mockTemplate, never()).header(anyString(), anyString());
        }
    }

    @Test
    void testRequestInterceptor_WithEmptyHeaders() {
        // Arrange
        RequestInterceptor interceptor = configuration.requestInterceptor();

        Vector<String> emptyHeaders = new Vector<>();
        when(mockAttributes.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getHeaderNames()).thenReturn(emptyHeaders.elements());

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(mockAttributes);

            // Act
            interceptor.apply(mockTemplate);

            // Assert - no headers should be added
            verify(mockTemplate, never()).header(anyString(), anyString());
        }
    }

    @Test
    void testRequestInterceptor_WithNullHeaderValue() {
        // Arrange
        RequestInterceptor interceptor = configuration.requestInterceptor();

        Vector<String> headerNames = new Vector<>();
        headerNames.add("X-User-Id");

        when(mockAttributes.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getHeaderNames()).thenReturn(headerNames.elements());
        when(mockRequest.getHeader("X-User-Id")).thenReturn(null);

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(mockAttributes);

            // Act
            interceptor.apply(mockTemplate);

            // Assert - header should still be added even with null value
            verify(mockTemplate).header("X-User-Id", (String) null);
        }
    }

}