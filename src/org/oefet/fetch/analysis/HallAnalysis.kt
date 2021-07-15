package org.oefet.fetch.analysis


import jisa.experiment.Combination
import jisa.gui.Plot
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultList
import org.oefet.fetch.gui.elements.FetChPlot
import org.oefet.fetch.quantities.*
import java.util.*
import kotlin.reflect.KClass

object HallAnalysis : Analysis {

    override fun analyse(quantities: List<Quantity>, labels: Map<KClass<out Quantity>, Map<Double, String>>): Analysis.Output {

        val processed = tabulate(quantities)
        val plots     = LinkedList<Plot>()

        for (table in processed) {

            // The columns indices for the value and its error (final two columns)
            val valueIndex = table.parameters.size
            val errorIndex = valueIndex + 1

            val value = table.table.getColumn(valueIndex) as Column<Double>
            val error = table.table.getColumn(errorIndex) as Column<Double>

            // Loop over all the parameter columns in the table
            for ((paramIndex, parameter) in table.parameters.withIndex()) {

                val param = table.table.getColumn(paramIndex) as Column<Double>

                // If the quantity isn't varied or is not meant to be displayed as a number, then skip it
                if (labels.containsKey(parameter::class)) continue
                if (table.table.getUniqueValues(param).size < 2) continue

                val splits     = LinkedList<Int>()
                val names      = LinkedHashMap<Int, Quantity>()
                var splitCount = 1

                // Loop over all other varied parameters in the table
                for ((splitIndex, splitParam) in table.parameters.withIndex()) {

                    val split = table.table.getColumn(splitIndex)

                    if (splitIndex != paramIndex && table.table.getUniqueValues(split).size > 1) {
                        splits            += splitIndex
                        names[splitIndex]  = splitParam
                        splitCount        *= table.table.getUniqueValues(split).size
                    }

                }

                // Don't plot if more values in legend than x-axis
                if ((table.table.getUniqueValues(param).size) < splitCount) continue

                // Create the plot and the data series
                val line   = table.table.getUniqueValues(param).size > 20
                val plot   = FetChPlot("${table.quantity.name} vs ${parameter.name}")
                val series = plot.createSeries()
                    .watch(table.table, param, value, error)
                    .setColour(AutoAnalysis.colours[plots.size % AutoAnalysis.colours.size])
                    .setMarkerVisible(!line)
                    .setLineVisible(line)

                if (splits.isNotEmpty()) {

                    series.split(

                        // Split by the unique combination of all varied parameters not on the x-axis
                        { row -> Combination(*splits.map { if (row[it].isFinite()) row[it] else Double.NEGATIVE_INFINITY }.toTypedArray()) },

                        // Label each legend item with any pre-defined labels, or default to x = n.nnn U
                        { row -> names.entries.joinToString(" \t ") { labels[it.value::class]?.get(row[it.key]) ?: "%s = %.4g %s".format(it.value.symbol, row[it.key], it.value.unit) } }

                    )

                    plot.isLegendVisible = true

                } else {

                    plot.isLegendVisible = false

                }

                // Make sure the plot is user-interactive via the mouse
                plot.isMouseEnabled = true
                plot.autoLimits()

                plots += plot

            }

        }

        return Analysis.Output(processed, plots)

    }

    private fun tabulate(quantities: List<Quantity>): List<Analysis.Tabulated> {

        val tables      = LinkedList<Analysis.Tabulated>()
        val quantitySet = quantities.map { it::class }.filter{ it != MConductivity::class }.toSet()

        for (quantityClass in quantitySet) {

            val filtered     = quantities.filter { it::class == quantityClass }
            val instance     = filtered.first()

            val table = ResultList(
                DoubleColumn("Temperature", "K"),
                DoubleColumn("Conductivity", "S/cm"),
                DoubleColumn("Gate", "V"),
                DoubleColumn("Device"),
                DoubleColumn(instance.name, instance.unit),
                DoubleColumn("${instance.name} Error", instance.unit)
            )

            for (value in filtered) {

                val temperature  = value.parameters.find { it is Temperature }
                val conductivity = value.parameters.find { it is Conductivity }
                val gate         = value.parameters.find { it is Gate }
                val device       = value.parameters.find { it is Device }

                table.addData(
                    temperature?.value  ?: Double.NaN,
                    conductivity?.value ?: Double.NaN,
                    gate?.value         ?: 0.0,
                    device?.value       ?: Double.NaN,
                    value.value,
                    value.error
                )

            }

            tables += Analysis.Tabulated(listOf(
                Temperature(0.0, 0.0),
                Conductivity(0.0, 0.0),
                Gate(0.0, 0.0),
                Device(0.0)
            ), instance, table)

        }

        val magnetoConductivities = quantities.filter { it is MConductivity }

        if (magnetoConductivities.isNotEmpty()) {

            val table = ResultList(
                DoubleColumn("Field", "T"),
                DoubleColumn("Temperature", "K"),
                DoubleColumn("Gate", "V"),
                DoubleColumn("Device"),
                DoubleColumn("Magneto-Conductivity", "S/cm"),
                DoubleColumn("Error", "S/cm")
            )

            for (value in magnetoConductivities) {

                val field       = value.parameters.find { it is BField } ?: continue
                val temperature = value.parameters.find { it is Temperature }
                val gate        = value.parameters.find { it is Gate }
                val device      = value.parameters.find { it is Device }

                table.addData(
                    field.value,
                    temperature?.value ?: Double.NaN,
                    gate?.value        ?: 0.0,
                    device?.value      ?: Double.NaN,
                    value.value,
                    value.error
                )

            }

            tables += Analysis.Tabulated(
                listOf(BField(0.0, 0.0), Temperature(0.0, 0.0), Gate(0.0, 0.0), Device(0.0)),
                MConductivity(0.0, 0.0),
                table
            )

        }

        return tables.sortedBy { it.quantity.name }

    }

}