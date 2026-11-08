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

/** Kleines Eingabefenster: Betrag eintippen, Umrechnung sofort sehen, ⇄ dreht die Richtung. */
class ConvertActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var result: TextView
    private lateinit var rateLine: TextView
    private lateinit var status: TextView
    private lateinit var swap: TextView
    private lateinit var inputCurrency: TextView

    /** false = € eingeben → ฿, true = ฿ eingeben → € */
    private var thbToEur = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convert)
        setFinishOnTouchOutside(true)

        input = findViewById(R.id.input)
        result = findViewById(R.id.result)
        rateLine = findViewById(R.id.rate_line)
        status = findViewById(R.id.status_line)
        swap = findViewById(R.id.swap)
        inputCurrency = findViewById(R.id.input_currency)
        findViewById<android.widget.ImageView>(R.id.title_art)
            .setImageBitmap(TitleArt.render(this, getString(R.string.app_name), 52f))

        thbToEur = RateStore.thbToEur(this)
        setInput(RateStore.amount(this))
        input.requestFocus()

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = updateResult()
        })
        input.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) { finish(); true } else false
        }

        swap.setOnClickListener { toggle() }
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

    /** Richtung umdrehen; das bisherige Ergebnis wird zur neuen Eingabe. */
    private fun toggle() {
        val rate = RateStore.rate(this)
        val amount = RateStore.parseAmount(input.text.toString())
        val newInput = if (rate != null && amount != null)
            RateStore.roundNice(RateStore.convert(amount, rate, thbToEur)) else amount
        thbToEur = !thbToEur
        if (newInput != null) setInput(newInput) else input.setText("")
        updateResult()
    }

    private fun setInput(v: Double) {
        input.setText(formatForInput(v))
        input.selectAll()
    }

    private fun updateResult() {
        val rate = RateStore.rate(this)
        val amount = RateStore.parseAmount(input.text.toString())
        inputCurrency.text = if (thbToEur) "฿" else "€"
        rateLine.text = if (rate != null) "1 € = ${RateStore.formatRate(rate)} ฿" else "Kurs wird geladen …"
        status.text = RateStore.statusLine(this)
        result.text = when {
            rate == null -> if (thbToEur) "– €" else "– ฿"
            amount == null -> RateStore.formatOut(0.0, thbToEur)
            else -> RateStore.formatOut(RateStore.convert(amount, rate, thbToEur), thbToEur)
        }
    }

    /** Beim Verlassen (Schließen, daneben tippen, Zurück) automatisch ins Widget übernehmen. */
    override fun onPause() {
        super.onPause()
        save()
    }

    private fun save() {
        RateStore.setThbToEur(this, thbToEur)
        RateStore.parseAmount(input.text.toString())?.let { RateStore.setAmount(this, it) }
        RateWidget.renderAll(applicationContext)
    }

    private fun formatForInput(v: Double): String =
        if (v == Math.floor(v)) v.toLong().toString() else v.toString().replace('.', ',')
}
