package com.gree.airconditioner;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LanWebConfig {
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> lanConnector() {
        return factory -> {
            String address = System.getProperty("gree.lan-address", "").trim();
            if (address.isEmpty()) return;
            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setProperty("address", address);
            connector.setPort(8081);
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}
