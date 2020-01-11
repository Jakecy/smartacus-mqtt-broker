package com.mqtt.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: chihaojie
 * @Date: 2020/1/3 9:52
 * @Version 1.0
 * @Note
 */
public class StrUtil {


    /**
     * ByteBuf 2 String
     */
    public static  final String   ByteBuf2String( ByteBuf buf){
        try{
            byte[] bytes = new byte[buf.readableBytes()];
            int readerIndex = buf.readerIndex();
            buf.getBytes(readerIndex, bytes);
            return new String(bytes,0,bytes.length,"UTF-8");
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * String 2 ByteBuf
     * @param str
     * @return
     */
    public static  final  ByteBuf  String2ByteBuf(String str){
       return Unpooled.copiedBuffer(str, CharsetUtil.UTF_8);
    }


    /**
     * 字节数组转换为字符串
     *
     * @return
     */
    public static String byteToStr(byte[] byt) throws UnsupportedEncodingException {
        String strRead = new String(byt, "UTF-8");
        return strRead;
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param src
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String bytes2Hex(byte[] src) throws UnsupportedEncodingException {
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] res = new char[src.length * 2]; // 每个byte对应两个字符
        final char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        for (int i = 0, j = 0; i < src.length; i++) {
            res[j++] = hexDigits[src[i] >> 4 & 0x0f]; // 先存byte的高4位
            res[j++] = hexDigits[src[i] & 0x0f]; // 再存byte的低4位
        }

        return decode(new String(res), "UTF-8");
    }

    /**
     * 将16进制数字解码成字符串,适用于所有字符（包括中文）
     */
    public static String decode(String bytes, String charset) throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length() / 2);
        final String hexString = "0123456789abcdef";
        //将每2位16进制整数组装成一个字节
        for (int i = 0; i < bytes.length(); i += 2)
            baos.write((hexString.indexOf(bytes.charAt(i)) << 4 | hexString.indexOf(bytes.charAt(i + 1))));
        return new String(baos.toByteArray(), charset);
    }

    /**
     * 字符串转换为字节数组
     * @param str
     * @return
     */
    public static byte[] strToByte(String str) {
        byte[] byBuffer = new byte[200];
        String strInput = str;
        byBuffer = strInput.getBytes();
        return byBuffer;
    }

    /**
     * 将URL编码转化为字符串
     * @param str
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String strToDecoder(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return str;
        }
    }

    /**
     * 将字符串转化为URL编码
     * @param str
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String strToEncoder(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    /**
     * 去掉字符串中的空格、回车、换行符、制表符
     * @param str
     * @return
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (str != null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n|");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
            // 将ASCII的值为160的替换为空字符串
            dest = dest.replace(backStr(160), "");
        }
        return dest;
    }

    /**
     * 去掉字符串中的空格、回车、换行符、制表符、斜杠、点、冒号
     * @param str
     * @return
     */
    public static String replaceBlanks(String str) {
        str = replaceBlank(str);
        String dest = "";
        if (str != null) {
            Pattern p = Pattern.compile("/|\\.|\\:|");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

    /**
     * 字符转ASC
     *
     * @param st
     * @return
     */
    public static int getAsc(String st) {
        byte[] gc = st.getBytes();
        int ascNum = (int) gc[0];
        return ascNum;
    }

    /**
     * ASC转字符
     * @param backnum
     * @return
     */
    public static char backchar(int backnum) {
        char strChar = (char) backnum;
        return strChar;
    }

    /**
     * ASC转字符串
     * @param
     */
    public static String backStr(int backnum) {
        char strChar = (char) backnum;
        return String.valueOf(strChar);
    }

    /**
     * 将Object转换为String
     * @param o
     * @return
     */
    public static String objToString(Object o) {
        if (o == null) {
            return "";
        }
        return o.toString();
    }

    /**
     * 判断对象是否为空
     * @param o
     * @return
     */
    public static boolean isEmpty(Object o) {
        boolean result = false;
        if (o == null) {
            result = true;
        } else {
            if ("".equals(o.toString())) {
                result = true;
            }
        }
        return result;
    }

    /**
     * encode by Base64
     */
    public static String encodeBase64(byte[] input) throws Exception {
        Class clazz = Class.forName("com.sun.org.apache.xerces.internal.impl.dv.util.Base64");
        Method mainMethod = clazz.getMethod("encode", byte[].class);
        mainMethod.setAccessible(true);
        Object retObj = mainMethod.invoke(null, new Object[]{input});
        return (String) retObj;
    }

    /**
     * decode by Base64
     */
    public static byte[] decodeBase64(String input) throws Exception {
        Class clazz = Class.forName("com.sun.org.apache.xerces.internal.impl.dv.util.Base64");
        Method mainMethod = clazz.getMethod("decode", String.class);
        mainMethod.setAccessible(true);
        Object retObj = mainMethod.invoke(null, input);
        return (byte[]) retObj;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 将指定byte数组以16进制的形式打印到控制台
     */
    public static void printHexString(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            System.out.print(hex.toUpperCase());
        }
    }

    /**
     * 将list集合转化为like查询语句    * @param names    * @return
     */
    public static String listToLikeString(List<String> names) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (int i = 0; i < names.size(); i++) {
            sb.append("'");
            sb.append(names.get(i));
            sb.append("'");
            if (i != names.size() - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 根据某个字符对字符串进行分割
     * @param str
     * @return
     */
    public static String[] spilt(String str, String reg) {
        str = replaceBlank(str);
        String[] spilt = str.split(reg);
        return spilt;
    }

    /**
     * 根据逗号进行分割
     * @param str
     * @return
     */
    public static String[] spilt(String str) {
        return spilt(str, ",");
    }
}
