package com.cjun.rtsp;


import org.junit.Test;

import java.util.Stack;


public class RTSPClientTest {
    RTSPClient rtspClient;//= new RTSPClient("rtsp://192.168.199.243:8554/yourname");

    @Test
    public void testGenerateSessionId() {
        String sessionId = rtspClient.generateSessionId();
        System.out.println(sessionId);
        assert sessionId.length() == RTSPClient.SESSION_ID_LENGHT;
    }

    @Test
    public void testRtspClient() {

    }

}
