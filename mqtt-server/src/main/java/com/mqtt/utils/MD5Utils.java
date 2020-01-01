package com.mqtt.utils;

import java.security.MessageDigest;

/**
 * @Author: chihaojie
 * @Date: 2019/9/2 19:12
 * @Version 1.0
 * @Note
 */
public class MD5Utils {

    private static final String SALT ="02j3vmlb4o53cu2ufaxpsdp72g8uva";

    public static String encode(String password) {
        password = password + SALT;
        return processEncode(password);
    }

    public static String processEncode(String password) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        char[] charArray = password.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++)
            byteArray[i] = (byte) charArray[i];
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }

            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    public static void main(String[] args) {
        System.out.println(MD5Utils.encode("abel"));
        System.out.println(MD5Utils.encode("admin"));
    }
}
