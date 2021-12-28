package xyz.shnulaa.udp;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Constant {
    public static final int md5Length = 32;
    public static final int headLength = 36;
    public static final int bodyLength = 1024;


    public static String INPUT_FILE_FULL_PATH = "d:\\test\\.bak_4.log";
    //    public static final String INPUT_FILE_FULL_PATH = "e:\\vm\\openwrt-x86-64-combined-ext4.img";
//    public static final String INPUT_FILE_FULL_PATH = "e:\\vm\\ubuntu-20.04.2-live-server-amd64.iso";
//    public static final String OUTPUT_FILE_FULL_PATH = "d:\\test\\" + UUID.randomUUID().toString();
    public static String OUTPUT_FILE_FULL_PATH = "/tmp/" + UUID.randomUUID().toString();

    // 9999 -> communication | other -> data transfer
    public static final List<Integer> PORTS = Arrays.asList(9999, 10000, 10001, 10002, 10003, 10004, 10005, 10006);


}
