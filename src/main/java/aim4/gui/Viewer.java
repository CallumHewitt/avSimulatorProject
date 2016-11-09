/*
Copyright (c) 2011 Tsz-Chiu Au, Peter Stone
University of Texas at Austin
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the University of Texas at Austin nor the names of its
contributors may be used to endorse or promote products derived from this
software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package aim4.gui;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.rmi.activation.ActivationInstantiator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import aim4.config.Constants;
import aim4.config.Debug;
import aim4.config.SimConfig;
import aim4.gui.frame.VehicleInfoFrame;
import aim4.gui.setuppanel.AIMSimSetupPanel;
import aim4.gui.viewer.AIMSimViewer;
import aim4.gui.viewer.SimViewer;
import aim4.im.IntersectionManager;
import aim4.map.Road;
import aim4.map.lane.Lane;
import aim4.sim.Simulator;
import aim4.sim.UdpListener;
import aim4.sim.AutoDriverOnlySimulator.AutoDriverOnlySimStepResult;
import aim4.sim.Simulator.SimStepResult;
import aim4.sim.setup.BasicSimSetup;
import aim4.sim.setup.SimFactory;
import aim4.sim.setup.SimSetup;
import aim4.util.Util;
import aim4.vehicle.VehicleSimView;

/**
 * The viewer is a Graphical User Interface (GUI) that allows a user to run the
 * AIM Simulator while watching the vehicles in real time.
 */
public class Viewer extends JFrame implements ActionListener, ItemListener {

    // ///////////////////////////////
    // CONSTANTS
    // ///////////////////////////////
    /** The serial version ID for serialization */
    private static final long serialVersionUID = 1L;
    /**
     * Whether or not the current simulation time is shown on screen.
     */
    public static final boolean IS_SHOW_SIMULATION_TIME = true;
    /**
     * Whether or not the simulator shows the vin of the vehicles on screen.
     */
    public static final boolean IS_SHOW_VIN_BY_DEFAULT = false;
    /**
     * Whether or not the IM Shapes are shown by default.
     */
    public static final boolean IS_SHOW_IM_DEBUG_SHAPES_BY_DEFAULT = false;
    /**
     * The String to display in the title bar of the main app.
     */
    private static final String TITLEBAR_STRING = "AV Simulator";
    /**
     * The width of the start/pause/resume button and the step buttons. {@value}
     * pixels.
     */
    private static final int DEFAULT_BUTTON_WIDTH = 100; // px
    /**
     * The height of the status pane. {@value} pixels.
     */
    private static final int DEFAULT_STATUS_PANE_HEIGHT = 200; // px

    // ///////////////////////////////
    // PRIVATE FIELDS
    // ///////////////////////////////
    /** Tabbed pane containing all of the simulation viewers */
    private JTabbedPane tabbedPane;
    /** The current viewer selected by the tabbedPane */
    private SimViewer selectedViewer;
    /** Panel containing the AIMSimViewer*/
    private AIMSimViewer aimViewer;
    /** The status pane on which to display statistics. */
    private StatusPanelContainer statusPanel;
    /** The Start/Pause/Resume Button */
    private JButton startButton;
    /** The Step Button */
    private JButton stepButton;
    // Menu Items
    /** Menu item "Autonomous Vehicles Only" */
    // private JCheckBoxMenuItem autoOnlySimTypeMenuItem;
    /** Menu item "Human Drivers Only" */
    // private JCheckBoxMenuItem humanOnlySimTypeMenuItem;
    /** Menu item "Human Drivers Only" */
    // private JCheckBoxMenuItem mixedSimTypeMenuItem;
    /** Menu item "Start Simulation Process" */
    private JMenuItem startMenuItem;
    /** Menu item "Step" */
    private JMenuItem stepMenuItem;
    /** Menu item "Reset" */
    private JMenuItem resetMenuItem;
    /** Menu item "Dump Data Collection Lines' Data" */
    private JMenuItem dumpDataMenuItem;
    /** Menu item for activating recording. */
    private JMenuItem startRecordingMenuItem;
    /** Menu item for deactivating recording. */
    private JMenuItem stopRecordingMenuItem;
    /** Menu item for starting the UDP listener */
    private JMenuItem startUdpListenerMenuItem;
    /** Menu item for stopping the UDP listener */
    private JMenuItem stopUdpListenerMenuItem;
    /** Menu item for controlling whether to show the simulation time */
    private JCheckBoxMenuItem showSimulationTimeMenuItem;
    /** Menu item for controlling whether to show VIN numbers */
    private JCheckBoxMenuItem showVinMenuItem;
    /** Menu item for controlling whether to show debug shapes */
    private JCheckBoxMenuItem showIMShapesMenuItem;
    /** Menu item for clearing simulator's debug point */
    private JMenuItem clearDebugPointsMenuItem;

