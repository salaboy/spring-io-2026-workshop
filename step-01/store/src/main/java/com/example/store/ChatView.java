package com.example.store;

import com.example.store.model.MerchItem;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Route("")
public class ChatView extends Composite<VerticalLayout> {

    private static final String SYSTEM_PROMPT = """
            You are a helpful store assistant for the Spring Merch store.
            You help customers find products and create orders.
            Use the available tools to look up inventory information when asked.
            When the user asks to see or browse items, use the displayMerchImages tool to show visual cards.
            Be concise and friendly in your responses.
            Allow the user to add products to the order, and print the order content if the user requests it.
            """;

    private final VerticalLayout catalogContainer;
    private final FlexLayout cardsLayout;
    private final MessageInput messageInput;
    private final MessageList messageList;
    private final List<MessageListItem> messages = new ArrayList<>();
    private final ChatClient chatClient;

    public ChatView(ChatClient.Builder chatClientBuilder, ChatController inventoryTools, MerchDisplayBroadcaster broadcaster) {
        var welcome = new MessageListItem(
                "Welcome to the Spring Store! Here you can order merch from the different Spring projects. " +
                "Ask in the chat if you are interested about merch for a specific project, " +
                "add them to your order and we will ship it to you 🌱",
                null, "Spring Store");
        welcome.setUserColorIndex(2);
        messages.add(welcome);

        messageList = new MessageList();
        messageList.setItems(welcome);
        messageList.setSizeFull();

        messageList.getElement().executeJs("""
                const observer = new MutationObserver(() => {
                    this.scrollTop = this.scrollHeight;
                });
                observer.observe(this, { childList: true, subtree: true });
                """);

        messageInput = new MessageInput();
        messageInput.setWidthFull();

        cardsLayout = new FlexLayout();
        cardsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cardsLayout.getStyle()
                .set("gap", "12px")
                .set("padding", "8px 0 4px 0")
                .set("overflow-y", "auto")
                .set("max-height", "260px");
        cardsLayout.setWidthFull();

        catalogContainer = buildCatalogContainer();

        chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(inventoryTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(
                        MessageWindowChatMemory.builder()
                                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                                .build())
                        .build())
                .build();

        messageInput.addSubmitListener(e -> handleUserMessage(e.getValue()));

        getContent().setSizeFull();
        getContent().addAndExpand(messageList);
        getContent().add(catalogContainer);
        getContent().add(messageInput);

        UI ui = UI.getCurrent();
        addAttachListener(e -> broadcaster.register(ui, this::showMerchCards));
        addDetachListener(e -> broadcaster.unregister(ui));
    }

    private void handleUserMessage(String userText) {
        var userItem = new MessageListItem(userText, Instant.now(), "You");
        userItem.setUserColorIndex(1);
        messages.add(userItem);
        messageList.setItems(messages.toArray(MessageListItem[]::new));

        UI ui = UI.getCurrent();
        CompletableFuture.supplyAsync(() ->
                chatClient.prompt()
                        .user(userText)
                        .call()
                        .content()
        ).thenAccept(response ->
                ui.access(() -> {
                    var assistantItem = new MessageListItem(response, Instant.now(), "Spring Store");
                    assistantItem.setUserColorIndex(2);
                    messages.add(assistantItem);
                    messageList.setItems(messages.toArray(MessageListItem[]::new));
                })
        ).exceptionally(ex -> {
            ui.access(() -> {
                var errorItem = new MessageListItem(
                        "Sorry, something went wrong. Please try again.",
                        Instant.now(), "Spring Store");
                errorItem.setUserColorIndex(2);
                messages.add(errorItem);
                messageList.setItems(messages.toArray(MessageListItem[]::new));
            });
            return null;
        });
    }

    private VerticalLayout buildCatalogContainer() {
        var title = new Span("🛍️ Spring Merch Catalog");
        title.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "14px")
                .set("color", "#3d3d3d");

        var closeButton = new Button("✕");
        closeButton.getStyle()
                .set("background", "none")
                .set("border", "none")
                .set("cursor", "pointer")
                .set("font-size", "16px")
                .set("color", "#888")
                .set("padding", "0 4px")
                .set("line-height", "1");
        closeButton.addClickListener(e -> catalogContainer.setVisible(false));

        var header = new HorizontalLayout(title, closeButton);
        header.setWidthFull();
        header.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.getStyle().set("padding", "0");

        var container = new VerticalLayout(header, cardsLayout);
        container.setPadding(false);
        container.setSpacing(false);
        container.getStyle()
                .set("padding", "10px 12px 8px 12px")
                .set("background", "#f8f9fa")
                .set("border-top", "1px solid #e0e0e0");
        container.setWidthFull();
        container.setVisible(false);
        return container;
    }

    private void showMerchCards(List<MerchItem> items) {
        cardsLayout.removeAll();
        items.forEach(item -> cardsLayout.add(createMerchCard(item)));
        catalogContainer.setVisible(!items.isEmpty());
    }

    private void submitToChat(String message) {
        messageInput.getElement().executeJs(
                "this.dispatchEvent(new CustomEvent('submit', { detail: { value: $0 }, bubbles: true, composed: true }))",
                message
        );
    }

    private Div createMerchCard(MerchItem item) {
        var card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "10px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.12)")
                .set("padding", "14px")
                .set("width", "150px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "8px")
                .set("text-align", "center")
                .set("cursor", "pointer")
                .set("transition", "transform 0.15s, box-shadow 0.15s");

        card.getElement().executeJs(
                "this.addEventListener('mouseenter', () => { this.style.transform='scale(1.05)'; this.style.boxShadow='0 4px 16px rgba(0,0,0,0.2)'; });" +
                "this.addEventListener('mouseleave', () => { this.style.transform=''; this.style.boxShadow='0 2px 8px rgba(0,0,0,0.12)'; });"
        );

        var logo = new Image(item.logoUrl(), item.projectName() + " logo");
        logo.setWidth("64px");
        logo.setHeight("64px");
        logo.getStyle().set("object-fit", "contain");

        var typeEmoji = switch (item.type()) {
            case "T-Shirt" -> "👕";
            case "Socks"   -> "🧦";
            case "Sticker" -> "🏷️";
            default        -> "📦";
        };

        var typeBadge = new Span(typeEmoji + " " + item.type());
        typeBadge.getStyle()
                .set("font-size", "12px")
                .set("font-weight", "bold")
                .set("background", "#6db33f")
                .set("color", "white")
                .set("padding", "2px 8px")
                .set("border-radius", "12px");

        var projectName = new Span(item.projectName());
        projectName.getStyle()
                .set("font-size", "11px")
                .set("color", "#555")
                .set("font-weight", "600");

        var price = new Span("$" + String.format("%.2f", item.price()));
        price.getStyle()
                .set("font-size", "14px")
                .set("font-weight", "bold")
                .set("color", "#333");

        var stock = new Span(item.quantity() + " in stock");
        stock.getStyle()
                .set("font-size", "11px")
                .set("color", "#888");

        card.add(logo, typeBadge, projectName, price, stock);
        card.addClickListener(e ->
                submitToChat("Add 1 " + item.projectName() + " " + item.type() + " to my order")
        );
        return card;
    }
}
