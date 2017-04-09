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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.github.roboremote.HIDManager.HIDEventListener;

import net.java.games.input.Event;

/**
 * Show, edit and process application preferences
 * 
 * @author Mar'yan Rachynskyy
 *
 */
public class PrefDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 7042514048632135153L;

	private final static String prefKey = "RoboRemotePrefs"; //$NON-NLS-1$

	// The preferences components
	private JTable connectionsTable;
	private JTable commandsTable;
	private AbstractTableModel connectionsTableModel;
	private AbstractTableModel commandsTableModel;
	private JComboBox<String> hidControllersSelector;

	// Preference values containers
	private Vector<String> dialogConnections = new Vector<String>();
	private Vector<String> dialogVideoStreams = new Vector<String>();
	private String[] kbCommandsList = new String[RoboCommandsModel.COMMANDS_COUNT];
	private String[] hidCommandsList = new String[RoboCommandsModel.COMMANDS_COUNT];

	// is set to the # of the command which trigger is going to be detected
	// -1 if no detection in progress
	private int detectingCommandNumber = -1;


	// Create the dialog but don't show it
	public PrefDialog() {
		super(RoboRemote.mainWindow.getFrame(), "Settings", true);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				actionPerformed(null);
			}
		});

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		setLayout(new BorderLayout());

		// Connections part
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Stored Connections"));

		connectionsTableModel = getConnectionsModel();
		connectionsTable = new JTable(connectionsTableModel);
		connectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(connectionsTable);

		Dimension tablesDimension = new Dimension(360, 360);
		connectionsTable.setPreferredScrollableViewportSize(tablesDimension);
		scrollPane.setMinimumSize(new Dimension (tablesDimension));		
		connectionsTable.setFillsViewportHeight(true);

		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;		
		c.fill = GridBagConstraints.BOTH;
		c.gridheight = 2;
		p.add(scrollPane, c);

		// Add and Remove buttons
		JButton b = new JButton(MainWindow.createResoruceIcon("add.png"));
		b.setActionCommand("AddConnection");
		b.addActionListener(this);

		c.gridx = 1;
		c.weightx = 0;
		c.weighty = 0;
		c.gridheight = 1;
		c.fill = GridBagConstraints.NONE;
		c.insets.set(0, 5, 5, 5);
		p.add(b, c);

		b = new JButton(MainWindow.createResoruceIcon("remove.png"));
		b.setActionCommand("RemoveConnection");
		b.addActionListener(this);

		c.gridy = 1;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		p.add(b, c);

		add(p, BorderLayout.CENTER);

		// Commands part
		JPanel commandsPanel = new JPanel(new GridBagLayout());
		commandsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Commands"));

		commandsTableModel = getCommandsModel();
		commandsTable = new JTable(commandsTableModel);
		commandsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Adjust the renderer to highlight row which trigger is being detected
		commandsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -4077708744453572708L;

			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus,
					int row, int column) {

				Component renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				if(row == detectingCommandNumber) {
					renderer.setBackground(Color.BLUE);
					renderer.setForeground(Color.WHITE);
				} else {
					renderer.setBackground(Color.WHITE);
					renderer.setForeground(Color.BLACK);
				}

				return renderer;
			}
		});


		scrollPane = new JScrollPane(commandsTable);
		commandsTable.setPreferredScrollableViewportSize(tablesDimension);
		scrollPane.setMinimumSize(new Dimension (tablesDimension));		
		commandsTable.setFillsViewportHeight(true);

		// record the key pressed if the trigger detection is active
		commandsTable.addKeyListener(
				new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(detectingCommandNumber>-1) {
							int keyCode = e.getKeyCode();
							switch(keyCode) {
							case KeyEvent.VK_ESCAPE: 
								stopDetectingTrigger();
								e.consume();
								break;
							case KeyEvent.VK_ENTER:
								// Enter can't be assigned and navigation keys can't be assigned
								break;
							default:
								String componentName = KeyEvent.getKeyText(keyCode);

								// remove duplicates
								for(int i=0; i<kbCommandsList.length;i++) {
									if(kbCommandsList[i].equals(componentName)) {
										commandsTableModel.setValueAt("", i, 1);	
									}								
								}								

								// Assign pressed key as a trigger
								commandsTableModel.setValueAt(componentName, detectingCommandNumber, 1);
								stopDetectingTrigger();
								e.consume();
								break;
							}
						}
					}
				}
				);

		// double click should initiate the command trigger detection 
		commandsTable.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2){
					int selectedRow = commandsTable.getSelectedRow();
					if(selectedRow>=0) {
						if(selectedRow!=detectingCommandNumber) {
							startDetectingTrigger(selectedRow);
						} else {
							stopDetectingTrigger();
						}
					}
				} 
			}
		});

		// cancel trigger detection if command selection changes
		ListSelectionListener listener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(detectingCommandNumber>-1) {
					if(e.getFirstIndex() != detectingCommandNumber) {
						stopDetectingTrigger();
					}
				}
			}
		}; 
		commandsTable.getSelectionModel().addListSelectionListener(listener);

		// Override the default Enter key behavior - it should initiate command trigger detection
		commandsTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
		commandsTable.getActionMap().put("Enter", new AbstractAction() {
			private static final long serialVersionUID = -5571337618417859516L;

			@Override
			public void actionPerformed(ActionEvent ae) {
				startDetectingTrigger(commandsTable.getSelectedRow());
			}
		});

		c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;

		commandsPanel.add(scrollPane, c);

		// joysticks list
		hidControllersSelector = new JComboBox<String>(RoboRemote.hidManager.getControllersList());		
		hidControllersSelector.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				
				if(detectingCommandNumber>-1) {
					stopDetectingTrigger();
				}
				
				String selectedControler = hidControllersSelector.getSelectedItem().toString();
				
				if(!selectedControler.equals(RoboRemote.hidManager.getSelectedController())) {
					RoboRemote.hidManager.setSelectedController(selectedControler);
					
					//clean up all the pre-selected HID triggers
					for(int i=0; i<hidCommandsList.length;i++) {
						hidCommandsList[i] = "";						
					}
					
					commandsTableModel.fireTableDataChanged();
				}
			}
		});

		c.weighty = 0;
		c.gridwidth = 1;
		c.gridx = 1;
		c.gridy = 1;
		c.insets.set(5, 5, 5, 5);
		commandsPanel.add(hidControllersSelector, c);

		c.weightx = 0;		
		c.gridx = 0;	
		commandsPanel.add(new JLabel("Controller:"), c);	

		add(commandsPanel, BorderLayout.LINE_END);

		// build the bottom buttons pane
		JRootPane rootPane = getRootPane();
		p = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		b = new JButton("Restore Defaults", MainWindow.createResoruceIcon("defaults.png"));
		b.setActionCommand("Defaults");
		b.addActionListener(this);
		p.add(b);

		b = new JButton("OK", MainWindow.createResoruceIcon("yes.png"));
		b.setActionCommand("OK");
		p.add(b);
		b.addActionListener(this);
		rootPane.setDefaultButton(b);

		b = new JButton("Cancel", MainWindow.createResoruceIcon("no.png"));
		b.setActionCommand("Cancel");
		p.add(b);
		b.addActionListener(this);
		for (ActionListener al : b.getActionListeners()) {
			rootPane.registerKeyboardAction(al, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
					JComponent.WHEN_IN_FOCUSED_WINDOW);
		}

		add(p, BorderLayout.PAGE_END);

		pack();

		// Restore the frame position and size
		final Rectangle DEFAULT_BOUNDS = new Rectangle(100, 100, 600, 400);
		Rectangle bounds = MainWindow.loadFrameBounds(prefKey, DEFAULT_BOUNDS);
		setBounds(bounds);

		// initialize the joystick listener
		RoboRemote.hidManager.addEventListener(new HIDEventListener() {
			@Override
			public void actionPerformed(Event e) {
				// Handle the main joystick input here
				if(detectingCommandNumber>-1) {
					float value = e.getValue();					
					if(e.getComponent().isAnalog()) {
						// analog axes are allowed only for directional commands
						if(detectingCommandNumber<4) {
							// detect only edge deviations from zero
							if((value > 0.5) || (value < -0.5)) {

								String sign = (value>0)?"+":"-";
								String oppositeSign = (value<0)?"+":"-";

								String componentName = e.getComponent().getName();

								// this is the ID of the sibling command to use the same axis
								int pairCommand = (detectingCommandNumber%2==0)?detectingCommandNumber+1:detectingCommandNumber-1;

								// record new trigger
								commandsTableModel.setValueAt(sign+componentName, detectingCommandNumber, 2);		
								commandsTableModel.setValueAt(oppositeSign+componentName, pairCommand, 2);

								// remove duplicates
								int possibleDuplicates = (detectingCommandNumber>1)?0:2;  
								if(hidCommandsList[detectingCommandNumber].equals(hidCommandsList[possibleDuplicates]) || 
										hidCommandsList[detectingCommandNumber].equals(hidCommandsList[possibleDuplicates+1])) {
									commandsTableModel.setValueAt("",possibleDuplicates,2);
									commandsTableModel.setValueAt("",possibleDuplicates+1,2);
								}								

								stopDetectingTrigger();
							}
						}
					} else {
						// this is a key and it is pressed
						if(value==1F) {
							String componentName = e.getComponent().getName();

							// remove duplicates
							for(int i=0; i<hidCommandsList.length;i++) {
								if(hidCommandsList[i].equals(componentName)) {
									commandsTableModel.setValueAt("", i, 2);	
								}								
							}

							// store the trigger
							commandsTableModel.setValueAt(componentName, detectingCommandNumber, 2);								

							stopDetectingTrigger();
						}
					}
				}
			}
		});
	}

	// returns a table model for the connections JTable
	public AbstractTableModel getConnectionsModel() {		
		return new AbstractTableModel() {
			private static final long serialVersionUID = -7725018785657406648L;
			public String getColumnName(int col) {
				return RoboCommandsModel.connectionsColumns[col];
			}
			public int getRowCount() { return dialogConnections.size(); }
			public int getColumnCount() { return RoboCommandsModel.connectionsColumns.length; }
			public Object getValueAt(int row, int col) {
				if(col==0) {
					return dialogConnections.elementAt(row);
				} else {
					return dialogVideoStreams.elementAt(row);
				}
			}
			public boolean isCellEditable(int row, int col)
			{ return true; }
			public void setValueAt(Object value, int row, int col) {
				if(col==0) {
					dialogConnections.setElementAt(value.toString(), row);
				} else {
					dialogVideoStreams.setElementAt(value.toString(), row);
				}
				fireTableCellUpdated(row, col);
			}
		};
	}

	// returns a table model for the commands JTable
	public AbstractTableModel getCommandsModel() {
		return new AbstractTableModel() {
			private static final long serialVersionUID = 7038261304559556915L;
			public String getColumnName(int col) {
				return RoboCommandsModel.commandsColumns[col];
			}
			public int getRowCount() { return RoboCommandsModel.COMMANDS_COUNT; }
			public int getColumnCount() { return RoboCommandsModel.commandsColumns.length; }
			public Object getValueAt(int row, int col) {
				switch(col) {
				case 0: return RoboCommandsModel.getCommandTitle(row);
				case 1: return kbCommandsList[row];
				case 2: return hidCommandsList[row];
				default: return "";
				}
			}
			public boolean isCellEditable(int row, int col)
			{ return false; }
			public void setValueAt(Object value, int row, int col) {
				if(col==1) {
					kbCommandsList[row] = value.toString();
				} else if(col==2) {
					hidCommandsList[row] = value.toString();
				}
			}
		};
	}

	private void startDetectingTrigger(int commandID) {
		detectingCommandNumber = commandID;
		commandsTableModel.fireTableDataChanged();
	}

	private void stopDetectingTrigger() {
		detectingCommandNumber = -1;
		commandsTableModel.fireTableDataChanged();
	}

	// a handler for all dialog actions
	public void actionPerformed(ActionEvent event) {
		// cancel command triggers detection
		detectingCommandNumber = -1;
		if (event != null) {
			String command = "" + event.getActionCommand(); //$NON-NLS-1$
			if (command.equals("Defaults")) { //$NON-NLS-1$				
				for(int i=0; i<RoboCommandsModel.COMMANDS_COUNT; i++) {
					kbCommandsList[i] = RoboCommandsModel.getCommandDefaultKey(i);
					hidCommandsList[i] = "";
				}
				commandsTableModel.fireTableDataChanged();
			} else if (command.equals("OK")) { //$NON-NLS-1$
				RoboCommandsModel.setPreferences(dialogConnections,
						dialogVideoStreams,
						kbCommandsList,
						hidCommandsList,
						hidControllersSelector.getSelectedItem().toString());
				RoboCommandsModel.saveSettings();
				
				// push what is needed to the main window
				RoboRemote.mainWindow.fillConnectionsCombo();
				
				// save the dialog size and position, borrow a method from the main window
				MainWindow.saveFrameBounds(prefKey, getBounds());				
				
				setVisible(false);				
			} else if (command.equals("AddConnection")) { //$NON-NLS-1$
				dialogConnections.add("");
				dialogVideoStreams.add("");
				connectionsTableModel.fireTableDataChanged();
			}  else if (command.equals("RemoveConnection")) { //$NON-NLS-1$
				int selectedRow = connectionsTable.getSelectedRow(); 
				if(selectedRow > -1) {
					dialogConnections.remove(selectedRow);
					dialogVideoStreams.remove(selectedRow);
					connectionsTableModel.fireTableDataChanged();
				}					
			} else {
				// save the dialog size and position
				MainWindow.saveFrameBounds(prefKey, getBounds());
				setVisible(false);
			}
		}
	}

	// initialize the the dialog and make it visible
	public void showDialog() {
		// Load the fresh settings		
		dialogConnections.clear();
		dialogVideoStreams.clear();
		dialogConnections.addAll(RoboCommandsModel.getConnections());
		dialogVideoStreams.addAll(RoboCommandsModel.getVideoStreams());
		kbCommandsList = RoboCommandsModel.getKbCommandsList();
		hidCommandsList = RoboCommandsModel.getHidCommandsList();
		
		hidControllersSelector.setSelectedItem(RoboCommandsModel.getSelectedController());
		
		setVisible(true);
	}

}