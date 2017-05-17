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
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import com.github.roboremote.HIDManager.HIDEventListener;
import com.github.roboremote.RoboCommandsModel.CommandRecord;
import com.github.roboremote.RoboCommandsModel.CommandsList;
import com.github.roboremote.RoboConnManager.RoboMessageListener;

import net.java.games.input.Event;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;

/*
 * Application main window class.
 */
public class MainWindow {
	private JFrame frame;
	private RoboConnManager connectionManager;
	private EmbeddedMediaPlayerComponent mediaPlayerComponent = null;
	private JPanel topLeftPane, topRightPane;

	// Preferences dialog
	PrefDialog prefsDialog = null;

	// Cached indicator icons
	private ImageIcon redLightIcon, greenLightIcon, grayLightIcon;

	private final static String prefKey = "RoboRemote"; //$NON-NLS-1$

	// Window controls
	private JCheckBox connectionCheckBox;
	private JComboBox<String> connectionsComboBox;
	private JLabel connectionIndicator;
	private JCheckBox videoCheckBox;
	private JTextField videoURLTextField;
	private JLabel videoIndicator;

	private JRadioButton idleButton;
	private JLabel[] modeIndicators = new JLabel[4];

	private JLabel robotLabel;
	private JLabel frontLeftDistanceLabel;
	private JLabel frontDistanceLabel;
	private JLabel frontRightDistanceLabel;
	private JLabel frontLeftIndicator;
	private JLabel frontRightIndicator;
	private JLabel groundLeftIndicator;
	private JLabel groundRightIndicator;

	private JLabel frontLightIndicator;
	private JLabel rearLightIndicator;
	private JLabel sideLightIndicator;

	private JSlider turningRate;
	private JSlider speedRate;

	private JTextArea logTextPane;
	private JTextField commandTextField;
	private JCheckBox showAllCheckBox;

	private JButton exitButton;

	// this timer gathers the analog controls values and feeds to the robot
	private Timer motorsTimer;

	// Constructor creates the main window and show it immediately
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

