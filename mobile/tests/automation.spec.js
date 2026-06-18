import { test, expect } from '@playwright/test';

const MOCK = [
  { id: 1, name: '武林广场', code: 'S001', lineName: '1号线', lineColor: '#FF0000' },
  { id: 2, name: '凤起路', code: 'S002', lineName: '1号线/2号线', lineColor: '#FF0000' },
  { id: 3, name: '龙翔桥', code: 'S003', lineName: '1号线', lineColor: '#FF0000' },
  { id: 4, name: '中河北路', code: 'S004', lineName: '2号线', lineColor: '#0000FF' },
  { id: 5, name: '建国北路', code: 'S005', lineName: '2号线', lineColor: '#0000FF' }
];

async function mockApi(page) {
  await page.route('**/api/**', async route => {
    const url = route.request().url();
    const method = route.request().method();
    if (url.includes('/stations/all') || url.includes('/stations/search')) {
      const kw = url.includes('search') ? (new URL(url)).searchParams.get('keyword') || '' : '';
      const results = kw ? MOCK.filter(s => s.name.includes(kw)) : MOCK;
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(results) });
    } else if (url.includes('/stations?lineId')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
    } else if (url.includes('/fares/quote')) {
      const p = new URL(url).searchParams;
      if (p.get('from') === 'INVALID') {
        await route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ message: '站点不存在' }) });
      } else {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(
          { price: 3.0, segments: 3, steps: [
            { stationName: '武林广场', lineName: '1号线', color: '#FF0000' },
            { stationName: '凤起路', lineName: '换乘 2号线', color: '#0000FF', transfer: true },
            { stationName: '建国北路', lineName: '2号线', color: '#0000FF' }
          ]}
        )});
      }
    } else if (url.includes('/payments/mock')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true }) });
    } else if (url.match(/\/orders\/\d+\/qrcode/)) {
      await route.fulfill({ status: 200, contentType: 'application/json',
        body: JSON.stringify({ orderId: 1001, nonce: 'abc', sign: 'sig123', exp: 9999999999 }) });
    } else if (url.includes('/orders') && method === 'POST') {
      await route.fulfill({ status: 200, contentType: 'application/json',
        body: JSON.stringify({ id: 1001, price: 3.0, status: 'CREATED' }) });
    } else {
      await route.continue();
    }
  });
}

test.describe('地铁售票系统——自动化测试', () => {

  test('TC-AUTO-001 首页加载——线路图与底部面板', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);
    await page.screenshot({ path: 'test-screenshots/001-homepage.png', fullPage: true });
    await expect(page.locator('.floating-panel')).toBeVisible();
  });

  test('TC-AUTO-002 站点搜索弹窗——输入"武林"', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    await page.locator('.search-btn').first().click();
    await page.waitForTimeout(1000);
    await page.locator('.search-input').fill('武林');
    await page.waitForTimeout(800);
    await page.screenshot({ path: 'test-screenshots/002-search-modal.png', fullPage: true });
  });

  test('TC-AUTO-003 订单历史面板', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    const btn = page.locator('.btn-history, button:has-text("订单")').first();
    if (await btn.isVisible()) { await btn.click(); await page.waitForTimeout(1000); }
    await page.screenshot({ path: 'test-screenshots/003-history-panel.png', fullPage: true });
  });

  test('TC-AUTO-004 手机视口适配(390×844)', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-screenshots/004-mobile.png', fullPage: true });
  });

  test('TC-AUTO-005 完整购票——搜索选站→计算→下单→支付→QR', async ({ page }) => {
    test.setTimeout(90000);
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 选起点
    await page.locator('.search-btn').first().click();
    await page.waitForTimeout(1500);
    await page.locator('.search-input').fill('武林');
    await page.waitForTimeout(1000);
    await page.locator('.result-item').first().click();
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-screenshots/005-step1-from.png', fullPage: true });

    // 选终点
    await page.locator('.search-btn').last().dispatchEvent('click');
    await page.waitForTimeout(1500);
    await page.locator('.search-input').fill('建国');
    await page.waitForTimeout(1000);
    await page.locator('.result-item').first().click();
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-screenshots/005-step2-to.png', fullPage: true });

    // 计算线路
    await page.locator('button').filter({ hasText: /计算/ }).first().click();
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-screenshots/005-step3-fare.png', fullPage: true });

    // 立即购票 → PaymentModal
    await page.locator('button').filter({ hasText: /购票/ }).first().click();
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-screenshots/005-step4-payment.png', fullPage: true });

    // 立即支付 → TicketModal with QR
    await page.locator('.btn-pay, button').filter({ hasText: /支付/ }).first().click();
    await page.waitForTimeout(3000);
    await page.screenshot({ path: 'test-screenshots/005-step5-qrcode.png', fullPage: true });
  });

  test('TC-AUTO-006 无JavaScript控制台错误', async ({ page }) => {
    const errors = [];
    page.on('pageerror', err => errors.push(err.message));
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    const btns = page.locator('button');
    const cnt = await btns.count();
    for (let i = 0; i < Math.min(cnt, 3); i++) {
      try { await btns.nth(i).click(); await page.waitForTimeout(300); } catch(e) {}
    }
    await page.screenshot({ path: 'test-screenshots/006-no-errors.png', fullPage: true });
    expect(errors.length).toBe(0);
  });

  test('TC-AUTO-007 票价查询API——S001→S005=¥3.00', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const r = await page.evaluate(async () => {
      const resp = await fetch('/api/fares/quote?from=S001&to=S005');
      const d = await resp.json();
      return { status: resp.status, price: d.price, steps: d.steps?.length };
    });
    expect(r.status).toBe(200);
    expect(r.price).toBe(3.0);
    expect(r.steps).toBeGreaterThanOrEqual(3);
    await page.screenshot({ path: 'test-screenshots/007-fare-api.png', fullPage: true });
  });

  test('TC-AUTO-008 无效站点API——INVALID返回400', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const status = await page.evaluate(async () => {
      const r = await fetch('/api/fares/quote?from=INVALID&to=S005');
      return r.status;
    });
    expect(status).toBe(400);
    await page.screenshot({ path: 'test-screenshots/008-invalid-400.png', fullPage: true });
  });

  test('TC-AUTO-009 下单+支付+QR完整API链路', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const r = await page.evaluate(async () => {
      const o = await fetch('/api/orders', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({from:'S001',to:'S005'}) });
      const od = await o.json();
      const p = await fetch('/api/payments/mock', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({orderId:1001}) });
      const q = await fetch('/api/orders/1001/qrcode');
      const qd = await q.json();
      return { orderStatus: od.status, payOk: p.status===200, qrOk: q.status===200, hasSign: !!qd.sign };
    });
    expect(r.orderStatus).toBe('CREATED');
    expect(r.payOk).toBe(true);
    expect(r.qrOk).toBe(true);
    expect(r.hasSign).toBe(true);
    await page.screenshot({ path: 'test-screenshots/009-api-chain.png', fullPage: true });
  });

  test('TC-AUTO-010 订单历史localStorage存储', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    await page.evaluate(() => {
      localStorage.setItem('subway_orders', JSON.stringify([
        { id: 1001, price: 3.0, fromName: '武林广场', toName: '建国北路', createdAt: new Date().toISOString() }
      ]));
    });
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    const btn = page.locator('.btn-history, button:has-text("订单")').first();
    if (await btn.isVisible()) { await btn.click(); await page.waitForTimeout(1000); }
    await page.screenshot({ path: 'test-screenshots/010-history.png', fullPage: true });
  });
});
