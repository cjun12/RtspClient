package com.cjun.rtsp;

public interface ISocketEventListener {
    void onConnected();

    void onRead();

    void onWrite();
}
