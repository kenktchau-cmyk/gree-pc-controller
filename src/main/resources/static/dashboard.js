(() => {
  'use strict';
  const ids = ['connection','connectionText','refresh','dialProgress','temperatureValue','dialNote','temperatureRange','decrease','increase','apply','powerTitle','powerDescription','power','powerAction','mode','fan','swing','feedback','updated','applySettings','settingsSummary'];
  const el = Object.fromEntries(ids.map(id => [id, document.getElementById(id)]));
  const state = { status: null, draft: null, busy: false, online: false, edited: false, settings: {}, settingEdits: {} };
  const settingValues = s => ({mode:s.Mod || '',fan:s.WdSpd || '',swing:s.SwUpDn ? String(s.SwUpDn.status) : ''});
  function settingChanges() {
    if (!state.status) return {};
    const actual = settingValues(state.status);
    return Object.fromEntries(Object.entries(state.settings).filter(([key,value]) => value !== '' && value !== actual[key]));
  }
  const modes = { AUTO:'Auto', COOL:'Cool', DRY:'Dry', FAN:'Fan', HEAT:'Heat' };
  const fans = { AUTO:'Auto', LOW:'Low', MEDIUM_LOW:'Medium-low', MEDIUM:'Medium', MEDIUM_HIGH:'Medium-high', HIGH:'High' };
  const names = value => String(value || 'Unavailable').toLowerCase().replace(/_/g,' ').replace(/^./,c=>c.toUpperCase());
  function message(text, type = '') { el.feedback.textContent = text; el.feedback.className = `feedback ${type}`; }
  function render() {
    const s = state.status;
    const usable = state.online && s && s.TemUn === 'CELSIUS' && !state.busy;
    const on = s && s.Pow === 'ON';
    const dirty = s && state.draft !== s.SetTem;
    const displayedTemperature = !state.online && s ? s.SetTem : state.draft;
    el.connection.className = `connection ${state.online ? 'online' : s ? 'offline' : ''}`;
    el.connectionText.textContent = state.online ? 'Connected' : s ? 'Disconnected' : state.busy ? 'Connecting' : 'Offline';
    document.body.classList.toggle('offline-state', !state.online);
    el.temperatureValue.textContent = displayedTemperature === null ? '—' : displayedTemperature;
    el.temperatureRange.value = state.draft === null ? 25 : state.draft;
    el.temperatureRange.setAttribute('aria-valuetext',`${state.draft ?? 'Unknown'} degrees Celsius`);
    el.dialProgress.style.strokeDasharray = `${displayedTemperature === null ? 0 : Math.max(0,Math.min(75,(displayedTemperature-16)/14*75))} 100`;
    el.dialNote.textContent = !s ? 'Waiting for your AC' : !state.online ? 'Last known setting' : dirty ? `Currently set to ${s.SetTem}°C` : 'Current target temperature';
    el.temperatureRange.disabled = !usable;
    el.decrease.disabled = !usable || state.draft <= 16;
    el.increase.disabled = !usable || state.draft >= 30;
    el.apply.disabled = !usable || !dirty;
    el.apply.textContent = state.busy ? 'Please wait…' : !state.online ? 'Waiting for connection' : dirty ? `Apply ${state.draft}°C` : 'Temperature is up to date';
    el.refresh.disabled = state.busy;
    el.power.disabled = !state.online || !s || state.busy;
    el.powerAction.textContent = !s ? 'Waiting for connection' : on ? 'Turn off' : 'Turn on';
    el.powerTitle.textContent = !s ? 'Not connected' : on ? 'Powered on' : 'Powered off';
    el.powerDescription.textContent = !s ? 'Connect to read your air conditioner’s status.' : !state.online ? 'Showing the last known power state.' : on ? 'Your air conditioner is running.' : 'Ready whenever you need it.';
    const card = el.power.closest('.power-card');
    card.classList.toggle('is-on', !!s && on);
    card.classList.toggle('is-off', !!s && !on);
    const settingsDirty = Object.keys(settingChanges()).length > 0;
    for (const key of ['mode','fan','swing']) {
      el[key].value = !s ? '' : !state.online ? settingValues(s)[key] : state.settings[key];
      el[key].disabled = !state.online || !s || state.busy;
    }
    el.applySettings.disabled = !state.online || state.busy || !settingsDirty;
    el.applySettings.textContent = state.busy ? 'Please wait…' : !state.online ? 'Waiting for connection' : settingsDirty ? 'Apply settings' : 'Settings are up to date';
    el.settingsSummary.textContent = !state.online ? 'Last known settings' : settingsDirty ? 'Changes not applied' : 'Live from your AC';
  }
  async function request(path, options = {}) {
    const abort = new AbortController();
    const timeout = setTimeout(() => abort.abort(), 12000);
    try {
      const response = await fetch(path, { ...options, cache:'no-store', signal:abort.signal });
      if (!response.ok) throw new Error('The air conditioner did not respond. Try Refresh.');
      return path === '/status' ? await response.json() : await response.text();
    } catch (error) {
      if (error.name === 'AbortError' || error instanceof TypeError) throw new Error('Connection lost. Check that the PC controller is running and your AC is online, then Refresh.');
      throw error;
    } finally { clearTimeout(timeout); }
  }
  async function readStatus() {
    const s = await request('/status');
    if (!['ON','OFF'].includes(s.Pow) || !Number.isInteger(s.SetTem)) throw new Error('Could not read the AC settings. Try Refresh.');
    state.status = s;
    state.online = true;
    for (const [key,value] of Object.entries(settingValues(s))) {
      if (!state.settingEdits[key] || state.settings[key] === value) {
        state.settings[key] = value;
        delete state.settingEdits[key];
      }
    }
    if (!state.edited || state.draft === s.SetTem) { state.draft = s.SetTem; state.edited = false; }
    el.updated.textContent = `Updated ${new Date().toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'})}`;
    if (s.TemUn !== 'CELSIUS') message('Set the AC to Celsius in GREE+ to use the temperature control.', 'error');
    return s;
  }
  async function refresh(manual = false) {
    if (state.busy) return;
    const wasOffline = !state.online;
    state.busy = true;
    render();
    try {
      await readStatus();
      if (state.status.TemUn === 'CELSIUS' && (manual || wasOffline)) message('Connected. Settings are up to date.');
    } catch (error) { state.online = false; message(error.message, 'error'); }
    finally { state.busy = false; render(); }
  }
  function edit(value) {
    if (state.busy || !state.online) return;
    state.draft = Math.max(16, Math.min(30, Number(value)));
    state.edited = true;
    message(state.draft === state.status.SetTem ? 'Temperature is up to date.' : `Ready to set ${state.draft}°C. Select Apply to confirm.`);
    render();
  }
  async function command(path, expected, success, options = {}) {
    if (state.busy || !state.online) return;
    state.busy = true;
    message('Sending to your air conditioner…');
    render();
    let acknowledged = false;
    try {
      const result = await request(path, options);
      if (result !== 'done') throw new Error('The AC did not confirm the command. Refresh to check its settings.');
      acknowledged = true;
      const s = await readStatus();
      if (!expected(s)) {
        message('The AC reported a different setting. This option may be restricted in the selected mode. Check the AC or select Refresh.', 'error');
        return;
      }
      message(success, 'success');
    } catch (error) {
      state.online = false;
      message(acknowledged ? 'Command accepted, but the resulting setting could not be confirmed. Refresh to check.' : error.message, 'error');
    } finally { state.busy = false; render(); }
  }
  el.temperatureRange.addEventListener('input', event => edit(event.target.value));
  el.decrease.addEventListener('click', () => edit(state.draft - 1));
  el.increase.addEventListener('click', () => edit(state.draft + 1));
  el.apply.addEventListener('click', () => {
    if (el.apply.disabled) return;
    const target = state.draft;
    command(`/temperature?temperature=${target}`, s => s.SetTem === target, `Temperature set to ${target}°C.`);
  });
  el.power.addEventListener('click', () => {
    if (el.power.disabled) return;
    const target = state.status.Pow === 'ON' ? 'OFF' : 'ON';
    command(target === 'ON' ? '/powerOn' : '/powerOff', s => s.Pow === target, `Air conditioner turned ${target.toLowerCase()}.`);
  });
  el.refresh.addEventListener('click', () => refresh(true));
  for (const key of ['mode','fan','swing']) {
    el[key].addEventListener('change', event => {
      if (state.busy || !state.online) return;
      state.settings[key] = event.target.value;
      state.settingEdits[key] = true;
      message('Settings selected. Select Apply settings to send them to your AC.');
      render();
    });
  }
  el.applySettings.addEventListener('click', () => {
    if (el.applySettings.disabled) return;
    const changes = settingChanges();
    const params = new URLSearchParams();
    for (const [key,value] of Object.entries(changes)) params.set(key === 'fan' ? 'fanSpeed' : key,value);
    command('/settings', s => Object.entries(changes).every(([key,value]) => settingValues(s)[key] === value),
      'Settings applied and confirmed by your AC.', {method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:params.toString()});
  });
  document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(); });
  setInterval(() => { if (!document.hidden) refresh(); }, 15000);
  refresh();
})();
