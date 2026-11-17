const assert = require('node:assert/strict');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.BROWSER_PATH?{executablePath:process.env.BROWSER_PATH}:{})});
 try{
  const context=await browser.newContext({viewport:{width:320,height:900}});
  const page=await context.newPage();
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.route('https://fonts.**',r=>r.abort());
  await page.goto(pathToFileURL(path.resolve(__dirname,'../Tres-Beta.html')).href,{waitUntil:'domcontentloaded'});
  assert.equal(await page.locator('[data-date]').count(),0);
  assert.equal(await page.locator('[data-theme-choice]').count(),0);
  assert.equal(await page.getByText('Habla. Captura. Continúa.').count(),0);
  assert.equal(await page.locator('.app').evaluate(e=>getComputedStyle(e).backgroundColor),'rgb(0, 0, 0)');
  await page.locator('[data-new]').click();await page.locator('[data-input]').fill('Monthly test');
  await page.locator('[data-due]').fill('2027-01-31');await page.locator('[data-repeat]').selectOption('monthly');
  await page.locator('[data-save]').click();await page.getByRole('button',{name:'Completar: Monthly test',exact:true}).click();
  let state=await page.evaluate(()=>JSON.parse(localStorage.getItem('tres-beta-v1')));
  assert.equal(state.notes.find(n=>n.text==='Monthly test'&&!n.done).due,'2027-02-28');
  await page.getByRole('button',{name:'Completar: Monthly test',exact:true}).click();
  state=await page.evaluate(()=>JSON.parse(localStorage.getItem('tres-beta-v1')));
  assert.equal(state.notes.find(n=>n.text==='Monthly test'&&!n.done).due,'2027-03-31');
  await page.evaluate(()=>{const s=JSON.parse(localStorage.getItem('tres-beta-v1'));const done=s.notes.filter(n=>n.done);done[0].completedAt=Date.now()-10*86400000;done[0].text='expired';done[1].completedAt=Date.now()-9*86400000;done[1].text='recent';localStorage.setItem('tres-beta-v1',JSON.stringify(s));});
  await page.reload({waitUntil:'domcontentloaded'});
  state=await page.evaluate(()=>JSON.parse(localStorage.getItem('tres-beta-v1')));
  assert.ok(!state.notes.some(n=>n.text==='expired'));assert.ok(state.notes.some(n=>n.text==='recent'));
  await page.locator('[data-settings]').click();await page.locator('[data-language="en-US"]').click();
  assert.equal(await page.locator('[data-language="en-US"]').getAttribute('aria-pressed'),'true');
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  assert.ok(await page.locator('[data-voice] svg').evaluate(e=>e.getBoundingClientRect().width>0),'mic icon not rendered');
  await page.locator('[data-tab="done"]').click();
  assert.ok(await page.getByText(/Se elimina en 1 día(?!s)/).count()>=1,'singular day label');
  // Two tabs: a note added in one must appear in the other and survive the other's next write.
  const other=await context.newPage();other.on('pageerror',e=>errors.push(e.message));
  await other.route('https://fonts.**',r=>r.abort());
  await other.goto(page.url(),{waitUntil:'domcontentloaded'});
  await other.locator('[data-new]').click();await other.locator('[data-input]').fill('From second tab');await other.locator('[data-save]').click();
  await page.locator('[data-tab="pending"]').click();
  await page.getByRole('button',{name:'Editar: From second tab',exact:true}).waitFor();
  await page.locator('[data-new]').click();await page.locator('[data-input]').fill('From first tab');await page.locator('[data-save]').click();
  state=await page.evaluate(()=>JSON.parse(localStorage.getItem('tres-beta-v1')));
  assert.ok(state.notes.some(n=>n.text==='From second tab')&&state.notes.some(n=>n.text==='From first tab'),'cross-tab write lost');
  assert.deepEqual(errors,[]);console.log('PASS beta appearance, scheduling, monthly anchor, retention, language settings, cross-tab sync and 320px layout');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
