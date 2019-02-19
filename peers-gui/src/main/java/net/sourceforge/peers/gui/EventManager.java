/*
    This file is part of Peers, a java SIP softphone.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2010-2013 Yohann Martineau 
*/

package net.sourceforge.peers.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import fm.icelink.*;
import fm.icelink.dtmf.Tone;
import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.media.AbstractSoundManager;
import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.core.useragent.SipListener;
import net.sourceforge.peers.sip.core.useragent.UserAgent;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaders;
import net.sourceforge.peers.sip.syntaxencoding.SipUriSyntaxException;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipMessage;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;

public class EventManager implements SipListener, MainFrameListener,
        CallFrameListener, ActionListener {

    public static final String PEERS_URL = "http://peers.sourceforge.net/";
    public static final String PEERS_USER_MANUAL = PEERS_URL + "user_manual";

    public static final String ACTION_EXIT          = "Exit";
    public static final String ACTION_ACCOUNT       = "Account";
    public static final String ACTION_PREFERENCES   = "Preferences";
    public static final String ACTION_ABOUT         = "About";
    public static final String ACTION_DOCUMENTATION = "Documentation";

    private UserAgent userAgent;
    private MainFrame mainFrame;
    private AccountFrame accountFrame;
    private Map<String, CallFrame> callFrames;
    private boolean closed;
    private Logger logger;

    private Connection connection;
    private fm.icelink.AudioTrack localAudioTrack;
    private String publicIpAddress;
    public static IceServer IceServers;

    public EventManager(MainFrame mainFrame, String peersHome,
            Logger logger, AbstractSoundManager soundManager, String transport) {
        this.mainFrame = mainFrame;
        this.logger = logger;
        callFrames = Collections.synchronizedMap(
                new HashMap<String, CallFrame>());
        closed = false;
        // create sip stack
        try {
            userAgent = new UserAgent(this, peersHome, logger, soundManager, transport);
        } catch (SocketException e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, "Peers sip port " +
                    		"unavailable, about to leave", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            });
        }
    }

    // sip events

    // never update gui from a non-swing thread, thus using
    // SwingUtilties.invokeLater for each event coming from sip stack.
    @Override
    public void registering(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                if (accountFrame != null) {
                    accountFrame.registering(sipRequest);
                }
                mainFrame.registering(sipRequest);
            }
        });

    }

    @Override
    public void registerFailed(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                //mainFrame.setLabelText("Registration failed");
                if (accountFrame != null) {
                    accountFrame.registerFailed(sipResponse);
                }
                mainFrame.registerFailed(sipResponse);
            }
        });

    }

    @Override
    public void registerSuccessful(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                if (closed) {
                    userAgent.close();
                    System.exit(0);
                    return;
                }
                if (accountFrame != null) {
                    accountFrame.registerSuccess(sipResponse);
                }
                mainFrame.registerSuccessful(sipResponse);
            }
        });

    }

    @Override
    public void calleePickup(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                CallFrame callFrame = getCallFrame(sipResponse);
                if (callFrame != null) {
                    callFrame.calleePickup();
                }

                if(connection != null){
                    String sdpMessage = new String(sipResponse.getBody());
                    fm.icelink.SessionDescription remoteSdp = new fm.icelink.SessionDescription();
                    remoteSdp.setSdpMessage(fm.icelink.sdp.Message.parse(sdpMessage));

                    // If video not supported by remote peer, video will be rejected by remote peer.
                    if(remoteSdp.getHasVideo() == false || remoteSdp.getSdpMessage().getVideoDescription().getMedia().getTransportPort() == 0) {
                        SessionDescription localDescription = connection.getLocalDescription();
                        fm.icelink.sdp.MediaDescription videoDescription = localDescription.getSdpMessage().getVideoDescription();
                        localDescription.getSdpMessage().removeMediaDescription(videoDescription);

                        disableVideo = true;
                        Reconnect(remoteSdp, localDescription);
                    }
                    else{
                        remoteSdp.setType(fm.icelink.SessionDescriptionType.Answer);
                        remoteSdp.setTieBreaker(java.util.UUID.randomUUID().toString());
                        connection.setRemoteDescription(remoteSdp);
                    }
                }
            }
        });
    }

    private void Reconnect(SessionDescription remoteSdp, SessionDescription localDescription) {
        connection.close();
        connection = CreateConnection();

        connection.createOffer().then((sd)->{
            connection.setLocalDescription(localDescription).then((dd) -> {
                fm.icelink.sdp.MediaDescription remoteVideoDescription = remoteSdp.getSdpMessage().getVideoDescription();
                remoteSdp.getSdpMessage().removeMediaDescription(remoteVideoDescription);
                remoteSdp.setType(SessionDescriptionType.Answer);
                remoteSdp.setTieBreaker(java.util.UUID.randomUUID().toString());
                connection.setRemoteDescription(remoteSdp);
            });
        });
    }

    @Override
    public void error(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                CallFrame callFrame = getCallFrame(sipResponse);
                if (callFrame != null) {
                    callFrame.error(sipResponse);
                }
            }
        });

    }

    @Override
    public void incomingCall(final SipRequest sipRequest,
            SipResponse provResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                disableVideo = false;
                SipHeaders sipHeaders = sipRequest.getSipHeaders();
                SipHeaderFieldName sipHeaderFieldName =
                    new SipHeaderFieldName(RFC3261.HDR_FROM);
                SipHeaderFieldValue from = sipHeaders.get(sipHeaderFieldName);
                final String fromValue = from.getValue();
                String callId = Utils.getMessageCallId(sipRequest);
                CallFrame callFrame = new CallFrame(fromValue, callId,
                        EventManager.this, logger);
                callFrames.put(callId, callFrame);
                callFrame.setSipRequest(sipRequest);
                callFrame.incomingCall();
            }
        });

    }

    @Override
    public void remoteHangup(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                CallFrame callFrame = getCallFrame(sipRequest);
                if (callFrame != null) {
                    callFrame.remoteHangup();
                }
                localMedia.stop();
            }
        });

    }

    @Override
    public void ringing(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                CallFrame callFrame = getCallFrame(sipResponse);
                if (callFrame != null) {
                    callFrame.ringing();
                }
            }
        });

    }

    // main frame events

    @Override
    public void register() {
        if (userAgent == null) {
            // if several peers instances are launched concurrently,
            // display error message and exit
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                Config config = userAgent.getConfig();
                if (config.getPassword() != null) {
                    try {
                        userAgent.register();
                    } catch (SipUriSyntaxException e) {
                        mainFrame.setLabelText(e.getMessage());
                    }
                }
            }
        });

    }

    @Override
    public void callClicked(final String uri) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                disableVideo = false;

                String callId = Utils.generateCallID(
                        userAgent.getConfig().getLocalInetAddress());
                CallFrame callFrame = new CallFrame(uri, callId,
                        EventManager.this, logger);
                callFrames.put(callId, callFrame);

                VideoChat videoChat = callFrame.getVideoChat();

                startLocalMedia(videoChat);
                connection = CreateConnection();
                connection.createOffer().then((sd)->{
                    SipRequest sipRequest;
                    connection.setLocalDescription(sd);
                    String sdpMessage = sd.getSdpMessage().toString();
                    try {
                        sipRequest = userAgent.invite(uri, callId, sdpMessage);
                        callFrame.setSipRequest(sipRequest);
                        callFrame.callClicked();
                    }
                    catch (Exception e) {
                            logger.error(e.getMessage(), e);
                            mainFrame.setLabelText(e.getMessage());
                            return;
                        }
                });
            }
        });
    }

    @Override
    public void windowClosed() {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                try {
                    userAgent.unregister();
                } catch (Exception e) {
                    logger.error("error while unregistering", e);
                }
                closed = true;
                try {
                    Thread.sleep(3 * RFC3261.TIMER_T1);
                } catch (InterruptedException e) {
                }
                System.exit(0);
            }
        });
    }

    // call frame events
    
    @Override
    public void hangupClicked(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                userAgent.terminate(sipRequest);
                localMedia.stop();
            }
        });
    }

    @Override
    public void pickupClicked(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                CallFrame callFrame = getCallFrame(sipRequest);

                SessionDescription remoteSdp = new SessionDescription();
                String remoteSdpString = new String(sipRequest.getBody());
                remoteSdp.setSdpMessage(fm.icelink.sdp.Message.parse(remoteSdpString));
                remoteSdp.setType(SessionDescriptionType.Offer);
                remoteSdp.setTieBreaker(java.util.UUID.randomUUID().toString());

                // If video not supported by remote peer.
                if(remoteSdp.getHasVideo() == false || remoteSdp.getSdpMessage().getVideoDescription().getMedia().getTransportPort() == 0) {
                    disableVideo = true;
                }

                startLocalMedia(callFrame.getVideoChat());
                connection = CreateConnection();


                connection.setRemoteDescription(remoteSdp)
                        .then((sd) -> {
                            connection.createAnswer()
                                    .then((localSdp)->{
                                        String callId = Utils.getMessageCallId(sipRequest);
                                        DialogManager dialogManager = userAgent.getDialogManager();
                                        Dialog dialog = dialogManager.getDialog(callId);
                                        userAgent.acceptCall(sipRequest, dialog, localSdp.getSdpMessage().toString());

                                        connection.setLocalDescription(localSdp);
                                    })
                                    .fail((ex) ->{
                                        logger.error("Create answer failed.", ex);
                                    });
                        })
                        .fail((ex) ->{
                            logger.error("Set remote description failed.", ex);
                        });
            }
        });
    }

    @Override
    public void busyHereClicked(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                userAgent.rejectCall(sipRequest);
            }
        });

    }

    @Override
    public void dtmf(final char digit) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(connection != null) {
                    AudioStream audioStream = connection.getAudioStream();
                    if(audioStream != null) {
                        Tone tone = new Tone(String.valueOf(digit));
                        audioStream.insertDtmfTone(tone);
                    }
                }
            }
        });
    }

    private CallFrame getCallFrame(SipMessage sipMessage) {
        String callId = Utils.getMessageCallId(sipMessage);
        return callFrames.get(callId);
    }

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        logger.debug("gui actionPerformed() " + action);
        Runnable runnable = null;
        if (ACTION_EXIT.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    windowClosed();
                }
            };
        } else if (ACTION_ACCOUNT.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (accountFrame == null ||
                            !accountFrame.isDisplayable()) {
                        accountFrame = new AccountFrame(userAgent, logger);
                        accountFrame.setVisible(true);
                    } else {
                        accountFrame.requestFocus();
                    }
                }
            };
        } else if (ACTION_PREFERENCES.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, "Not implemented yet");
                }
            };
        } else if (ACTION_ABOUT.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    AboutFrame aboutFrame = new AboutFrame(
                            userAgent.getPeersHome(), logger);
                    aboutFrame.setVisible(true);
                }
            };
        } else if (ACTION_DOCUMENTATION.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        URI uri = new URI(PEERS_USER_MANUAL);
                        java.awt.Desktop.getDesktop().browse(uri);
                    } catch (URISyntaxException e) {
                        logger.error(e.getMessage(), e);
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            };
        }
        if (runnable != null) {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private LocalMedia localMedia = null;
    private AecContext aecContext = null;
    private LayoutManager layoutManager = null;
    private boolean disableVideo = false;

    private Connection CreateConnection()
    {
        // Create connection to remote client.
        final RemoteMedia remoteMedia = new RemoteMedia(false, disableVideo, aecContext);
        remoteMedia.getViewSink().addOnProcessFrame((frame) -> {
            logger.debug("Received video frame with length: " + frame.getBuffer().getDataBuffer().getLength());
        });
        final AudioStream audioStream = new AudioStream(localMedia, remoteMedia);
        audioStream.setLocalSend(true);
        audioStream.setLocalReceive(true);

        publicIpAddress = userAgent.getConfig().getLocalInetAddress().getHostAddress();
        audioStream.setEncryptionPolicy(EncryptionPolicy.Disabled);
        if(!disableVideo){
            final VideoStream videoStream = new VideoStream(localMedia, remoteMedia);
            videoStream.setLocalSend(true);
            videoStream.setLocalReceive(true);
            videoStream.setEncryptionPolicy(EncryptionPolicy.Disabled);
            connection = new Connection(new Stream[]{audioStream, videoStream});
        }
        else{
            connection = new Connection(new Stream[]{audioStream});
        }

        //To make sure connection is using specified ip address used for sip register.
        if(publicIpAddress != null) {
            connection.setPrivateIPAddress(publicIpAddress);
        }

        connection.setIceAddressTypes(new fm.icelink.AddressType[]{fm.icelink.AddressType.IPv4});
        connection.setIcePolicy(fm.icelink.IcePolicy.Disabled);
        connection.setTrickleIcePolicy(TrickleIcePolicy.NotSupported);

        connection.addOnStateChange(new fm.icelink.IAction1<Connection>() {
            public void invoke(Connection c) {
                if (c.getState() == ConnectionState.Connecting) {
                    if (remoteMedia.getView() != null) {
                        // set remote view on layout manager
                        fm.icelink.java.VideoComponent view = remoteMedia.getView();
                        view.setName("remoteView_" + remoteMedia.getId());
                        layoutManager.addRemoteView(remoteMedia.getId(), view);
                    }
                }
                else if (c.getState() == ConnectionState.Connected)
                {
                }
                else if (c.getState() == ConnectionState.Closing ||
                        c.getState() == ConnectionState.Failing) {
                    // Remove the remote view from the layout.
                    if (layoutManager!= null) {
                        layoutManager.removeRemoteView(remoteMedia.getId());
                    }
                    remoteMedia.destroy();
                }
                else if (c.getState() == ConnectionState.Closed) {
                }
                else if (c.getState() == ConnectionState.Failed) {
                }
            }
        });

        return connection;
    }

    public fm.icelink.Future<fm.icelink.LocalMedia> startLocalMedia(VideoChat videoChat)
    {
        return fm.icelink.openh264.Utility.downloadOpenH264()
                .then((o) -> {
                    aecContext = new AecContext();
                    localMedia = new LocalMedia(false,disableVideo,aecContext);

                    localMedia.getVideoSourceInputs().then((SourceInput[] inputs) -> {
                        videoChat.videoDevices.setModel(new javax.swing.DefaultComboBoxModel(inputs));
                        videoChat.videoDevices.addActionListener((e) -> {
                            switchVideoDevice((javax.swing.JComboBox<String>)e.getSource());
                        });
                    });

                    localMedia.getAudioSourceInputs().then((SourceInput[] inputs) -> {
                        videoChat.audioDevices.setModel(new javax.swing.DefaultComboBoxModel(inputs));
                        videoChat.audioDevices.addActionListener((e) -> {
                            switchAudioDevice((javax.swing.JComboBox<String>)e.getSource());
                        });
                    });

                    fm.icelink.java.VideoComponent view = localMedia.getView();

                    layoutManager = new fm.icelink.java.LayoutManager(videoChat.container);
                    layoutManager.setLocalView(view);

                    return localMedia.start();
                });
    }

    public fm.icelink.Future<fm.icelink.LocalMedia> stopLocalMedia()
    {
        return Promise.wrapPromise(() -> {
            if (localMedia != null) {
                return localMedia.stop().then((o) -> {
                    LayoutManager lm = layoutManager;
                    if (lm != null) {
                        lm.removeRemoteViews();
                        lm.unsetLocalView();
                        layoutManager = null;
                    }
                    localMedia.destroy(); // localMedia.destroy() will also destroy aecContext.
                    localMedia = null;
                });
            }
            else
            {
                return Promise.resolveNow(null);
            }
        });
    }

    private void switchAudioDevice(javax.swing.JComboBox<String> box)
    {
        SourceInput newInput = (SourceInput)box.getSelectedItem();
        localMedia.changeAudioSourceInput(newInput);
    }

    private void switchVideoDevice(javax.swing.JComboBox<String> box)
    {
        SourceInput newInput = (SourceInput)box.getSelectedItem();
        localMedia.changeVideoSourceInput(newInput);
    }
}
