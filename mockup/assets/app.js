/* Zcut mockup — interaction and navigation */
(function(){
  'use strict';
  const phone = document.getElementById('phoneScreen');
  const rail = document.getElementById('rail');
  const inspector = document.getElementById('inspector');
  const galleryGrid = document.getElementById('galleryGrid');
  const stageTitle = document.getElementById('stageTitle');
  const stageSub = document.getElementById('stageSub');
  const screenCounter = document.getElementById('screenCounter');
  const toastEl = document.getElementById('toast');
  const modal = document.getElementById('modalBackdrop');
  const modalBody = document.getElementById('modalBody');
  const modalTitle = document.getElementById('modalTitle');
  const modalActions = document.getElementById('modalActions');
  const state = { current: 'home', view: 'prototype' };
  let toastTimer = null;

  function esc(value){
    return String(value == null ? '' : value).replace(/[&<>"']/g, function(c){
      return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];
    });
  }

  function renderRail(){
    const groups = [];
    SCREENS.forEach(function(s){
      if(!groups.includes(s.group)) groups.push(s.group);
    });
    rail.innerHTML = groups.map(function(group){
      const items = SCREENS.filter(function(s){return s.group === group;}).map(function(s){
        const tag = (s.tagType === 'backend' || s.tagType === 'mixed') ? '<span class="tag">B</span>' : '';
        return '<button class="rail-item" data-screen="'+s.id+'"><span class="rail-num">'+s.num+'</span><span>'+esc(s.title)+'</span>'+tag+'</button>';
      }).join('');
      return '<div class="rail-label">'+group+'</div>'+items;
    }).join('')+
    '<div class="rail-foot"><b>31 feature areas</b><br>42 screens · 1 interactive prototype<br><span style="color:var(--lime)">Semua fitur terbuka.</span><div class="meter"><i></i></div></div>';
  }

  function updateRail(){
    rail.querySelectorAll('[data-screen]').forEach(function(btn){
      btn.classList.toggle('active', btn.getAttribute('data-screen') === state.current);
    });
  }

  function renderInspector(){
    const s = screenById(state.current);
    const tagClass = s.tagType === 'backend' ? 'coral' : s.tagType === 'mixed' ? 'violet' : 'lime';
    inspector.innerHTML = '<div class="inspector-kicker">Screen '+s.num+' / '+SCREENS.length+'</div>'+
      '<h2>'+esc(s.title)+'</h2>'+
      '<p class="ins-desc">'+esc(s.desc)+'</p>'+
      '<div class="ins-tags"><span class="badge '+tagClass+'">'+esc(s.tag)+'</span><span class="badge">'+esc(s.feature)+'</span><span class="badge">'+esc(s.group)+'</span></div>'+
      '<div class="ins-section"><h4>Interaksi di mockup</h4><ul class="ins-list">'+s.interactions.map(function(x){return '<li>'+esc(x)+'</li>';}).join('')+'</ul></div>'+
      '<div class="ins-section"><h4>Implementasi Android</h4><ul class="ins-list">'+
        '<li>Compose screen · package <b style="color:var(--muted)">com.zaaam.zcut</b></li>'+
        '<li>State via ViewModel + StateFlow</li>'+
        '<li>Data layer Room/DataStore · local-first</li>'+
        '<li>Export Media3 Transformer · no watermark</li>'+
      '</ul></div>'+
      '<div class="ins-actions"><button class="btn primary" data-action="open-current">'+ico('arrow')+' Open in prototype</button><button class="btn" data-action="copy-screen">'+ico('copy')+' Copy screen link</button><button class="btn ghost" data-action="request-acc">'+ico('check')+' Review / ACC</button></div>'+
      '<div class="ins-meta">Stage 2 mockup · <b>not final UI</b>. Approval must come via Telegram before Android UI work starts.</div>';
  }

  function renderPhone(id){
    const screen = screenById(id);
    const html = (RENDER[screen.id] || RENDER.home)();
    phone.innerHTML = statusbar()+html+'<div class="homebar"></div>';
    const scroller = phone.querySelector('.screen-scroll');
    if(scroller) scroller.scrollTop = 0;
    state.current = screen.id;
    updateRail();
    renderInspector();
    if(stageTitle) stageTitle.textContent = screen.title;
    if(stageSub) stageSub.textContent = screen.desc;
    if(screenCounter) screenCounter.textContent = screen.num+' / '+SCREENS.length;
    const url = new URL(window.location.href);
    url.searchParams.set('screen', screen.id);
    url.searchParams.delete('view');
    history.replaceState({}, '', url.toString());
  }

  function go(id){
    if(id === 'screen-map' || id === 'map'){ showMap(); return; }
    if(id === 'prototype'){ showPrototype(); return; }
    if(!screenById(id) || !RENDER[id]){ showToast('Screen belum tersedia di mockup.', 'coral'); return; }
    showPrototype();
    renderPhone(id);
  }

  function showPrototype(){
    state.view = 'prototype';
    document.body.classList.remove('map-mode');
    document.getElementById('tabPrototype').classList.add('active');
    document.getElementById('tabMap').classList.remove('active');
    const url = new URL(window.location.href);
    url.searchParams.delete('view');
    history.replaceState({}, '', url.toString());
  }

  function showMap(){
    state.view = 'map';
    document.body.classList.add('map-mode');
    document.getElementById('tabPrototype').classList.remove('active');
    document.getElementById('tabMap').classList.add('active');
    renderGallery();
    const url = new URL(window.location.href);
    url.searchParams.set('view','map');
    url.searchParams.delete('screen');
    history.replaceState({}, '', url.toString());
    window.scrollTo({top:0,behavior:'instant'});
  }

  function renderGallery(){
    galleryGrid.innerHTML = SCREENS.map(function(s){
      const html = (RENDER[s.id] || RENDER.home)();
      const badgeClass = s.tagType === 'backend' ? 'backend' : s.tagType === 'mixed' ? 'mixed' : s.tagType === 'removed' ? 'removed' : 'local';
      return '<div class="g-card" data-screen="'+s.id+'"><span class="g-badge '+badgeClass+'">'+esc(s.tag)+'</span><div class="g-stage"><div class="g-mini">'+statusbar()+html+'<div class="homebar"></div></div></div><div class="g-meta"><span class="g-num">'+s.num+' · '+esc(s.group)+'</span><b>'+esc(s.title)+'</b><span>'+esc(s.feature)+'</span></div></div>';
    }).join('');
  }

  function showToast(message, tone){
    toastEl.innerHTML = ico(tone === 'coral' ? 'info' : 'check')+'<span>'+esc(message)+'</span>';
    toastEl.classList.toggle('coral', tone === 'coral');
    toastEl.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function(){ toastEl.classList.remove('show'); }, 3200);
  }

  function openModal(title, body, actions){
    modalTitle.textContent = title;
    modalBody.innerHTML = body;
    modalActions.innerHTML = actions || '<button class="btn primary" data-action="close-modal">Got it</button>';
    modal.classList.add('show');
  }
  function closeModal(){ modal.classList.remove('show'); }

  function copyText(text, okMessage){
    const done = function(){ showToast(okMessage || 'Copied.'); };
    if(navigator.clipboard && navigator.clipboard.writeText){
      navigator.clipboard.writeText(text).then(done).catch(function(){ fallbackCopy(text); done(); });
    } else { fallbackCopy(text); done(); }
  }
  function fallbackCopy(text){
    const ta = document.createElement('textarea');
    ta.value = text; ta.style.position='fixed'; ta.style.opacity='0';
    document.body.appendChild(ta); ta.select();
    try{ document.execCommand('copy'); }catch(e){}
    document.body.removeChild(ta);
  }

  function handleAction(action, el){
    switch(action){
      case 'play': {
        const isPlay = el.innerHTML.indexOf('M9 6') !== -1;
        el.innerHTML = ico(isPlay ? 'pause' : 'play');
        showToast(isPlay ? 'Preview playing · mockup playback' : 'Preview paused');
        break;
      }
      case 'undo': showToast('Undo stack · 12 steps available'); break;
      case 'redo': showToast('Redo stack · 4 steps available'); break;
      case 'delete': showToast('Clip dihapus dari timeline (mockup).'); break;
      case 'duplicate': showToast('Clip diduplikasi (mockup).'); break;
      case 'split': showToast('Clip dipisah di playhead (mockup).'); break;
      case 'add-keyframe': showToast('Keyframe ditambahkan di posisi playhead.'); break;
      case 'fullscreen': showToast('Fullscreen preview (mockup).'); break;
      case 'mute': showToast('Audio preview dimute.'); break;
      case 'export-start': startExport(el); break;
      case 'tts-generate': {
        const r = document.getElementById('ttsResult');
        if(r){ r.style.display = 'block'; r.scrollIntoView({behavior:'smooth',block:'nearest'}); }
        showToast('Voice generated locally · Android TTS.');
        break;
      }
      case 'music-generate': {
        const r = document.getElementById('musicResult');
        if(r){ r.style.display = 'block'; r.scrollIntoView({behavior:'smooth',block:'nearest'}); }
        showToast('Loop lokal dibuat · generator prosedural.');
        break;
      }
      case 'ai-generate': {
        const r = document.getElementById('aiResult');
        if(r){ r.style.display = 'block'; r.scrollIntoView({behavior:'smooth',block:'nearest'}); }
        showToast('Preview lokal dibuat. Generasi asli BUTUH BACKEND.', 'coral');
        break;
      }
      case 'transcribe': showToast('SpeechRecognizer lokal · contoh transkrip ditampilkan.'); break;
      case 'translate': showToast('Terjemahan on-device opsional (ML Kit) · mockup.'); break;
      case 'export-srt': showToast('SRT diexport ke workspace lokal (mockup).'); break;
      case 'clipper-start': showToast('Heuristik lokal menemukan 3 highlight.'); break;
      case 'agent-send': {
        const input = document.getElementById('agentInput');
        if(input && input.value.trim()){
          appendAgent(input.value.trim());
          input.value = '';
        } else { showToast('Tulis instruksi dulu.', 'coral'); }
        break;
      }
      case 'agent-suggest': {
        appendAgent(el.textContent.trim());
        break;
      }
      case 'share-now': showToast('Android share sheet terbuka di build asli.'); break;
      case 'share-app': showToast('Direct publish butuh SDK/API platform. Gunakan share sheet.', 'coral'); break;
      case 'copy-link': copyText('https://github.com/az925-crypto/Zcut', 'Link repo dicopy.'); break;
      case 'copy-screen': copyText(window.location.href, 'Link screen dicopy.'); break;
      case 'save-gallery': showToast('Disimpan ke Movies/Zcut (mockup).'); break;
      case 'save-photo': showToast('Foto diexport ke gallery (mockup).'); break;
      case 'backup': showToast('Backup zip lokal dibuat (mockup).'); break;
      case 'toggle': {
        if(el.classList.contains('switch')){ el.classList.toggle('off'); showToast(el.classList.contains('off') ? 'Dimatikan.' : 'Diaktifkan.'); }
        break;
      }
      case 'pick-filter': case 'pick-effect': case 'pick-mask': case 'pick-layout': case 'pick-cam': case 'pick-bg': case 'pick-avatar': case 'pick-sticker': case 'pick-transition': case 'pick-voice': {
        if(el.parentElement){ el.parentElement.querySelectorAll('.active').forEach(function(x){x.classList.remove('active');}); }
        el.classList.add('active');
        showToast('Pilihan diperbarui.');
        break;
      }
      case 'select-media': {
        const badge = el.querySelector('.badge');
        if(badge){ badge.remove(); } else {
          const b = document.createElement('span'); b.className='badge lime'; b.style.position='absolute'; b.style.right='4px'; b.style.top='4px'; b.textContent='✓'; el.querySelector('.tile-art').appendChild(b);
        }
        break;
      }
      case 'photo-tool': {
        el.parentElement.querySelectorAll('.active').forEach(function(x){x.classList.remove('active');});
        el.classList.add('active');
        break;
      }
      case 'reset': showToast('Nilai dikembalikan ke default.'); break;
      case 'favorite': showToast('Template disimpan ke favorit lokal.'); break;
      case 'open-result': showToast('Hasil dibuka di preview (mockup).'); break;
      case 'replace-video': showToast('Pilih video lain dari gallery.'); break;
      case 'preview-voice': showToast('Preview suara lokal (mockup).'); break;
      case 'record-voice': showToast('Voiceover recorder butuh izin mikrofon di build asli.'); break;
      case 'add-sfx': showToast('SFX ditambahkan ke track audio.'); break;
      case 'preview-avatar': showToast('Preview avatar · render penuh BUTUH BACKEND.', 'coral'); break;
      case 'digital-human': showToast('Render avatar/lip sync BUTUH BACKEND.', 'coral'); break;
      case 'ad-generate': showToast('Draft iklan lokal dibuat dari script + template.'); break;
      case 'split-live': showToast('Live photo dipisah menjadi still + motion.'); break;
      case 'combine-live': showToast('Still + motion digabung (mockup).'); break;
      case 'play-live': showToast('Memutar live photo 3.2s.'); break;
      case 'shutter': showToast('Capture (mockup) · CameraX di build asli.'); break;
      case 'flash': showToast('Flash toggle (mockup).'); break;
      case 'flip-camera': showToast('Kamera depan/belakang (mockup).'); break;
      case 'import': showToast('Import dari folder lokal via SAF (mockup).'); break;
      case 'browse': showToast('SAF file picker terbuka di build asli.'); break;
      case 'clear-cache': showToast('Cache preview dibersihkan (mockup).'); break;
      case 'open-current': renderPhone(state.current); showToast('Screen dibuka ulang.'); break;
      case 'request-acc': openApproval(); break;
      case 'close-modal': closeModal(); break;
      case 'copy-approval': copyText('Zcut Stage 2 mockup — review all screens, then reply ACC via Telegram.', 'Pesan review dicopy.'); break;
      default: showToast('Interaksi ini belum diimplementasikan di mockup.', 'coral');
    }
  }

  function appendAgent(text){
    const chat = document.getElementById('agentChat');
    if(!chat) return;
    const user = document.createElement('div');
    user.className = 'msg user';
    user.textContent = text;
    chat.appendChild(user);
    const bot = document.createElement('div');
    bot.className = 'msg bot';
    bot.innerHTML = '<span class="msg-tag">Zcut Agent · local plan</span>Siap. Aku akan memakai tool lokal: pilih klip, trim, caption, dan music loop. Tidak ada upload. Tinjau plan sebelum apply.';
    chat.appendChild(bot);
    const scroller = chat.closest('.screen-scroll');
    if(scroller) scroller.scrollTop = scroller.scrollHeight;
  }

  function startExport(btn){
    const progress = document.getElementById('exportProgress');
    const bar = document.getElementById('exportBar');
    const pct = document.getElementById('exportPct');
    const status = document.getElementById('exportStatus');
    if(!progress) return;
    progress.style.display = 'block';
    btn.disabled = true;
    btn.style.opacity = '.6';
    btn.innerHTML = ico('refresh')+' Rendering…';
    let value = 0;
    const steps = ['Preparing timeline…','Rendering video…','Mixing audio…','Finalizing MP4…'];
    const timer = setInterval(function(){
      value = Math.min(100, value + 8 + Math.round(Math.random()*7));
      if(bar) bar.style.width = value+'%';
      if(pct) pct.textContent = value+'%';
      if(status) status.textContent = steps[Math.min(steps.length-1, Math.floor(value/26))];
      if(value >= 100){
        clearInterval(timer);
        if(status) status.textContent = 'Complete';
        showToast('Render selesai · tanpa watermark.');
        setTimeout(function(){ go('export-done'); }, 500);
      }
    }, 190);
  }

  function openApproval(){
    openModal('Review Zcut mockup', '<p>Mockup ini mencakup 42 layar untuk 31 area fitur. Semua fitur terbuka, tanpa paywall, watermark, atau server CapCut.</p><p><b>ACC tidak bisa dilakukan dari halaman ini.</b> Balas <b>ACC</b> via Telegram setelah review. Setelah ACC, implementasi UI Android dimulai.</p><div class="panel" style="background:var(--panel-2)"><div class="kv"><span>Stage</span><b>2 · Mockup</b></div><div class="kv"><span>Screens</span><b>42</b></div><div class="kv"><span>Backend flags</span><b>7 area</b></div><div class="kv"><span>Paywall/ads</span><b>Removed</b></div></div>', '<button class="btn" data-action="copy-approval">'+ico('copy')+' Copy review note</button><button class="btn primary" data-action="close-modal">Close</button>');
  }

  function openNotes(){
    openModal('Design notes', '<p><b>Direction:</b> dark studio, electric lime, coral for destructive/attention states. Space Grotesk-like display weight with compact mono labels.</p><p><b>Rules:</b> no CapCut logos, fonts, effect packages, or templates. All art in this mockup is CSS-generated. Backend-dependent tools carry a visible badge instead of a paywall.</p><p><b>Next:</b> after ACC, translate these screens to Compose and implement the local data/render layers first.</p>', '<button class="btn primary" data-action="close-modal">Close</button>');
  }

  document.addEventListener('click', function(e){
    const mapCard = e.target.closest('[data-screen]');
    if(mapCard){ go(mapCard.getAttribute('data-screen')); return; }
    const goEl = e.target.closest('[data-go]');
    if(goEl){ e.preventDefault(); go(goEl.getAttribute('data-go')); return; }
    const actEl = e.target.closest('[data-action]');
    if(actEl){ e.preventDefault(); handleAction(actEl.getAttribute('data-action'), actEl); return; }
    const tab = e.target.closest('[data-tab]');
    if(tab){
      if(tab.getAttribute('data-tab') === 'map') showMap();
      else if(tab.getAttribute('data-tab') === 'notes') openNotes();
      else showPrototype();
      return;
    }
  });

  document.getElementById('tabPrototype').addEventListener('click', showPrototype);
  document.getElementById('tabMap').addEventListener('click', showMap);
  document.getElementById('tabNotes').addEventListener('click', openNotes);
  document.getElementById('approveBtn').addEventListener('click', openApproval);
  document.getElementById('closeModal').addEventListener('click', closeModal);
  modal.addEventListener('click', function(e){ if(e.target === modal) closeModal(); });
  document.addEventListener('keydown', function(e){ if(e.key === 'Escape') closeModal(); });

  renderRail();
  const params = new URLSearchParams(window.location.search);
  if(params.get('view') === 'map'){ showMap(); }
  else { renderPhone(params.get('screen') || 'home'); }
})();
