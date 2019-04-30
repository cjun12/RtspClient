package com.cjun.rtsp;

import com.sun.istack.internal.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RTSPClient extends Client {
    private static final Logger logger = LoggerFactory.getLogger(RTSPClient.class);

    private static final String VERSION = "RTSP/1.0";
    public static final int SESSION_ID_LENGHT = 16;

//
//    private String serverUrl;
//    private String host;
//    private String path;
//    private int port;

    private URI target;

    private int seq;
    private AtomicInteger times = new AtomicInteger(0);

    private DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss 'GMT'");
    private List<RTSPClient.Listener> listeners;

    private MimeType mimeType;
    private String sessionId;

    private boolean isResponseEnd = true;

    public RTSPClient(String url) {
        target = URI.create(url);
        seq = 0;
        try {
            int port = target.getPort() == -1 ? 80 : target.getPort();
            connect(target.getHost(), port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum state {
        SETUP, PLAY, PAUSE, TEARDOWN
    }

    public String generateSessionId() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SESSION_ID_LENGHT >> 1];
        random.nextBytes(bytes);
        int resultLenBytes = 0;
        StringBuilder sb = new StringBuilder(SESSION_ID_LENGHT + 5);
        for (int i = 0; i < bytes.length && resultLenBytes < SESSION_ID_LENGHT; i++) {
            byte b1 = (byte) ((bytes[i] & 0xf0) >> 4);
            byte b2 = (byte) (bytes[i] & 0x0f);
            sb.append((char) (b1 % 10 + '0')).append((char) (b2 % 10 + '0'));
            resultLenBytes++;
        }
        return sb.toString();
    }

    public String options() {
        StringBuilder sb = new StringBuilder();
        sb.append("OPTIONS ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n")
                .append("\r\n");
        return sb.toString();
    }

    public String describe() {
        return describe(null);
    }

    public String describe(String accept) {
        StringBuilder sb = new StringBuilder();
        sb.append("DESCRIBE ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n");
        if (accept != null) {
            sb.append("Accept: ").append(accept).append("\r\n");
        }
        sb.append("\r\n");

        return sb.toString();
    }


    public String setup(String trackId) {
        return setup(trackId, null);
    }

//    public String setup(String accept) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("SETUP ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
//                .append("CSeq: ").append(seq++).append("\r\n")
//                .append("\r\n");
//        if (accept != null) {
//            sb.append("Accept: ").append(accept).append("\r\n");
//        }
//        return sb.toString();
//    }

    public String announce(@NotNull String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("ANNOUNCE ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n")
                .append("Date: ").append(dateFormat.format(new Date())).append("\r\n")
                .append("Session: ").append(sessionId).append("\r\n")
                .append("Content-Type: ").append(mimeType.toString()).append("\r\n")
                .append("Content-Lenght").append(content.getBytes().length).append("\r\n")
                .append("\r\n")
                .append(content);
        return sb.toString();
    }

    public String setup(String trackID, String transport) {
        StringBuilder sb = new StringBuilder();
        sb.append("SETUP ").append(target.toString()).append(trackID).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n");
        if (transport != null) {
            sb.append("Transport: ").append(transport).append("\r\n");
        }
        sb.append("\r\n");
        return sb.toString();
    }

    public String play() {
        return play(null);
    }

    public String play(String range) {
        StringBuilder sb = new StringBuilder();
        sb.append("PLAY ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n")
                .append("Session: ").append(sessionId).append("\r\n");
        if (range != null) {
            sb.append("Range: ").append(range).append("\r\n");
        }
        sb.append("\r\n");
        return sb.toString();
    }

    public String pause() {
        StringBuilder sb = new StringBuilder();
        sb.append("PAUSE ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n")
                .append("Session: ").append(sessionId).append("\r\n")
                .append("\r\n");
        return sb.toString();
    }

    public String teardown() {
        StringBuilder sb = new StringBuilder();
        sb.append("TEARDOWN ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n")
                .append("Session: ").append(sessionId).append("\r\n")
                .append("\r\n");
        return sb.toString();
    }

    public String getParameter(String content) {
        StringBuilder sb = new StringBuilder();
        int length = 0;
        if (content != null) {
            length = content.getBytes().length;
        }
        sb.append("GET_PARAMETER ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n")
                .append("Content-Type: text/parameter\r\n")
                .append("Session: ").append(sessionId).append("\r\n")
                .append("Content-Length: ").append(length).append("\r\n")
                .append("\r\n");
        sb.append(content);
        return sb.toString();
    }

    public String setParameter(String content) {
        StringBuilder sb = new StringBuilder();
        int length = 0;
        if (content != null) {
            length = content.getBytes().length;
        }
        sb.append("SET_PARAMETER ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n")
                .append("Content-Type: text/parameter\r\n")
                .append("Session: ").append(sessionId).append("\r\n")
                .append("Content-Length: ").append(length).append("\r\n")
                .append("\r\n");
        sb.append(content);

        return sb.toString();
    }

    public String record(String url) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET_PARAMETER ").append(target.toString()).append(" ").append(VERSION).append("\r\n")
                .append("CSeq: ").append(seq++).append("\r\n")
                .append("Session: ").append(sessionId).append("\r\n")
                .append("Conference: ").append(url).append("\r\n")
                .append("\r\n");
        return sb.toString();
    }


    public interface Listener {

    }

    public void addListener(Listener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public void onWrite() {
        super.onWrite();
    }

    @Override
    public void onRead() {
        byte[] bytes = receive();
        logger.debug("-----------{}-------------", times.incrementAndGet());
        System.out.println(new String(bytes));
    }
}
