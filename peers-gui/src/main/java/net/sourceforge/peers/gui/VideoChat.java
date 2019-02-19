package net.sourceforge.peers.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import fm.*;
import fm.icelink.StreamDirection;
import fm.icelink.StreamType;
import fm.icelink.java.VideoComponent;
import net.sourceforge.peers.gui.CallFrame;


public class VideoChat extends JPanel implements ActionListener
{
    public JComponent container;

    public JComboBox<String> audioDevices;
    public JComboBox<String> videoDevices;

    private JEditorPane textBox;
    private String textLog = "";

    private JLabel sessionIdLabel;

    private CallFrame callFrame;

	public VideoChat(CallFrame callFrame)
	{
        this.callFrame = callFrame;
        initializeContent();
	}

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String command = actionEvent.getActionCommand();
        callFrame.keypadEvent(command.charAt(0));
    }

    private void addOnClosing(Window window, final EmptyAction action)
    {
        window.addWindowListener(new WindowListener()
        {
            public void windowActivated(WindowEvent e) { }
            public void windowClosed(WindowEvent e) { }
            public void windowClosing(WindowEvent e)
            {
                action.invoke();
            }
            public void windowDeactivated(WindowEvent e) { }
            public void windowDeiconified(WindowEvent e) { }
            public void windowIconified(WindowEvent e) { }
            public void windowOpened(WindowEvent e) { }
        });
    }

    private void addOnOpened(Window window, final EmptyAction action)
    {
        window.addWindowListener(new WindowListener()
        {
            public void windowActivated(WindowEvent e) { }
            public void windowClosed(WindowEvent e) { }
            public void windowClosing(WindowEvent e) { }
            public void windowDeactivated(WindowEvent e) { }
            public void windowDeiconified(WindowEvent e) { }
            public void windowIconified(WindowEvent e) { }
            public void windowOpened(WindowEvent e)
            {
                action.invoke();
            }
        });
    }

    void initializeContent()
    {
        GridBagConstraints c;
        this.setLayout(new GridBagLayout());

        JPanel videoPane = new JPanel(new GridBagLayout());
        videoPane.setBackground(Color.white);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1d;
        c.gridx = 0;
        this.add(videoPane, c);

        container = new JLayeredPane();
        container.setBounds(0, 0, 640, 480);
        container.setBackground(java.awt.Color.black);
        container.setOpaque(true);
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        videoPane.add(container, c);

        JLabel status = new JLabel();
        status.setBounds(0, 480, 640, 24);
        status.setText("Run additional instances of this example (.NET/JS/Java/iOS/etc.) to see the application in action.");
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.PAGE_END;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        videoPane.add(status, c);

        audioDevices = new JComboBox<>();
        audioDevices.setPrototypeDisplayValue("");
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.PAGE_END;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 2;
        c.weightx = 0.25;
        c.weighty = 0.0;
        videoPane.add(audioDevices, c);

        videoDevices = new JComboBox<String>();
        videoDevices.setPrototypeDisplayValue("");
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.PAGE_END;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 2;
        c.weightx = 0.25;
        c.weighty = 0.0;
        videoPane.add(videoDevices, c);

        JPanel textPane = new JPanel(new GridBagLayout());
        textPane.setBackground(Color.white);
        textPane.setPreferredSize(new Dimension(286, getHeight()));
        textPane.setMinimumSize(new Dimension(286, getHeight()));
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1d;
        c.gridx = 1;
        c.insets = new Insets(0, 5, 0, 0);
        this.add(textPane, c);

        textBox = new JEditorPane("text/html", "");
        textBox.setText("");
        textBox.setBorder(BorderFactory.createLineBorder(Color.black));
        textBox.setEditable(false);
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1d;
        c.gridwidth = 2;
        c.gridy = 0;
        textPane.add(textBox, c);
    }
}
