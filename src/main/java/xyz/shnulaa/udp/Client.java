package xyz.shnulaa.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client {
//    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//    private boolean wakeup = false;

    private static final List<Integer> ports = Arrays.asList(10000, 10001, 10002);

    public static void main(String[] args) throws IOException {
        final List<DatagramChannel> channels = initChannel();
        if (channels.isEmpty()) {
            System.err.println("init Channel error.");
            return;
        }



        try (DatagramChannel channel = DatagramChannel.open();) {
            SocketAddress sa = new InetSocketAddress("localhost", 9999);
            channel.connect(sa);

            for (int i = 0; i < 100; i++) {
                channel.write(Charset.defaultCharset().encode(String.valueOf(i)));
            }
        }


    }

    private static List<DatagramChannel> initChannel() throws IOException {
        return ports.stream().map(p -> {
            try {
                DatagramChannel channel = DatagramChannel.open();
                SocketAddress sa = new InetSocketAddress("localhost", p);
                channel.connect(sa);
                return channel;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

    }
}
