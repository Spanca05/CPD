package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ThreadSafeMap<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final Lock lock = new ReentrantLock();

    public V get(K key) {
        lock.lock();
        try {
            return map.get(key);
        } finally {
            lock.unlock();
        }
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            map.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    public V remove(K key) {
        lock.lock();
        try {
            return map.remove(key);
        } finally {
            lock.unlock();
        }
    }

    public boolean containsKey(K key) {
        lock.lock();
        try {
            return map.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    public V getOrCreate(K key, Supplier<V> supplier) {
        lock.lock();
        try {
            V value = map.get(key);

            if (value == null) {
                value = supplier.get();
                map.put(key, value);
            }

            return value;
        } finally {
            lock.unlock();
        }
    }

    public Set<K> keySnapshot() {
        lock.lock();
        try {
            return Set.copyOf(map.keySet());
        } finally {
            lock.unlock();
        }
    }
}
