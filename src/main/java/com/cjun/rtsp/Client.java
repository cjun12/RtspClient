package com.cjun.rtsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Client implements ISocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private static final int BUFF_SIZE = 4096;

    private SocketChannel channel;
    private ByteBuffer receiveBuf;
    private ByteBuffer sendBuff;

    private String host;
    private int port;

    private final static Object lock = new Object();

    public Client() {
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.socket().setSoTimeout(5 * 1000);
            receiveBuf = ByteBuffer.allocate(BUFF_SIZE);
            sendBuff = ByteBuffer.allocate(BUFF_SIZE);
            Dispatcher.getInstance().register(channel, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        return channel.connect(new InetSocketAddress(host, port));
    }

    public void send(String str) {
        sendBuff.clear();
        sendBuff.put(str.getBytes());
        sendBuff.flip();
        synchronized (lock) {
            try {
                if (!channel.isConnected()) {
                    logger.debug("not connected");
                    lock.wait(5000);
                }
                channel.write(sendBuff);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                try {
                    channel.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        sendBuff.clear();
        logger.debug("send complete:\n{}", str);
    }

    public byte[] receive() {
        byte[] bytes = null;
        try {
            int n = channel.read(receiveBuf);
            bytes = new byte[n];
            if (n > 0) {
                receiveBuf.flip();
                if (receiveBuf.hasRemaining()) {
                    System.arraycopy(receiveBuf.array(), 0, bytes, 0, n);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    @Override
    public void onConnected() {
        logger.debug("onConnected ...");
        try {
            while (!channel.finishConnect()) {
                connect(host, port);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Dispatcher.getInstance().unregister(channel);
            synchronized (lock) {
                lock.notifyAll();
                try {
                    channel.close();
                    Dispatcher.getInstance().stop();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onWrite() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void onRead() {
        logger.debug("onRead ...");
    }
}
