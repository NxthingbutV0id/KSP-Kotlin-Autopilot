//NxthingbutV0id - 2023/5/27

import krpc.client.*
import krpc.client.services.SpaceCenter
import krpc.client.services.SpaceCenter.*
import kotlin.math.*

fun main() {
    val program = LaunchProgram()
    program.launch(80000F)
}

class LaunchProgram {
    private val conn: Connection = Connection.newInstance("Orbit Program")
    private val spaceCenter: SpaceCenter = newInstance(conn)
    private val vessel: Vessel = spaceCenter.activeVessel
    private val rf: ReferenceFrame = vessel.surfaceReferenceFrame
    private val flight: Flight = vessel.flight(rf)
    private val altitude: Stream<Double> = conn.addStream(flight, "getMeanAltitude")
    private val apoapsis: Stream<Double> = conn.addStream(vessel.orbit, "getApoapsisAltitude")
    private val periapsis: Stream<Double> = conn.addStream(vessel.orbit, "getPeriapsisAltitude")
    private var running = false

    private fun init() {
        vessel.control.sas = false
        vessel.control.rcs = false
        vessel.control.throttle = 1.0F
        running = true
    }

    fun launch(targetApoapsis: Float) {
        init()
        println("3...")
        Thread.sleep(1000)
        println("2...")
        Thread.sleep(1000)
        println("1...")
        Thread.sleep(1000)

        vessel.control.activateNextStage()
        vessel.autoPilot.engage()
        vessel.autoPilot.targetPitchAndHeading(90F, 90F)
        println("Liftoff!")
        while (running) {
            gravityTurn()
            if (apoapsis.get() > targetApoapsis * 0.9) {
                limitThrottle(targetApoapsis)
            }
            if (altitude.get() >= 70000) {
                circularize(targetApoapsis)
            }
        }
        programFinished()
    }

    private fun gravityTurn() {
        val newPitch: Double = 90 - sqrt(altitude.get())/3
        if (newPitch > 0.0) {
            vessel.autoPilot.targetPitch = newPitch.toFloat()
        }
    }

    private fun limitThrottle(targetApoapsis: Float) {
        val newThrottle = -((10 * apoapsis.get())/targetApoapsis)+10
        vessel.control.throttle = newThrottle.toFloat()
    }

    private fun circularize(targetApoapsis: Float) {
        vessel.control.throttle = 0F

        while (vessel.orbit.periapsisAltitude < 0.9*targetApoapsis) {
            burnForPeriapsis(targetApoapsis)
        }

        vessel.control.throttle = 0F
        running = false
    }

    private fun burnForPeriapsis(targetApoapsis: Float) {
        val newThrottle = 1 - (periapsis.get()/targetApoapsis)
        vessel.control.throttle = newThrottle.toFloat()
        val newPitch = 360 - ((360 * apoapsis.get())/targetApoapsis)
        vessel.autoPilot.targetPitch = newPitch.toFloat()
    }

    private fun programFinished() {
        println("Program Finished")
        println("Final Orbit:")
        println("Apoapsis: ${apoapsis.get().toLong()} meters")
        println("Periapsis: ${periapsis.get().toLong()} meters")
        println("Eccentricity: ${round(vessel.orbit.eccentricity * 100)/100}")
        conn.close()
    }
}