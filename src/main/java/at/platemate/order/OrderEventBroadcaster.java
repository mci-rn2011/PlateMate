package at.platemate.order;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

@Service
public class OrderEventBroadcaster {

    private final Map<Long, List<Runnable>> listenersByRestaurant = new ConcurrentHashMap<>();
    private final List<Runnable> globalListeners = new CopyOnWriteArrayList<>();

    public Registration subscribe(Long restaurantId, Runnable listener) {
        listenersByRestaurant.computeIfAbsent(restaurantId, key -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> listenersByRestaurant.getOrDefault(restaurantId, List.of()).remove(listener);
    }

    public Registration subscribeToAll(Runnable listener) {
        globalListeners.add(listener);
        return () -> globalListeners.remove(listener);
    }

    public void publish(CustomerOrder order) {
        if (order == null || order.getRestaurant() == null || order.getRestaurant().getId() == null) {
            return;
        }
        listenersByRestaurant.getOrDefault(order.getRestaurant().getId(), List.of())
                .forEach(Runnable::run);
        globalListeners.forEach(Runnable::run);
    }

    public void publishGlobal() {
        globalListeners.forEach(Runnable::run);
    }

    @FunctionalInterface
    public interface Registration {
        void unregister();
    }
}
