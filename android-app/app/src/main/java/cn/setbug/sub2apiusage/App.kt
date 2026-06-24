package cn.setbug.sub2apiusage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val PageBackground = Color(0xFFF4F7FB)
private val CardBackground = Color.White
private val PrimaryText = Color(0xFF0F172A)
private val SecondaryText = Color(0xFF64748B)
private val SoftBlue = Color(0xFFE0F2FE)
private val SoftPurple = Color(0xFFF3E8FF)
private val SoftGreen = Color(0xFFDCFCE7)
private val SoftOrange = Color(0xFFFFEDD5)
private val AccentBlue = Color(0xFF2563EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sub2ApiUsageApp(viewModel: Sub2ApiViewModel = rememberSub2ApiViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current.applicationContext
    val snackbarHostState = remember { SnackbarHostState() }
    var editingSiteId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedSiteIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val editingSite = state.sites.firstOrNull { it.id == editingSiteId }
    val allExpanded = state.sites.isNotEmpty() && expandedSiteIds.size == state.sites.size

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Sub2API 用量") },
                    actions = {
                        if (state.sites.isNotEmpty()) {
                            TextButton(onClick = {
                                expandedSiteIds = if (allExpanded) emptyList() else state.sites.map { it.id }
                            }) {
                                Text(if (allExpanded) "全部收起" else "全部展开")
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    editingSiteId = null
                    showEditor = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "添加站点")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(PageBackground)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        IntroCard()
                    }

                    if (state.sites.isEmpty()) {
                        item {
                            EmptyCard()
                        }
                    }

                    itemsIndexed(state.sites, key = { _, site -> site.id }) { _, site ->
                        val loading = state.loadingSiteId == site.id
                        val usage = state.usageBySiteId[site.id]
                        val expanded = site.id in expandedSiteIds
                        SiteCard(
                            site = site,
                            usage = usage,
                            loading = loading,
                            expanded = expanded,
                            onToggleExpand = {
                                expandedSiteIds = if (expanded) {
                                    expandedSiteIds - site.id
                                } else {
                                    expandedSiteIds + site.id
                                }
                            },
                            onRefresh = {
                                viewModel.refreshSite(site.id)
                                UsageWidgetProvider.refreshAll(context)
                            },
                            onEdit = {
                                editingSiteId = site.id
                                showEditor = true
                            },
                            onDelete = {
                                expandedSiteIds = expandedSiteIds - site.id
                                viewModel.deleteSite(site.id)
                                UsageWidgetProvider.refreshAll(context)
                            }
                        )
                    }
                }

                if (showEditor) {
                    SiteEditorSheet(
                        initialSite = editingSite,
                        onDismiss = {
                            editingSiteId = null
                            showEditor = false
                        },
                        onSave = { site ->
                            viewModel.saveSite(site)
                            UsageWidgetProvider.refreshAll(context)
                            editingSiteId = null
                            showEditor = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFE0F2FE), Color(0xFFF5F3FF))
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("今日查询面板", color = AccentBlue, fontWeight = FontWeight.Bold)
                Text("支持保存多个站点，每个站点单独配置 URL、账号和密码。", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Text("查询后会展示请求数、Token、消费、时延和衍生指标。", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("还没有站点", fontWeight = FontWeight.Bold, color = PrimaryText)
            Text(
                "点右下角加号，添加不同网站的 URL、邮箱和密码后就能单独查询。",
                color = SecondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SiteCard(
    site: Sub2ApiSite,
    usage: UsageSnapshot?,
    loading: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(site.name.ifBlank { site.baseUrl }, fontWeight = FontWeight.Bold, color = PrimaryText)
                    Spacer(Modifier.height(4.dp))
                    Text(site.baseUrl, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(2.dp))
                    Text(site.email, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                }
                if (usage != null) {
                    Text(usage.totalActualCost, color = AccentBlue, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(10.dp))
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = SecondaryText
                )
            }

            if (expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text("编辑") }
                    TextButton(onClick = onRefresh) { Text("查询") }
                    TextButton(onClick = onDelete) { Text("删除") }
                }

                when {
                    loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("正在读取今日用量…", color = SecondaryText)
                        }
                    }
                    usage != null -> {
                        HeroMetrics(usage)
                        DetailMetrics(usage)
                        Text(
                            text = "更新时间：${usage.updatedAtLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                    else -> {
                        Text("还没有查询数据，点“查询”开始。", color = SecondaryText)
                    }
                }
            } else {
                Text(
                    text = when {
                        loading -> "正在读取今日用量…"
                        usage != null -> "${usage.totalRequests} 请求 · ${usage.totalTokens} Token · ${usage.totalActualCost}"
                        else -> "点一下展开，查询这个站点的今日数据"
                    },
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun HeroMetrics(usage: UsageSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroMetricCard(
                title = "总请求数",
                value = usage.totalRequests,
                subtitle = "平均 ${usage.averageTokensPerRequest} Token/次",
                background = SoftBlue,
                modifier = Modifier.weight(1f)
            )
            HeroMetricCard(
                title = "总消费",
                value = usage.totalActualCost,
                subtitle = "标准 ${usage.standardCost}",
                background = SoftPurple,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroMetricCard(
                title = "总 Token",
                value = usage.totalTokens,
                subtitle = "输入 ${usage.inputShare} · 输出 ${usage.outputShare}",
                background = SoftGreen,
                modifier = Modifier.weight(1f)
            )
            HeroMetricCard(
                title = "平均耗时",
                value = usage.averageDurationMs,
                subtitle = "平均 ${usage.averageCostPerRequest}/次",
                background = SoftOrange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroMetricCard(
    title: String,
    value: String,
    subtitle: String,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            Text(value, color = PrimaryText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DetailMetrics(usage: UsageSnapshot) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("更多明细", fontWeight = FontWeight.Bold, color = PrimaryText)
            MetricRow("输入 Token", usage.inputTokens)
            MetricRow("输出 Token", usage.outputTokens)
            MetricRow("输入占比", usage.inputShare)
            MetricRow("输出占比", usage.outputShare)
            MetricRow("标准消费", usage.standardCost)
            MetricRow("实际-标准", usage.costGap)
            MetricRow("平均每次请求 Token", usage.averageTokensPerRequest)
            MetricRow("平均每次请求消费", usage.averageCostPerRequest)
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SecondaryText, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, color = PrimaryText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun SiteEditorSheet(
    initialSite: Sub2ApiSite?,
    onDismiss: () -> Unit,
    onSave: (Sub2ApiSite) -> Unit,
) {
    var name by rememberSaveable(initialSite?.id) { mutableStateOf(initialSite?.name ?: "") }
    var baseUrl by rememberSaveable(initialSite?.id) { mutableStateOf(initialSite?.baseUrl ?: "") }
    var email by rememberSaveable(initialSite?.id) { mutableStateOf(initialSite?.email ?: "") }
    var password by rememberSaveable(initialSite?.id) { mutableStateOf(initialSite?.password ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(if (initialSite == null) "添加站点" else "编辑站点", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("站点名称") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("BASE_URL") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("EMAIL") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("PASSWORD") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        onSave(
                            (initialSite ?: Sub2ApiSite.new()).copy(
                                name = name.trim(),
                                baseUrl = baseUrl.trim(),
                                email = email.trim(),
                                password = password,
                            )
                        )
                    }) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
