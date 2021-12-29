package xyz.shnulaa.udp;

import xyz.shnulaa.udp.worker.AggregationTakeWorker;
import xyz.shnulaa.udp.worker.CommunicationWorker;
import xyz.shnulaa.udp.worker.HandleBodyWorker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChannelPoolServer {
    public List<Selector> getSelectors() {
        return selectors;
    }

    private final List<Selector> selectors;
    ;
    private final Selector selectorCommunication;

    private ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();


    private ConcurrentHashMap<String, BlockingQueue<String>> queue = new ConcurrentHashMap<>();

    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);


    private ExecutorService communicationService;
    private ExecutorService transferService;
    private ExecutorService aggregationService;

    private CharBuffer charBuffer;

    private AtomicBoolean interuptAWorker;


    ////////////////////////////////////////////////////
    private final Map<String, Value> positionMap;
    private BlockingQueue<Event> blockQueue = new PriorityBlockingQueue<>();


    private final AtomicLong totalIndex = new AtomicLong(1);
    private final AtomicLong retryIndex = new AtomicLong(Integer.MAX_VALUE);

    public BlockingQueue<Event> getBlockQueue() {
        return blockQueue;
    }

    public Map<String, Value> getPositionMap() {
        return positionMap;
    }

    public AtomicLong getTotalIndex() {
        return totalIndex;
    }

    public AtomicLong getRetryIndex() {
        return retryIndex;
    }


    /**
     * ChannelPool
     */
    public ChannelPoolServer() {
        List<Selector> selectors = initServerPool();
        assert !selectors.isEmpty();
        this.interuptAWorker = new AtomicBoolean(false);
        this.positionMap = new ConcurrentHashMap<>();

        this.selectorCommunication = selectors.iterator().next();
        this.selectors = selectors.stream().skip(1).collect(Collectors.toList());

        this.communicationService = Executors.newFixedThreadPool(1);
        this.transferService = Executors.newFixedThreadPool(8);
        this.aggregationService = Executors.newFixedThreadPool(1);

        Stream.of(getSelectorCommunication()).map(CommunicationWorker::new).forEach(getCommunicationService()::submit);
        getSelectors().stream().map(HandleBodyWorker::new).forEach(getTransferService()::submit);
        this.aggregationService.submit(new AggregationTakeWorker());
    }

    private List<Selector> initServerPool() {
        return Constant.PORTS.stream().map(p -> {
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
        public static final ChannelPoolServer channelPool = new ChannelPoolServer();
    }

    public static ChannelPoolServer getInstance() {
        return SingletonHolder.channelPool;
    }

    public Map<String, BlockingQueue<String>> getQueue() {
        return queue;
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


    public ExecutorService getCommunicationService() {
        return communicationService;
    }

    public ExecutorService getTransferService() {
        return transferService;
    }

    public ExecutorService getAggregationService() {
        return aggregationService;
    }

    public AtomicBoolean getInteruptAWorker() {
        return interuptAWorker;
    }


}
