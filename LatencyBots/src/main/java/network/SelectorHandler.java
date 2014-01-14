/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: Tomas
 * Date: 11.01.14
 * Time: 12:52
 */
public class SelectorHandler {
    private static final Logger log = LoggerFactory.getLogger(SelectorHandler.class);

    protected final Executor executor;
    protected final Selector selector;

    public SelectorHandler() {
        executor = createExecutor();
        selector = createSelector();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                selectorThread = Thread.currentThread();
            }
        });
        executor.execute(selectionTask);
    }

    public void connect(InetSocketAddress remoteAddress, ConnectCallback connectCallback) {
        connect(remoteAddress, connectCallback, null);
    }

    public void connect(final InetSocketAddress remoteAddress, final ConnectCallback connectCallback, final InetSocketAddress localAddress) {
        executeInSelectorThread(new Runnable() {
            @Override
            public void run() {
                SocketChannel socketChannel = null;
                try {
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);

                    if (localAddress != null) {
                        socketChannel.socket().bind(localAddress);
                    }

                    connectCallback.beforeSocketConnect(socketChannel.socket());

                    final SelectionKey selectionKey = socketChannel.register(selector, 0, connectCallback);

                    if (socketChannel.connect(remoteAddress)) {
                        completeConnection(selectionKey, socketChannel, connectCallback);
                    }
                    else {
                        selectionKey.interestOps(SelectionKey.OP_CONNECT);
                        connectCallback.connecting(socketChannel);
                    }
                }
                catch (Exception e) {
                    connectFail(e, socketChannel, connectCallback);
                }
            }
        });
    }

    public void bind(InetSocketAddress localAddress, BindCallback bindCallback) {
        bind(localAddress, bindCallback, 0);
    }

    public void bind(final InetSocketAddress localAddress, final BindCallback bindCallback, final int backlog) {
        executeInSelectorThread(new Runnable() {
            @Override
            public void run() {
                ServerSocketChannel serverSocketChannel = null;
                try {
                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false);
                    final ServerSocket serverSocket = serverSocketChannel.socket();

                    bindCallback.beforeServerSocketBind(serverSocket);

                    serverSocket.bind(localAddress, backlog);
                    if (!(serverSocket.getLocalSocketAddress() instanceof InetSocketAddress)) {
                        throw new IllegalArgumentException("bound to unknown SocketAddress " + serverSocket.getLocalSocketAddress());
                    }

                    final SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, bindCallback);

                    bindCallback.bound(serverSocketChannel, createServerChannelInterest(selectionKey));
                }
                catch (Exception e) {
                    try {
                        bindCallback.fail(e); //can throw Exception to prevent channel close or to do itself
                        if (serverSocketChannel != null) {
                            serverSocketChannel.close();
                        }
                    }
                    catch (Exception e1) {
                        //ignore
                    }
                }
            }
        });
    }

    protected ServerChannelInterest createServerChannelInterest(SelectionKey selectionKey) {
        return new ServerChannelInterestImpl(selectionKey);
    }

    protected void executeInSelectorThread(Runnable runnable) {
        executor.execute(runnable);

        if (sleeping.compareAndSet(true, false)) {
            selector.wakeup();
        }
    }

    protected void executeNowOrInSelectorThread(Runnable runnable) {
        if (withinSelectorThread()) {
            runnable.run();
        }
        else {
            executeInSelectorThread(runnable);
        }
    }

    protected interface OpsModifier {

        int modifyOps(int previousOps);
    }

    protected abstract static class OpsModifierBase implements OpsModifier {
        protected final int ops;

        public OpsModifierBase(int ops) {
            this.ops = ops;
        }
    }

    protected static class EnableOps extends OpsModifierBase {

        public EnableOps(int ops) {
            super(ops);
        }

        @Override
        public int modifyOps(int previousOps) {
            return previousOps | ops;
        }
    }

    protected static class DisableOps extends OpsModifierBase {

        public DisableOps(int ops) {
            super(ops);
        }

        @Override
        public int modifyOps(int previousOps) {
            return previousOps & ~ops;
        }
    }

    protected void modifyInterestOps(final SelectionKey selectionKey, final OpsModifier opsModifier, final FailCallback failCallback) {
        executeNowOrInSelectorThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final int currentOps = selectionKey.interestOps();
                    final int newOps = opsModifier.modifyOps(currentOps);
                    if (newOps != currentOps) {
                        selectionKey.interestOps(newOps);
                    }
                }
                catch (Exception e) {
                    failCallback.fail(e);
                }
            }
        });
    }

    private volatile Thread selectorThread;
    protected boolean withinSelectorThread() {
        return Thread.currentThread().equals(selectorThread);
    }

    protected final AtomicBoolean sleeping = new AtomicBoolean(true);

    private final Runnable selectionTask = new Runnable() {
        @Override
        public void run() {
            if (selector.isOpen()) {
                try {
                    select();
                }
                catch (Exception e) {
                    selectionException(e);
                }

                sleeping.set(true);
                executor.execute(selectionTask);
            }
        }
    };

    protected void select() throws IOException {
        if (selector.select() > 0) {
            final Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

            while (keyIterator.hasNext()) {
                final SelectionKey selectionKey = keyIterator.next();
                keyIterator.remove();

                if (selectionKey.isValid()) {

                    try {
                        final int readyOps = selectionKey.readyOps();
                        selectionKey.interestOps(selectionKey.interestOps() & ~(readyOps & (SelectionKey.OP_READ | SelectionKey.OP_WRITE)));

                        if ((readyOps & SelectionKey.OP_CONNECT) > 0) {
                            finishConnection(selectionKey);
                        }
                        if ((readyOps & SelectionKey.OP_ACCEPT) > 0) {
                            acceptConnections(selectionKey);
                        }
                        if ((readyOps & SelectionKey.OP_READ) > 0) {
                            readableChannel(selectionKey);
                        }
                        if ((readyOps & SelectionKey.OP_WRITE) > 0) {
                            writableChannel(selectionKey);
                        }
                    }
                    catch (CancelledKeyException e) {
                        //can be ignored because another thread can close channel
                    }
                }
            }
        }
    }

    protected void finishConnection(SelectionKey selectionKey) {
        final ConnectCallback connectCallback = (ConnectCallback) selectionKey.attachment();
        final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            if (socketChannel.finishConnect()) {
                completeConnection(selectionKey, socketChannel, connectCallback);
            }
        }
        catch (Exception e) {
            connectFail(e, socketChannel, connectCallback);
        }
    }

    protected void connectFail(Exception e, SocketChannel socketChannel, ConnectCallback connectCallback) {
        try {
            connectCallback.fail(e); //can throw Exception to prevent channel close or to do itself
            if (socketChannel != null) {
                socketChannel.close();
            }
        }
        catch (Exception e1) {
            //ignore
        }
    }

    protected void completeConnection(SelectionKey selectionKey, SocketChannel socketChannel, ConnectionCallback connectionCallback) {
        selectionKey.interestOps(SelectionKey.OP_READ);
        final ChannelIOCallback channelIOCallback = connectionCallback.connected(socketChannel, createChannelInterest(selectionKey));
        if (channelIOCallback != null) {
            selectionKey.attach(channelIOCallback);
        }
        else {
            try {
                socketChannel.close();
            }
            catch (IOException e) {
                //silent close channel
            }
        }
    }

    protected ChannelInterest createChannelInterest(SelectionKey selectionKey) {
        return new ChannelInterestImpl(selectionKey);
    }

    private void acceptConnections(SelectionKey serverKey) {
        final BindCallback bindCallback = (BindCallback) serverKey.attachment();
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) serverKey.channel();

        for (int i = 1; i <= bindCallback.acceptLimitPerCycle() && serverKey.isValid(); i++) {
            try {
                final SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel == null) {
                    break;
                }

                socketChannel.configureBlocking(false);
                final SelectionKey clientKey = socketChannel.register(selector, 0);

                completeConnection(clientKey, socketChannel, bindCallback);
            }
            catch (Exception e) {
                bindCallback.fail(e);
            }
        }
    }

    private void readableChannel(SelectionKey selectionKey) {
        ((ChannelIOCallback) selectionKey.attachment()).channelReadable((SocketChannel) selectionKey.channel());
    }

    private void writableChannel(SelectionKey selectionKey) {
        ((ChannelIOCallback) selectionKey.attachment()).ChannelWritable((SocketChannel) selectionKey.channel());
    }

    /**
     * Must be dedicated single worker thread
     */
    protected Executor createExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    protected Selector createSelector() {
        try {
            return Selector.open();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void selectionException(Exception e) {
        log.error("Exception during selection loop", e);
    }

    public interface FailCallback {

        void fail(Exception e);

        public static final FailCallback EmptyFailCallback = new FailCallback() {
            @Override
            public void fail(Exception e) {}
        };
        public static final FailCallback LogFailEnableReadInterest = new FailCallback() {
            @Override
            public void fail(Exception e) {
                log.warn("Exception during enable read interest", e);
            }
        };
        public static final FailCallback LogFailDisableReadInterest = new FailCallback() {
            @Override
            public void fail(Exception e) {
                log.warn("Exception during disable read interest", e);
            }
        };
        public static final FailCallback LogFailEnableWriteInterest = new FailCallback() {
            @Override
            public void fail(Exception e) {
                log.warn("Exception during enable write interest", e);
            }
        };
        public static final FailCallback LogFailDisableWriteInterest = new FailCallback() {
            @Override
            public void fail(Exception e) {
                log.warn("Exception during disable write interest", e);
            }
        };
        public static final FailCallback LogFailEnableAcceptInterest = new FailCallback() {
            @Override
            public void fail(Exception e) {
                log.warn("Exception during enable accept interest", e);
            }
        };
        public static final FailCallback LogFailDisableAcceptInterest = new FailCallback() {
            @Override
            public void fail(Exception e) {
                log.warn("Exception during disable accept interest", e);
            }
        };
    }

    public interface ConnectionCallback {

        ChannelIOCallback connected(SocketChannel socketChannel, ChannelInterest channelInterest);
    }

    public interface ConnectCallback extends FailCallback, ConnectionCallback {

        void beforeSocketConnect(Socket socket);

        void connecting(SocketChannel socketChannel);
    }

    public interface BindCallback extends FailCallback, ConnectCallback {

        void beforeServerSocketBind(ServerSocket serverSocket);

        void bound(ServerSocketChannel serverSocketChannel, ServerChannelInterest serverChannelInterest);

        int acceptLimitPerCycle();
    }

    public interface ChannelInterest {

        void enableReadInterest();
        void enableReadInterest(FailCallback failCallback);
        void disableReadInterest();
        void disableReadInterest(FailCallback failCallback);

        void enableWriteInterest();
        void enableWriteInterest(FailCallback failCallback);
        void disableWriteInterest();
        void disableWriteInterest(FailCallback failCallback);
    }

    protected class ChannelInterestImpl implements ChannelInterest {
        protected final SelectionKey selectionKey;

        public ChannelInterestImpl(SelectionKey selectionKey) {
            this.selectionKey = selectionKey;
        }

        @Override
        public void enableReadInterest() {
            enableReadInterest(FailCallback.LogFailEnableReadInterest);
        }

        @Override
        public void enableReadInterest(FailCallback failCallback) {
            modifyInterestOps(selectionKey, new EnableOps(SelectionKey.OP_READ), failCallback);
        }

        @Override
        public void disableReadInterest() {
            disableReadInterest(FailCallback.LogFailDisableReadInterest);
        }

        @Override
        public void disableReadInterest(FailCallback failCallback) {
            modifyInterestOps(selectionKey, new DisableOps(SelectionKey.OP_READ), failCallback);
        }

        @Override
        public void enableWriteInterest() {
            enableWriteInterest(FailCallback.LogFailEnableWriteInterest);
        }

        @Override
        public void enableWriteInterest(FailCallback failCallback) {
            modifyInterestOps(selectionKey, new EnableOps(SelectionKey.OP_WRITE), failCallback);
        }

        @Override
        public void disableWriteInterest() {
            disableWriteInterest(FailCallback.LogFailDisableWriteInterest);
        }

        @Override
        public void disableWriteInterest(FailCallback failCallback) {
            modifyInterestOps(selectionKey, new DisableOps(SelectionKey.OP_WRITE), failCallback);
        }
    }

    public interface ServerChannelInterest {

        void enableAcceptInterest();
        void enableAcceptInterest(FailCallback failCallback);
        void disableAcceptInterest();
        void disableAcceptInterest(FailCallback failCallback);
    }

    protected class ServerChannelInterestImpl implements ServerChannelInterest {
        protected final SelectionKey selectionKey;

        public ServerChannelInterestImpl(SelectionKey selectionKey) {
            this.selectionKey = selectionKey;
        }

        @Override
        public void enableAcceptInterest() {
            enableAcceptInterest(FailCallback.LogFailEnableAcceptInterest);
        }

        @Override
        public void enableAcceptInterest(FailCallback failCallback) {
            modifyInterestOps(selectionKey, new EnableOps(SelectionKey.OP_ACCEPT), failCallback);
        }

        @Override
        public void disableAcceptInterest() {
            disableAcceptInterest(FailCallback.LogFailDisableAcceptInterest);
        }

        @Override
        public void disableAcceptInterest(FailCallback failCallback) {
            modifyInterestOps(selectionKey, new DisableOps(SelectionKey.OP_ACCEPT), failCallback);
        }
    }

    public interface ChannelIOCallback {

        void channelReadable(SocketChannel socketChannel);

        void ChannelWritable(SocketChannel socketChannel);
    }
}
