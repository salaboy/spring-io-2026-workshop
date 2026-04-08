package com.example.warehouse.step02;

import io.github.microcks.testcontainers.MicrocksContainer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.Environment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

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

    @Bean
    @ConditionalOnProperty(name = "microcks.enabled", havingValue = "true")
    MicrocksContainer microcks(Environment env) {
        return new MicrocksContainer("quay.io/microcks/microcks-uber:1.13.2-native")
            .withAccessToHost(true)   // We need this to access our webapp while it runs
            .withMainArtifacts("warehouse-openapi-1.0.0.yaml");
    }
}