package com.example.store.step03;

import io.dapr.testcontainers.*;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.connection.KafkaConnection;

import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_VERSION;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    Network network() {
        return Network.newNetwork();
    }

    @Bean(name="jaegerContainer")
    GenericContainer<?> jaegerContainer(Network network) {
        return new GenericContainer<>(DockerImageName.parse("jaegertracing/jaeger"))
                .withNetwork(network)
                .withExposedPorts(16686, 4317, 4318)
                .withNetworkAliases("jaeger");
    }

    @Bean
    OtlpTracingConnectionDetails otlpTracingConnectionDetails(GenericContainer<?> jaegerContainer) {
        return transport -> "http://" + jaegerContainer.getHost() + ":"
                + jaegerContainer.getMappedPort(4318) + "/v1/traces";
    }

    @Bean
    KafkaContainer kafkaContainer(Network network) {
        KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka")
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withListener("kafka:19092");
        return kafkaContainer;
    }

    @Bean
    MicrocksContainersEnsemble microcksEnsemble(Network network, KafkaContainer kafkaContainer) {
        return new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.13.2-native")
                .withAsyncFeature()
                .withAccessToHost(true)
                .withKafkaConnection(new KafkaConnection("kafka:19092"))
                .withMainArtifacts("shipping-service.proto", "shipping-asyncapi-1.0.0.yaml")
                .withSecondaryArtifacts("shipping-asyncapi-examples.yaml")
                .withAsyncDependsOn(kafkaContainer)
                .withMicrocksEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4317")
                .withMicrocksEnv("OTEL_TRACES_EXPORTER", "otlp");
    }

    @Bean
    @ServiceConnection
    public DaprContainer daprContainer(Network daprNetwork,
                                       KafkaContainer kafkaContainer,
                                       MicrocksContainersEnsemble microcksEnsemble){

        Map<String, String> kafkaProperties = new HashMap<>();
        kafkaProperties.put("brokers", "kafka:19092");
        kafkaProperties.put("authType", "none");

        DockerImageName myDaprImage = DockerImageName.parse("daprio/daprd:"+DAPR_VERSION);
        DaprContainer daprContainer = new DaprContainer(myDaprImage)
                .withAppName("store-dapr")
                .withNetwork(daprNetwork)
                .withComponent(new Component("pubsub", "pubsub.kafka", "v1", kafkaProperties))
                .withSubscription(new Subscription(
                        "shipping-events-subscription",
                        "pubsub", "pubsubTopic", "/api/events"))

                .withConfiguration(new Configuration("daprConfig",
                        new TracingConfigurationSettings("1", true,
                                new OtelTracingConfigurationSettings("jaeger:4318", false, "http"), null), null))
  //Uncomment if you want to troubleshoot Dapr related problems
//            .withDaprLogLevel(DaprLogLevel.DEBUG)
//            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
                .withAppPort(8080)
                .withAppHealthCheckPath("/actuator/health")
                .withAppChannelAddress("host.testcontainers.internal")
                .dependsOn(kafkaContainer);

        if (microcksEnsemble != null) {
            daprContainer.withSubscription(new Subscription(
                "shipping-events-subscription", "pubsub",
                microcksEnsemble.getAsyncMinionContainer()
                        .getKafkaMockTopic("Shipping Events", "1.0.0", "SEND sendShipmentEvents"),
                "/api/events"));
        }

        return daprContainer;
    }
}
