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

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.KeyStroke;

import net.java.games.input.Event;

/**
 * A container for the Remote Control Settings
 */
public class RoboCommandsModel {

	// Key for the settings in the Java preferences
	private static final String prefKey = "RoboRemoteSettings"; //$NON-NLS-1$
	private static final String PREFIX_CONN = "CONN_"; //$NON-NLS-1$
	private static final String PREFIX_VIDEO = "VIDEO_"; //$NON-NLS-1$
	private static final String LAST_USED_CONN = "LAST_USED_CONN"; //$NON-NLS-1$
	private static final String PREFIX_KBD = "KBD_"; //$NON-NLS-1$
	private static final String PREFIX_HID = "HID_"; //$NON-NLS-1$
	private static final String SELECTED_CONTROLLER = "SELECTED_CONTROLLER"; //$NON-NLS-1$

	// constants for the connections and commands lists
	public static final String[] connectionsColumns = { "Address", "Video Stream" };
	public static final String[] commandsColumns = { "Command", "Keyboard", "HID Action" };

	// the list of the command codes with the title and default keyboard key
	public static enum CommandsList {
		CMD_FORWWARD("Forward", KeyEvent.VK_W, "W"),
		CMD_REVERSE("Reverse", KeyEvent.VK_S, "S"),
		CMD_LEFT("Left", KeyEvent.VK_A, "A"),
		CMD_RIGHT("Right", KeyEvent.VK_D, "D"),
		CMD_LIGHTS_FRONT("Lights Front", KeyEvent.VK_Z, "LF"),
		CMD_LIGHTS_REAR("Lights Rear", KeyEvent.VK_X, "LR"),
		CMD_LIGHTS_SIDES("Lights Sides", KeyEvent.VK_C, "LS"), 
		CMD_MODE_IDLE("Mode Idle", KeyEvent.VK_1, "MI"),
		CMD_MODE_AI("Mode AI", KeyEvent.VK_2, "MA"),
		CMD_MODE_SCENARIO("Mode Scenario", KeyEvent.VK_3, "MS"),
		CMD_MODE_RC("Mode RC", KeyEvent.VK_4, "MR"),
		CMD_RESCAN_DISTANCES("Rescan Distances", KeyEvent.VK_E, "R");

		private final String title;
		private final int defaultKbCommnd;
		private final String remoteCommand;

		CommandsList(String title, int defaultKbCommnd, String remoteCommand) {
			this.title = title;
			this.defaultKbCommnd = defaultKbCommnd;
			this.remoteCommand = remoteCommand;
		}

		public String getTitle() {
			return title;
		}

		public int getDefaultKbCommand() {
			return defaultKbCommnd;
		}

		public String getRemoteCommand() {
			return remoteCommand;
		}
	}

	// a short way to get the commands count
	public static final int COMMANDS_COUNT = CommandsList.values().length;

	// a string wich should terminate all remote commands which are sent to
	// Arduino
	public static final String REMOTE_CMD_TERM = "\n"; //$NON-NLS-1$

	// all analog deviations from the previous value smaller than this delta
	// will be ignored
	private static final float IGNORE_DELTA = 0.03f;

	// container for the list of the last sent values
	private static HashMap<CommandsList, Float> lastCmdValueMap = new HashMap<CommandsList, Float>();

	// a short way to get the command title
	public static String getCommandTitle(int cmdID) {
		return CommandsList.values()[cmdID].getTitle();
	}

	// a short way to get the command's default keyboard key
	public static int getCommandDefaultKey(int cmdID) {
		return CommandsList.values()[cmdID].getDefaultKbCommand();
	}

	// a short way to get command's current keystroke string
	public static String getCommandKeyString(int cmdID) {
		if (kbCommandsList[cmdID] == 0) {
			return "<NONE>";
		} else {
			return KeyEvent.getKeyText(kbCommandsList[cmdID]);
		}
	}

	// a short way to get command's current keycode
	public static int getCommandKeyCode(int cmdID) {
		return kbCommandsList[cmdID];
	}

	// a short way to get command's current keystroke
	public static KeyStroke getCommandKey(int cmdID) {
		return KeyStroke.getKeyStroke(kbCommandsList[cmdID], 0);
	}

	// This transparent container is used to pass the command code together with
	// the optional value from the source to the processing code
	public static class CommandRecord {
		public CommandsList command;
		public float value;

		CommandRecord(CommandsList command, float value) {
			this.command = command;
			this.value = value;
		}
	}

	// containers for the connections and commands variables
	private static Vector<String> connections = new Vector<String>();
	private static Vector<String> videoStreams = new Vector<String>();
	private static String lastUsedConnection = "";
	private static int[] kbCommandsList = new int[COMMANDS_COUNT]; // store
																	// KeyCodes
																	// here
	private static String[] hidCommandsList = new String[COMMANDS_COUNT];
	private static String selectedController = "";

	// these hash maps are used for quick mapping of the incoming events to the
	// command codes
	private static HashMap<Integer, CommandsList> keyCodeToCmdMap = new HashMap<Integer, CommandsList>();
	private static HashMap<String, CommandsList> hidToCmdMap = new HashMap<String, CommandsList>();

