package event;

import java.util.HashMap;
import java.util.Map;
import main.BaseComp;

public class EventManager {
    public interface Action {
        void run(BaseComp component);
    }
    private final Map<String, Action> actions ;
    public EventManager() {
        this.actions = new HashMap<>();
    }

    public void register(String eventName, Action action) {
        this.actions.put(eventName, action);
    }
    public void trigger(String eventName, BaseComp component) {
        Action action = this.actions.get(eventName);
        if (action != null) {
            action.run(component);
        }
    }



}

