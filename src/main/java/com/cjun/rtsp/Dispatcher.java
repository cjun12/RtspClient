package com.cjun.rtsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Dispatcher {
    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private Selector selector;
    private ConcurrentHashMap<SocketChannel, SelectionKey> clients = new ConcurrentHashMap<>();
    private static Dispatcher sInstance;
    private ExecutorService executorService;
    private Runnable dispatcher = () -> {
        logger.debug("Dispatcher Start ...");
        try {
            while (!clients.isEmpty()) {
                int n = selector.select(2000);
                if (n > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    for (Iterator<SelectionKey> iterator = keys.iterator(); iterator.hasNext(); ) {
                        SelectionKey selectionKey = iterator.next();
                        if (!selectionKey.isValid()) {
                            iterator.remove();
                            continue;
                        }

                        ISocketEventListener event = (ISocketEventListener) selectionKey.attachment();
                        if (selectionKey.isConnectable()) {
                            event.onConnected();
                        } else if (selectionKey.isReadable()) {
                            event.onRead();
                        } else if (selectionKey.isWritable()) {
                            event.onWrite();
                        }
                        iterator.remove();
                    }
                }
                Thread.sleep(500);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("Dispatcher End ...");
    };

    private Dispatcher() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void register(SocketChannel channel, int ops, ISocketEventListener listener) throws ClosedChannelException {
        if (channel.isBlocking()) {
            throw new IllegalBlockingModeException();
        }
        SelectionKey key = channel.register(selector, ops, listener);

        clients.put(channel, key);

        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(dispatcher);
        }
        logger.debug("Register Success ...");
    }

    public void unregister(SocketChannel channel) {
        clients.get(channel).cancel();
        clients.remove(channel);
    }


    public static Dispatcher getInstance() {
        if (sInstance == null) {
            synchronized (Dispatcher.class) {
                if (sInstance == null) {
                    sInstance = new Dispatcher();
                }
            }
        }
        return sInstance;
    }

    public void stop() {
        try {
            selector.close();
            executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
