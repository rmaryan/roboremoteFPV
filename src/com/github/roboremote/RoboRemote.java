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

import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoboRemote {
	public static Logger logger;
	public static MainWindow mainWindow;
	public static HIDManager hidManager;

	public static final Preferences settings = Preferences.userNodeForPackage(RoboRemote.class);

	public static void main(String[] args) {
		// Setting the desired logging level
		// The log levels are ERROR > WARN > INFO > DEBUG > TRACE
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
		logger = LoggerFactory.getLogger(RoboRemote.class);

		RoboCommandsModel.loadSettings();
		hidManager = new HIDManager();
		mainWindow = new MainWindow();
	}
}
