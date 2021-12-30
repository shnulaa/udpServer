package xyz.shnulaa.udp.worker;

import xyz.shnulaa.udp.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static xyz.shnulaa.udp.Constant.*;

public class CommunicationWorker implements Callable<Void> {

    private final Selector selector;
    private final AtomicLong packageIndex;


    public CommunicationWorker(Selector _selector) {
        this.selector = _selector;
        this.packageIndex = new AtomicLong(0);
    }

    @Override
    public Void call() throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate((TOTAL_BODY_LENGTH + UUID_HEAD_LENGTH + 1) * SEND_PER_PACKAGE * 3);
        ChannelPoolServer server = ChannelPoolServer.getInstance();
        AtomicInteger __index = new AtomicInteger(1);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int eventsCount = selector.select();
                Utils.log("CommunicationWorker -> index:" + __index.getAndIncrement());
                if (eventsCount > 0) {
                    Set<?> selectedKeys = selector.selectedKeys();
                    Iterator<?> iterator = selectedKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = (SelectionKey) iterator.next();
                        iterator.remove();
                        if (sk.isReadable()) {
                            String all = Utils.fetchContent(byteBuffer, sk);
                            if (all.isEmpty()) {
                                continue;
                            }

                            String totalLength = all.substring(0, TOTAL_BODY_LENGTH);
                            String uuIds = all.substring(TOTAL_BODY_LENGTH, all.length());

                            String md5Key = Utils.getMD5Str(uuIds);
                            String[] uuidArray = uuIds.split(",");
                            Map<String, Value> map = server.getPositionMap();
//                            Utils.log(String.format("CommunicationWorker -> index:" + __index.getAndIncrement() + ", totalLength:%s, md5Key:%s.", totalLength, md5Key));

                            synchronized (Utils.class) {
                                if (!map.containsKey(md5Key)) {
                                    Map<String, Integer> positionMap = new HashMap<>(uuidArray.length);
                                    AtomicInteger index = new AtomicInteger(0);
                                    for (String uuid : uuidArray) {
                                        positionMap.put(uuid, index.getAndIncrement());
                                    }
                                    int totalLengthInt = Integer.parseInt(totalLength.trim());
                                    long offset = packageIndex.getAndAdd(totalLengthInt / 2);
                                    map.put(md5Key, new Value(offset, positionMap, totalLengthInt, uuidArray.length));
//                                    Utils.log(String.format("CommunicationWorker -> index:" + __index.getAndIncrement() + ", totalLength:%s, md5Key:%s, offset:%s.", totalLength, md5Key, offset));
                                }
                            }
                        } else {
//                            Utils.log("CommunicationWorker -> index:" + __index.getAndIncrement() + ", isReadable:" + sk.isReadable());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}