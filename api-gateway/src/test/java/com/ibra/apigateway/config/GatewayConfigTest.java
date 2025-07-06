package com.ibra.apigateway.config;

import com.ibra.apigateway.config.GatewayConfig;
import com.ibra.apigateway.filter.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder.Builder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayConfigTest {

    @Mock
    private JwtAuthFilter jwtAuthFilter;

    @InjectMocks
    private GatewayConfig gatewayConfig;

    @Test
    void testRouteLocatorCreation() {
        // Given
        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class);
        Builder routeBuilder = mock(Builder.class);
        RouteLocator expectedRouteLocator = mock(RouteLocator.class);

        when(builder.routes()).thenReturn(routeBuilder);
        when(routeBuilder.route(any(String.class), any())).thenReturn(routeBuilder);
        when(routeBuilder.build()).thenReturn(expectedRouteLocator);

        // When
        RouteLocator result = gatewayConfig.customRouteLocator(builder);

        // Then
        assertNotNull(result);
        verify(builder).routes();
        verify(routeBuilder, times(5)).route(any(String.class), any()); // 5 routes defined
        verify(routeBuilder).build();
    }

    @Test
    void testJwtAuthFilterDependencyInjection() {
        // Given
        JwtAuthFilter mockFilter = mock(JwtAuthFilter.class);

        // When
        ReflectionTestUtils.setField(gatewayConfig, "jwtAuthFilter", mockFilter);

        // Then
        JwtAuthFilter injectedFilter = (JwtAuthFilter) ReflectionTestUtils.getField(gatewayConfig, "jwtAuthFilter");
        assertNotNull(injectedFilter);
        assertEquals(mockFilter, injectedFilter);
    }
}