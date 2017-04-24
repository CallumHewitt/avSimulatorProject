package aim4.gui.parampanel.cpm.components;

import aim4.gui.parampanel.cpm.CPMBasicParamPanel;
import aim4.sim.setup.cpm.BasicCPMSimSetup;
import aim4.sim.setup.cpm.CPMSingleWidthSimSetup;

import javax.swing.*;
import java.awt.*;

/**
 * A label which shows an updated value for the area of the car park.
 */
public class CPMMapAreaLabel extends JLabel {

    /**
     * The string to put before the value.
     */
    private final String prefix;
    private double value;


    public CPMMapAreaLabel(String prefix, BasicCPMSimSetup simSetup) {
        super(prefix);
        this.prefix = prefix;
        this.value = calculateArea(simSetup);
        this.setAlignmentX(Component.CENTER_ALIGNMENT);
        setText(prefix + String.format("%.2f", value));
    }

    private double calculateArea(BasicCPMSimSetup simSetup) {
        if (simSetup instanceof CPMSingleWidthSimSetup) { // TODO CPM We should get this value from somewhere else and remove this assertion
            double parkingAreaLength = (2 * ((CPMSingleWidthSimSetup)simSetup).getAccessLength())
                    + (2 * ((CPMSingleWidthSimSetup)simSetup).getLaneWidth()) + simSetup.getParkingLength();

            // Add the area of the parking area (w*h) (+1 to account for the top WEST road)
            double totalArea = parkingAreaLength
                    * ((((CPMSingleWidthSimSetup)simSetup).getNumberOfParkingLanes() + 1)
                    * ((CPMSingleWidthSimSetup)simSetup).getLaneWidth());

            // Add the West road, but only up to the
            // length of the parking area

            return totalArea;
        } else {
            // TODO CPM finish this
            return 0;
        }
    }

    public void updateAreaValue(CPMBasicParamPanel paramPanel) {

        double totalArea = paramPanel.calculateCarParkArea();

        // Add the West road, but only up to the
        // length of the parking area
        setText(prefix + String.format("%.2f", totalArea));
    }
}