package cn.setbug.sub2apiusage

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigActivity : AppCompatActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repository = siteRepository(this)
        val sites = repository.loadSites()
        val emptyView = findViewById<TextView>(R.id.emptyHint)
        val spinner = findViewById<Spinner>(R.id.siteSpinner)
        val confirmButton = findViewById<Button>(R.id.confirmButton)

        if (sites.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            spinner.visibility = View.GONE
            confirmButton.isEnabled = false
            return
        }

        val labels = sites.map { site -> site.name.ifBlank { site.baseUrl } }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        var selectedIndex = 0
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        confirmButton.setOnClickListener {
            val site = sites.getOrNull(selectedIndex) ?: return@setOnClickListener
            WidgetStore.saveWidgetSite(this, appWidgetId, site.id)
            val manager = AppWidgetManager.getInstance(this)
            UsageWidgetProvider.updateWidget(this, manager, appWidgetId)
            UsageWidgetProvider.refreshWidget(this, manager, appWidgetId)

            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }
}
