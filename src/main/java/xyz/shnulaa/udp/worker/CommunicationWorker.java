package xyz.shnulaa.udp.worker;

import xyz.shnulaa.udp.ChannelPool;
import xyz.shnulaa.udp.Utils;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import static xyz.shnulaa.udp.Contant.bodyLength;

public class CommunicationWorker implements Callable<Void> {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Selector selector;

    public CommunicationWorker(Selector _selector) {
        this.selector = _selector;
    }

    @Override
    public Void call() throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bodyLength * 30);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int eventsCount = selector.select();
                if (eventsCount > 0) {
                    Set<?> selectedKeys = selector.selectedKeys();
                    Iterator<?> iterator = selectedKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = (SelectionKey) iterator.next();
                        iterator.remove();
                        if (sk.isReadable()) {
                            String after = Utils.fetchContent(byteBuffer, sk);
                            if (after.isEmpty()) {
                                continue;
                            }


                            Future<CharBuffer> f = ChannelPool.getInstance().getAggregationService().submit(new AggregationWorker(after));
                            CharBuffer result = f.get();
                            if (result != null) {
                                result.flip();
                                byte[] bytes = Utils.hexStringToByteArray(result.toString());
                                try (FileOutputStream output = new FileOutputStream("d:\\aa.zip", true)) {
                                    System.out.println("write to file.... length：" + bytes.length);
                                    output.write(bytes);
                                }
                            }

                            ChannelPool.getInstance().clearCharBuffer();


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