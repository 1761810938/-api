(() => {
  const SITE_ORIGIN = 'https://aiapi888.setbug.cn';
  const PANEL_ID = 'sub2api-usage-site-panel';
  const STYLE_ID = 'sub2api-usage-site-panel-style';
  const STORAGE_KEY = 'sub2api-usage-site-panel-config';
  const REFRESH_INTERVAL_MS = 10 * 60 * 1000;
  const METRICS = [
    {
      label: '总请求数',
      tone: 'blue',
      value: (usage) => formatNumber(usage.totalRequests, { compact: false }),
      subtitle: () => '今日范围内',
    },
    {
      label: '总 Token',
      tone: 'amber',
      value: (usage) => formatNumber(usage.totalTokens),
      subtitle: (usage) => `输入: ${formatNumber(usage.inputTokens)} / 输出: ${formatNumber(usage.outputTokens)}`,
    },
    {
      label: '总消费',
      tone: 'green',
      value: (usage) => formatCost(usage.totalActualCost),
      subtitle: (usage) => `实际 / ${formatCost(usage.standardCost)} 标准`,
    },
    {
      label: '平均耗时',
      tone: 'purple',
      value: (usage) => formatDuration(usage.averageDurationMs),
      subtitle: () => '每次请求',
    },
  ];

  if (location.origin !== SITE_ORIGIN) {
    alert(`请先打开 ${SITE_ORIGIN} 后再运行这个脚本。`);
    return;
  }

  const existing = document.getElementById(PANEL_ID);
  if (existing) {
    existing.remove();
    document.getElementById(STYLE_ID)?.remove();
  }

  let refreshTimer = null;

  injectStyles();
  const refs = createPanel();
  restoreConfig(refs);
  bindEvents(refs);

  if (hasConfig(refs)) {
    loadUsage(refs, false);
  }

  function injectStyles() {
    const style = document.createElement('style');
    style.id = STYLE_ID;
    style.textContent = `
      #${PANEL_ID} {
        position: fixed;
        right: 16px;
        bottom: 16px;
        width: min(380px, calc(100vw - 20px));
        max-height: min(82vh, 760px);
        overflow: auto;
        z-index: 2147483647;
        padding: 16px;
        border-radius: 20px;
        background: rgba(15, 23, 42, 0.96);
        color: #f8fafc;
        box-shadow: 0 18px 50px rgba(0, 0, 0, 0.32);
        border: 1px solid rgba(148, 163, 184, 0.22);
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        backdrop-filter: blur(18px);
      }
      #${PANEL_ID} * { box-sizing: border-box; }
      #${PANEL_ID} .sub2api-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 12px;
      }
      #${PANEL_ID} .sub2api-title {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
      }
      #${PANEL_ID} .sub2api-eyebrow,
      #${PANEL_ID} .sub2api-hint,
      #${PANEL_ID} .sub2api-status-detail,
      #${PANEL_ID} .sub2api-subtitle {
        margin: 0;
        color: #cbd5e1;
        line-height: 1.5;
        font-size: 13px;
      }
      #${PANEL_ID} .sub2api-eyebrow {
        text-transform: uppercase;
        letter-spacing: 0.08em;
        font-size: 11px;
      }
      #${PANEL_ID} .sub2api-close {
        border: 0;
        border-radius: 999px;
        width: 32px;
        height: 32px;
        background: rgba(148, 163, 184, 0.16);
        color: #f8fafc;
        cursor: pointer;
      }
      #${PANEL_ID} .sub2api-form {
        margin-top: 14px;
        display: grid;
        gap: 10px;
      }
      #${PANEL_ID} label {
        display: grid;
        gap: 6px;
        font-size: 13px;
        font-weight: 600;
      }
      #${PANEL_ID} input {
        width: 100%;
        border: 1px solid rgba(148, 163, 184, 0.18);
        border-radius: 14px;
        background: rgba(15, 23, 42, 0.62);
        color: #f8fafc;
        padding: 12px 13px;
      }
      #${PANEL_ID} .sub2api-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }
      #${PANEL_ID} .sub2api-btn {
        border: 0;
        border-radius: 14px;
        padding: 10px 14px;
        color: #f8fafc;
        cursor: pointer;
      }
      #${PANEL_ID} .sub2api-btn-primary { background: #2563eb; }
      #${PANEL_ID} .sub2api-btn-secondary { background: rgba(148, 163, 184, 0.18); }
      #${PANEL_ID} .sub2api-btn-ghost {
        background: transparent;
        border: 1px solid rgba(148, 163, 184, 0.22);
      }
      #${PANEL_ID} .sub2api-status {
        margin-top: 12px;
        padding: 12px;
        border-radius: 16px;
        background: rgba(30, 41, 59, 0.65);
      }
      #${PANEL_ID} .sub2api-status-title {
        margin: 0 0 4px;
        font-size: 15px;
        font-weight: 700;
      }
      #${PANEL_ID}[data-tone='success'] .sub2api-status-title { color: #86efac; }
      #${PANEL_ID}[data-tone='warning'] .sub2api-status-title { color: #fbbf24; }
      #${PANEL_ID}[data-tone='danger'] .sub2api-status-title { color: #fca5a5; }
      #${PANEL_ID}[data-tone='loading'] .sub2api-status-title { color: #93c5fd; }
      #${PANEL_ID} .sub2api-grid {
        display: grid;
        gap: 10px;
        margin-top: 12px;
      }
      #${PANEL_ID} .sub2api-grid.hidden { display: none; }
      #${PANEL_ID} .sub2api-card {
        padding: 14px;
        border-radius: 16px;
        border: 1px solid rgba(148, 163, 184, 0.16);
        background: rgba(255, 255, 255, 0.04);
      }
      #${PANEL_ID} .sub2api-label {
        margin: 0;
        font-size: 13px;
        font-weight: 600;
        color: #cbd5e1;
      }
      #${PANEL_ID} .sub2api-value {
        margin: 9px 0 7px;
        font-size: 28px;
        font-weight: 800;
        line-height: 1.1;
      }
      #${PANEL_ID} .sub2api-card.blue .sub2api-value { color: #83b7ff; }
      #${PANEL_ID} .sub2api-card.amber .sub2api-value { color: #ffd37c; }
      #${PANEL_ID} .sub2api-card.green .sub2api-value { color: #6ee7a0; }
      #${PANEL_ID} .sub2api-card.purple .sub2api-value { color: #d7a8ff; }
      @media (max-width: 640px) {
        #${PANEL_ID} {
          right: 10px;
          left: 10px;
          bottom: 10px;
          width: auto;
        }
        #${PANEL_ID} .sub2api-actions > * {
          flex: 1 1 100%;
        }
      }
    `;
    document.head.appendChild(style);
  }

  function createPanel() {
    const panel = document.createElement('section');
    panel.id = PANEL_ID;
    panel.dataset.tone = 'idle';
    panel.innerHTML = `
      <div class="sub2api-header">
        <div>
          <p class="sub2api-eyebrow">同站脚本版</p>
          <h2 class="sub2api-title">Sub2API 今日用量</h2>
          <p class="sub2api-hint">当前站点：${location.origin}</p>
        </div>
        <button class="sub2api-close" type="button" aria-label="关闭">×</button>
      </div>
      <form class="sub2api-form">
        <label>
          <span>登录账号</span>
          <input data-role="email" type="email" placeholder="name@example.com" autocomplete="username" required />
        </label>
        <label>
          <span>登录密码</span>
          <input data-role="password" type="password" placeholder="请输入密码" autocomplete="current-password" required />
        </label>
        <div class="sub2api-actions">
          <button class="sub2api-btn sub2api-btn-primary" data-role="submit" type="submit">保存并查询</button>
          <button class="sub2api-btn sub2api-btn-secondary" data-role="refresh" type="button">仅刷新</button>
          <button class="sub2api-btn sub2api-btn-ghost" data-role="clear" type="button">清空本地配置</button>
        </div>
      </form>
      <div class="sub2api-status">
        <p class="sub2api-status-title" data-role="status-title">等待查询</p>
        <p class="sub2api-status-detail" data-role="status-detail">填写账号密码后点击“保存并查询”。</p>
      </div>
      <div class="sub2api-grid hidden" data-role="grid"></div>
    `;
    document.body.appendChild(panel);

    return {
      panel,
      form: panel.querySelector('.sub2api-form'),
      emailInput: panel.querySelector('[data-role="email"]'),
      passwordInput: panel.querySelector('[data-role="password"]'),
      submitButton: panel.querySelector('[data-role="submit"]'),
      refreshButton: panel.querySelector('[data-role="refresh"]'),
      clearButton: panel.querySelector('[data-role="clear"]'),
      closeButton: panel.querySelector('.sub2api-close'),
      statusTitle: panel.querySelector('[data-role="status-title"]'),
      statusDetail: panel.querySelector('[data-role="status-detail"]'),
      grid: panel.querySelector('[data-role="grid"]'),
    };
  }

  function bindEvents(refs) {
    refs.form.addEventListener('submit', async (event) => {
      event.preventDefault();
      await loadUsage(refs, true);
    });

    refs.refreshButton.addEventListener('click', async () => {
      await loadUsage(refs, true);
    });

    refs.clearButton.addEventListener('click', () => {
      localStorage.removeItem(STORAGE_KEY);
      refs.form.reset();
      refs.grid.classList.add('hidden');
      clearTimer();
      renderStatus(refs, '等待查询', '填写账号密码后点击“保存并查询”。', 'idle');
    });

    refs.closeButton.addEventListener('click', () => {
      clearTimer();
      refs.panel.remove();
      document.getElementById(STYLE_ID)?.remove();
    });
  }

  async function loadUsage(refs, persist) {
    const email = refs.emailInput.value.trim();
    const password = refs.passwordInput.value;

    if (!email || !password) {
      refs.grid.classList.add('hidden');
      renderStatus(refs, '配置缺失', '请填写 EMAIL 和 PASSWORD。', 'warning');
      return;
    }

    if (persist) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ email, password }));
    }

    renderStatus(refs, '正在查询', '正在登录并读取今日用量…', 'loading');

    try {
      const usage = await fetchTodayUsage({ email, password });
      renderUsage(refs, usage);
      scheduleRefresh(refs);
    } catch (error) {
      clearTimer();
      refs.grid.classList.add('hidden');
      const normalized = normalizeError(error);
      renderStatus(refs, normalized.title, normalized.detail, 'danger');
    }
  }

  async function fetchTodayUsage({ email, password }) {
    const loginResponse = await postJson('/api/v1/auth/login', { email, password }, '登录失败');
    const loginPayload = unwrapApiResponse(loginResponse, '登录失败');
    const accessToken = trimString(loginPayload.access_token);

    if (!accessToken) {
      if (loginPayload.requires_2fa) {
        throw new WidgetError('需要 2FA', '当前账号启用了双因素验证，无法自动登录。');
      }
      throw new WidgetError('登录失败', '登录响应缺少 access_token');
    }

    const statsResponse = await getJson(buildStatsUrl(getTimezone()), {
      Authorization: `Bearer ${accessToken}`,
    });
    const stats = unwrapApiResponse(statsResponse, '读取失败');

    return {
      totalRequests: toFiniteNumber(stats.today_requests),
      totalTokens: toFiniteNumber(stats.today_tokens),
      inputTokens: toFiniteNumber(stats.today_input_tokens),
      outputTokens: toFiniteNumber(stats.today_output_tokens),
      standardCost: toFiniteNumber(stats.today_cost),
      totalActualCost: toFiniteNumber(stats.today_actual_cost),
      averageDurationMs: toFiniteNumber(stats.average_duration_ms),
    };
  }

  async function postJson(url, body, fallbackTitle) {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      credentials: 'same-origin',
    });
    return parseJsonResponse(response, fallbackTitle, url);
  }

  async function getJson(url, headers) {
    const response = await fetch(url, {
      method: 'GET',
      headers,
      credentials: 'same-origin',
    });
    return parseJsonResponse(response, '请求失败', url);
  }

  async function parseJsonResponse(response, fallbackTitle, url) {
    if (!response) {
      throw new WidgetError(fallbackTitle, '没有收到服务器响应');
    }

    const status = Number(response.status ?? 200);
    const route = summarizeUrl(url);
    const previewSource = cloneResponse(response);
    let payload;

    try {
      payload = await response.json();
    } catch {
      const preview = await safeResponsePreview(previewSource || response);
      const statusText = Number.isFinite(status) ? `HTTP ${status}` : 'HTTP 状态未知';
      throw new WidgetError(fallbackTitle, `${route}${statusText}，非 JSON 响应：${preview}`);
    }

    if (status < 200 || status >= 300) {
      throw new WidgetError(fallbackTitle, `${route}${readableMessage(payload) || `HTTP ${status}`}`);
    }

    return payload;
  }

  function renderUsage(refs, usage) {
    refs.grid.replaceChildren(...METRICS.map((metric) => createMetricCard(metric, usage)));
    refs.grid.classList.remove('hidden');
    renderStatus(refs, '查询成功', `上次更新时间：${new Date().toLocaleString('zh-CN')}，页面打开期间会自动刷新。`, 'success');
  }

  function createMetricCard(metric, usage) {
    const article = document.createElement('article');
    article.className = `sub2api-card ${metric.tone}`;

    const label = document.createElement('p');
    label.className = 'sub2api-label';
    label.textContent = metric.label;

    const value = document.createElement('p');
    value.className = 'sub2api-value';
    value.textContent = metric.value(usage);

    const subtitle = document.createElement('p');
    subtitle.className = 'sub2api-subtitle';
    subtitle.textContent = metric.subtitle(usage);

    article.append(label, value, subtitle);
    return article;
  }

  function renderStatus(refs, title, detail, tone) {
    refs.panel.dataset.tone = tone;
    refs.statusTitle.textContent = title;
    refs.statusDetail.textContent = detail;
  }

  function restoreConfig(refs) {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return;

    try {
      const data = JSON.parse(raw);
      refs.emailInput.value = typeof data.email === 'string' ? data.email : '';
      refs.passwordInput.value = typeof data.password === 'string' ? data.password : '';
    } catch {
      localStorage.removeItem(STORAGE_KEY);
    }
  }

  function hasConfig(refs) {
    return Boolean(refs.emailInput.value.trim() && refs.passwordInput.value);
  }

  function scheduleRefresh(refs) {
    clearTimer();
    refreshTimer = window.setTimeout(() => {
      loadUsage(refs, false);
    }, REFRESH_INTERVAL_MS);
  }

  function clearTimer() {
    if (refreshTimer) {
      window.clearTimeout(refreshTimer);
      refreshTimer = null;
    }
  }

  function normalizeError(error) {
    if (error && typeof error === 'object' && typeof error.title === 'string' && typeof error.detail === 'string') {
      return error;
    }

    if (error instanceof TypeError) {
      return {
        title: '网络错误',
        detail: '当前站点请求失败，请确认页面已经正常打开并且站点接口可用。',
      };
    }

    return {
      title: '加载失败',
      detail: error instanceof Error ? error.message : '请稍后重试。',
    };
  }

  function buildStatsUrl(timezone) {
    const params = new URLSearchParams({ timezone: timezone || 'Asia/Shanghai' });
    return `/api/v1/usage/dashboard/stats?${params.toString()}`;
  }

  function formatNumber(value, options = {}) {
    const number = toFiniteNumber(value);
    if (options.compact === false) {
      return Math.round(number).toLocaleString('en-US');
    }
    const absolute = Math.abs(number);
    if (absolute >= 1_000_000_000) return `${trimTrailingZeros(number / 1_000_000_000, 2)}B`;
    if (absolute >= 1_000_000) return `${trimTrailingZeros(number / 1_000_000, 2)}M`;
    if (absolute >= 1_000) return `${trimTrailingZeros(number / 1_000, 2)}K`;
    return String(Math.round(number));
  }

  function formatCost(value) {
    const number = toFiniteNumber(value);
    if (number === 0) return '$0.00';
    if (Math.abs(number) < 1) return `$${trimTrailingZeros(number, 6)}`;
    return `$${trimTrailingZeros(number, 4)}`;
  }

  function formatDuration(value) {
    const ms = toFiniteNumber(value);
    if (ms >= 1000) return `${trimTrailingZeros(ms / 1000)} s`;
    return `${Math.round(ms)} ms`;
  }

  function getTimezone() {
    try {
      return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai';
    } catch {
      return 'Asia/Shanghai';
    }
  }

  function cloneResponse(response) {
    if (!response || typeof response.clone !== 'function') return null;
    try {
      return response.clone();
    } catch {
      return null;
    }
  }

  async function safeResponsePreview(response) {
    if (typeof response.text !== 'function') return '无响应正文';
    try {
      return compactText(await response.text()).slice(0, 120) || '空响应';
    } catch {
      return '无法读取响应正文';
    }
  }

  function summarizeUrl(url) {
    if (!url) return '';
    try {
      const parsed = new URL(url, location.origin);
      return `${parsed.pathname}：`;
    } catch {
      return '';
    }
  }

  function unwrapApiResponse(payload, fallbackTitle) {
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code === 0) return payload.data ?? {};
      throw new WidgetError(fallbackTitle, readableMessage(payload) || `code=${payload.code}`);
    }
    return payload ?? {};
  }

  function toFiniteNumber(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number : 0;
  }

  function trimString(value) {
    return typeof value === 'string' ? value.trim() : '';
  }

  function trimTrailingZeros(value, digits = 1) {
    return Number(value).toFixed(digits).replace(/\.?0+$/, '');
  }

  function compactText(value) {
    return String(value ?? '').replace(/\s+/g, ' ').trim();
  }

  function readableMessage(value) {
    if (!value) return '';
    if (value instanceof Error) return value.message;
    if (typeof value === 'string') return value;
    if (typeof value.message === 'string') return value.message;
    if (typeof value.error === 'string') return value.error;
    if (typeof value.detail === 'string') return value.detail;
    return '';
  }

  class WidgetError extends Error {
    constructor(title, detail) {
      super(`${title}: ${detail}`);
      this.name = 'WidgetError';
      this.title = title;
      this.detail = detail;
    }
  }
})();
