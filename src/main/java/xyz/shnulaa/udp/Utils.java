package xyz.shnulaa.udp;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

public class Utils {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static final boolean IS_LOG_ENABLE = true;
    private static final boolean IS_LOG_ERROR_ENABLE = true;

    public static String bytesToHex(byte[] bytes, int nRead) {
        if (nRead < bytes.length) {
            byte[] after = Arrays.copyOfRange(bytes, 0, nRead);
            return bytesToHex(after);
        } else {
            return bytesToHex(bytes);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String getMD5Str(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBytes(StandardCharsets.UTF_8));

            //converting byte array to Hexadecimal String
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }

            digest = sb.toString();

        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
//            Logger.getLogger(StringReplace.class.getName()).log(Level.SEVERE, null, ex);
        }
        return digest;
    }


    public static void error(String message) {
        if (IS_LOG_ERROR_ENABLE) {
            System.err.println(message);
        }
    }

    public static void log(String message) {
        if (IS_LOG_ENABLE) {
            System.out.println(message);
        }
    }

    public static String fetchContent(ByteBuffer byteBuffer, SelectionKey sk) throws IOException {
        DatagramChannel datagramChannel = (DatagramChannel) sk.channel();


//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        int numRead = 0;
//        while (numRead >= 0) {
////            byteBuffer.rewind();
//            DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
//            numRead = datagramChannel.read(byteBuffer);
//            byteBuffer.rewind();
//
//            outputStream.write(byteBuffer.array());
//
//        }
//        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);


//        DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);

        SocketAddress sa = datagramChannel.receive(byteBuffer);
        byteBuffer.flip();
        // 测试：通过将收到的ByteBuffer首先通过缺省的编码解码成CharBuffer 再输出
        CharBuffer charBuffer = Charset.defaultCharset().decode(byteBuffer);
        byteBuffer.clear();
        return charBuffer.toString();
    }

    public static void main(String[] args) {
        System.out.println(getMD5Str("aaaaaaaaaaaaaaaa111111111111111111111111111111111111111a"));
    }
}
