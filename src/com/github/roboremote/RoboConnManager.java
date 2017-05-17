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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashSet;

import javax.swing.SwingUtilities;

/**
 * A wrapper for all remote telnet connections
 */
public class RoboConnManager implements Runnable {

	// Event listeners declarations
	public interface RoboMessageListener {
		void messageReceived(final String message);
	}

	private HashSet<RoboMessageListener> messageListeners = new HashSet<RoboMessageListener>();

	// the robot connection socket
	private Socket socket = new Socket();
	private volatile BufferedReader inputStream;
	private PrintStream outputStream;

	// this thread will be reading the stream from the robot
	private Thread socketReaderThread;
	private volatile boolean readingSuspended = true;

	RoboConnManager() {
		// create the socket reading thread
		socketReaderThread = new Thread(this);
		socketReaderThread.start();
	}

	// this is the input stream reading thread runnable
	public void run() {
		while (true) {
			try {
				// reading might be suspended
				// if so - wait until this change
				synchronized (this) {
					while (readingSuspended)
						wait();
				}

				if (inputStream != null) {
					String message = inputStream.readLine();
					if (message != null) {
						// send the message to all listeners
						for (RoboMessageListener listener : messageListeners) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									listener.messageReceived(message);
								}
							});
						}
					}
				}
			} catch (SocketException se) {
				if (!readingSuspended) {
					RoboRemote.logger.error("Connection read error.", se);
				}
			} catch (IOException e) {
				// something wrong with the stream
				RoboRemote.logger.error("Connection read error.", e);
			} catch (InterruptedException ie) {
				// the reading was interrupted
				// that is OK
			}
		}
	}

	// initialize a new connection closing the old one if needed
	public synchronized void connect(String address)
			throws URISyntaxException, IllegalArgumentException, UnknownHostException, IOException {
		if (socket != null) {
			if (socket.isConnected()) {
				disconnect();
			}
		}

		URI uri = new URI("telnet://" + address);
		socket = new Socket(uri.getHost(), uri.getPort());
		// create input and output streams
		inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outputStream = new PrintStream(socket.getOutputStream(), true);

		// notify the reading thread to start processing the input stream
		readingSuspended = false;
		notify();
	}

	// @return true if socket is connected
	public boolean isConnected() {
		if (socket != null) {
			return socket.isConnected();
		} else {
			return false;
		}
	}

	// close the connection
	public synchronized void disconnect() {
		try {
			readingSuspended = true; // flag the reading thread to stop
			socket.close(); // close port
			socket = null;
			inputStream = null;
			outputStream = null;
		} catch (IOException e) {
			RoboRemote.logger.error("Connection lost.", e);
		}
	}

	// Sends a message to the remote recipient if it is connected
	public void sendMessage(String message) {
		if (socket != null) {
			if (socket.isConnected()) {
				outputStream.println(message);
			}
		}
	}

	// registers the incoming events listener
	public void addMessageListener(RoboMessageListener listener) {
		messageListeners.add(listener);
	}
}