	// stores values in the settings containers
	public static void setPreferences(final Vector<String> in_connections, final Vector<String> in_videoStreams,
			final int[] in_kbCommandsList, final String[] in_hidCommandsList, final String in_selectedController) {

		connections.clear();
		videoStreams.clear();

		for (int i = 0; i < in_connections.size(); i++) {
			String connection = in_connections.get(i);
			// Video stream can be empty. Connection - never
			if (!connection.isEmpty()) {
				connections.add(connection);
				videoStreams.add(in_videoStreams.get(i));
			}
		}

		System.arraycopy(in_kbCommandsList, 0, kbCommandsList, 0, COMMANDS_COUNT);
		System.arraycopy(in_hidCommandsList, 0, hidCommandsList, 0, COMMANDS_COUNT);

		rebuildHashMaps();

		selectedController = in_selectedController;
	}

	// settings getters
	public static Vector<String> getConnections() {
		return connections;
	}

	public static Vector<String> getVideoStreams() {
		return videoStreams;
	}

	public static String[] getHidCommandsList() {
		return hidCommandsList;
	}

	public static String getSelectedController() {
		return selectedController;
	}

	public static String getLastUsedConnection() {
		return lastUsedConnection;
	}

	public static void setLastUsedConnection(String lastUsedConnection) {
		RoboCommandsModel.lastUsedConnection = lastUsedConnection;
	}

	// saves the settings in the Java preferences
	public static void saveSettings() {
		Preferences settingsPrefsNode = RoboRemote.settings.node(prefKey);

		for (int i = 0; i < connections.size(); i++) {
			String connection = connections.get(i);
			// Video stream can be empty. Connection - never
			if (!connection.isEmpty()) {
				settingsPrefsNode.put(PREFIX_CONN + String.valueOf(i), connection);
				settingsPrefsNode.put(PREFIX_VIDEO + String.valueOf(i), videoStreams.get(i));
			}
		}

		settingsPrefsNode.put(LAST_USED_CONN, lastUsedConnection);

		CommandsList[] commandsList = CommandsList.values();

		for (int i = 0; i < COMMANDS_COUNT; i++) {
			settingsPrefsNode.putInt(PREFIX_KBD + commandsList[i].getTitle(), kbCommandsList[i]);
			settingsPrefsNode.put(PREFIX_HID + commandsList[i].getTitle(), hidCommandsList[i]);
		}

		settingsPrefsNode.put(SELECTED_CONTROLLER, selectedController);
	}

	// loads settings from the Java preferences
	public static void loadSettings() {
		Preferences settingsPrefsNode = RoboRemote.settings.node(prefKey);

		connections.clear();
		videoStreams.clear();

		int i = 0;

		while (true) {
			String connection = settingsPrefsNode.get(PREFIX_CONN + String.valueOf(i), "");
			if (connection.isEmpty())
				break;
			String videoStream = settingsPrefsNode.get(PREFIX_VIDEO + String.valueOf(i), "");
			connections.add(connection);
			videoStreams.add(videoStream);
			i++;
		}

		lastUsedConnection = settingsPrefsNode.get(LAST_USED_CONN, "");

		CommandsList[] commandsList = CommandsList.values();

		for (i = 0; i < COMMANDS_COUNT; i++) {
			kbCommandsList[i] = settingsPrefsNode.getInt(PREFIX_KBD + commandsList[i].getTitle(), -1);
			hidCommandsList[i] = settingsPrefsNode.get(PREFIX_HID + commandsList[i].getTitle(), "");
		}

		// if no commands were previously set up - load the defaults
		if (kbCommandsList[0] == -1) {
			for (i = 0; i < COMMANDS_COUNT; i++) {
				kbCommandsList[i] = commandsList[i].getDefaultKbCommand();
			}
		}

		selectedController = settingsPrefsNode.get(SELECTED_CONTROLLER, "");

		rebuildHashMaps();
	}

	// rebuild the hash maps which are used to map quickly keyboard and controller events to
	// the command codes
	private static void rebuildHashMaps() {
		keyCodeToCmdMap.clear();
		hidToCmdMap.clear();

		CommandsList[] commandsList = CommandsList.values();

		for (int i = 0; i < COMMANDS_COUNT; i++) {
			keyCodeToCmdMap.put(kbCommandsList[i], commandsList[i]);
			hidToCmdMap.put(hidCommandsList[i], commandsList[i]);
		}
	}
	
	// Return the command code with argument if needed which corresponds to the keyboard code
	// or null if command should be ignored
	public static CommandRecord keyEventToCommandRecord(KeyEvent e) {
		CommandsList command = keyCodeToCmdMap.get(e.getKeyCode());
		if(command == null) {
			return null;
		} else {
			return new CommandRecord(command, 1);
		}
	}
	
	// Return the command code with argument if needed which is parsed from the
	// HID event or null if command should be ignored
	public static CommandRecord hidEventToCommandRecord(Event e) {
		net.java.games.input.Component component = e.getComponent();

		String componentName = component.getName();
		float value = e.getValue();

		// analog component
		if (component.isAnalog()) {
			String sign = (value > 0) ? "+" : "-";
			componentName = sign + componentName;
		}

		CommandsList command = hidToCmdMap.get(componentName);
		if (command == null) {
			return null;
		} else {
			// debounce the commands
			Float lastValue = lastCmdValueMap.get(command);
			if (lastValue != null) {
				if (component.isAnalog()) {
					// don't send a command if the change is insignificant
					if (Math.abs(lastValue - value) <= IGNORE_DELTA) {
						return null;
					}
				} else {
					// don't send the same command twice
					if (value == lastValue) {
						return null;
					}
				}
			}

			// store the value we are going to send
			lastCmdValueMap.put(command, value);

			return new CommandRecord(command, value);
		}
	}
}
