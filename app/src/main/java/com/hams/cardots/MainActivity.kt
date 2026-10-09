package com.hams.cardots

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var btn: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("cardots", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }
        val title = TextView(this).apply {
            text = "Araç Noktaları"
            textSize = 26f
        }
        val info = TextView(this).apply {
            text = "Başlat'a bas, sonra istediğin uygulamayı aç. Ekranın kenarlarında araç " +
                "hareketine göre kayan noktalar görünür; bu araçta telefona bakınca olan mide " +
                "bulantısını azaltmaya yardım eder."
            textSize = 15f
            setPadding(0, dp(12), 0, dp(24))
        }
        btn = Button(this).apply {
            textSize = 18f
            setOnClickListener { toggle() }
        }
        val label = TextView(this).apply {
            text = "Hassasiyet (noktalar ne kadar çok kaysın)"
            setPadding(0, dp(32), 0, dp(8))
        }
        val seek = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("gain", 50)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    prefs.edit().putInt("gain", p).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT
        root.addView(title, LinearLayout.LayoutParams(mp, wc))
        root.addView(info, LinearLayout.LayoutParams(mp, wc))
        root.addView(btn, LinearLayout.LayoutParams(mp, wc))
        root.addView(label, LinearLayout.LayoutParams(mp, wc))
        root.addView(seek, LinearLayout.LayoutParams(mp, wc))
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        DotsService.listener = { refresh() }
        refresh()
    }

    override fun onPause() {
        DotsService.listener = null
        super.onPause()
    }

    private fun refresh() {
        btn.text = if (DotsService.running) "Durdur" else "Başlat"
    }

    private fun toggle() {
        val svc = Intent(this, DotsService::class.java)
        if (DotsService.running) {
            stopService(svc)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Diğer uygulamaların üzerinde gösterme iznini aç", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        startForegroundService(svc)
    }
}
