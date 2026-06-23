import {
  fetchTodayUsage,
  formatCost,
  formatDuration,
  formatNumber,
} from './sub2api-usage-widget.js';

export { createBrowserContext, normalizeError };

const STORAGE_KEY = 'sub2api-usage-pwa-config';
const REFRESH_INTERVAL_MS = 10 * 60 * 1000;

const METRICS = [
  {
    label: '总请求数',
    value: (usage) => formatNumber(usage.totalRequests, { compact: false }),
    subtitle: () => '今日范围内',
    tone: 'blue',
  },
  {
    label: '总 Token',
    value: (usage) => formatNumber(usage.totalTokens),
    subtitle: (usage) => `输入: ${formatNumber(usage.inputTokens)} / 输出: ${formatNumber(usage.outputTokens)}`,
    tone: 'amber',
  },
  {
    label: '总消费',
    value: (usage) => formatCost(usage.totalActualCost),
    subtitle: (usage) => `实际 / ${formatCost(usage.standardCost)} 标准`,
    tone: 'green',
  },
  {
    label: '平均耗时',
    value: (usage) => formatDuration(usage.averageDurationMs),
    subtitle: () => '每次请求',
    tone: 'purple',
  },
];

const form = document.querySelector('#configForm');
const baseUrlInput = document.querySelector('#baseUrl');
const emailInput = document.querySelector('#email');
const passwordInput = document.querySelector('#password');
const refreshButton = document.querySelector('#refreshButton');
const clearButton = document.querySelector('#clearButton');
const installButton = document.querySelector('#installButton');
const statusCard = document.querySelector('#statusCard');
const statusTitle = document.querySelector('#statusTitle');
const statusDetail = document.querySelector('#statusDetail');
const metricsGrid = document.querySelector('#metricsGrid');

let installPrompt = null;
let refreshTimer = null;

boot();

function boot() {
  restoreConfig();
  registerInstallPrompt();
  registerServiceWorker();
  bindEvents();

  if (hasConfig()) {
    loadUsage({ persist: false });
  }
}

function bindEvents() {
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    await loadUsage({ persist: true });
  });

  refreshButton.addEventListener('click', async () => {
    await loadUsage({ persist: true });
  });

  clearButton.addEventListener('click', () => {
    localStorage.removeItem(STORAGE_KEY);
    form.reset();
    clearTimer();
    renderIdleState();
  });

  installButton.addEventListener('click', async () => {
    if (!installPrompt) return;
    installButton.disabled = true;
    await installPrompt.prompt();
    await installPrompt.userChoice;
    installPrompt = null;
    installButton.classList.add('hidden');
    installButton.disabled = false;
  });
}

async function loadUsage({ persist }) {
  const env = readEnvFromForm();

  if (!env.BASE_URL || !env.EMAIL || !env.PASSWORD) {
    renderStatus('配置缺失', '请填写 BASE_URL、EMAIL、PASSWORD。', 'warning');
    metricsGrid.classList.add('hidden');
    return;
  }

  if (persist) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(env));
  }

  renderStatus('正在刷新', '正在登录并拉取今日用量…', 'loading');

  try {
    const usage = await fetchTodayUsage(createBrowserContext(env));
    renderUsage(usage);
    scheduleRefresh();
  } catch (error) {
    clearTimer();
    renderError(error);
  }
}

function createBrowserContext(env) {
  return {
    env,
    http: {
      async post(url, options = {}) {
        return fetch(url, {
          method: 'POST',
          headers: options.headers,
          body: JSON.stringify(options.body ?? {}),
        });
      },
      async get(url, options = {}) {
        return fetch(url, {
          method: 'GET',
          headers: options.headers,
        });
      },
    },
  };
}

function renderUsage(usage) {
  metricsGrid.replaceChildren(...METRICS.map((metric) => createMetricCard(metric, usage)));
  metricsGrid.classList.remove('hidden');

  const updatedAt = new Date();
  renderStatus('刷新成功', `上次更新时间：${updatedAt.toLocaleString('zh-CN')}，将自动再次刷新。`, 'success');
}

function createMetricCard(metric, usage) {
  const article = document.createElement('article');
  article.className = `metric-card tone-${metric.tone}`;

  const label = document.createElement('p');
  label.className = 'metric-label';
  label.textContent = metric.label;

  const value = document.createElement('p');
  value.className = 'metric-value';
  value.textContent = metric.value(usage);

  const subtitle = document.createElement('p');
  subtitle.className = 'metric-subtitle';
  subtitle.textContent = metric.subtitle(usage);

  article.append(label, value, subtitle);
  return article;
}

function renderError(error) {
  metricsGrid.classList.add('hidden');
  const { title, detail } = normalizeError(error);
  renderStatus(title, detail, 'danger');
}

function normalizeError(error) {
  if (error && typeof error === 'object' && typeof error.title === 'string' && typeof error.detail === 'string') {
    return { title: error.title, detail: error.detail };
  }

  if (error instanceof TypeError) {
    return {
      title: '网络或跨域错误',
      detail: '浏览器无法直接请求你的 Sub2API 站点。请确认站点启用了 HTTPS 和 CORS，或者把这个 PWA 部署到与 Sub2API 相同的域名下。',
    };
  }

  return {
    title: '加载失败',
    detail: error instanceof Error ? error.message : '请稍后重试。',
  };
}

function renderStatus(title, detail, tone) {
  statusTitle.textContent = title;
  statusDetail.textContent = detail;
  statusCard.dataset.tone = tone;
}

function renderIdleState() {
  metricsGrid.classList.add('hidden');
  renderStatus('等待加载', '填写配置后点击“保存并刷新”。', 'idle');
}

function readEnvFromForm() {
  return {
    BASE_URL: baseUrlInput.value.trim(),
    EMAIL: emailInput.value.trim(),
    PASSWORD: passwordInput.value,
  };
}

function restoreConfig() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return;

  try {
    const env = JSON.parse(raw);
    baseUrlInput.value = typeof env.BASE_URL === 'string' ? env.BASE_URL : '';
    emailInput.value = typeof env.EMAIL === 'string' ? env.EMAIL : '';
    passwordInput.value = typeof env.PASSWORD === 'string' ? env.PASSWORD : '';
  } catch {
    localStorage.removeItem(STORAGE_KEY);
  }
}

function hasConfig() {
  return Boolean(baseUrlInput.value.trim() && emailInput.value.trim() && passwordInput.value);
}

function scheduleRefresh() {
  clearTimer();
  refreshTimer = window.setTimeout(() => {
    loadUsage({ persist: false });
  }, REFRESH_INTERVAL_MS);
}

function clearTimer() {
  if (refreshTimer) {
    window.clearTimeout(refreshTimer);
    refreshTimer = null;
  }
}

function registerInstallPrompt() {
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    installPrompt = event;
    installButton.classList.remove('hidden');
  });

  window.addEventListener('appinstalled', () => {
    installPrompt = null;
    installButton.classList.add('hidden');
  });
}

async function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return;

  try {
    await navigator.serviceWorker.register('./sw.js');
  } catch {
    // 忽略注册失败，不影响页面主流程。
  }
}
