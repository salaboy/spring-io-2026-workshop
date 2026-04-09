package com.example.store.step04;

import com.example.store.step04.model.Event;
import com.example.store.step04.shipping.ShippingServiceGrpc;
import io.dapr.client.DaprClient;
import io.dapr.spring.boot.properties.pubsub.DaprPubSubProperties;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration
@EnableConfigurationProperties({DaprPubSubProperties.class})
public class StoreConfiguration {

    @Bean
    public DaprMessagingTemplate<Event> messagingTemplate(DaprClient daprClient,
                                                          DaprPubSubProperties daprPubSubProperties) {
        return new DaprMessagingTemplate<>(daprClient, daprPubSubProperties.getName(), false);
    }

    @Bean
    public RestClient warehouseRestClient(ObservationRegistry observationRegistry,
                                          @Value("${warehouse.url}") String warehouseUrl) {
        return RestClient.builder()
                .baseUrl(warehouseUrl)
                .observationRegistry(observationRegistry)
                .build();
    }

    @Bean
    public ManagedChannel shippingChannel(@Value("${shipping.url}") String shippingUrl) {
        return ManagedChannelBuilder.forTarget(shippingUrl).usePlaintext().build();
    }

    @Bean
    public ShippingServiceGrpc.ShippingServiceBlockingStub shippingStub(
            ManagedChannel shippingChannel, OpenTelemetry openTelemetry) {
        ClientInterceptor tracingInterceptor = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        openTelemetry.getPropagators().getTextMapPropagator()
                                .inject(Context.current(), headers,
                                        (carrier, key, value) -> carrier.put(
                                                Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
                        super.start(responseListener, headers);
                    }
                };
            }
        };
        return ShippingServiceGrpc.newBlockingStub(shippingChannel).withInterceptors(tracingInterceptor);
    }
}
