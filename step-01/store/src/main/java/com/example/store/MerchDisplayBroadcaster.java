package com.example.store;

import com.example.store.model.MerchItem;
import com.vaadin.flow.component.UI;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class MerchDisplayBroadcaster {

    private final Map<Integer, UI> uis = new ConcurrentHashMap<>();
    private final Map<Integer, Consumer<List<MerchItem>>> callbacks = new ConcurrentHashMap<>();

    public void register(UI ui, Consumer<List<MerchItem>> callback) {
        uis.put(ui.getUIId(), ui);
        callbacks.put(ui.getUIId(), callback);
    }

    public void unregister(UI ui) {
        uis.remove(ui.getUIId());
        callbacks.remove(ui.getUIId());
    }

    public void broadcast(List<MerchItem> items) {
        callbacks.forEach((uiId, callback) -> {
            UI ui = uis.get(uiId);
            if (ui != null) {
                ui.access(() -> callback.accept(items));
            }
        });
    }
}
