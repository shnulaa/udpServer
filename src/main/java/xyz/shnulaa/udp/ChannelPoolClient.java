package xyz.shnulaa.udp;

import xyz.shnulaa.udp.worker.CommunicationWorker;
import xyz.shnulaa.udp.worker.HandleBodyWorker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        Utils.log(data);
//        System.out.println(data.length() / Constant.bodyLength);
        String[] array = data.split("(?<=\\G.{" + Constant.bodyLength + "})");
        List<String> uuIdList =
                IntStream.rangeClosed(1, array.length).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList());
        String uuidStr = null;
        try {
            uuidStr = String.join(",", uuIdList);
//            System.out.println(uuidStr);
            channelCommunication.write(Charset.defaultCharset().encode(uuidStr));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String md5 = Utils.getMD5Str(uuidStr);

        for (int index = 0; index < array.length; index++) {
            try {
                int chanelIndex = index % this.clientChannels.size();
                String total = md5 + uuIdList.get(index) + array[index];
//                System.out.println("md5:" + md5 + ", uuid:" + uuidStr);
                Utils.log("send ->" + total + ", index:" + index);
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
        byte[] data = new byte[Constant.bodyLength * 20];
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


}
