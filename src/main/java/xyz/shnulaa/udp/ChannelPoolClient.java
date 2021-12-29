package xyz.shnulaa.udp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static xyz.shnulaa.udp.Constant.SEND_PER_PACKAGE;
import static xyz.shnulaa.udp.Constant.TOTAL_BODY_LENGTH;

public class ChannelPoolClient {
    private List<DatagramChannel> clientChannels;
    private DatagramChannel channelCommunication;
    private String ip;


    /**
     * ChannelPool
     */
    public ChannelPoolClient() {
    }

    public ChannelPoolClient init(String ip) {
        this.ip = ip;
        List<DatagramChannel> channels = initClientPool(ip);
        assert !channels.isEmpty();
        this.channelCommunication = channels.iterator().next();
        this.clientChannels = channels.stream().skip(1).collect(Collectors.toList());
        return this;
    }

    /**
     * initClientPool
     *
     * @return
     */
    private List<DatagramChannel> initClientPool(String ip) {
        return Constant.PORTS.stream().map(p -> {
            try {
                DatagramChannel channel = DatagramChannel.open();
                SocketAddress sa = new InetSocketAddress(ip, p);
                channel.connect(sa);
                return channel;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * releaseClientPool
     */
    public void releaseClientPool() {
        for (DatagramChannel clientChannel : this.clientChannels) {
            try {
                clientChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 1,2,3,4,5   1,2,3,4
    public void send(String data) {
        String[] array = data.split("(?<=\\G.{" + Constant.BODY_LENGTH + "})");
        List<String> uuIdList =
                IntStream.rangeClosed(1, array.length).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList());
        String uuidStr = null;
        try {
            uuidStr = String.join(",", uuIdList);
            int totalBodyLength = Stream.of(array).mapToInt(String::length).sum();
            String all = String.format("%" + TOTAL_BODY_LENGTH + "s", totalBodyLength) + uuidStr;
            channelCommunication.write(Charset.defaultCharset().encode(all));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String md5 = Utils.getMD5Str(uuidStr);

        for (int index = 0; index < array.length; index++) {
            try {
                int chanelIndex = index % this.clientChannels.size();
                String total = md5 + uuIdList.get(index) + array[index];
//                System.out.println("md5:" + md5 + ", uuid:" + uuidStr);
//                Utils.log("send -> index:" + index + ", total size:" + total.length());
                this.clientChannels.get(chanelIndex).write(Charset.defaultCharset().encode(total));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(byte[] body) {
        String hex = Utils.bytesToHex(body, body.length);
        send(hex);
    }

    public void send(InputStream is) throws IOException {
        int nRead;
        byte[] data = new byte[Constant.BODY_LENGTH * SEND_PER_PACKAGE];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            String hex = Utils.bytesToHex(data, nRead);
            send(hex);
        }
    }


    /**
     * SingletonHolder
     *
     * @author luyil
     */
    static class SingletonHolder {
        public static final ChannelPoolClient channelPool = new ChannelPoolClient();
    }

    public static ChannelPoolClient getInstance() {
        return SingletonHolder.channelPool;
    }

    public static void main(String[] args) {
//        System.out.println("Converting integer " + 1 + " to Hex String: " + String.format("%10s", 1));
//        System.out.println("Converting integer " + Integer.MAX_VALUE + " to Hex String: " + String.format("%10s", Integer.MAX_VALUE));
//
//        Integer.parseInt(String.format("%10s", 1).trim());

        CharBuffer charBuffer = CharBuffer.allocate(10);
        charBuffer.position(8);
        charBuffer.put("11");

        charBuffer.position(2);
        charBuffer.put("22");
//        charBuffer.put("12", 3, 5);
        System.out.println(charBuffer.remaining());
        System.out.println(charBuffer.remaining());
    }


}
