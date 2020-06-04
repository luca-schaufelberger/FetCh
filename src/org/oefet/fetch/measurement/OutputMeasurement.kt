package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.experiment.Col
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.maths.Range

class OutputMeasurement : FetChMeasurement() {

    private var sdSMU : SMU? = null
    private var sgSMU : SMU? = null
    private var gdSMU : SMU? = null
    private var fpp1  : VMeter? = null
    private var fpp2  : VMeter? = null
    private var tm    : TMeter? = null

    override val type = "Output"

    // Measurement paramaters
    val label   = StringParameter("Basic", "Label", null, "Output")
    val intTime = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    val delTime = DoubleParameter("Basic", "Delay Time", "s", 0.5)
    val minVSD  = DoubleParameter("Source-Drain", "Start", "V", 0.0)
    val maxVSD  = DoubleParameter("Source-Drain", "Stop", "V", 60.0)
    val numVSD  = IntegerParameter("Source-Drain", "No. Steps", null, 61)
    val symVSD  = BooleanParameter("Source-Drain", "Sweep Both Ways", null, true)
    val minVSG  = DoubleParameter("Source-Gate", "Start", "V", 0.0)
    val maxVSG  = DoubleParameter("Source-Gate", "Stop", "V", 60.0)
    val numVSG  = IntegerParameter("Source-Gate", "No. Steps", null, 7)

    override fun loadInstruments(instruments: Instruments) {

        if (!instruments.hasSD || !instruments.hasSG) {
            throw Exception("Source-Drain and Source-Gate SMUs must be configured first")
        }

        sdSMU = instruments.sdSMU!!
        sgSMU = instruments.sgSMU!!
        gdSMU = instruments.gdSMU
        fpp1  = instruments.fpp1
        fpp2  = instruments.fpp2
        tm    = instruments.tm

    }

    override fun setLabel(value: String?) {
        label.value = value
    }

    override fun run(results: ResultTable) {

        val intTime = intTime.value
        val delTime = (delTime.value * 1000).toInt()
        val minVSD  = minVSD.value
        val maxVSD  = maxVSD.value
        val numVSD  = numVSD.value
        val minVSG  = minVSG.value
        val maxVSG  = maxVSG.value
        val numVSG  = numVSG.value
        val symVSD  = symVSD.value

        val sdSMU = this.sdSMU!!
        val sgSMU = this.sgSMU!!

        val sdVoltages = if (symVSD) {
            Range.linear(minVSD, maxVSD, numVSD).mirror()
        } else {
            Range.linear(minVSD, maxVSD, numVSD)
        }
        val sgVoltages = Range.linear(minVSG, maxVSG, numVSG)


        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = minVSD
        sgSMU.voltage = minVSG
        gdSMU?.voltage = 0.0

        // Configure integration times
        sdSMU.integrationTime = intTime
        sgSMU.integrationTime = intTime
        fpp1?.integrationTime = intTime
        fpp2?.integrationTime = intTime

        sdSMU.turnOn()
        sgSMU.turnOn()
        gdSMU?.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()

        for (vSG in sgVoltages) {

            sgSMU.voltage = vSG

            for (vSD in sdVoltages) {

                sdSMU.voltage = vSD

                sleep(delTime)

                results.addData(
                    vSD, vSG,
                    sdSMU.voltage, sdSMU.current,
                    sgSMU.voltage, sgSMU.current,
                    fpp1?.voltage ?: Double.NaN, fpp2?.voltage ?: Double.NaN,
                    tm?.temperature ?: Double.NaN,
                    gdSMU?.current ?: Double.NaN
                )

            }

        }

    }

    override fun onFinish() {

        runRegardless { sdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }

    }

    override fun getLabel(): String = label.value

    override fun getName(): String = "Output Measurement"

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Set SD Voltage", "V"),
            Col("Set SG Voltage", "V"),
            Col("SD Voltage", "V"),
            Col("SD Current", "A"),
            Col("SG Voltage", "V"),
            Col("SG Current", "A"),
            Col("Four Point Probe 1", "V"),
            Col("Four Point Probe 2", "V"),
            Col("Temperature", "K"),
            Col("Ground Current", "A")
        )

    }

    override fun onInterrupt() {

        Util.errLog.println("Transfer measurement interrupted.")

    }

}