    // ///////////////////////////////
    // CLASS CONSTRUCTORS
    // ///////////////////////////////

    /**
     * Create a new viewer object.
     */
    public Viewer() {
        this(false);
    }

    /**
     * Create a new viewer object.
     *
     * @param isRunNow      whether or not the simulation is run immediately
     */
    public Viewer(final boolean isRunNow) {
        super(TITLEBAR_STRING);
        // for debugging
        Debug.viewer = this.selectedViewer;

        // Lastly, schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void run() {
                createAndShowGUI(isRunNow);
            }

        });
    }

    // ///////////////////////////////
    // GUI METHODS
    // ///////////////////////////////

    /**
     * Create a new GUI and show it.
     *
     * @param isRunNow      whether or not the simulation is run immediately
     */
    private void createAndShowGUI(boolean isRunNow) {
        // Apple specific property.
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                "AIM Viewer");
        // Make sure that the program quits when we close the window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Build the GUI
        createMenuBar();
        createTabbedPane();
        createStatusPanel();
        setComponentsLayout();
        pack(); // pick the layout and show it
        setVisible(true);
        initGUIsetting();

        if (isRunNow) {
            selectedViewer.startButtonHandler();
            selectedViewer.requestCanvasFocusInWindow();
        }
    }

    /**
     * Create the menu system.
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu;
        JMenuItem menuItem;

        setJMenuBar(menuBar);

        // The File menu exists only on non-Mac OS X environment
        if (!System.getProperty("os.name").equals("Mac OS X")) {
            // File
            menu = new JMenu("File");
            menuBar.add(menu);
            // File->Quit
            menuItem = new JMenuItem("Quit AIM");
            menuItem.addActionListener(this);
            menu.add(menuItem);
        }

        // Simulation
        menu = new JMenu("Simulator");
        menuBar.add(menu);

    /*
    // Simulator->Autonomous Vehicles Only
    autoOnlySimTypeMenuItem =
        new JCheckBoxMenuItem("Autonomous Vehicles Only", true);
    autoOnlySimTypeMenuItem.addActionListener(this);
    menu.add(autoOnlySimTypeMenuItem);
    // Simulator->Human Drivers Only
    humanOnlySimTypeMenuItem =
        new JCheckBoxMenuItem("Human Drivers Only", false);
    humanOnlySimTypeMenuItem.addActionListener(this);
    menu.add(humanOnlySimTypeMenuItem);
    // Simulator->Mixed Drivers
    mixedSimTypeMenuItem = new JCheckBoxMenuItem("Mixed Drivers", false);
    mixedSimTypeMenuItem.addActionListener(this);
    menu.add(mixedSimTypeMenuItem);
    // Simulator->separator
    // menu.addSeparator();
    */

        // Simulator->Start Simulation
        startMenuItem = new JMenuItem("Start");
        startMenuItem.addActionListener(this);
        menu.add(startMenuItem);
        // Simulator->Step
        stepMenuItem = new JMenuItem("Step");
        stepMenuItem.addActionListener(this);
        menu.add(stepMenuItem);
        // Simulator->Reset
        resetMenuItem = new JMenuItem("Reset");
        resetMenuItem.addActionListener(this);
        menu.add(resetMenuItem);

        // Data
        menu = new JMenu("Data");
        menuBar.add(menu);
        // Data->Dump Data Collection Lines' Data
        dumpDataMenuItem = new JMenuItem("Dump Data Collection Lines' Data");
        dumpDataMenuItem.addActionListener(this);
        menu.add(dumpDataMenuItem);

        // Recording
        menu = new JMenu("Recording");
        menuBar.add(menu);
        // Recording->Start
        startRecordingMenuItem = new JMenuItem("Start");
        startRecordingMenuItem.addActionListener(this);
        menu.add(startRecordingMenuItem);
        // Recording->Stop
        stopRecordingMenuItem = new JMenuItem("Stop");
        stopRecordingMenuItem.addActionListener(this);
        menu.add(stopRecordingMenuItem);

        // UDP
        menu = new JMenu("UDP");
        menuBar.add(menu);
        // UDP->Start Listening
        startUdpListenerMenuItem = new JMenuItem("Start Listening");
        startUdpListenerMenuItem.addActionListener(this);
        startUdpListenerMenuItem.setEnabled(false);
        menu.add(startUdpListenerMenuItem);
        // UDP->Stop Listening
        stopUdpListenerMenuItem = new JMenuItem("Stop Listening");
        stopUdpListenerMenuItem.addActionListener(this);
        stopUdpListenerMenuItem.setEnabled(false);
        menu.add(stopUdpListenerMenuItem);

        // View
        menu = new JMenu("View");
        menuBar.add(menu);
        // View->Show simulation time
        showSimulationTimeMenuItem = new JCheckBoxMenuItem("Show Simulation Time",
                IS_SHOW_SIMULATION_TIME);
        showSimulationTimeMenuItem.addItemListener(this);
        menu.add(showSimulationTimeMenuItem);
        // View->Show VIN numbers
        showVinMenuItem = new JCheckBoxMenuItem("Show VINs",
                IS_SHOW_VIN_BY_DEFAULT);
        showVinMenuItem.addItemListener(this);
        menu.add(showVinMenuItem);
        // View->Show IM Shapes
        showIMShapesMenuItem = new JCheckBoxMenuItem("Show IM Shapes", false);
        showIMShapesMenuItem.addItemListener(this);
        menu.add(showIMShapesMenuItem);

        // Debug
        menu = new JMenu("Debug");
        menuBar.add(menu);
        // Debug->Clear Debug Points
        clearDebugPointsMenuItem = new JMenuItem("Clear Debug Points");
        clearDebugPointsMenuItem.addActionListener(this);
        menu.add(clearDebugPointsMenuItem);
    }

    /**
     * Create tab panel
     */
    private void createTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                selectedViewer = (SimViewer) ((JTabbedPane) e.getSource()).getSelectedComponent();
                Debug.viewer = selectedViewer;
            }
        });

        aimViewer = new AIMSimViewer(statusPanel);

        tabbedPane.add("AIM", aimViewer);

        selectedViewer = (SimViewer) tabbedPane.getSelectedComponent();
    }

    /**
     * Create all components in the viewer
     */
    private void createStatusPanel() {
        statusPanel = new StatusPanelContainer(this);
        startButton = new JButton("Start");
        startButton.addActionListener(this);
        stepButton = new JButton("Step");
        stepButton.setEnabled(false);
        stepButton.addActionListener(this);
    }

    /**
     * Set the layout of the viewer
     */
    private void setComponentsLayout() {
        // set the group layout
        Container pane = getContentPane();
        GroupLayout layout = new GroupLayout(pane);
        pane.setLayout(layout);
        // Turn on automatically adding gaps between components
        layout.setAutoCreateGaps(false);
        // Turn on automatically creating gaps between components that touch
        // the edge of the container and the container.
        layout.setAutoCreateContainerGaps(false);
        // layout for the horizontal axis
        layout.setHorizontalGroup(layout.createParallelGroup(
                GroupLayout.Alignment.LEADING).addComponent(tabbedPane).addGroup(layout.createSequentialGroup().addGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(
                        startButton,
                        DEFAULT_BUTTON_WIDTH,
                        GroupLayout.DEFAULT_SIZE,
                        DEFAULT_BUTTON_WIDTH).addComponent(
                        stepButton, DEFAULT_BUTTON_WIDTH,
                        GroupLayout.DEFAULT_SIZE,
                        DEFAULT_BUTTON_WIDTH)).addComponent(
                statusPanel)));
        // layout for the vertical axis
        layout.setVerticalGroup(
                layout.createSequentialGroup().addComponent(tabbedPane).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.CENTER).addGroup(layout.createSequentialGroup().addComponent(
                                startButton).addComponent(stepButton)).addComponent(statusPanel,
                                DEFAULT_STATUS_PANE_HEIGHT,
                                GroupLayout.DEFAULT_SIZE,
                                DEFAULT_STATUS_PANE_HEIGHT)));
    }

    public Simulator getSelectedSimulator() {
        return selectedViewer.getSimulator();
    }

    public void setTargetSimSpeed(double target) {
        selectedViewer.setTargetSimSpeed(target);
    }

    public void setTargetFrameRate(double target) {
        selectedViewer.setTargetFrameRate(target);
    }

    /**
     * Initialize the GUI setting.
     */
    private void initGUIsetting() {
        resetButtonMenuItem();
        startRecordingMenuItem.setEnabled(true);
        stopRecordingMenuItem.setEnabled(false);
        startUdpListenerMenuItem.setEnabled(false);
        stopUdpListenerMenuItem.setEnabled(false);
        showSimulationTimeMenuItem.setSelected(IS_SHOW_SIMULATION_TIME);
        showVinMenuItem.setSelected(IS_SHOW_VIN_BY_DEFAULT);
        showIMShapesMenuItem.setSelected(IS_SHOW_IM_DEBUG_SHAPES_BY_DEFAULT);
    }

    /**
     * Use the simulation start GUI setting.
     */
    private void setSimStartGUIsetting() {
        selectedViewer.showCard(ViewerCardType.CANVAS);
        selectedViewer.initWithMap();
        statusPanel.init();

        // update the buttons
        startButton.setText("Pause");
        stepButton.setEnabled(false);
        // update the menu items
    /*
    autoOnlySimTypeMenuItem.setEnabled(false);
    humanOnlySimTypeMenuItem.setEnabled(false);
    mixedSimTypeMenuItem.setEnabled(false);
    */
        startMenuItem.setText("Pause");
        stepMenuItem.setEnabled(false);
        resetMenuItem.setEnabled(true);
        dumpDataMenuItem.setEnabled(true);
        startUdpListenerMenuItem.setEnabled(true);
        clearDebugPointsMenuItem.setEnabled(true);
    }

    /**
     * Use the simulation reset GUI setting.
     */
    private void setSimResetGUIsetting() {
        selectedViewer.cleanUp();
        statusPanel.clear();
        resetButtonMenuItem();
    }

    /**
     * Reset the button menu items.
     */
    private void resetButtonMenuItem() {
        selectedViewer.showCard(ViewerCardType.SIM_SETUP_PANEL);
        // update the buttons
        startButton.setText("Start");
        stepButton.setEnabled(false);
        // update the menu items
    /*
    autoOnlySimTypeMenuItem.setSelected(true);
    humanOnlySimTypeMenuItem.setSelected(false);
    mixedSimTypeMenuItem.setSelected(false);
    */
        startMenuItem.setText("Start");
        stepMenuItem.setEnabled(false);
        resetMenuItem.setEnabled(false);
        dumpDataMenuItem.setEnabled(false);
        startUdpListenerMenuItem.setEnabled(false);
        clearDebugPointsMenuItem.setEnabled(false);
    }

    // ///////////////////////////////
    // SIM METHODS
    // ///////////////////////////////

    /**
     * Start the simulation process.
     *
     * @param initSimSetup the initial simulation setup.
     */
    public void startSimProcess(SimSetup initSimSetup) {
        selectedViewer.createSimulator(initSimSetup);
        // initialize the GUI
        setSimStartGUIsetting();
        // start the thread
        selectedViewer.startSimProcess();
    }

    /**
     * Pause the simulation process.
     */
    public void pauseSimProcess() {
        selectedViewer.pauseSimProcess();

        // update the buttons
        startButton.setText("Resume");
        stepButton.setEnabled(true);
        // update the menu items
        startMenuItem.setText("Resume");
        stepMenuItem.setEnabled(true);
    }


    /**
     * Resume the simulation process.
     */
    public void resumeSimProcess() {
        selectedViewer.resumeSimProcess();

        // update the buttons
        startButton.setText("Pause");
        stepButton.setEnabled(false);
        // update the menu items
        startMenuItem.setText("Pause");
        stepMenuItem.setEnabled(false);
    }

    /**
     * Reset the simulation process.
     */
    public void resetSimProcess() {
        selectedViewer.resetSimProcess();
        setSimResetGUIsetting();
    }

    public void startUdpListening() {
        assert startUdpListenerMenuItem.isEnabled();

        if (selectedViewer.udpListenerHasStarted()) {
            startUdpListenerMenuItem.setEnabled(false);
            stopUdpListenerMenuItem.setEnabled(true);
        } else {
            System.err.printf("Failed to start UDP listener...\n");
        }
    }

    /**
     * Stop the UDP listening
     */
    private void stopUdpListening() {
        assert stopUdpListenerMenuItem.isEnabled();

        if (!selectedViewer.udpListenerHasStarted()) {
            startUdpListenerMenuItem.setEnabled(true);
            stopUdpListenerMenuItem.setEnabled(false);
            selectedViewer.removeUdpListener();
        } else {
            System.err.printf("Failed to stop UDP listener...\n");
        }
    }

    // ///////////////////////////////////////////
    // ActionListener interface for menu items
    // ///////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
    /*
    if (e.getSource() == autoOnlySimTypeMenuItem) {
      throw new RuntimeException("Cannot change simulation type yet.");
      // // very ugly code. need update
      // GridLayoutUtil.setFCFSManagers((GridLayout)sim.getLayout(),
      // sim.getSimulatedTime(),
      // 1.0);
      // GridLayoutUtil.setUniformRandomSpawnPoints((GridLayout)
      // sim.getLayout(),
      // 0.25);
      // sim = new AutoDriverOnlySimulator(sim.getLayout());
      // autoOnlySimTypeMenuItem.setSelected(true);
      // humanOnlySimTypeMenuItem.setSelected(false);
      // mixedSimTypeMenuItem.setSelected(false);
    } else if (e.getSource() == humanOnlySimTypeMenuItem) {
      throw new RuntimeException("Human drivers only simulation not "
          + "implemented yet");
    } else if (e.getSource() == mixedSimTypeMenuItem) {
      throw new RuntimeException("Mixed drivers simulation not "
          + "implemented yet");
    } else
    */
        if (e.getSource() == startMenuItem || e.getSource() == startButton) {
            selectedViewer.startButtonHandler();
            selectedViewer.requestCanvasFocusInWindow();
        } else if (e.getSource() == stepMenuItem || e.getSource() == stepButton) {
            selectedViewer.stepButtonHandler();
            selectedViewer.requestCanvasFocusInWindow();
        } else if (e.getSource() == resetMenuItem) {
            resetSimProcess();
        } else if (e.getSource() == dumpDataMenuItem) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.SAVE_DIALOG);
            int returnVal = chooser.showDialog(this, "Save");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                boolean isDumpData = false;
                String outFileName = null;
                try {
                    outFileName = chooser.getSelectedFile().getCanonicalPath();
                    isDumpData = true;
                } catch (IOException ioe) {
                    // nothing
                }
                if (isDumpData) {
                    selectedViewer.printDataCollectionLinesData(outFileName);
                }
            }
        } else if (e.getSource() == startRecordingMenuItem) {
            if (!selectedViewer.isRecording()) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = chooser.showDialog(this, "Choose Directory");
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        selectedViewer.setImageDir(chooser.getSelectedFile().getCanonicalPath());
                        selectedViewer.setRecording(true);
                    } catch (IOException ioe) {
                        // nothing
                    }
                    if (selectedViewer.isRecording()) {
                        startRecordingMenuItem.setEnabled(false);
                        stopRecordingMenuItem.setEnabled(true);
                    }
                }
            }
        } else if (e.getSource() == stopRecordingMenuItem) {
            if (selectedViewer.isRecording()) {
                selectedViewer.setRecording(false);
                selectedViewer.setImageCounter(0);
                startRecordingMenuItem.setEnabled(true);
                stopRecordingMenuItem.setEnabled(false);
            }
        } else if (e.getSource() == startUdpListenerMenuItem) {
            startUdpListening();
        } else if (e.getSource() == stopUdpListenerMenuItem) {
            stopUdpListening();
        } else if (e.getSource() == clearDebugPointsMenuItem) {
            Debug.clearLongTermDebugPoints();
        } else if ("Quit".equals(e.getActionCommand())) {
            System.exit(0);
        } // else ignore other events
    }

    // ///////////////////////////////
    // ItemListener interface
    // ///////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();

        if (source == showSimulationTimeMenuItem) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedViewer.setIsShowSimulationTime(true);
            } else {
                selectedViewer.setIsShowSimulationTime(false);
            }
            selectedViewer.updateCavas();
        } else if (source == showVinMenuItem) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedViewer.setIsShowVin(true);
            } else {
                selectedViewer.setIsShowVin(false);
            }
            selectedViewer.updateCavas();
        } else if (source == showIMShapesMenuItem) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedViewer.setIsShowIMDebugShapes(true);
            } else {
                selectedViewer.setIsShowIMDebugShapes(false);
            }
            selectedViewer.updateCavas();
        }
    }

}
