/**
 *  Copyright (C) 2017 by Mar'yan Rachynskyy
 *  rmaryan@gmail.com
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.roboremote;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import com.github.roboremote.HIDManager.HIDEventListener;
import com.github.roboremote.RoboCommandsModel.CommandRecord;

import net.java.games.input.Event;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;

public class MainWindow {
	private JFrame frame;
	private RoboConnManager connectionManager;
	private EmbeddedMediaPlayerComponent mediaPlayerComponent;
	private JPanel topLeftPane, topRightPane;

	// Preferences dialog
	PrefDialog prefsDialog = null;

	// Cached indicator icons
	private ImageIcon redLightIcon, greenLightIcon, grayLightIcon;

	private final static String prefKey = "RoboRemote"; //$NON-NLS-1$

	// Window controls
	private JCheckBox connectionCheckBox;
	private JComboBox<String> connectionsComboBox;
	private JCheckBox videoCheckBox;
	private JTextField videoURLTextField;

	private JSlider turningRate;
	private JSlider speedRate;

	// Core Window Actions
	private Action actionFW, actionRW, actionLeft, actionRight, actionModeIdle, actionModeAI, actionModeScenario,
	actionModeRC, actionPreferences, actionExit;

	/**
	 * Constructor creates the main window and show it immediately
	 *
	 */
	public MainWindow() {
		// initialize the connections manager
		connectionManager = new RoboConnManager();

		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private void createAndShowGUI() {
		initializeActions();

		// Create and set up the window.
		frame = new JFrame("Robot Remote Control Dashboard");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setLayout(new GridBagLayout());

		// Set the application icon
		ImageIcon ii = createResoruceIcon("roboremote.png"); //$NON-NLS-1$
		if (ii != null) {
			frame.setIconImage(ii.getImage());
		}

		// Cache icons for the indicator lights
		redLightIcon = createResoruceIcon("red.png"); //$NON-NLS-1$
		greenLightIcon = createResoruceIcon("green.png"); //$NON-NLS-1$
		grayLightIcon = createResoruceIcon("gray.png"); //$NON-NLS-1$

		// Create the main panels
		JPanel topPane = new JPanel(new BorderLayout());
		topLeftPane = new JPanel(new BorderLayout());
		topRightPane = new JPanel(new GridBagLayout());

		topPane.add(topLeftPane, BorderLayout.CENTER);
		topPane.add(topRightPane, BorderLayout.LINE_END);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 3;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		frame.add(topPane, c);

		// build the GUI panel by panel
		initializeVideoPane();
		initializeMovementPane();
		initializeLightsPane();
		initializeConnectPane();
		initializeLogPane();
		initializeDistancePane();
		initializeModePane();
		initializeButtonsPane();

		// Catch the window closing event
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				actionExit.actionPerformed(null);
			}
		});

		// Display the window.
		frame.pack();
		readSettings();
		frame.setVisible(true);
	}

	private void initializeButtonsPane() {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		p.add(new JButton(actionPreferences));
		p.add(new JButton(actionExit));

		// Placing the pane to the parent container
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 3;
		c.gridy = 3;
		c.fill = GridBagConstraints.BOTH;

		frame.add(p, c);
	}

	private void initializeModePane() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Mode"));

		// Mode radio buttons
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets.set(0, 0, 0, 5);

		JRadioButton idleButton = new JRadioButton(actionModeIdle);
		idleButton.setSelected(true);
		p.add(idleButton, c);

		JRadioButton aiButton = new JRadioButton(actionModeAI);
		c.gridy = 1;
		p.add(aiButton, c);

		JRadioButton scenarioButton = new JRadioButton(actionModeScenario);
		c.gridy = 2;
		p.add(scenarioButton, c);

		JRadioButton rcButton = new JRadioButton(actionModeRC);
		c.gridy = 3;
		p.add(rcButton, c);

		// Group the radio buttons.
		ButtonGroup group = new ButtonGroup();
		group.add(idleButton);
		group.add(aiButton);
		group.add(scenarioButton);
		group.add(rcButton);

		// Mode Indicators
		c.gridx = 1;
		c.gridy = 0;
		p.add(new JLabel(grayLightIcon), c);
		c.gridy = 1;
		p.add(new JLabel(grayLightIcon), c);
		c.gridy = 2;
		p.add(new JLabel(grayLightIcon), c);
		c.gridy = 3;
		p.add(new JLabel(grayLightIcon), c);

		// spacer
		c.gridx = 3;
		c.gridy = 4;
		c.weightx = 1;
		c.weighty = 1;
		p.add(new JLabel(), c);

		// Placing the pane to the parent container
		c = new GridBagConstraints();
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;

		topRightPane.add(p, c);
	}

	private void initializeDistancePane() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(
				BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Distances"));

		JLabel robotLabel = new JLabel(createResoruceIcon("robot_top.png"));
		JLabel frontLeftDistanceLabel = new JLabel("0.0");
		JLabel frontDistanceLabel = new JLabel("0.0");
		JLabel frontRightDistanceLabel = new JLabel("0.0");
		JLabel frontLeftIndicator = new JLabel(grayLightIcon);
		JLabel frontRightIndicator = new JLabel(grayLightIcon);
		JLabel groundLeftIndicator = new JLabel(grayLightIcon);
		JLabel groundRightIndicator = new JLabel(grayLightIcon);

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.3;

		p.add(frontLeftDistanceLabel, c);
		c.gridx = 1;
		c.gridwidth = 2;
		p.add(frontDistanceLabel, c);
		c.gridx = 3;
		c.gridwidth = 1;
		p.add(frontRightDistanceLabel, c);
		c.gridy = 1;
		c.gridx = 0;
		p.add(groundLeftIndicator, c);
		c.gridx = 1;
		p.add(frontLeftIndicator, c);
		c.gridx = 2;
		p.add(frontRightIndicator, c);
		c.gridx = 3;
		p.add(groundRightIndicator, c);
		c.gridy = 2;
		c.gridx = 0;
		c.gridwidth = 4;
		p.add(robotLabel, c);
		c.gridwidth = 2;
		c.gridy = 3;
		c.gridx = 1;
		p.add(new JButton(createResoruceIcon("Refresh.png")), c);

		// Placing the pane to the parent container
		c = new GridBagConstraints();
		c.gridy = 2;
		c.fill = GridBagConstraints.BOTH;

		topRightPane.add(p, c);
	}

	private void initializeLogPane() {
		JPanel p = new JPanel(new BorderLayout(3, 5));
		p.setBorder(
				BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Console"));

		p.add(new JTextPane(), BorderLayout.CENTER);
		p.add(new JTextField(), BorderLayout.PAGE_END);

		// Placing the pane to the parent container
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 2;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;

		frame.add(p, c);
	}

	private void initializeConnectPane() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets.set(3, 0, 3, 5);

		// Connect line
		connectionCheckBox = new JCheckBox("Connect");
		connectionCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isConnected = connectionManager.isConnected();

				if(connectionCheckBox.isSelected() != isConnected) {
					if(isConnected) {
						connectionManager.disconnect();
					} else {
						RoboRemote.logger.info("Connecting...");
						try {
							connectionManager.connect(connectionsComboBox.getSelectedItem().toString());
						} catch (IllegalArgumentException | URISyntaxException | IOException e1) {
							JOptionPane.showMessageDialog(frame,
									e1.getLocalizedMessage(),
									"Connection error",
									JOptionPane.ERROR_MESSAGE);
							RoboRemote.logger.error("Connection error", e1);
						} finally {
							connectionCheckBox.setSelected(connectionManager.isConnected());
						}
					}
				}
			}

		});

		p.add(connectionCheckBox, c);
		c.gridx = 1;
		c.weightx = 0.5;
		connectionsComboBox = new JComboBox<String>();
		connectionsComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(connectionCheckBox.isSelected()) {
					connectionManager.disconnect();
					connectionCheckBox.setSelected(false);
				}
				//find the video stream pair
				int selectedConnection = connectionsComboBox.getSelectedIndex();
				if(selectedConnection >=0) {
					//TODO disconnect video if connected
					if(videoCheckBox.isSelected()) {
						videoCheckBox.setSelected(false);
					}
					videoURLTextField.setText(RoboCommandsModel.getVideoStreams().get(selectedConnection));
				}
			}
		});

		p.add(connectionsComboBox, c);
		c.gridx = 2;
		c.weightx = 0;
		p.add(new JLabel(grayLightIcon), c);

		// Video line
		videoCheckBox = new JCheckBox("Video");
		c.gridx = 0;
		c.gridy = 1;
		p.add(videoCheckBox, c);
		c.gridx = 1;
		videoURLTextField = new JTextField();
		videoURLTextField.setEditable(false);
		p.add(videoURLTextField, c);
		c.gridx = 2;		
		p.add(new JLabel(grayLightIcon), c);

		// Placing the pane to the parent container
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		topRightPane.add(p, c);

	}

	private void initializeMovementPane() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(
				BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Movement"));

		GridBagConstraints c = new GridBagConstraints();
		c.insets.set(5, 5, 5, 5);
		c.weighty = 0.1;
		c.weightx = 0.1;

		JButton b = new JButton(actionLeft);
		b.setHideActionText(true);
		c.gridy = 1;
		p.add(b, c);

		turningRate = new JSlider(JSlider.HORIZONTAL, -255, 255, 0);
		turningRate.setFocusable(false);
		turningRate.setPaintTicks(true);
		turningRate.setMajorTickSpacing(255);

		// Create the label table for the turning slider
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put(new Integer(0), new JLabel("^"));
		labelTable.put(new Integer(-255), new JLabel("Left"));
		labelTable.put(new Integer(255), new JLabel("Right"));
		turningRate.setLabelTable(labelTable);
		turningRate.setPaintLabels(true);

		c.gridy = 3;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(turningRate, c);

		b = new JButton(actionFW);
		b.setHideActionText(true);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		p.add(b, c);

		b = new JButton(actionRW);
		b.setHideActionText(true);
		c.gridy = 2;
		p.add(b, c);

		b = new JButton(actionRight);
		b.setHideActionText(true);
		c.gridx = 2;
		c.gridy = 1;
		p.add(b, c);

		speedRate = new JSlider(JSlider.VERTICAL, -255, 255, 0);
		speedRate.setPaintTicks(true);
		speedRate.setMajorTickSpacing(255);

		// Create the label table for the speed slider
		labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put(new Integer(0), new JLabel("<"));
		labelTable.put(new Integer(-255), new JLabel("RW"));
		labelTable.put(new Integer(255), new JLabel("FW"));
		speedRate.setLabelTable(labelTable);
		speedRate.setPaintLabels(true);

		c.gridx = 3;
		c.gridy = 0;
		c.gridheight = 3;
		c.fill = GridBagConstraints.VERTICAL;
		p.add(speedRate, c);

		// Placing the pane to the parent container
		c = new GridBagConstraints();
		c.gridy = 2;
		c.fill = GridBagConstraints.BOTH;
		frame.add(p, c);
	}

	private void initializeLightsPane() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Lights"));

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets.set(0, 0, 0, 5);

		// Lights buttons
		p.add(new JCheckBox("Headlight"), c);
		c.gridy = 1;
		p.add(new JCheckBox("Rear Light"), c);
		c.gridy = 2;
		p.add(new JCheckBox("Side Lights"), c);

		// Lights indicators
		c.gridx = 1;
		c.gridy = 0;
		p.add(new JLabel(grayLightIcon), c);
		c.gridy = 1;
		p.add(new JLabel(grayLightIcon), c);
		c.gridy = 2;
		p.add(new JLabel(grayLightIcon), c);

		// spacer
		c.gridy = 3;
		c.weighty = 1;
		p.add(new JLabel(), c);

		// Placing the pane to the parent container
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 2;
		c.fill = GridBagConstraints.BOTH;
		frame.add(p, c);
	}

	private void initializeVideoPane() {
		// Initialize the VLC engine
		boolean foundVLC = new NativeDiscovery().discover();

		// Place the video view panel
		topLeftPane.setBorder(
				BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Video"));

		// check if VLC is available
		// if not - an error message to be shown
		if (foundVLC) {
			mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
			topLeftPane.add(mediaPlayerComponent, BorderLayout.CENTER);
		} else {
			JLabel errorLabel = new JLabel("VLC not found. Please reinstall VLC and try again.");
			errorLabel.setForeground(Color.RED);
			topLeftPane.add(errorLabel, BorderLayout.CENTER);
		}
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	public static ImageIcon createResoruceIcon(String imageName) {
		java.net.URL imageURL = MainWindow.class.getResource("images/" + imageName); //$NON-NLS-1$
		if (imageURL == null) {
			return null;
		} else {
			return new ImageIcon(imageURL);
		}
	}

	/**
	 * Create all global actions.
	 */
	private void initializeActions() {
		actionFW = new ActionFW();
		actionRW = new ActionRW();
		actionLeft = new ActionLeft();
		actionRight = new ActionRight();
		actionModeIdle = new ActionModeIdle();
		actionModeAI = new ActionModeAI();
		actionModeScenario = new ActionModeScenario();
		actionModeRC = new ActionModeRC();
		actionPreferences = new ActionPreferences();
		actionExit = new ActionExit();

		// initialize the joystick listener
		RoboRemote.hidManager.addEventListener(new HIDEventListener() {
			@Override
			public void actionPerformed(Event e) {
				if(prefsDialog != null) {
					if(prefsDialog.isVisible()) {
						// do nothing settings dialog is being shown
						return;
					}
				}

				CommandRecord cRec = RoboCommandsModel.hidEventToCommandRecord(e);
				if(cRec!=null) {
					processCommand(cRec);
				}
			}
		});
	}

	// converts float analog values 0..1 to int 0..255
	private int axisToInteger(float value) {
		return Math.round(Math.abs(value)*255);
	}

	// this is the method which processes commands from the user
	// it updates the GUI correspondingly
	// and sends commands to the robot
	protected void processCommand(CommandRecord cRec) {
		// TODO COMPLETE THIS
		RoboRemote.logger.info(""+cRec.value);		

		switch(cRec.command) {
		case CMD_FORWWARD:			
			speedRate.setValue(axisToInteger(cRec.value));
			connectionManager.sendMessage(cRec.command.getRemoteCommand()+RoboCommandsModel.REMOTE_CMD_TERM);
			break;
		case CMD_REVERSE:
			speedRate.setValue(-axisToInteger(cRec.value));
			connectionManager.sendMessage(cRec.command.getRemoteCommand()+RoboCommandsModel.REMOTE_CMD_TERM);
			break;
		case CMD_LEFT:
			turningRate.setValue(-axisToInteger(cRec.value));
			connectionManager.sendMessage(cRec.command.getRemoteCommand()+RoboCommandsModel.REMOTE_CMD_TERM);
			break;
		case CMD_RIGHT:
			turningRate.setValue(axisToInteger(cRec.value));
			connectionManager.sendMessage(cRec.command.getRemoteCommand()+RoboCommandsModel.REMOTE_CMD_TERM);
			break;
		case CMD_LIGHTS_FRONT:
		case CMD_LIGHTS_REAR:
		case CMD_LIGHTS_SIDES:
		case CMD_MODE_IDLE:
		case CMD_MODE_AI:
		case CMD_MODE_SCENARIO:
		case CMD_MODE_RC:
		case CMD_RESCAN_DISTANCES:
			connectionManager.sendMessage(cRec.command.getRemoteCommand()+RoboCommandsModel.REMOTE_CMD_TERM);
		}

	}

	public class ActionFW extends AbstractAction {
		private static final long serialVersionUID = -1575160183057701805L;

		public ActionFW() {
			super("Forward", createResoruceIcon("up.png"));
			putValue(SHORT_DESCRIPTION, "Forward");
		}

		public void actionPerformed(ActionEvent e) {
			// TODO Implement FW action
			connectionManager.sendMessage("R\n");
		}
	}

	public class ActionRW extends AbstractAction {
		private static final long serialVersionUID = -1406217878298940415L;

		public ActionRW() {
			super("Reverse", createResoruceIcon("down.png"));
			putValue(SHORT_DESCRIPTION, "Reverse");
		}

		public void actionPerformed(ActionEvent e) {
			// TODO Implement reverse action
		}
	}

	public class ActionLeft extends AbstractAction {
		private static final long serialVersionUID = -1977628799705481765L;

		public ActionLeft() {
			super("Left", createResoruceIcon("left.png"));
			putValue(SHORT_DESCRIPTION, "Turn Left");
		}

		public void actionPerformed(ActionEvent e) {
			// TODO Implement turning left action
		}
	}

	public class ActionRight extends AbstractAction {
		private static final long serialVersionUID = 2840797437924278347L;

		public ActionRight() {
			super("Right", createResoruceIcon("right.png"));
			putValue(SHORT_DESCRIPTION, "Turn Right");
		}

		public void actionPerformed(ActionEvent e) {
			// TODO Implement turning right action
		}
	}

	public class ActionModeIdle extends AbstractAction {
		private static final long serialVersionUID = -3427170486702088118L;

		public ActionModeIdle() {
			super("Idle");
			putValue(SHORT_DESCRIPTION, "Engage Mode Idle");
		}

		public void actionPerformed(ActionEvent e) {
			// TODO Implement mode
		}
	}

	public class ActionModeAI extends AbstractAction {
		private static final long serialVersionUID = 1200599241811124579L;

		public ActionModeAI() {
			super("AI");
			putValue(SHORT_DESCRIPTION, "Engage Mode AI");
		}

		public void actionPerformed(ActionEvent e) {
			// TODO Implement mode AI
		}
	}

	public class ActionModeScenario extends AbstractAction {
		private static final long serialVersionUID = -1470362534560221765L;

		public ActionModeScenario() {
			super("Scenario");
			putValue(SHORT_DESCRIPTION, "Engage Mode Scenario");
		}

		public void actionPerformed(ActionEvent e) {
			// TODO Implement mode Scenario
		}
	}

	public class ActionModeRC extends AbstractAction {
		private static final long serialVersionUID = -2805793759661430990L;

		public ActionModeRC() {
			super("Remote Control");
			putValue(SHORT_DESCRIPTION, "Engage Mode Remote Control");
		}

		public void actionPerformed(ActionEvent e) {
			// TODO Implement mode Remote Control
		}
	}

	public class ActionPreferences extends AbstractAction {
		private static final long serialVersionUID = 1799941748235958576L;

		public ActionPreferences() {
			super("Settings", createResoruceIcon("settings.png"));
			putValue(SHORT_DESCRIPTION, "Open Settings dialog");
			putValue(MNEMONIC_KEY, KeyEvent.VK_S);
		}

		public void actionPerformed(ActionEvent e) {
			//TODO disconnect if current connection is active
			if (prefsDialog == null) {
				prefsDialog = new PrefDialog();
			}			
			prefsDialog.showDialog();
		}
	}

	public class ActionExit extends AbstractAction {
		private static final long serialVersionUID = -2775859792316580649L;

		public ActionExit() {
			super("Exit", createResoruceIcon("exit.png"));
			putValue(SHORT_DESCRIPTION, "Exit the application");
			putValue(MNEMONIC_KEY, KeyEvent.VK_X);
		}

		public void actionPerformed(ActionEvent e) {
			writeSettings();
			frame.setVisible(false);
			frame.dispose();
			System.exit(0);
		}
	}

	/**
	 * Load the application settings
	 */
	private void readSettings() {
		// the window size and position defaults
		final Rectangle DEFAULT_BOUNDS = new Rectangle(80, 80, 900, 500);

		Rectangle bounds = MainWindow.loadFrameBounds(prefKey, DEFAULT_BOUNDS);
		frame.setBounds(bounds);

		fillConnectionsCombo();

		RoboRemote.hidManager.setSelectedController(RoboCommandsModel.getSelectedController());
	}

	/**
	 * Load the connections list from the preferences and add them to the combo box
	 * Select the last active one
	 */
	public void fillConnectionsCombo() {
		connectionsComboBox.removeAllItems();
		Vector<String> connectionsList = RoboCommandsModel.getConnections();
		for(String s: connectionsList) {
			connectionsComboBox.addItem(s);
		}

		connectionsComboBox.setSelectedItem(RoboCommandsModel.getLastUsedConnection());
	}

	/**
	 * Store the main window settings
	 */
	private void writeSettings() {
		MainWindow.saveFrameBounds(prefKey, frame.getBounds());	
		RoboCommandsModel.setLastUsedConnection(connectionsComboBox.getSelectedItem().toString());
		RoboCommandsModel.saveSettings();
	}

	/**
	 * This method loads the specified frame bounds from the preferences and
	 * validates if this frame will fit any of the system screens
	 * 
	 * @param defaultBounds
	 *            default frame bounds
	 * @return a proper frame bounds either from preferences or default
	 */
	public static Rectangle loadFrameBounds(final String prefKey, final Rectangle defaultBounds) {

		Preferences windowPrefsNode = RoboRemote.settings.node(prefKey);

		Rectangle bounds = new Rectangle();
		bounds.width = windowPrefsNode.getInt("width", defaultBounds.width); //$NON-NLS-1$
		bounds.height = windowPrefsNode.getInt("height", defaultBounds.height); //$NON-NLS-1$
		bounds.x = windowPrefsNode.getInt("posX", defaultBounds.x); //$NON-NLS-1$
		bounds.y = windowPrefsNode.getInt("posY", defaultBounds.y); //$NON-NLS-1$

		// now check if such coordinates can fit any display
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gd = ge.getScreenDevices();

		Rectangle finalBounds = defaultBounds;

		for (int i = 0; i < gd.length; i++) {
			GraphicsConfiguration gc = gd[i].getDefaultConfiguration();
			Rectangle r = gc.getBounds();
			if (r.contains(bounds)) {
				finalBounds = bounds;
			}
		}

		return finalBounds;
	}

	/**
	 * Store the frame bounds in the preferences node specified
	 * 
	 * @param bounds
	 *            the frame bounds
	 */
	public static void saveFrameBounds(final String prefKey, final Rectangle bounds) {
		Preferences windowPrefsNode = RoboRemote.settings.node(prefKey);
		windowPrefsNode.putInt("width", bounds.width); //$NON-NLS-1$
		windowPrefsNode.putInt("height", bounds.height); //$NON-NLS-1$
		windowPrefsNode.putInt("posX", bounds.x); //$NON-NLS-1$
		windowPrefsNode.putInt("posY", bounds.y); //$NON-NLS-1$
	}

	/**
	 * @return the frame
	 */
	public JFrame getFrame() {
		return frame;
	}
}
