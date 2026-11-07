package de.yannick.thbwidget

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

/** Kleines Eingabefenster: EUR eintippen, THB sofort sehen, "Übernehmen" schreibt es ins Widget. */
class ConvertActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var result: TextView
    private lateinit var rateLine: TextView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convert)
        setFinishOnTouchOutside(true)

        input = findViewById(R.id.input)
        result = findViewById(R.id.result)
        rateLine = findViewById(R.id.rate_line)
        status = findViewById(R.id.status_line)

        val start = RateStore.amount(this)
        input.setText(formatForInput(start))
        input.selectAll()
        input.requestFocus()

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = updateResult()
        })
        input.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) { save(); true } else false
        }

        findViewById<Button>(R.id.save).setOnClickListener { save() }
        findViewById<Button>(R.id.cancel).setOnClickListener { finish() }

        updateResult()

        // Kurs im Hintergrund auffrischen, damit die Umrechnung aktuell ist
        Thread {
            if (RateStore.refresh(applicationContext)) {
                runOnUiThread { if (!isFinishing) updateResult() }
                RateWidget.renderAll(applicationContext)
            }
        }.start()
    }

    private fun updateResult() {
        val rate = RateStore.rate(this)
        val amount = RateStore.parseAmount(input.text.toString())
        rateLine.text = if (rate != null) "1 € = ${RateStore.formatRate(rate)} ฿" else "Kurs wird geladen …"
        status.text = RateStore.statusLine(this)
        result.text = when {
            rate == null -> "– ฿"
            amount == null -> "0 ฿"
            else -> RateStore.formatThb(amount * rate)
        }
    }

    private fun save() {
        RateStore.parseAmount(input.text.toString())?.let { RateStore.setAmount(this, it) }
        RateWidget.renderAll(applicationContext)
        finish()
    }

    private fun formatForInput(v: Double): String =
        if (v == Math.floor(v)) v.toLong().toString() else v.toString().replace('.', ',')
}
