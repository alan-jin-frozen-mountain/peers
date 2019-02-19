package net.sourceforge.peers.sip.transport;

import java.io.IOException;
import java.net.Socket;
import java.io.InputStream;
import java.io.DataInputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.transaction.TransactionManager;
public class TcpMessageReceiver extends MessageReceiver {
    private Socket socket;

    public TcpMessageReceiver(Socket socket,
                              TransactionManager transactionManager,
                              TransportManager transportManager, Config config,
                              Logger logger)
            throws SocketException {
        super(socket.getLocalPort(), transactionManager,
                transportManager, config, logger);
        this.socket = socket;
    }

    @Override
    protected void listen() throws IOException {
        final int noException = 0;
        final int socketTimeoutException = 1;
        final int ioException = 2;
        // AccessController.doPrivileged added for plugin compatibility
        int result = AccessController.doPrivileged(
                new PrivilegedAction<Integer>() {
                    public Integer run() {
                        try {
                            byte[] data = new byte[BUFFER_SIZE];
                            InputStream in = socket.getInputStream();
                            int len = in.read(data, 0,BUFFER_SIZE);
                            byte[] trimedData;
                            if (len > 0) {
                                logger.debug("Read data with len: "+ len);
                                trimedData = new byte[len];
                                for(int i = 0; i < len; i ++) {
                                    trimedData[i] = data[i];
                                }
                                processMessage(trimedData, (java.net.InetAddress) socket.getInetAddress(),
                                        socket.getPort(), RFC3261.TRANSPORT_TCP);
                            }
                        } catch (SocketTimeoutException e) {
                            return socketTimeoutException;
                        } catch (IOException e) {
                            logger.error("cannot receive packet", e);
                            //return ioException;
                        }
                        return noException;
                    }
                });
        switch (result) {
            case socketTimeoutException:
                return;
            case ioException:
                throw new IOException();
            case noException:
                break;
            default:
                break;
        }
    }
}
