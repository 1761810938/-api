package cn.setbug.sub2apiusage

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sub2ApiUsageApp(viewModel: Sub2ApiViewModel = rememberSub2ApiViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current.applicationContext
    val snackbarHostState = remember { SnackbarHostState() }
    var editingSiteId by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val editingSite = state.sites.firstOrNull { it.id == editingSiteId }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Sub2API 用量") }
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
                    .background(Color(0xFFF3F8FB))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "支持保存多个站点，每个站点单独配置 URL、账号和密码。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475569)
                        )
                    }

                    if (state.sites.isEmpty()) {
                        item {
                            EmptyCard()
                        }
                    }

                    itemsIndexed(state.sites, key = { _, site -> site.id }) { _, site ->
                        val loading = state.loadingSiteId == site.id
                        val usage = state.usageBySiteId[site.id]
                        SiteCard(
                            site = site,
                            usage = usage,
                            loading = loading,
                            onRefresh = {
                                viewModel.refreshSite(site.id)
                                UsageWidgetProvider.refreshAll(context)
                            },
                            onEdit = {
                                editingSiteId = site.id
                                showEditor = true
                            },
                            onDelete = {
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
private fun EmptyCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("还没有站点", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("点右下角加号，添加不同网站的 URL、邮箱和密码后就能单独查询。", color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun SiteCard(
    site: Sub2ApiSite,
    usage: UsageSnapshot?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(site.name.ifBlank { site.baseUrl }, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(site.baseUrl, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(2.dp))
                    Text(site.email, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onRefresh) { Text("查询") }
            }

            Spacer(Modifier.height(8.dp))

            when {
                loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp))
                        Text("正在读取今日用量…")
                    }
                }
                usage != null -> {
                    MetricsGrid(usage)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "更新时间：${usage.updatedAtLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                else -> {
                    Text("还没有查询数据，点“查询”开始。", color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
private fun MetricsGrid(usage: UsageSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricRow("总请求数", usage.totalRequests)
        MetricRow("总 Token", usage.totalTokens)
        MetricRow("输入 / 输出", "${usage.inputTokens} / ${usage.outputTokens}")
        MetricRow("总消费", usage.totalActualCost)
        MetricRow("标准消费", usage.standardCost)
        MetricRow("平均耗时", usage.averageDurationMs)
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF475569))
        Text(value, fontWeight = FontWeight.SemiBold)
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
