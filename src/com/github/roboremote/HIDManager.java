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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;

/**
 * This class wraps all the HID-related activities. 
 *  
 */
public class HIDManager {

	// the list of the supported controller types
	private HashSet<Controller.Type> supportedControlerTypes = null;
	// the list of the controllers detected in the system
	private HashMap<String,Controller> controllersList = null;

	// Selected controller
	private Controller selectedController = null;

	// Timer which controls the controller's polling
	private final long TIMER_PERIOD = 50;
	private Timer pollTimer;

	// all analog values around zero within this range will be changed to zero
	private final float ZERO_DEAD_ZONE = 0.03f;

	// Event listeners declarations
	public interface HIDEventListener {
		void actionPerformed(Event e);
	}
	private HashSet<HIDEventListener> eventListeners = new HashSet<HIDEventListener>(); 

	public HIDManager() {
		// generate list of the supported controlers
		supportedControlerTypes = new HashSet<Controller.Type>();
		supportedControlerTypes.add(Controller.Type.GAMEPAD);
		supportedControlerTypes.add(Controller.Type.HEADTRACKER);
		supportedControlerTypes.add(Controller.Type.RUDDER);
		supportedControlerTypes.add(Controller.Type.STICK);
		supportedControlerTypes.add(Controller.Type.WHEEL);

		controllersList = new HashMap<String,Controller>();

		// "JInput does not rescan the controllers. It's focus is as a games API, most gamers have their hardware plugged in when they start a game."
		// http://www.java-gaming.org/index.php?topic=21694.0
		// Let's not refresh the list then - generate everything at the beginning
		Controller[] allControllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

		// store the controllers list
		for (int i = 0; i < allControllers.length; i++) {
			if(supportedControlerTypes.contains(allControllers[i].getType())) {
				controllersList.put(allControllers[i].getName(),allControllers[i]);
			}
		}

		// start the polling cycle
		pollTimer = new Timer(true);
		this.pollTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if(selectedController!=null) {
					selectedController.poll();
					Event e = new Event();

					// process all the events in the queue
					while (selectedController.getEventQueue().getNextEvent(e)) {

						net.java.games.input.Component component = e.getComponent();

						if(component.isAnalog()) {
							float value = e.getValue();

							// check if we need to stick to zero
							if(java.lang.Math.abs(value)<ZERO_DEAD_ZONE) {
								value = 0;
								e.set(e.getComponent(), 0, e.getNanos());
							}
						}

						// send the event to all listeners
						for (HIDEventListener listener : eventListeners) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									listener.actionPerformed(e);
								}
							});
						}
					}            	 
				}                
			}
		}, 0, TIMER_PERIOD);

	}

	public String[] getControllersList() {
		return controllersList.keySet().toArray(new String[controllersList.size()]);
	}

	public void setSelectedController(String controllerName) {
		selectedController = controllersList.get(controllerName);
	}

	public String getSelectedController() {
		if(selectedController == null) {
			return "";
		} else {
			return 	selectedController.getName();
		}
	}

	public void addEventListener(HIDEventListener l) {
		eventListeners.add(l);
	}

}

