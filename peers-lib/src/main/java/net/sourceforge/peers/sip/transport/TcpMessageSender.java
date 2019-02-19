package net.sourceforge.peers.sip.transport;
import java.io.IOException;
import java.net.Socket;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sip.RFC3261;
public class TcpMessageSender extends MessageSender {

    private Socket socket;

    public TcpMessageSender(InetAddress inetAddress, int port,
                            Socket socket, Config config,
                            Logger logger) throws SocketException {
        super(socket.getLocalPort(), inetAddress, port,
                config, RFC3261.TRANSPORT_TCP, logger);
        this.socket = socket;
    }

    @Override
    public synchronized void sendMessage(SipMessage sipMessage) throws IOException {
        logger.debug("TcpMessageSender.sendMessage");
        if (sipMessage == null) {
            return;
        }
        byte[] buf = sipMessage.toString().getBytes();
        sendBytes(buf);
        StringBuffer direction = new StringBuffer();
        direction.append("SENT to ").append(inetAddress.getHostAddress());
        direction.append("/").append(port);
        logger.traceNetwork(new String(buf), direction.toString());
    }

    @Override
    public synchronized void sendBytes(final byte[] bytes) throws IOException {
        logger.debug("TcpMessageSender.sendBytes");

        logger.debug("TcpMessageSender.sendBytes " + bytes.length
                + " " + inetAddress + ":" + port);
        // AccessController.doPrivileged added for plugin compatibility
        AccessController.doPrivileged(
                new PrivilegedAction<Void>() {

                    @Override
                    public Void run() {
                        try {
                            OutputStream out = socket.getOutputStream();
                            int length = bytes.length;
                            if (length > 0) {
                                out.write(bytes);
                            }

                        } catch (Throwable t) {
                            logger.error("throwable", new Exception(t));
                        }
                        return null;
                    }
                }
        );

        logger.debug("TcpMessageSender.sendBytes packet sent");
    }

}
