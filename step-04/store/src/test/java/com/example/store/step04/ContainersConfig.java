package com.example.store.step04;

import io.dapr.testcontainers.*;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_VERSION;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    Map<String, String> postgreSQLDetails = new HashMap<>();

    {{
        postgreSQLDetails.put("host", "postgresql");
        postgreSQLDetails.put("user", "postgres");
        postgreSQLDetails.put("password", "postgres");
        postgreSQLDetails.put("database", "dapr");
        postgreSQLDetails.put("port", "5432");
        postgreSQLDetails.put("actorStateStore", String.valueOf(true));

    }}

    private Component stateStoreComponent = new Component("kvstore",
            "state.postgresql", "v2", postgreSQLDetails);

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

    @Bean(name="warehouseContainer")
    GenericContainer<?> warehouseContainer(Network network) {
        return new GenericContainer<>(DockerImageName.parse("ghcr.io/salaboy/springio-warehouse:step-02"))
                .withNetwork(network)
                .withExposedPorts(8086)
                .withNetworkAliases("warehouse")
                .withEnv("MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT", "http://jaeger:4318/v1/traces");
    }

    @Bean(name="warehouseMcpContainer")
    GenericContainer<?> warehouseMcpContainer(Network network) {
        return new GenericContainer<>(DockerImageName.parse("ghcr.io/salaboy/springio-warehouse-mcp:step-02"))
                .withNetwork(network)
                .withExposedPorts(8087)
                .withNetworkAliases("warehouse-mcp")
                .withEnv("MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT", "http://jaeger:4318/v1/traces")
                .withEnv("APPLICATION_WAREHOUSE_BASE_URL", "http://warehouse:8086");
    }

    @Bean(name="shippingContainer")
    GenericContainer<?> shippingContainer(Network network, DaprContainer shippingDaprContainer) {
        return new GenericContainer<>(DockerImageName.parse("ghcr.io/salaboy/springio-shipping:step-04"))
                .withNetwork(network)
                .withExposedPorts(9091)
                .withNetworkAliases("shipping")
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://jaeger:4318")
                .withEnv("OTEL_SERVICE_NAME", "shipping")
                .withEnv("OTEL_TRACES_EXPORTER", "otlp")
                .withEnv("DAPR_HOST", "shipping-dapr")
                .withEnv("DAPR_PORT", "50001")
                .dependsOn(shippingDaprContainer);

    }

    @Bean
    DynamicPropertyRegistrar warehouseProperties(GenericContainer<?> warehouseContainer) {
        return registry -> registry.add("warehouse.url",
                () -> "http://localhost:" + warehouseContainer.getMappedPort(8086));
    }

    @Bean
    DynamicPropertyRegistrar storeProperties(GenericContainer<?> warehouseMcpContainer) {
        return registry -> registry.add("spring.ai.mcp.client.streamable-http.connections.warehouse-mcp.url",
                () -> "http://localhost:" + warehouseMcpContainer.getMappedPort(8087));
    }

    @Bean
    DynamicPropertyRegistrar shippingProperties(GenericContainer<?> shippingContainer) {
        return registry -> registry.add("shipping.url",
                () -> "localhost:" + shippingContainer.getMappedPort(9091));
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
    public WorkflowDashboardContainer workflowDashboard(Network network, PostgreSQLContainer postgreSQLContainer) {
        return new WorkflowDashboardContainer(WorkflowDashboardContainer.getDefaultImageName())
                .withNetwork(network)
                .withStateStoreComponent(stateStoreComponent)
                .withExposedPorts(8080)
                .dependsOn(postgreSQLContainer);
    }

    @Bean
    public PostgreSQLContainer postgreSQLContainer(Network network) {
        return new PostgreSQLContainer(DockerImageName.parse("postgres"))
                .withNetworkAliases("postgresql")
                .withDatabaseName("dapr")
                .withUsername("postgres")
                .withPassword("postgres")
                .withNetwork(network);
    }

    @Bean
    @ServiceConnection
    public DaprContainer daprContainer(Network daprNetwork,
                                       KafkaContainer kafkaContainer,
                                       PostgreSQLContainer postgreSQLContainer){

        Map<String, String> kafkaProperties = new HashMap<>();
        kafkaProperties.put("brokers", "kafka:19092");
        kafkaProperties.put("authType", "none");

        DockerImageName myDaprImage = DockerImageName.parse("daprio/daprd:"+DAPR_VERSION);
        return new DaprContainer(myDaprImage)
                .withAppName("store-dapr")
                .withNetwork(daprNetwork)
                .withReusablePlacement(true)
                .withReusableScheduler(true)
                .withComponent(stateStoreComponent)
                .withComponent(new Component("pubsub", "pubsub.kafka", "v1", kafkaProperties))
                .withSubscription(new Subscription(
                        "shipping-events-subscription",
                        "pubsub", "shipments", "/api/events"))

                .withConfiguration(new Configuration("daprConfig",
                        new TracingConfigurationSettings("1", true,
                                new OtelTracingConfigurationSettings("jaeger:4318", false, "http"), null), null))
  //Uncomment if you want to troubleshoot Dapr related problems
//            .withDaprLogLevel(DaprLogLevel.DEBUG)
//            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
                .withAppPort(8080)
                .withAppHealthCheckPath("/actuator/health")
                .withAppChannelAddress("host.testcontainers.internal")
                .dependsOn(kafkaContainer)
                .dependsOn(postgreSQLContainer);
    }


    @Bean
    public DaprContainer shippingDaprContainer(Network daprNetwork,
                                       KafkaContainer kafkaContainer){

        Map<String, String> kafkaProperties = new HashMap<>();
        kafkaProperties.put("brokers", "kafka:19092");
        kafkaProperties.put("authType", "none");

        DockerImageName myDaprImage = DockerImageName.parse("daprio/daprd:"+DAPR_VERSION);
        return new DaprContainer(myDaprImage)
                .withAppName("shipping-dapr")
                .withNetworkAliases("shipping-dapr")
                .withNetwork(daprNetwork)
                .withReusablePlacement(true)
                .withReusableScheduler(true)
                .withComponent(new Component("pubsub", "pubsub.kafka", "v1", kafkaProperties))

                .withConfiguration(new Configuration("daprConfig",
                        new TracingConfigurationSettings("1", true,
                                new OtelTracingConfigurationSettings("jaeger:4318", false, "http"), null), null))
                //Uncomment if you want to troubleshoot Dapr related problems
//            .withDaprLogLevel(DaprLogLevel.DEBUG)
//            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
                .withAppPort(9091)
                .withAppProtocol(DaprProtocol.GRPC)
                .withAppChannelAddress("shipping")
                .dependsOn(kafkaContainer);
    }

}
