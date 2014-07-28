package cucumber.runtime;

import cucumber.runtime.java.ObjectFactory;
import net.thucydides.core.Thucydides;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Thucydides factory for cucumber tests.
 *
 * @author Liviu Carausu (liviu.carausu@gmail.com).
 */

public class ThucydidesObjectFactory implements ObjectFactory {

    private final Set<Class<?>> classes = Collections.synchronizedSet(new HashSet<Class<?>>());

    private final Map<Class<?>, Object> instances = Collections.synchronizedMap(new HashMap<Class<?>, Object>());

    public void start() {
    }

    public void stop() {
        instances.clear();
    }

    public void addClass(Class<?> clazz) {
        classes.add(clazz);
    }

    public <T> T getInstance(Class<T> type) {
        T instance = type.cast(instances.get(type));
        if (instance == null) {
            instance = cacheNewInstance(type);
        }
        return instance;
    }

    private <T> T cacheNewInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getConstructor();
            T instance = constructor.newInstance();
            Thucydides.initializeWithoutStepListener(instance);
            instances.put(type, instance);
            return instance;
        } catch (NoSuchMethodException e) {
            throw new CucumberException(String.format("%s doesn't have an empty constructor.", type), e);
        } catch (Exception e) {
            throw new CucumberException(String.format("Failed to instantiate %s", type), e);
        }
    }
}
