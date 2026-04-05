package com.example.store.step03.tracing;

import java.util.stream.Collectors;

import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.content.Content;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Adds prompt and completion content as high-cardinality OTel span attributes.
 *
 * Spring AI 2.0.0-M4 only logs these via log-prompt/log-completion — it does not
 * include them as span attributes. This convention extends the default to add:
 *   gen_ai.prompt      — the full prompt text sent to the model
 *   gen_ai.completion  — the full completion text returned by the model
 *
 *   Check very useful article: https://jettro.dev/reliable-ai-with-spring-ai-guardrails-observability-and-evaluations-f1fa08e67a4b
 */
@Configuration
public class ChatObservationConventionConfig {

    @Bean
    public ChatModelObservationConvention promptAndCompletionObservationConvention() {
        return new DefaultChatModelObservationConvention() {

            @Override
            public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
                KeyValues keyValues = super.getHighCardinalityKeyValues(context);

                // Add prompt content
                if (!CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
                    String prompt = context.getRequest().getInstructions().stream()
                            .map(Content::getText)
                            .collect(Collectors.joining("\n"));
                    if (StringUtils.hasText(prompt)) {
                        keyValues = keyValues.and("gen_ai.prompt", prompt);
                    }
                }

                // Add completion content
                if (context.getResponse() != null && !CollectionUtils.isEmpty(context.getResponse().getResults())) {
                    String completion = context.getResponse().getResults().stream()
                            .filter(g -> g.getOutput() != null && StringUtils.hasText(g.getOutput().getText()))
                            .map(g -> g.getOutput().getText())
                            .collect(Collectors.joining("\n"));
                    if (StringUtils.hasText(completion)) {
                        keyValues = keyValues.and("gen_ai.completion", completion);
                    }
                }

                return keyValues;
            }
        };
    }

}
