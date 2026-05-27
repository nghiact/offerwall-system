package com.ctn.offerwall.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApiGatewayApplicationTests {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void contextLoads() {
    }

    @Test
    void exposesOfferCategoryRoute() {
        assertThat(routeDefinitionLocator.getRouteDefinitions()
                .map(routeDefinition -> routeDefinition.getId())
                .collectList()
                .block())
                .contains("offer-category-service");
    }
}
