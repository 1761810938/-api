package cn.setbug.sub2apiusage

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val ACTION_REFRESH_WIDGET = "cn.setbug.sub2apiusage.action.REFRESH_WIDGET"

class UsageWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
            refreshWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetStore.removeWidget(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_REFRESH_WIDGET) return
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val manager = AppWidgetManager.getInstance(context)
        updateWidget(context, manager, appWidgetId)
        refreshWidget(context, manager, appWidgetId)
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java))
            ids.forEach { appWidgetId ->
                updateWidget(context, manager, appWidgetId)
                refreshWidget(context, manager, appWidgetId)
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val repository = siteRepository(context)
            val siteId = WidgetStore.loadWidgetSite(context, appWidgetId)
            val site = repository.loadSites().firstOrNull { it.id == siteId }
            val snapshot = WidgetStore.loadSnapshot(context, appWidgetId)
            val error = WidgetStore.loadError(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_usage)

            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            if (site == null) {
                views.setTextViewText(R.id.widgetTitle, "Sub2API 用量")
                views.setTextViewText(R.id.widgetSubtitle, "先在 App 里添加站点")
                views.setTextViewText(R.id.widgetBadge, "SETUP")
                views.setTextViewText(R.id.widgetPrimary, "未配置")
                views.setTextViewText(R.id.widgetSecondary, "添加小组件后选择站点")
                views.setTextViewText(R.id.widgetMetricLabelLeft, "站点")
                views.setTextViewText(R.id.widgetMetricValueLeft, "未绑定")
                views.setTextViewText(R.id.widgetMetricLabelRight, "状态")
                views.setTextViewText(R.id.widgetMetricValueRight, "等待设置")
                views.setTextViewText(R.id.widgetFooter, "点这里打开 App")
                views.setOnClickPendingIntent(R.id.widgetRoot, launchPendingIntent)
            } else {
                views.setTextViewText(R.id.widgetTitle, site.name.ifBlank { site.baseUrl })
                views.setTextViewText(R.id.widgetSubtitle, site.email)
                when {
                    snapshot != null -> {
                        views.setTextViewText(R.id.widgetBadge, "LIVE")
                        views.setTextViewText(R.id.widgetPrimary, "${snapshot.totalRequests} 请求")
                        views.setTextViewText(R.id.widgetSecondary, "${snapshot.totalActualCost} · 平均 ${snapshot.averageCostPerRequest}/次")
                        views.setTextViewText(R.id.widgetMetricLabelLeft, "总 Token")
                        views.setTextViewText(R.id.widgetMetricValueLeft, snapshot.totalTokens)
                        views.setTextViewText(R.id.widgetMetricLabelRight, "平均耗时")
                        views.setTextViewText(R.id.widgetMetricValueRight, snapshot.averageDurationMs)
                        views.setTextViewText(R.id.widgetFooter, "更新：${snapshot.updatedAtLabel}")
                    }
                    !error.isNullOrBlank() -> {
                        views.setTextViewText(R.id.widgetBadge, "ERROR")
                        views.setTextViewText(R.id.widgetPrimary, "查询失败")
                        views.setTextViewText(R.id.widgetSecondary, error.take(48))
                        views.setTextViewText(R.id.widgetMetricLabelLeft, "站点")
                        views.setTextViewText(R.id.widgetMetricValueLeft, site.name.ifBlank { "已绑定" })
                        views.setTextViewText(R.id.widgetMetricLabelRight, "操作")
                        views.setTextViewText(R.id.widgetMetricValueRight, "点按重试")
                        views.setTextViewText(R.id.widgetFooter, "点这里打开 App")
                    }
                    else -> {
                        views.setTextViewText(R.id.widgetBadge, "READY")
                        views.setTextViewText(R.id.widgetPrimary, "等待查询")
                        views.setTextViewText(R.id.widgetSecondary, "添加后会自动刷新")
                        views.setTextViewText(R.id.widgetMetricLabelLeft, "总 Token")
                        views.setTextViewText(R.id.widgetMetricValueLeft, "--")
                        views.setTextViewText(R.id.widgetMetricLabelRight, "平均耗时")
                        views.setTextViewText(R.id.widgetMetricValueRight, "--")
                        views.setTextViewText(R.id.widgetFooter, "点按立即刷新")
                    }
                }

                val refreshIntent = Intent(context, UsageWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widgetRoot, refreshPendingIntent)
            }

            views.setOnClickPendingIntent(R.id.widgetFooter, launchPendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun refreshWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val repository = siteRepository(context)
            val siteId = WidgetStore.loadWidgetSite(context, appWidgetId)
            val site = repository.loadSites().firstOrNull { it.id == siteId } ?: return
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { Sub2ApiClient.fetchTodayUsage(site) }
                    .onSuccess { snapshot ->
                        WidgetStore.saveSnapshot(context, appWidgetId, snapshot)
                        updateWidget(context, appWidgetManager, appWidgetId)
                    }
                    .onFailure { error ->
                        WidgetStore.saveError(context, appWidgetId, error.message ?: "查询失败")
                        updateWidget(context, appWidgetManager, appWidgetId)
                    }
            }
        }
    }
}