	// Create the GUI and show it. For thread safety, this method should be
	// invoked from the event-dispatching thread.
	private void createAndShowGUI() {
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

		// initialize the keyboard, HID and connection action handlers
		initializeActions();

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
				exitButton.doClick();
			}
		});

		// Display the window.
		frame.pack();
		readSettings();
		frame.setVisible(true);
	}

	private void initializeButtonsPane() {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton prefButton = new JButton("Settings", createResoruceIcon("settings.png"));
		prefButton.setMnemonic(KeyEvent.VK_S);

		prefButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (prefsDialog == null) {
					prefsDialog = new PrefDialog();
				}
				prefsDialog.showDialog();
			}
		});
		p.add(prefButton);

		exitButton = new JButton("Exit", createResoruceIcon("exit.png"));
		exitButton.setMnemonic(KeyEvent.VK_X);
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				writeSettings();
				// release VLC resources on exit
				if (mediaPlayerComponent != null) {
					mediaPlayerComponent.getMediaPlayer().stop();
					mediaPlayerComponent.release();
				}
				frame.setVisible(false);
				frame.dispose();
				System.exit(0);
			}
		});
		p.add(exitButton);

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

		idleButton = new JRadioButton("Idle");
		idleButton.setSelected(true);
		idleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				processUserCommand(new CommandRecord(RoboCommandsModel.CommandsList.CMD_MODE_IDLE, 1));
			}
		});
		p.add(idleButton, c);

		JRadioButton aiButton = new JRadioButton("AI");
		aiButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				processUserCommand(new CommandRecord(RoboCommandsModel.CommandsList.CMD_MODE_AI, 1));
			}
		});
		c.gridy = 1;
		p.add(aiButton, c);

		JRadioButton scenarioButton = new JRadioButton("Scenario");
		scenarioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				processUserCommand(new CommandRecord(RoboCommandsModel.CommandsList.CMD_MODE_SCENARIO, 1));
			}
		});
		c.gridy = 2;
		p.add(scenarioButton, c);

		JRadioButton rcButton = new JRadioButton("RC");
		rcButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				processUserCommand(new CommandRecord(RoboCommandsModel.CommandsList.CMD_MODE_RC, 1));
			}
		});
		c.gridy = 3;
		p.add(rcButton, c);

		// Group the radio buttons.
		ButtonGroup group = new ButtonGroup();
		group.add(idleButton);
		group.add(aiButton);
		group.add(scenarioButton);
		group.add(rcButton);

		// Mode Indicators
		for (int i = 0; i < modeIndicators.length; i++) {
			modeIndicators[i] = new JLabel(grayLightIcon);
		}
		c.gridx = 1;
		c.gridy = 0;
		p.add(modeIndicators[0], c);
		c.gridy = 1;
		p.add(modeIndicators[1], c);
		c.gridy = 2;
		p.add(modeIndicators[2], c);
		c.gridy = 3;
		p.add(modeIndicators[3], c);

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

		robotLabel = new JLabel(createResoruceIcon("robot_top.png"));
		frontLeftDistanceLabel = new JLabel("0");
		frontDistanceLabel = new JLabel("0");
		frontRightDistanceLabel = new JLabel("0");
		frontLeftIndicator = new JLabel(grayLightIcon);
		frontRightIndicator = new JLabel(grayLightIcon);
		groundLeftIndicator = new JLabel(grayLightIcon);
		groundRightIndicator = new JLabel(grayLightIcon);

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

		logTextPane = new JTextArea();
		logTextPane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(logTextPane);
		p.add(scrollPane, BorderLayout.CENTER);

		commandTextField = new JTextField();
		commandTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				connectionManager.sendMessage(commandTextField.getText() + RoboCommandsModel.REMOTE_CMD_TERM);
				logTextPane.append("<" + commandTextField.getText() + "\n");
				commandTextField.setText("");
			}
		});

		showAllCheckBox = new JCheckBox("show all");

		JPanel commandsPanel = new JPanel(new BorderLayout());

		commandsPanel.add(commandTextField, BorderLayout.CENTER);
		commandsPanel.add(showAllCheckBox, BorderLayout.LINE_END);

		p.add(commandsPanel, BorderLayout.PAGE_END);

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

				if (connectionCheckBox.isSelected() != isConnected) {
					if (isConnected) {
						disconnectRobot();
					} else {
						connectionIndicator.setIcon(grayLightIcon);						
						if (connectionsComboBox.getSelectedIndex() != -1) {
							
							connectionCheckBox.setEnabled(false);
							connectionsComboBox.setEnabled(false);
							
							// let's start connection in background, so the main window does not freeze
							SwingWorker<Object, Object> swingWorker = new SwingWorker<Object, Object>() {
								private volatile String connectionErrorMessage = null;
								@Override
								public Object doInBackground() {
									try {									
										connectionManager.connect(connectionsComboBox.getSelectedItem().toString());
									} catch (IllegalArgumentException | URISyntaxException | IOException e1) {
										RoboRemote.logger.error("Connection error", e1);
										connectionErrorMessage = e1.getLocalizedMessage();
									}
									return null;
								}

								@Override
								protected void done() {
									if(connectionErrorMessage==null) {
										boolean connected = connectionManager.isConnected();
										connectionCheckBox.setSelected(connected);
										connectionIndicator.setIcon(connected ? greenLightIcon : grayLightIcon);
										idleButton.doClick();
									} else {
										connectionIndicator.setIcon(redLightIcon);
										connectionCheckBox.setSelected(false);
										JOptionPane.showMessageDialog(frame, connectionErrorMessage, "Connection error",
												JOptionPane.ERROR_MESSAGE);
									}
									connectionCheckBox.setEnabled(true);
									connectionsComboBox.setEnabled(true);
								}
							};

							swingWorker.execute();
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
				if (connectionCheckBox.isSelected()) {
					disconnectRobot();
					connectionCheckBox.setSelected(false);
				}
				// find the video stream pair
				int selectedConnection = connectionsComboBox.getSelectedIndex();
				if (selectedConnection >= 0) {
					if (videoCheckBox.isSelected()) {
						videoCheckBox.setSelected(false);
						if (mediaPlayerComponent != null) {
							mediaPlayerComponent.getMediaPlayer().stop();
						}
						videoIndicator.setIcon(grayLightIcon);
					}
					videoURLTextField.setText(RoboCommandsModel.getVideoStreams().get(selectedConnection));
				}
			}
		});

		p.add(connectionsComboBox, c);
		c.gridx = 2;
		c.weightx = 0;
		connectionIndicator = new JLabel(grayLightIcon);
		p.add(connectionIndicator, c);

		// Video line
		videoCheckBox = new JCheckBox("Video");

		videoCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (mediaPlayerComponent != null) {
					if (videoCheckBox.isSelected()) {
						videoIndicator.setIcon(grayLightIcon);
						mediaPlayerComponent.getMediaPlayer().playMedia(videoURLTextField.getText());
					} else {
						videoIndicator.setIcon(grayLightIcon);
						mediaPlayerComponent.getMediaPlayer().stop();
					}
				}
			}
		});

		c.gridx = 0;
		c.gridy = 1;
		p.add(videoCheckBox, c);
		c.gridx = 1;
		videoURLTextField = new JTextField();
		videoURLTextField.setEditable(false);
		p.add(videoURLTextField, c);

		c.gridx = 2;
		videoIndicator = new JLabel(grayLightIcon);
		p.add(videoIndicator, c);

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

		JButton b = new JButton(createResoruceIcon("left.png"));
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

		b = new JButton(createResoruceIcon("up.png"));
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		p.add(b, c);

		b = new JButton(createResoruceIcon("down.png"));
		c.gridy = 2;
		p.add(b, c);

		b = new JButton(createResoruceIcon("right.png"));
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
		frontLightIndicator = new JLabel(grayLightIcon);
		rearLightIndicator = new JLabel(grayLightIcon);
		sideLightIndicator = new JLabel(grayLightIcon);
		c.gridx = 1;
		c.gridy = 0;
		p.add(frontLightIndicator, c);
		c.gridy = 1;
		p.add(rearLightIndicator, c);
		c.gridy = 2;
		p.add(sideLightIndicator, c);

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

			// listen to the video status and indicate it in the UI
			mediaPlayerComponent.getMediaPlayer().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
				@Override
				public void playing(MediaPlayer mediaPlayer) {
					videoIndicator.setIcon(greenLightIcon);
				}

				@Override
				public void finished(MediaPlayer mediaPlayer) {
					videoCheckBox.setSelected(false);
					videoIndicator.setIcon(grayLightIcon);
				}

				@Override
				public void error(MediaPlayer mediaPlayer) {
					videoCheckBox.setSelected(false);
					videoIndicator.setIcon(redLightIcon);
					JOptionPane.showMessageDialog(frame, "Can't play video: " + videoURLTextField.getText(),
							"Video Error", JOptionPane.ERROR_MESSAGE);
				}
			});
		} else {
			JLabel errorLabel = new JLabel("VLC not found. Please reinstall VLC and try again.");
			errorLabel.setForeground(Color.RED);
			topLeftPane.add(errorLabel, BorderLayout.CENTER);
		}
	}

	// Returns an ImageIcon, or null if the path was invalid.
	public static ImageIcon createResoruceIcon(String imageName) {
		java.net.URL imageURL = MainWindow.class.getResource("images/" + imageName); //$NON-NLS-1$
		if (imageURL == null) {
			return null;
		} else {
			return new ImageIcon(imageURL);
		}
	}

	// Initialize event handlers
	private void initializeActions() {
		// initialize key mapping - the actions map part here only
		JRootPane rootPane = frame.getRootPane();

		AbstractAction uiCommandAction = new AbstractAction() {
			private static final long serialVersionUID = 6425138801681596694L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// everything typed in the command field should be ignored
				if (!commandTextField.hasFocus()) {
					// TODO complete this
					System.out.println("Action triggered: " + e.getActionCommand());
					System.out.println("Action triggered: " + e.toString());
				}
			}
		};

		// registering handler for all the bindings
		for (CommandsList command : CommandsList.values()) {
			rootPane.getActionMap().put(command.getTitle(), uiCommandAction);
			System.out.println("Action armed: "+command.getTitle());
		}

		// initialize the joystick listener
		RoboRemote.hidManager.addEventListener(new HIDEventListener() {
			@Override
			public void actionPerformed(Event e) {
				if (prefsDialog != null) {
					if (prefsDialog.isVisible()) {
						// do nothing settings dialog is being shown
						return;
					}
				}

				CommandRecord cRec = RoboCommandsModel.hidEventToCommandRecord(e);
				if (cRec != null) {
					processUserCommand(cRec);
				}
			}
		});

		// listen to the messages from the robot
		connectionManager.addMessageListener(new RoboMessageListener() {
			@Override
			public void messageReceived(String message) {
				message = message.trim();
				processRobotMessage(message);
				if (!showAllCheckBox.isSelected()) {
					if (message.charAt(0) != '~') {
						return;
					}
				}
				logTextPane.append(message + "\n");
			}
		});

		// start the timer which will feed the rudder and throttle commands to
		// the robot on regular basis
		motorsTimer = new Timer(true);
		motorsTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (connectionManager.isConnected()) {
					String message = generateMotorsCommand(speedRate.getValue(), turningRate.getValue());
					connectionManager.sendMessage(message);
				}
			}
		}, 1000, 100);
	}

	// converts float analog values 0..1 to int 0..255
	private int axisToInteger(float value) {
		return Math.round(Math.abs(value) * 255);
	}

	// generates the motors control command string
	private String generateMotorsCommand(int speedValue, int turningValue) {
		int leftSpeed = 0;
		int rightSpeed = 0;

		// we set the speed axis value to the leading track speed
		// and then identify how much the secondary track speed should deviate
		if (speedValue > 0) {
			if (turningValue > 0) {
				leftSpeed = speedValue + 255;
				rightSpeed = leftSpeed - turningValue * 2;
				rightSpeed = rightSpeed < 0 ? 0 : rightSpeed;
			} else {
				rightSpeed = speedValue + 255;
				leftSpeed = rightSpeed + turningValue * 2;
				leftSpeed = leftSpeed < 0 ? 0 : leftSpeed;
			}
		} else {
			if (turningValue > 0) {
				leftSpeed = speedValue + 255;
				rightSpeed = leftSpeed + turningValue * 2;
				rightSpeed = rightSpeed > 511 ? 511 : rightSpeed;
			} else {
				rightSpeed = speedValue + 255;
				leftSpeed = rightSpeed - turningValue * 2;
				leftSpeed = leftSpeed > 511 ? 511 : leftSpeed;
			}
		}
		String message = "X" + String.format("%03d", leftSpeed) + String.format("%03d", rightSpeed)
		+ RoboCommandsModel.REMOTE_CMD_TERM;
		return message;
	}

	// this is the method which processes commands from the user
	// it updates the GUI correspondingly
	// and sends commands to the robot
	protected void processUserCommand(CommandRecord cRec) {

		// make sure Swing components are updated in the Swing thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				switch (cRec.command) {
				case CMD_FORWWARD:
					speedRate.setValue(axisToInteger(cRec.value));
					break;
				case CMD_REVERSE:
					speedRate.setValue(-axisToInteger(cRec.value));
					break;
				case CMD_LEFT:
					turningRate.setValue(-axisToInteger(cRec.value));
					break;
				case CMD_RIGHT:
					turningRate.setValue(axisToInteger(cRec.value));
					break;
				default: {
					String message = cRec.command.getRemoteCommand() + RoboCommandsModel.REMOTE_CMD_TERM;
					connectionManager.sendMessage(message);

					if (showAllCheckBox.isSelected()) {
						logTextPane.append("<" + message);
					}
				}
				}
			}
		});
	}

	// here we process everything which is received from the robot
	private void processRobotMessage(String message) {

		int length = message.length();
		if (length < 2)
			return;

		switch (message.charAt(0)) {
		case 'M':
			// mode response
			for (int i = 0; i < modeIndicators.length; i++) {
				modeIndicators[i].setIcon(grayLightIcon);
			}
			switch (message) {
			case "MI":
				modeIndicators[0].setIcon(greenLightIcon);
				break;
			case "MA":
				modeIndicators[1].setIcon(greenLightIcon);
				break;
			case "MS":
				modeIndicators[2].setIcon(greenLightIcon);
				break;
			case "MR":
				modeIndicators[3].setIcon(greenLightIcon);
				break;
			}

			break;
		case 'L':
			// lights response
			if (length == 3) {
				char state = message.charAt(2);
				switch (message.charAt(1)) {
				case 'F':
					frontLightIndicator.setIcon((state == '1') ? greenLightIcon : grayLightIcon);
					break;
				case 'R':
					rearLightIndicator.setIcon((state == '1') ? greenLightIcon : grayLightIcon);
					break;
				case 'S':
					sideLightIndicator.setIcon((state == '1') ? greenLightIcon : grayLightIcon);
					break;
				}
			}
			break;
		case 'R':
			// range response
			if (length > 3) {
				switch (message.charAt(1)) {
				case 'L':
					frontLeftDistanceLabel.setText(message.substring(2));
					break;
				case 'F':
					frontDistanceLabel.setText(message.substring(2));
					break;
				case 'R':
					frontRightDistanceLabel.setText(message.substring(2));
					break;
				case 'O':
					if (length == 6) {
						char frontLeft = message.charAt(2);
						char groundLeft = message.charAt(3);
						char groundRight = message.charAt(4);
						char frontRight = message.charAt(5);
						frontLeftIndicator.setIcon((frontLeft == '1') ? redLightIcon : greenLightIcon);
						groundLeftIndicator.setIcon((groundLeft == '1') ? redLightIcon : greenLightIcon);
						groundRightIndicator.setIcon((groundRight == '1') ? redLightIcon : greenLightIcon);
						frontRightIndicator.setIcon((frontRight == '1') ? redLightIcon : greenLightIcon);
					}
					break;
				}
			}
			break;
		}
	}

	// this method disconnects the UI from the robot
	// and resets all the indicators
	protected void disconnectRobot() {
		connectionManager.disconnect();
		connectionIndicator.setIcon(grayLightIcon);
		frontLeftDistanceLabel.setText("0");
		frontDistanceLabel.setText("0");
		frontRightDistanceLabel.setText("0");
		frontLeftIndicator.setIcon(grayLightIcon);
		frontRightIndicator.setIcon(grayLightIcon);
		groundLeftIndicator.setIcon(grayLightIcon);
		groundRightIndicator.setIcon(grayLightIcon);
		for (int i = 0; i < modeIndicators.length; i++) {
			modeIndicators[i].setIcon(grayLightIcon);
		}

		frontLightIndicator.setIcon(grayLightIcon);
		rearLightIndicator.setIcon(grayLightIcon);
		sideLightIndicator.setIcon(grayLightIcon);
	}

	// Load the application settings
	private void readSettings() {
		// the window size and position defaults
		final Rectangle DEFAULT_BOUNDS = new Rectangle(80, 10, 900, 840);

		Rectangle bounds = MainWindow.loadFrameBounds(prefKey, DEFAULT_BOUNDS);
		frame.setBounds(bounds);

		// load connections list to the combo boxes
		fillConnectionsCombo();

		// select the active controller
		RoboRemote.hidManager.setSelectedController(RoboCommandsModel.getSelectedController());

		// rebuild the key bindings according to the user preferences
		rebuildKeyBindings();
	}

	// Load the connections list from the preferences and add them to the combo
	// box Select the last active one
	public void fillConnectionsCombo() {
		connectionsComboBox.removeAllItems();
		Vector<String> connectionsList = RoboCommandsModel.getConnections();
		for (String s : connectionsList) {
			connectionsComboBox.addItem(s);
		}

		connectionsComboBox.setSelectedItem(RoboCommandsModel.getLastUsedConnection());
	}

	// Load the key codes from the preferences and create the bindings
	public void rebuildKeyBindings() {
		JRootPane rootPane = frame.getRootPane();

		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
		
		// registering keycodes for all commands
		for (int i = 0; i < RoboCommandsModel.COMMANDS_COUNT; i++) {
			rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(RoboCommandsModel.getCommandKey(i),
					RoboCommandsModel.getCommandTitle(i));
			System.out.println("Action Bound: "+RoboCommandsModel.getCommandKey(i) + "->" + RoboCommandsModel.getCommandTitle(i));
		}
	}

	// Store the main window settings
	private void writeSettings() {
		MainWindow.saveFrameBounds(prefKey, frame.getBounds());
		if (connectionsComboBox.getSelectedIndex() != -1) {
			RoboCommandsModel.setLastUsedConnection(connectionsComboBox.getSelectedItem().toString());
		}
		RoboCommandsModel.saveSettings();
	}

	// This method loads the specified frame bounds from the preferences and
	// validates if this frame will fit any of the system screens
	// 
	// @param defaultBounds
	//            default frame bounds
	// @return a proper frame bounds either from preferences or default
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

	// Store the frame bounds in the preferences node specified
	// @param bounds the frame bounds
	public static void saveFrameBounds(final String prefKey, final Rectangle bounds) {
		Preferences windowPrefsNode = RoboRemote.settings.node(prefKey);
		windowPrefsNode.putInt("width", bounds.width); //$NON-NLS-1$
		windowPrefsNode.putInt("height", bounds.height); //$NON-NLS-1$
		windowPrefsNode.putInt("posX", bounds.x); //$NON-NLS-1$
		windowPrefsNode.putInt("posY", bounds.y); //$NON-NLS-1$
	}

	// @return the frame
	public JFrame getFrame() {
		return frame;
	}
}