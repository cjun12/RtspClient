package com.cjun.rtsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        RTSPClient rtspClient = new RTSPClient("rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov");
//        logger.debug("options ...");
        rtspClient.send(rtspClient.options());
//        logger.debug("describe ...");
        rtspClient.send(rtspClient.describe());
//        logger.debug("setup ...");
        rtspClient.send(rtspClient.setup("trackID=0", "RTP/AVP;unicast;client_port=55111-55112"));
        rtspClient.send(rtspClient.play("npt=0-"));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("end ...");

    }
}
