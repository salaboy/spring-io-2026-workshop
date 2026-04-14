package com.example.store.step02;

import io.github.microcks.testcontainers.MicrocksContainer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    public Network getDaprNetwork(Environment env) {
        boolean reuse = env.getProperty("reuse", Boolean.class, false);
        if (reuse) {
            Network defaultDaprNetwork = new Network() {
                @Override
                public String getId() {
                    return "my-network";
                }

                @Override
                public void close() {

                }

            };

            List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd()
                    .withNameFilter("my-network").exec();
            if (networks.isEmpty()) {
                Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("my-network")).build().getId();
                return defaultDaprNetwork;
            } else {
                return defaultDaprNetwork;
            }
        } else {
            return Network.newNetwork();
        }
    }

    @Bean(name="jaegerContainer")
    GenericContainer<?> jaegerContainer(Environment env, Network network) {
        boolean reuse = env.getProperty("reuse", Boolean.class, false);
        System.out.println(">> Reusing Jaeger:" + reuse);
        return new GenericContainer<>(DockerImageName.parse("jaegertracing/jaeger"))
                .withNetwork(network)
                .withReuse(reuse)
                .withExposedPorts(16686, 4317, 4318)
                .withNetworkAliases("jaeger");
    }

    @Bean
    OtlpTracingConnectionDetails otlpTracingConnectionDetails(GenericContainer<?> jaegerContainer) {
        return transport -> "http://" + jaegerContainer.getHost() + ":"
                + jaegerContainer.getMappedPort(4318) + "/v1/traces";
    }

    @Bean
    @ConditionalOnProperty(name = "microcks.enabled", havingValue = "true")
    MicrocksContainer microcks(Network network) {
        return new MicrocksContainer("quay.io/microcks/microcks-uber:1.13.2-native")
            .withNetwork(network)
            .withNetworkAliases("microcks")
            .withMainArtifacts("anthropic-openapi.yaml", "warehouse-openapi-1.0.0.yaml")
            .withSecondaryArtifacts("anthropic-metadata.yaml", "anthropic-examples.yaml")
            .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4317")
            .withEnv("OTEL_TRACES_EXPORTER", "otlp")
            .withDebugLogLevel();
    }

    @Bean
    public DynamicPropertyRegistrar properties(@Nullable MicrocksContainer microcks) {
        return (registrar) -> {
            if (microcks != null) {
                registrar.add("spring.ai.anthropic.base-url", () -> microcks.getRestMockEndpoint("Anthropic API", "0.83.0"));
            }
        };
    }

    @Bean(name="warehouseMcpContainer")
    @ConditionalOnProperty(name = "microcks.enabled", havingValue = "true")
    GenericContainer<?> warehouseMcpContainer(Network network, MicrocksContainer microcks) {
        return new GenericContainer<>(DockerImageName.parse("ghcr.io/salaboy/springio-warehouse-mcp:step-02"))
                .withNetwork(network)
                .withExposedPorts(8087)
                .withNetworkAliases("warehouse-mcp")
                .withEnv("MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT", "http://jaeger:4318/v1/traces")
                .withEnv("APPLICATION_WAREHOUSE_BASE_URL", "http://microcks:8080/rest/Warehouse+API/1.0.0");
    }

    @Bean
    public DynamicPropertyRegistrar storeProperties(@Qualifier("warehouseMcpContainer") @Nullable GenericContainer<?> warehouseMcpContainer) {
        return registry -> {
            if (warehouseMcpContainer != null) {
                registry.add("spring.ai.mcp.client.streamable-http.connections.warehouse-mcp.url",
                        () -> "http://localhost:" + warehouseMcpContainer.getMappedPort(8087));
            }
        };
    }
}
