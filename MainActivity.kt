package com.smsbuddy.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var sw: SwitchMaterial
    private lateinit var tvStatus: TextView
    private lateinit var tvCount: TextView
    private lateinit var etGmail: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var etTo: TextInputEditText
    private var updating = false

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enable() else {
            setSwitch(false)
            toast("Allow SMS access so SMS Buddy can see new texts")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sw = findViewById(R.id.switchForward)
        tvStatus = findViewById(R.id.tvStatus)
        tvCount = findViewById(R.id.tvCount)
        etGmail = findViewById(R.id.etGmail)
        etPass = findViewById(R.id.etPassword)
        etTo = findViewById(R.id.etTo)

        etGmail.setText(Prefs.gmail(this))
        etPass.setText(Prefs.password(this))
        etTo.setText(Prefs.to(this))

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            if (saveFields()) toast("Saved 💜")
        }
        findViewById<MaterialButton>(R.id.btnTest).setOnClickListener { sendTest() }

        sw.setOnCheckedChangeListener { _, checked ->
            if (updating) return@setOnCheckedChangeListener
            if (checked) tryEnable() else {
                Prefs.setEnabled(this, false)
                refresh()
            }
        }
        setSwitch(Prefs.isEnabled(this) && hasSmsPermission())
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun hasSmsPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    private fun tryEnable() {
        if (!saveFields()) { setSwitch(false); return }
        if (hasSmsPermission()) enable() else permLauncher.launch(Manifest.permission.RECEIVE_SMS)
    }

    private fun enable() {
        Prefs.setEnabled(this, true)
        setSwitch(true)
        refresh()
        toast("Forwarding is on 🎉")
    }

    private fun setSwitch(on: Boolean) {
        updating = true; sw.isChecked = on; updating = false
    }

    private fun refresh() {
        tvStatus.text = if (Prefs.isEnabled(this)) "On: new texts fly to your inbox ✈️" else "Off: flip the switch to start"
        tvCount.text = Prefs.count(this).toString()
    }

    private fun saveFields(): Boolean {
        val g = etGmail.text.toString().trim()
        val p = etPass.text.toString().replace(" ", "")
        val t = etTo.text.toString().trim().ifEmpty { g }
        if (!Patterns.EMAIL_ADDRESS.matcher(g).matches()) { etGmail.error = "Enter your Gmail address"; return false }
        if (p.length < 16) { etPass.error = "Paste the 16-letter app password"; return false }
        if (!Patterns.EMAIL_ADDRESS.matcher(t).matches()) { etTo.error = "Enter a valid email"; return false }
        Prefs.save(this, g, p, t)
        etTo.setText(t)
        return true
    }

    private fun sendTest() {
        if (!saveFields()) return
        toast("Sending a test…")
        val g = Prefs.gmail(this); val p = Prefs.password(this); val t = Prefs.to(this)
        thread {
            try {
                Mailer.send(g, p, t, "✅ SMS Buddy test", "Hooray! Your setup works. Texts will land here. 💌")
                runOnUiThread { toast("Test sent! Check your inbox 🎉") }
            } catch (e: Exception) {
                runOnUiThread { toast("Couldn't send: ${e.message}") }
            }
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
