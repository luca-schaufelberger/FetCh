package org.oefet.fetch.action

import jisa.control.RTask
import jisa.devices.interfaces.Switch
import jisa.gui.Doc
import jisa.gui.Element
import jisa.results.ResultTable

class Switch : FetChAction("Relay Switch") {

    var task: RTask? = null

    val on     by userInput ("Basic", "On", false)
    val switch by requiredInstrument("Switch", Switch::class)

    val element = Doc("Switch").apply { addText("Switching Relay...").setAlignment(Doc.Align.CENTRE) }

    override fun createDisplay(data: ResultTable): Element = element

    override fun run(results: ResultTable) {
        switch.isOn = on
    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getLabel(): String {
        return if (on) "On" else "Off"
    }

}