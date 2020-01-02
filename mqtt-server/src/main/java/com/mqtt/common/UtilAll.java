package com.mqtt.common;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * @Author: chihaojie
 * @Date: 2020/1/2 16:22
 * @Version 1.0
 * @Note
 */
public class UtilAll {

    public static int getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName(); // format: "pid@hostname"
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
            return -1;
        }
    }

    public static void sleep(long sleepMs) {
        if (sleepMs < 0) {
            return;
        }
        try {
            Thread.sleep(sleepMs);
        } catch (Throwable ignored) {

        }

    }

    public static String currentStackTrace() {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            sb.append("\n\t");
            sb.append(ste.toString());
        }

        return sb.toString();
    }
}
