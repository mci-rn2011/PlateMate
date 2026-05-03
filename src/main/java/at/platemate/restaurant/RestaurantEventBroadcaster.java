package at.platemate.restaurant;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

@Service
public class RestaurantEventBroadcaster {

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public Registration subscribe(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void publish(Restaurant restaurant) {
        listeners.forEach(Runnable::run);
    }

    @FunctionalInterface
    public interface Registration {
        void unregister();
    }
}
