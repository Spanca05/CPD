package utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeQueue<T> {
    private final Deque<T> queue = new ArrayDeque<>();
    private final Lock lock = new ReentrantLock();

    public void addLast(T item) {
        lock.lock();
        try {
            queue.addLast(item);
        } finally {
            lock.unlock();
        }
    }

    public T removeFirst() {
        lock.lock();
        try {
            return queue.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    public T peekFirst() {
        lock.lock();
        try {
            return queue.peekFirst();
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    public List<T> snapshot() {
        lock.lock();
        try {
            return new ArrayList<>(queue);
        } finally {
            lock.unlock();
        }
    }
}
