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

public class ChannelPool {

    public List<Selector> getSelectors() {
        return selectors;
    }

    private final List<DatagramChannel> clientChannels;
    private final List<Selector> selectors;

    private final DatagramChannel channelCommunication;


    private final Selector selectorCommunication;

    // 9999 -> communication | other -> data transfer
    private static final List<Integer> ports = Arrays.asList(9999, 10000, 10001, 10002, 10003, 10004, 10005, 10006);

    private ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();


    private ConcurrentHashMap<String, BlockingQueue<String>> queue = new ConcurrentHashMap<>();

    //    private BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);

    private ExecutorService transferService;
    private ExecutorService aggregationService;

    private CharBuffer charBuffer;

    /**
     *
     */
    public ChannelPool() {
        List<DatagramChannel> channels = initClientPool("localhost");
        assert !channels.isEmpty();
        List<Selector> selectors = initServerPool();
        assert !selectors.isEmpty();

        this.channelCommunication = channels.iterator().next();
        this.selectorCommunication = selectors.iterator().next();
        this.clientChannels = channels.stream().skip(1).collect(Collectors.toList());
        this.selectors = selectors.stream().skip(1).collect(Collectors.toList());

        this.transferService = Executors.newFixedThreadPool(8);
        this.aggregationService = Executors.newFixedThreadPool(1);
    }

    /**
     * initClientPool
     *
     * @return
     */
    private List<DatagramChannel> initClientPool(String ip) {
        return ports.stream().map(p -> {
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
//        System.out.println(hex);
        send(hex);
    }

    public void send(InputStream is) throws IOException {
        int nRead;
        byte[] data = new byte[Constant.bodyLength * 400];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            String hex = Utils.bytesToHex(data, nRead);
            send(hex);
        }
    }

    private List<Selector> initServerPool() {
        return ports.stream().map(p -> {
            try {
                DatagramChannel channel = DatagramChannel.open();
                channel.socket().bind(new InetSocketAddress(p));
                channel.configureBlocking(false);
                Selector selector = Selector.open();
                channel.register(selector, SelectionKey.OP_READ);
                return selector;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());


    }

    /**
     * SingletonHolder
     *
     * @author luyil
     */
    static class SingletonHolder {
        public static final ChannelPool channelPool = new ChannelPool();
    }

    public static ChannelPool getInstance() {
        return SingletonHolder.channelPool;
    }


    public static void main(String[] args) throws InterruptedException {
        System.out.println("7bd3479c-1e7a-4469-b575-5ed06619aea1".length());
        ChannelPool channelPool = ChannelPool.getInstance();

        try {
            Stream.of(channelPool.getSelectorCommunication()).map(CommunicationWorker::new).forEach(channelPool.getTransferService()::submit);
            channelPool.getSelectors().stream().map(HandleBodyWorker::new).forEach(channelPool.getTransferService()::submit);
            try (InputStream inputStream = new FileInputStream(new File(Constant.INPUT_FILE_FULL_PATH));) {
                System.out.println("send...........");
                channelPool.send(inputStream);
            }

            channelPool.getTransferService().awaitTermination(1, TimeUnit.DAYS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (channelPool.getTransferService() != null) {
                channelPool.getTransferService().shutdown();
            }

            if (channelPool.getAggregationService() != null) {
                channelPool.getAggregationService().shutdown();
            }
        }
    }


    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }


    public Map<String, BlockingQueue<String>> getQueue() {
        return queue;
    }


    public ConcurrentHashMap<String, String> getMap() {
        return map;
    }

    public Selector getSelectorCommunication() {
        return selectorCommunication;
    }


    public CharBuffer initCharBuffer(int size) {
        if (charBuffer == null) {
            charBuffer = CharBuffer.allocate(size);
        }
        return charBuffer;
    }

    public void clearCharBuffer() {
        if (charBuffer != null) {
            charBuffer.clear();
        }
    }


    public ExecutorService getTransferService() {
        return transferService;
    }

    public ExecutorService getAggregationService() {
        return aggregationService;
    }


}
