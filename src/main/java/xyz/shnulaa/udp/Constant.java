package xyz.shnulaa.udp;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Constant {

    public static final int TOTAL_BODY_LENGTH = 10;
    public static final int MD5_LENGTH = 32; // UUID array to MD5
    public static final int UUID_HEAD_LENGTH = 36; // UUID length
    public static final int BODY_LENGTH = 1024; // body size
    public static final int SEND_PER_PACKAGE = 20;


    public static String INPUT_FILE_FULL_PATH = "d:\\test\\.bak_4.log";
    //    public static final String INPUT_FILE_FULL_PATH = "e:\\vm\\openwrt-x86-64-combined-ext4.img";
//    public static final String INPUT_FILE_FULL_PATH = "e:\\vm\\ubuntu-20.04.2-live-server-amd64.iso";
//    public static final String OUTPUT_FILE_FULL_PATH = "d:\\test\\" + UUID.randomUUID().toString();
    public static String OUTPUT_FILE_FULL_PATH = "/tmp/" + UUID.randomUUID().toString();

    // 9999 -> communication | other -> data transfer
    public static final List<Integer> PORTS = Arrays.asList(9999, 10000, 10001, 10002, 10003, 10004, 10005, 10006);


}
