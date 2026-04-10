package com.example.store.step03;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private static final String SYSTEM_PROMPT = """
            You are a helpful store assistant for the Spring Merch store.
            You help customers find products and create orders.
            Use the available tools to look up inventory information when asked.
            When the user asks to see or browse items, use the displayMerchImages tool to show visual cards.
            Be concise and friendly in your responses.
            Allow the user to add products to the order, and print the order content if the user requests it.
            After placing the order you MUST make sure that the order is shipped using the following details 
            shipping address customerId: lbroudoux, city: Le Mans, country: France.

            MERCH DISPLAY RULE:
            When the displayMerchImages tool returns results, you MUST embed a <merch-items> JSON block verbatim in your response.
            Place the <merch-items> block at the start of your response, then add your message after it.
            Do not paraphrase, reformat, or omit the block.

            ORDER CONFIRMATION RULE:
            After the placeOrder tool returns successfully, you MUST embed an <order-placed> block immediately before your confirmation text.
            The block must contain a JSON object with:
              - orderId: the order ID string from the tool result (e.g. "A1B2C3D4")
              - items: array of { name: "<projectName> <type>", quantity: <number>, unitPrice: <number> }
              - total: total price as a number  
            Example:
            <order-placed>{"orderId":"A1B2C3D4","items":[{"name":"Spring Boot T-Shirt","quantity":2,"unitPrice":29.99},{"name":"Spring AI Sticker","quantity":3,"unitPrice":4.99}],"total":74.95}</order-placed>
            Then follow with your friendly confirmation message.
            """;

    private final ChatClient chatClient;
    private final InMemoryChatMemoryRepository memoryRepository = new InMemoryChatMemoryRepository();
    private final ToolCallbackProvider mcpTools;

    public ChatRestController(ChatClient.Builder chatClientBuilder,
                              ToolCallbackProvider mcpTools,
                              ChatController inventoryTools) {
        this.mcpTools = mcpTools;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(inventoryTools)
                .build();
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(
                MessageWindowChatMemory.builder()
                        .chatMemoryRepository(memoryRepository)
                        .build())
                .conversationId(request.conversationId())
                .build();

        String response = chatClient.prompt()
                .advisors(advisor)
                .toolCallbacks(mcpTools)
                .user(request.message())
                .call()
                .content();

        return new ChatResponse(response);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(
                MessageWindowChatMemory.builder()
                        .chatMemoryRepository(memoryRepository)
                        .build())
                .conversationId(request.conversationId())
                .build();

        return chatClient.prompt()
                .advisors(advisor)
                .toolCallbacks(mcpTools)
                .user(request.message())
                .stream()
                .content();
    }

    public record ChatRequest(String conversationId, String message) {}
    public record ChatResponse(String response) {}
}
