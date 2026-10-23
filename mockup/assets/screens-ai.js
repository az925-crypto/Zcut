/* Zcut mockup — AI, capture, share, system and special layout screens */
RENDER['ai-clipper'] = function(){
  const app = appbar('AI Clipper',{back:'ai-studio',sub:'Long video → short moments',actions:'<span class="badge lime">LOCAL*</span>'});
  const content = '<div class="screen-pad">'+
    '<div class="panel" style="display:flex;gap:10px;align-items:center"><div class="list-thumb" style="width:62px;height:62px"><div class="art city"></div></div><div class="fr-text"><b>long_vlog_0842.mp4</b><span>08:42 · 1080p · 1.2 GB</span></div><button class="icon-btn" data-action="replace-video">'+ico('refresh')+'</button></div>'+
    '<div class="mini-label">Target length</div><div class="chips"><button class="chip active">15s</button><button class="chip">30s</button><button class="chip">60s</button><button class="chip">Custom</button></div>'+
    '<div class="mini-label">Style</div><div class="chips"><button class="chip active">Vlog</button><button class="chip">Funny</button><button class="chip">Cinematic</button><button class="chip">Product</button></div>'+
    '<button class="primary-wide" style="margin-top:10px" data-action="clipper-start">'+ico('scissors')+' Find highlights</button>'+
    '<div class="panel" style="margin-top:10px">'+backendNote('ASR dan highlight scoring penuh ditandai BUTUH BACKEND. Heuristik silence/scene berjalan lokal.')+'</div>'+
    '<div class="mini-label">Suggested cuts</div>'+
    '<div id="clipResults" class="list">'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art street"></div><span class="badge lime" style="position:absolute;left:3px;bottom:3px">92</span></div><div class="list-info"><b>“the quiet side of the city”</b><span>00:12.4 · 15s · hook + payoff</span></div>'+ico('forward','chev')+'</div>'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art food"></div><span class="badge lime" style="position:absolute;left:3px;bottom:3px">87</span></div><div class="list-info"><b>“tiny ramen shop”</b><span>00:31.8 · 15s · close-up</span></div>'+ico('forward','chev')+'</div>'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art night"></div><span class="badge lime" style="position:absolute;left:3px;bottom:3px">81</span></div><div class="list-info"><b>“last train home”</b><span>00:48.2 · 15s · ending</span></div>'+ico('forward','chev')+'</div>'+
    '</div>'+
    '<button class="secondary-wide" style="margin-top:9px" data-go="editor">'+ico('plus')+' Create draft from all 3</button>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER['ai-agent'] = function(){
  const app = appbar('Zcut Agent',{back:'ai-studio',sub:'Tool-calling assistant · local rules',actions:'<span class="badge lime">OFFLINE</span>'});
  const content = '<div class="screen-pad">'+
    '<div class="chat" id="agentChat">'+
      '<div class="msg bot"><span class="msg-tag">Zcut Agent</span>What are we making? I can build a rough cut, add captions, or find the best 15 seconds.</div>'+
      '<div class="msg user">Make a 20-second night-city edit with captions and a soft beat.</div>'+
      '<div class="msg bot"><span class="msg-tag">Plan · local tools</span>I will select 4 clips, add a 0.8s dissolve, place captions from the transcript, and use the local lo-fi generator. No upload needed.</div>'+
      '<div class="msg bot"><span class="msg-tag">Ready</span>3 tool calls ready. Review the plan before I touch the timeline.</div>'+
    '</div>'+
    '<div class="chips"><button class="chip" data-action="agent-suggest">Tighten the intro</button><button class="chip" data-action="agent-suggest">Add captions</button><button class="chip" data-action="agent-suggest">Find a hook</button></div>'+
    '<div class="chat-input"><input id="agentInput" placeholder="Describe an edit…" aria-label="Message Zcut Agent"><button data-action="agent-send">'+ico('arrow')+'</button></div>'+
    '<div class="panel" style="margin-top:10px"><div class="row-between"><span style="font-size:8px;color:var(--muted)">LLM backend</span><span class="badge coral">OPTIONAL</span></div><p style="margin-top:6px">Tool calls, timeline validation, dan preview berjalan lokal. Kualitas bahasa penuh bisa ditingkatkan dengan backend sendiri.</p></div>'+
    '<button class="secondary-wide" style="margin-top:9px" data-go="editor">'+ico('play')+' Apply plan to draft</button>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER['ai-generate'] = function(){
  const app = appbar('Generate',{back:'ai-studio',sub:'Text to image / video',actions:'<span class="badge coral">BACKEND</span>'});
  const content = '<div class="screen-pad">'+
    '<div class="ai-hero"><h2>Describe a shot.</h2><p>Generation needs a model backend. The UI, history, and import flow are local.</p></div>'+
    '<textarea class="input textarea" style="margin-top:10px" aria-label="Generation prompt">a rain-soaked Tokyo alley at midnight, neon reflections, cinematic 35mm</textarea>'+
    '<div class="segmented" style="margin-top:9px"><button class="active">Image</button><button>Video</button></div>'+
    '<div class="mini-label">Aspect</div><div class="chips"><button class="chip active">9:16</button><button class="chip">1:1</button><button class="chip">16:9</button><button class="chip">4:5</button></div>'+
    '<div class="mini-label">Style</div><div class="chips"><button class="chip active">Cinematic</button><button class="chip">Anime</button><button class="chip">Product</button><button class="chip">Documentary</button></div>'+
    '<div class="panel" style="margin-top:10px">'+backendNote('Text-to-image/video, inpainting, video extend, face cover, dan translate membutuhkan backend/model sendiri. Tidak ada panggilan ke server CapCut.')+'</div>'+
    '<button class="primary-wide" style="margin-top:10px" data-action="ai-generate">'+ico('sparkles')+' Generate</button>'+
    '<div id="aiResult" class="panel" style="display:none;margin-top:10px"><div class="row-between"><b style="font-size:9px">Preview placeholder</b><span class="badge coral">BACKEND</span></div><div style="height:110px;border-radius:8px;overflow:hidden;margin-top:7px;position:relative"><div class="art night"></div></div><p style="margin-top:6px">Hubungkan backend sendiri untuk menghasilkan aset. Kamu juga bisa import hasil dari galeri.</p></div>'+
    '<div class="mini-label">History</div><div class="grid-3">'+
      '<div class="tile" data-action="open-result"><div class="tile-art" style="height:60px"><div class="art night"></div></div></div>'+
      '<div class="tile" data-action="open-result"><div class="tile-art" style="height:60px"><div class="art pink"></div></div></div>'+
      '<div class="tile" data-action="open-result"><div class="tile-art" style="height:60px"><div class="art blue"></div></div></div>'+
    '</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER['digital-human'] = function(){
  const app = appbar('Digital Human',{back:'ai-studio',sub:'Avatar presenter · backend render',actions:'<span class="badge coral">BACKEND</span>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:230px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art portrait"></div><div style="position:absolute;inset:0;background:linear-gradient(0deg,rgba(0,0,0,.7),transparent 55%)"></div><div style="position:absolute;left:12px;bottom:12px"><b style="font-size:11px">Ava · Studio</b><span style="display:block;font-size:7px;color:var(--muted);margin-top:2px">Preview only · render butuh backend</span></div><button class="play-badge" data-action="preview-avatar">'+ico('play')+'</button></div>'+
    '<div class="mini-label">Avatar</div><div class="grid-4">'+
      '<div class="tile" data-action="pick-avatar"><div class="tile-art" style="height:54px"><div class="art portrait"></div></div></div>'+
      '<div class="tile" data-action="pick-avatar"><div class="tile-art" style="height:54px"><div class="art pink"></div></div></div>'+
      '<div class="tile" data-action="pick-avatar"><div class="tile-art" style="height:54px"><div class="art blue"></div></div></div>'+
      '<div class="tile" data-action="pick-avatar"><div class="tile-art" style="height:54px"><div class="art gold"></div></div></div>'+
    '</div>'+
    '<div class="panel" style="margin-top:10px"><h4>Script</h4><textarea class="input textarea" aria-label="Avatar script">Welcome back to the channel. Today we are cutting a night-city short in under a minute.</textarea></div>'+
    '<div class="panel" style="margin-top:9px"><div class="kv"><span>Voice</span><b>Aria · local TTS</b></div><div class="kv"><span>Lip sync</span><b>Backend</b></div><div class="kv"><span>Background</span><b>Studio</b></div></div>'+
    '<button class="primary-wide" style="margin-top:10px" data-action="digital-human">'+ico('user')+' Generate avatar video</button>'+
    '<div class="panel" style="margin-top:9px">'+backendNote('Render avatar dan lip sync membutuhkan backend/model. Script, TTS, subtitle, dan background lokal tetap bisa disiapkan.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.camera = function(){
  return screenScroll('<div class="camera-screen">'+
    '<div class="camera-view"><div class="art city"></div><div class="cam-grid"></div>'+
      '<div class="cam-top"><button class="icon-btn" data-go="home">'+ico('x')+'</button><div class="badge" style="background:rgba(0,0,0,.35)">9:16 · 1080p</div><button class="icon-btn" data-action="flash">'+ico('flash')+'</button></div>'+
      '<div style="position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:70px;height:70px;border:1px solid rgba(255,255,255,.35);border-radius:50%"></div>'+
      '<div class="cam-modes"><span>PHOTO</span><span class="active">VIDEO</span><span>PORTRAIT</span><span>SLOW-MO</span></div>'+
    '</div>'+
    '<div class="camera-controls"><button class="cam-side" data-go="media-picker">'+ico('gallery')+'</button><button class="shutter" data-action="shutter"><i></i></button><button class="cam-side" data-action="flip-camera">'+ico('refresh')+'</button></div>'+
    '<div style="display:flex;justify-content:center;gap:16px;padding:0 0 12px;font-size:7px;color:var(--muted)"><span>BEAUTY ON</span><span>FILTER · TOKYO</span><span>TIMER · OFF</span></div>'+
  '</div>', 'camera-scroll');
};

RENDER['photo-editor'] = function(){
  const app = appbar('Photo',{back:'camera',sub:'Local photo editor',actions:'<button class="accent-btn" data-action="save-photo">Export</button>'});
  const content = '<div class="photo-stage"><div class="photo"><div class="art portrait"></div></div><span class="thumb-label">PORTRAIT · 4:5</span></div>'+
    '<div class="photo-tools">'+
      '<button class="photo-tool active" data-action="photo-tool">'+ico('crop')+'Crop</button>'+
      '<button class="photo-tool" data-action="photo-tool">'+ico('adjust')+'Adjust</button>'+
      '<button class="photo-tool" data-action="photo-tool">'+ico('filter')+'Filter</button>'+
      '<button class="photo-tool" data-action="photo-tool">'+ico('beauty')+'Retouch</button>'+
      '<button class="photo-tool" data-action="photo-tool">'+ico('text')+'Text</button>'+
      '<button class="photo-tool" data-action="photo-tool">'+ico('sticker')+'Sticker</button>'+
      '<button class="photo-tool" data-action="photo-tool">'+ico('cutout')+'Cutout</button>'+
      '<button class="photo-tool" data-action="photo-tool">'+ico('brush')+'Brush</button>'+
    '</div>'+
    '<div class="screen-pad" style="padding-top:4px">'+
      '<div class="segmented"><button class="active">Light</button><button>Color</button><button>Detail</button></div>'+
      slider('Exposure','+4',54)+slider('Contrast','+8',58,'coral')+slider('Highlights','-12',38,'cyan')+slider('Shadows','+10',60)+
      '<div class="grid-2" style="margin-top:8px"><button class="secondary-wide" data-action="reset">'+ico('refresh')+' Reset</button><button class="secondary-wide" data-go="filters">'+ico('filter')+' Filters</button></div>'+
    '</div>';
  return screenScroll(app+content);
};

RENDER.share = function(){
  const app = appbar('Share',{back:'export-done',sub:'Share sheet · no watermark',actions:'<span class="badge lime">READY</span>'});
  const content = '<div class="screen-pad">'+
    '<div class="export-preview" style="height:170px"><div class="art tokyo"></div><div class="ep-overlay"><div><b>Midnight in Tokyo.mp4</b><span>1080p · 00:24 · 38.2 MB</span></div></div></div>'+
    '<div class="mini-label">Caption</div><textarea class="input textarea" style="min-height:66px" aria-label="Share caption">the quiet side of the city at midnight #zcut #nightwalk #tokyo</textarea>'+
    '<div class="mini-label">Share to</div><div class="share-apps">'+
      '<div class="share-app" data-action="share-app"><span class="sa-icon tiktok" style="font-weight:900;font-size:12px">T</span><span>TikTok</span></div>'+
      '<div class="share-app" data-action="share-app"><span class="sa-icon ig" style="font-weight:900;font-size:10px">IG</span><span>Instagram</span></div>'+
      '<div class="share-app" data-action="share-app"><span class="sa-icon yt" style="font-weight:900;font-size:10px">YT</span><span>YouTube</span></div>'+
      '<div class="share-app" data-action="share-app"><span class="sa-icon wa" style="font-weight:900;font-size:9px">WA</span><span>WhatsApp</span></div>'+
      '<div class="share-app" data-action="copy-link"><span class="sa-icon" style="color:var(--cyan)">'+ico('copy')+'</span><span>Copy</span></div>'+
    '</div>'+
    '<button class="primary-wide" style="margin-top:12px" data-action="share-now">'+ico('share')+' Share now</button>'+
    '<button class="secondary-wide" style="margin-top:8px" data-action="save-gallery">'+ico('download')+' Save to gallery</button>'+
    '<div class="panel" style="margin-top:10px">'+backendNote('Publish langsung ke platform membutuhkan SDK/API dan kredensial masing-masing. Share sheet Android berjalan lokal tanpa backend Zcut.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.profile = function(){
  const app = appbar('Profile',{noBack:true,sub:'Local profile · no account',actions:'<button class="icon-btn" data-go="settings">'+ico('settings')+'</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="profile-head"><div class="avatar">A</div><div style="flex:1"><b>Azka</b><span>Local creator · this device only</span></div><span class="badge lime">FREE</span></div>'+
    '<div class="unlock-banner">'+ico('unlock')+' All features unlocked. No subscription, no credits, no watermark.</div>'+
    '<div class="grid-3" style="margin-top:10px">'+
      '<div class="panel" style="text-align:center;padding:10px 5px"><b style="font-size:15px">12</b><span style="display:block;font-size:7px;color:var(--muted);margin-top:2px">Drafts</span></div>'+
      '<div class="panel" style="text-align:center;padding:10px 5px"><b style="font-size:15px">3</b><span style="display:block;font-size:7px;color:var(--muted);margin-top:2px">Exports</span></div>'+
      '<div class="panel" style="text-align:center;padding:10px 5px"><b style="font-size:15px">2.4</b><span style="display:block;font-size:7px;color:var(--muted);margin-top:2px">GB used</span></div>'+
    '</div>'+
    '<div class="panel" style="margin-top:10px">'+
      featureRow('grid','Templates','Local packs + your imports','templates')+
      featureRow('folder','Drafts','Backup and restore','drafts')+
      featureRow('sparkles','AI Studio','Local tools + backend options','ai-studio')+
      featureRow('settings','Settings','Theme, storage, privacy','settings')+
      featureRow('info','About Zcut','v0.1 · clean-room build','settings')+
    '</div>'+
    '<div class="hint" style="margin-top:10px">Tidak ada login, iklan, atau billing di build ini.</div>'+
  '</div>'+bottomNav('profile');
  return screenScroll(app+content);
};

RENDER.settings = function(){
  const app = appbar('Settings',{back:'profile',sub:'Local-first preferences',actions:'<span class="badge lime">NO ADS</span>'});
  const content = '<div class="screen-pad">'+
    '<div class="mini-label">Appearance</div><div class="panel">'+
      '<div class="settings-row"><span class="sr-icon">'+ico('palette')+'</span><div class="sr-text"><b>Theme</b><span>Dark studio</span></div><span class="badge">DARK</span></div>'+
      '<div class="settings-row"><span class="sr-icon">'+ico('globe')+'</span><div class="sr-text"><b>Language</b><span>English · Bahasa Indonesia</span></div><span class="badge">EN</span></div>'+
    '</div>'+
    '<div class="mini-label">Storage</div><div class="panel">'+
      '<div class="settings-row"><span class="sr-icon">'+ico('folder')+'</span><div class="sr-text"><b>Workspace</b><span>2.4 GB · 12 drafts</span></div><button class="btn sm" data-action="clear-cache">Clean</button></div>'+
      '<div class="settings-row"><span class="sr-icon">'+ico('cloud')+'</span><div class="sr-text"><b>Backup</b><span>Local zip · no cloud</span></div><span class="badge lime">LOCAL</span></div>'+
    '</div>'+
    '<div class="mini-label">Export defaults</div><div class="panel">'+
      '<div class="settings-row"><span class="sr-icon">'+ico('video')+'</span><div class="sr-text"><b>Resolution</b><span>1080p · 30 fps</span></div>'+ico('forward','chev')+'</div>'+
      '<div class="settings-row"><span class="sr-icon">'+ico('volume')+'</span><div class="sr-text"><b>Audio</b><span>AAC 48 kHz</span></div>'+ico('forward','chev')+'</div>'+
    '</div>'+
    '<div class="mini-label">Privacy</div><div class="panel">'+
      '<div class="settings-row"><span class="sr-icon">'+ico('shield')+'</span><div class="sr-text"><b>Telemetry</b><span>Off · no analytics SDK</span></div><span class="switch off" data-action="toggle"></span></div>'+
      '<div class="settings-row"><span class="sr-icon">'+ico('cloud')+'</span><div class="sr-text"><b>Network access</b><span>Only optional backend you configure</span></div><span class="badge">NONE</span></div>'+
    '</div>'+
    '<div class="panel" style="margin-top:10px"><div class="row-between"><span style="font-size:8px;color:var(--muted)">Billing / ads / credits</span><span class="badge lime">REMOVED</span></div><p style="margin-top:6px">Modul monetisasi sengaja tidak direkonstruksi. Semua fitur tetap terbuka.</p></div>'+
    '<div class="hint" style="margin-top:9px">Zcut v0.1 · package com.zaaam.zcut</div>'+
  '</div>'+bottomNav('profile');
  return screenScroll(app+content);
};

RENDER['split-screen'] = function(){
  const app = appbar('Split screen',{back:'editor',sub:'4 panels · center crop',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="split-preview" style="grid-template-columns:1fr 1fr;grid-template-rows:1fr 1fr"><div class="art tokyo"></div><div class="art beach"></div><div class="art portrait"></div><div class="art city"></div></div>'+
    '<div class="mini-label">Layout</div><div class="layout-grid">'+
      '<div class="layout-item" data-action="pick-layout"><div class="layout-preview" style="grid-template-columns:1fr"><i></i></div></div>'+
      '<div class="layout-item" data-action="pick-layout"><div class="layout-preview" style="grid-template-columns:1fr 1fr"><i></i><i></i></div></div>'+
      '<div class="layout-item active" data-action="pick-layout"><div class="layout-preview" style="grid-template-columns:1fr 1fr;grid-template-rows:1fr 1fr"><i></i><i></i><i></i><i></i></div></div>'+
      '<div class="layout-item" data-action="pick-layout"><div class="layout-preview" style="grid-template-columns:1fr 1fr 1fr;grid-template-rows:1fr 1fr"><i></i><i></i><i></i><i></i><i></i><i></i></div></div>'+
      '<div class="layout-item" data-action="pick-layout"><div class="layout-preview" style="grid-template-columns:1fr 1fr 1fr;grid-template-rows:1fr 1fr 1fr"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></div>'+
    '</div>'+
    '<div class="mini-label">Ratio</div><div class="chips"><button class="chip active">1:1</button><button class="chip">4:5</button><button class="chip">9:16</button><button class="chip">16:9</button></div>'+
    slider('Border','4',20)+slider('Corner radius','12',30,'cyan')+
    '<div class="panel"><div class="kv"><span>Background</span><b>#08090D</b></div><div class="kv"><span>Center crop</span><b>On</b></div><div class="kv"><span>Audio</span><b>Mix all</b></div></div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.gameplay = function(){
  const app = appbar('Gameplay',{back:'editor',sub:'Game + facecam overlay',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="gameplay-preview"><div class="art city"></div><div class="facecam"><div class="art portrait"></div></div><span class="thumb-label">GAMEPLAY + FACECAM</span></div>'+
    '<div class="mini-label">Facecam position</div><div class="layout-grid">'+
      '<div class="layout-item" data-action="pick-cam"><div class="layout-preview" style="grid-template-columns:1fr;position:relative"><i style="position:absolute;width:10px;height:12px;right:2px;top:2px"></i></div></div>'+
      '<div class="layout-item" data-action="pick-cam"><div class="layout-preview" style="grid-template-columns:1fr;position:relative"><i style="position:absolute;width:10px;height:12px;left:2px;top:2px"></i></div></div>'+
      '<div class="layout-item active" data-action="pick-cam"><div class="layout-preview" style="grid-template-columns:1fr;position:relative"><i style="position:absolute;width:10px;height:12px;right:2px;bottom:2px"></i></div></div>'+
      '<div class="layout-item" data-action="pick-cam"><div class="layout-preview" style="grid-template-columns:1fr;position:relative"><i style="position:absolute;width:10px;height:12px;left:2px;bottom:2px"></i></div></div>'+
    '</div>'+
    slider('Facecam size','74',52)+slider('Border','2',20,'cyan')+
    '<div class="panel"><div class="kv"><span>Shape</span><b>Rounded</b></div><div class="kv"><span>Chroma key</span><b>Off</b></div><div class="kv"><span>Game audio</span><b>Mix</b></div></div>'+
    '<div class="panel" style="margin-top:9px">'+backendNote('Rekaman gameplay memakai MediaProjection lokal. Tidak ada upload ke server.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER['live-photo'] = function(){
  const app = appbar('Live photo',{back:'editor',sub:'Motion photo toolkit',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:200px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art beach"></div><div style="position:absolute;left:10px;top:10px;display:flex;align-items:center;gap:5px;background:rgba(0,0,0,.45);border:1px solid rgba(255,255,255,.2);padding:4px 6px;border-radius:4px;font-size:6px;font-weight:900;letter-spacing:.08em"><span style="width:5px;height:5px;border-radius:50%;background:var(--coral)"></span>LIVE · 3.2s</div><button class="play-badge" data-action="play-live">'+ico('play')+'</button></div>'+
    '<div class="grid-2" style="margin-top:10px"><button class="secondary-wide" data-action="split-live">'+ico('scissors')+' Split photo</button><button class="secondary-wide" data-action="combine-live">'+ico('layers')+' Combine</button></div>'+
    '<div class="mini-label">Export as</div><div class="chips"><button class="chip active">Video</button><button class="chip">GIF</button><button class="chip">Still + motion</button></div>'+
    slider('Duration','3.2s',32)+slider('Motion strength','60%',60,'cyan')+
    '<div class="panel"><div class="kv"><span>Key photo</span><b>Frame 01</b></div><div class="kv"><span>Stabilize</span><b>On</b></div><div class="kv"><span>Audio</span><b>Keep ambient</b></div></div>'+
    '<div class="hint" style="margin-top:9px">Live photo diproses lokal; tidak ada upload.</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER['ad-maker'] = function(){
  const app = appbar('Ad Maker',{back:'ai-studio',sub:'Script → first cut · local',actions:'<span class="badge lime">LOCAL*</span>'});
  const content = '<div class="screen-pad">'+
    '<div class="export-preview" style="height:140px"><div class="art gold"></div><div class="ep-overlay"><div><b>Product ad · 15s</b><span>9:16 · local template · TTS</span></div></div></div>'+
    '<div class="script-box"><div class="row-between" style="margin-bottom:4px"><b style="font-size:9px">Script</b><span class="badge lime">3 SCENES</span></div>'+
      '<div class="script-line"><span class="sl-num">01</span><span class="sl-text">Meet the bottle that keeps up with your night.</span><span class="sl-time">0–4s</span></div>'+
      '<div class="script-line"><span class="sl-num">02</span><span class="sl-text">Cold for 24 hours. No plastic aftertaste.</span><span class="sl-time">4–9s</span></div>'+
      '<div class="script-line"><span class="sl-num">03</span><span class="sl-text">Built for the city. Ready when you are.</span><span class="sl-time">9–15s</span></div>'+
    '</div>'+
    '<div class="mini-label">Product</div><input class="input" value="Orbit Bottle · Matte Black" aria-label="Product name">'+
    '<div class="mini-label">CTA</div><div class="chips"><button class="chip active">Shop now</button><button class="chip">Learn more</button><button class="chip">Get offer</button></div>'+
    '<div class="panel" style="margin-top:10px"><div class="row-between"><h4 style="margin:0">Voiceover</h4><span class="switch" data-action="toggle"></span></div><div class="kv"><span>Voice</span><b>Aria · local TTS</b></div><div class="kv"><span>Music</span><b>Local procedural</b></div></div>'+
    '<button class="primary-wide" style="margin-top:10px" data-action="ad-generate">'+ico('sparkles')+' Generate ad draft</button>'+
    '<div class="panel" style="margin-top:9px">'+backendNote('Smart ad generatif dan product album AI ditandai BUTUH BACKEND. Script, TTS, caption, dan template berjalan lokal.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};
