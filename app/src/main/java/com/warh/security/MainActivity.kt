package com.warh.security

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var rootStatus: TextView
    private lateinit var result: TextView
    private lateinit var scanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bg = Color.rgb(10, 15, 25)
        val green = Color.rgb(70, 240, 175)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 35, 24, 24)
            setBackgroundColor(bg)
        }

        val title = TextView(this).apply {
            text = "WARH"
            textSize = 38f
            setTextColor(green)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "NETWORK SECURITY TOOLKIT"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        rootStatus = TextView(this).apply {
            text = "Checking root access..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(16, 22, 16, 22)
            setBackgroundColor(Color.rgb(25, 38, 52))
        }

        scanButton = Button(this).apply {
            text = "SCAN MY WI-FI"
            isAllCaps = false
            setOnClickListener { scanNetwork() }
        }

        result = TextView(this).apply {
            text = "Scan results will appear here."
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(16, 20, 16, 20)
            setBackgroundColor(Color.rgb(25, 38, 52))
        }

        val warning = TextView(this).apply {
            text = "Only scan networks you own or have permission to test."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 18, 0, 18)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(rootStatus)
        layout.addView(scanButton)
        layout.addView(warning)
        layout.addView(result)

        setContentView(ScrollView(this).apply {
            addView(layout)
        })

        thread {
            val hasRoot = checkRoot()
            runOnUiThread {
                rootStatus.text = if (hasRoot) {
                    "ROOT ACCESS: AVAILABLE"
                } else {
                    "ROOT ACCESS: NOT AVAILABLE"
                }
                rootStatus.setTextColor(
                    if (hasRoot) green else Color.YELLOW
                )
                scanButton.isEnabled = hasRoot
            }
        }
    }

    private fun checkRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("su", "-c", "id")
            )
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor() == 0 && output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    private fun scanNetwork() {
        scanButton.isEnabled = false
        result.text = "Checking your Wi-Fi network..."

        thread {
            val report = StringBuilder()

            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()

                val wifi = interfaces.toList().firstOrNull {
                    it.isUp && !it.isLoopback &&
                    (it.name.startsWith("wlan") ||
                     it.name.startsWith("wifi"))
                }

                val address = wifi?.interfaceAddresses
                    ?.firstOrNull { it.address is Inet4Address }

                val ip = address?.address?.hostAddress

                if (ip == null) {
                    report.append("Wi-Fi IPv4 address not found.")
                } else {
                    report.append("YOUR WI-FI INFORMATION\n\n")
                    report.append("Local IP: $ip\n")
                    report.append("Interface: ${wifi?.name}\n\n")
                    report.append("Basic network check completed.")
                }
            } catch (e: Exception) {
                report.append("Scan error: ${e.message}")
            }

            runOnUiThread {
                result.text = report.toString()
                scanButton.isEnabled = checkRoot()
            }
        }
    }
}
