package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.ProbeStation
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.gui.Colour
import jisa.gui.Series
import org.oefet.fetch.gui.elements.FetChPlot

class PositionChange : FetChAction("Change Position") {

    val pControl      by requiredConfig("Position Controller", ProbeStation::class)
    val xposition     by input("Position", "x Position [m]", 1e-3)
    val yposition     by input("Position", "y Position [m]", 1e-3)
    val zposition     by input("Position", "z Position [m]", 1e-3)

    override fun createPlot(data: ResultTable): FetChPlot {

        val plot =  FetChPlot("Change Position to ($yposition m, $xposition m, $zposition m)")

        plot.isLegendVisible = false

        return plot

    }

    override fun run(results: ResultTable) {

        pControl.isLocked  = false
        pControl.setXYPosition(xposition, yposition)
        pControl.zPosition = zposition

    }

    override fun onFinish() {
        
    }

    override fun getLabel(): String {
        return "$xposition, $yposition, $zposition"
    }

}