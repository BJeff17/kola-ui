package kola.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kola.main.BaseComp;

public class EventManager {
    @FunctionalInterface
    public interface Action {
        void run(BaseComp component, UiEvent event);
    }

    private final Map<String, Action> legacyActions;
    private final Map<UiEvent.Type, List<Action>> typedActions;

    public EventManager() {
        this.legacyActions = new HashMap<>();
        this.typedActions = new EnumMap<>(UiEvent.Type.class);
    }

    public void register(String eventName, Action action) {
        this.legacyActions.put(eventName, action);
    }

    public void trigger(String eventName, BaseComp component) {
        Action action = this.legacyActions.get(eventName);
        if (action != null) {
            action.run(component, null);
        }
    }

    public void register(UiEvent.Type eventType, Action action) {
        this.typedActions.computeIfAbsent(eventType, key -> new ArrayList<>()).add(action);
    }

    public void trigger(UiEvent event, BaseComp component) {
        if (event == null) {
            return;
        }
        List<Action> handlers = this.typedActions.get(event.getType());
        if (handlers == null) {
            return;
        }
        for (Action handler : handlers) {
            handler.run(component, event);
        }
    }

    public boolean hasHandlers(UiEvent.Type eventType) {
        List<Action> handlers = this.typedActions.get(eventType);
        return handlers != null && !handlers.isEmpty();
    }

}
