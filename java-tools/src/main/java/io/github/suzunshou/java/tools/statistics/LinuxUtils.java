package io.github.suzunshou.java.tools.statistics;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * for statistics pps & bytes
 */
public class LinuxUtils {
    private static final Logger log = LoggerFactory.getLogger(LinuxUtils.class);

    /**
     * 获取每秒的网卡流量
     *
     * @param net 网卡，如：eth0
     * @return
     */
    public static long getTotalBytesPerSec(String net) {
        return getPerSec(net, BYTES, BYTES_RX, BYTES_TX);
    }

    /**
     * 获取每秒的PPS
     *
     * @param net 网卡，如：eth0
     * @return
     */
    public static long getTotalPacketsPerSec(String net) {
        return getPerSec(net, PPS, PPS_RX, PPS_TX);
    }

    private static long getPerSec(String net, String type, String rx, String tx) {
        File file = new File(BASE_PATH);
        if (!file.exists() || !file.isDirectory()) {
            return 0;
        }

        long rxFirst = getRxNow(net, rx);
        long txFirst = getTxNow(net, tx);

        if (rxFirst == 0 || txFirst == 0) {
            return 0;
        }

        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            log.warn("get {} InInterruptedException", type, e);
            Thread.currentThread().interrupt();
        }

        long rxSecond = getRxNow(net, rx);
        long txSecond = getTxNow(net, tx);

        if (rxSecond == 0 || txSecond == 0 || txSecond < txFirst || rxSecond < rxFirst) {
            return 0;
        }

        long result = (txSecond - txFirst) + (rxSecond - rxFirst);
        log.info("get {} PerSec:{},txSec:{},rxSec:{}", type, result, txSecond - txFirst, rxSecond - rxFirst);
        return result;
    }

    private static long getTxNow(String net, String tx) {
        File file = new File(BASE_PATH + net + "/statistics/" + tx);
        String result = readFromFile(file);

        if (StringUtils.isEmpty(result)) {
            return 0;
        }

        return Long.parseLong(result.trim());
    }

    private static long getRxNow(String net, String rx) {
        File file = new File(BASE_PATH + net + "/statistics/" + rx);

        String result = readFromFile(file);

        if (StringUtils.isEmpty(result)) {
            return 0;
        }

        return Long.parseLong(result.trim());
    }

    private static String readFromFile(File file) {
        if (!file.exists()) {
            return null;
        }
        String res = "";
        try {
            List<String> lines = Files.readLines(file, Charsets.UTF_8);
            res = lines.get(0);
        } catch (IOException e) {
            log.warn("read file error", e);
        }
        return res;
    }

    private static final String BASE_PATH = "/sys/class/net/";
    private static final String PPS = "pps";
    private static final String BYTES = "bytes";
    private static final String PPS_TX = "tx_packets";
    private static final String PPS_RX = "rx_packets";
    private static final String BYTES_TX = "tx_bytes";
    private static final String BYTES_RX = "rx_bytes";
}
