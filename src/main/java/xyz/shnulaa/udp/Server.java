//package xyz.shnulaa.udp;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.SocketAddress;
//import java.net.SocketException;
//import java.nio.ByteBuffer;
//import java.nio.CharBuffer;
//import java.nio.channels.DatagramChannel;
//import java.nio.channels.SelectionKey;
//import java.nio.channels.Selector;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Iterator;
//import java.util.Set;
//
//public class Server {
//
//    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//    public static void main(String[] args) throws IOException {
//        DatagramChannel channel = DatagramChannel.open();
//        channel.socket().bind(new InetSocketAddress(10001));
//        channel.configureBlocking(false);
//        Selector selector = Selector.open();
//        channel.register(selector, SelectionKey.OP_READ);
//
//        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
//        while (!Thread.currentThread().isInterrupted()) {
//            try {
//                int eventsCount = selector.select();
//                if (eventsCount > 0) {
//                    Set<?> selectedKeys = selector.selectedKeys();
//                    Iterator<?> iterator = selectedKeys.iterator();
//                    while (iterator.hasNext()) {
//                        SelectionKey sk = (SelectionKey) iterator.next();
//                        iterator.remove();
//                        if (sk.isReadable()) {
//                            DatagramChannel datagramChannel = (DatagramChannel) sk
//                                    .channel();
//                            SocketAddress sa = datagramChannel
//                                    .receive(byteBuffer);
//                            //datagramChannel.receive(byteBuffer);
//                            byteBuffer.flip();
//
//                            // 测试：通过将收到的ByteBuffer首先通过缺省的编码解码成CharBuffer 再输出
//                            CharBuffer charBuffer = Charset.defaultCharset()
//                                    .decode(byteBuffer);
//                            System.err.println("[" + sdf.format(new Date()) + "]<- "
//                                    + charBuffer.toString());
//                            byteBuffer.clear();
//
//                            String echo = "This is the reply message from server: " + charBuffer.toString();
//                            ByteBuffer buffer = Charset.defaultCharset()
//                                    .encode(echo);
//                            datagramChannel.send(buffer, sa);
//                            //datagramChannel.write(buffer);
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//    }
//}
