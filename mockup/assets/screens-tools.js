/* Zcut mockup — visual, audio, export and draft screens */
RENDER.effects = function(){
  const app = appbar('Effects',{back:'editor',sub:'Local shader packs · no EffectSDK',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const items = [
    ['Glow','night','filter:brightness(1.1) saturate(1.2)','LOCAL'],['VHS','street','filter:contrast(1.2) sepia(.2)','LOCAL'],
    ['Chromatic','pink','filter:hue-rotate(20deg) saturate(1.5)','LOCAL'],['Dream','beach','filter:blur(.4px) brightness(1.1)','LOCAL'],
    ['Pixel','city','filter:contrast(1.4)','LOCAL'],['AR Mask','portrait','filter:saturate(1.3)','BACKEND'],
    ['Light leak','gold','filter:brightness(1.15)','LOCAL'],['Grain','night','filter:contrast(1.1)','LOCAL'],['Prism','blue','filter:hue-rotate(-20deg)','LOCAL']
  ];
  const content = '<div class="screen-pad">'+
    '<div style="height:174px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art night" style="filter:brightness(1.1) saturate(1.2)"></div><span class="thumb-label">GLOW · 70%</span></div>'+
    slider('Intensity','70%',70)+
    '<div class="chips"><button class="chip active">Trending</button><button class="chip">Retro</button><button class="chip">Light</button><button class="chip">AR</button><button class="chip">Distort</button></div>'+
    '<div class="effect-grid" style="margin-top:9px">'+items.map(function(it,i){
      return '<div class="effect-card '+(i===0?'active':'')+'" data-action="pick-effect"><div class="art '+it[1]+'" style="position:absolute;inset:0;'+it[2]+'"></div><span class="ec-badge">'+it[3]+'</span><span class="ec-label">'+it[0]+'</span></div>';
    }).join('')+'</div>'+
    '<div class="panel" style="margin-top:12px">'+backendNote('AR/face effect lanjutan ditandai BUTUH BACKEND. Effect dasar tetap dirender lokal dengan shader Zcut.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.mask = function(){
  const app = appbar('Mask & Chroma',{back:'editor',sub:'Overlay · shape mask',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const shapes = [['none','x'],['linear','adjust'],['mirror','copy'],['circle','eye'],['rect','crop'],['heart','heart'],['star','star']];
  const content = '<div class="screen-pad">'+
    '<div style="height:184px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art portrait"></div><div style="position:absolute;left:24%;top:18%;width:52%;height:64%;border:1px dashed rgba(201,255,74,.8);border-radius:50%;box-shadow:0 0 0 999px rgba(0,0,0,.35)"></div><span class="thumb-label">MASK · ELLIPSE</span></div>'+
    '<div class="mask-shapes" style="margin-top:10px">'+shapes.map(function(s,i){
      return '<div class="mask-shape '+(i===3?'active':'')+'" data-action="pick-mask">'+ico(s[1])+'</div>';
    }).join('')+'</div>'+
    slider('Feather','18',18)+slider('Position X','0',50)+slider('Position Y','-8',46,'coral')+
    '<div class="panel"><div class="row-between"><h4 style="margin:0">Chroma key</h4><span class="switch" data-action="toggle"></span></div><div class="kv"><span>Key color</span><b><span style="display:inline-block;width:10px;height:10px;border-radius:3px;background:#25d366;margin-right:4px;vertical-align:-1px"></span>#25D366</b></div><div class="kv"><span>Strength</span><b>64%</b></div><div class="kv"><span>Spill suppression</span><b>On</b></div></div>'+
    '<div class="grid-2" style="margin-top:9px"><button class="secondary-wide" data-action="invert">'+ico('refresh')+' Invert</button><button class="secondary-wide" data-go="matting">'+ico('cutout')+' Cutout</button></div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.matting = function(){
  const app = appbar('Cutout',{back:'editor',sub:'On-device subject segmentation',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="checker" style="height:196px;border-radius:14px;position:relative;overflow:hidden;border:1px solid var(--line)"><div class="art portrait" style="position:absolute;inset:0;clip-path:ellipse(37% 53% at 50% 54%)"></div><span class="thumb-label">SUBJECT · 94%</span></div>'+
    '<div class="grid-2" style="margin-top:10px"><button class="secondary-wide" data-action="cutout-toggle">'+ico('cutout')+' Remove BG</button><button class="secondary-wide" data-action="blur-bg">'+ico('eye')+' Blur BG</button></div>'+
    '<div class="mini-label">Background</div><div class="grid-3">'+
      '<div class="tile" data-action="pick-bg"><div class="tile-art" style="height:54px"><div class="art blue"></div></div><div class="tile-meta"><b>Studio</b><span>Local</span></div></div>'+
      '<div class="tile" data-action="pick-bg"><div class="tile-art" style="height:54px"><div class="art green"></div></div><div class="tile-meta"><b>Forest</b><span>Local</span></div></div>'+
      '<div class="tile" data-action="pick-bg"><div class="tile-art" style="height:54px"><div class="art gold"></div></div><div class="tile-meta"><b>Sunset</b><span>Local</span></div></div>'+
    '</div>'+
    slider('Edge refine','62%',62)+slider('Shadow','22%',22,'coral')+
    '<div class="panel" style="margin-top:8px">'+backendNote('AI background generation ditandai BUTUH BACKEND. Segmentasi subjek memakai model on-device.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.beauty = function(){
  const app = appbar('Retouch',{back:'editor',sub:'Face + body · local GPU',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:180px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art portrait"></div><div class="bounding-box" style="left:34%;top:16%;width:32%;height:52%"></div><span class="thumb-label">FACE DETECTED · 1</span></div>'+
    '<div class="segmented" style="margin-top:10px"><button class="active">Face</button><button>Body</button><button>Makeup</button></div>'+
    '<div class="slider-stack" style="margin-top:8px">'+
      slider('Skin smooth','34',34)+slider('Whiten','18',18,'cyan')+slider('Face slim','22',22,'coral')+
      slider('Eyes','12',12)+slider('Nose','8',8,'coral')+slider('Lips','16',16,'cyan')+
    '</div>'+
    '<div class="panel"><div class="row-between"><h4 style="margin:0">Preserve texture</h4><span class="switch" data-action="toggle"></span></div><p style="margin-top:6px">Mengurangi plastik look dengan mempertahankan detail pori.</p></div>'+
    '<div class="hint" style="margin-top:8px">Semua proses di perangkat. Tidak ada upload wajah.</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.stickers = function(){
  const app = appbar('Stickers',{back:'editor',sub:'Overlay · local assets',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:184px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art street"></div><div style="position:absolute;left:14px;top:18px;background:var(--lime);color:#0a0b0f;font-weight:900;font-size:14px;padding:5px 9px;border-radius:7px;transform:rotate(-7deg)">LOL</div><div style="position:absolute;right:16px;bottom:22px;border:1px solid #fff;color:#fff;font-size:8px;font-weight:900;letter-spacing:.08em;padding:5px 8px;border-radius:99px;background:rgba(0,0,0,.3);backdrop-filter:blur(4px)">NO GATE</div><span class="thumb-label">2 STICKERS</span></div>'+
    '<div class="chips" style="margin-top:10px"><button class="chip active">Trending</button><button class="chip">Emoji</button><button class="chip">Text</button><button class="chip">Shapes</button><button class="chip">GIF</button></div>'+
    '<div class="grid-4" style="margin-top:9px">'+
      ['✦','LOL','100','♡','!!','▲','◎','★'].map(function(s){return '<button class="sticker-tile" data-action="pick-sticker">'+s+'</button>';}).join('')+
    '</div>'+
    '<div class="panel" style="margin-top:10px"><div class="row-between"><h4 style="margin:0">Track subject</h4><span class="switch off" data-action="toggle"></span></div><p style="margin-top:6px">Tracking objek lanjutan butuh backend. Fallback lokal: keyframe manual.</p></div>'+
    '<div class="hint" style="margin-top:8px">Sticker dibuat sendiri; tidak ada aset CapCut.</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.text = function(){
  const app = appbar('Text',{back:'editor',sub:'Title · local font pack',actions:'<button class="accent-btn" data-go="editor">Add</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:176px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art tokyo"></div><div style="position:absolute;inset:0;display:grid;place-items:center;text-align:center"><div style="font-size:28px;font-weight:900;letter-spacing:.12em;text-shadow:0 3px 24px rgba(0,0,0,.65)">MIDNIGHT</div></div><span class="thumb-label">TITLE · 42 PT</span></div>'+
    '<input class="input" style="margin-top:10px" value="MIDNIGHT" aria-label="Text content">'+
    '<div class="mini-label">Font</div><div class="chips"><button class="chip active">Display</button><button class="chip">Serif</button><button class="chip">Mono</button><button class="chip">Hand</button><button class="chip">Import</button></div>'+
    '<div class="mini-label">Style</div><div class="chips"><button class="chip active">Bold</button><button class="chip">Outline</button><button class="chip">Shadow</button><button class="chip">Neon</button><button class="chip">Gradient</button></div>'+
    '<div class="mini-label">Color</div><div class="swatch-row"><span class="swatch white active"></span><span class="swatch lime"></span><span class="swatch coral"></span><span class="swatch cyan"></span><span class="swatch violet"></span><span class="swatch black"></span></div>'+
    slider('Size','42',54)+slider('Spacing','12',30,'cyan')+slider('Opacity','100%',100)+
    '<div class="mini-label">Animation</div><div class="chips"><button class="chip">None</button><button class="chip active">Rise</button><button class="chip">Typewriter</button><button class="chip">Pop</button><button class="chip">Glitch</button></div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.captions = function(){
  const app = appbar('Captions',{back:'editor',sub:'Transcript · local + backend',actions:'<button class="accent-btn" data-action="export-srt">SRT</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:176px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art street"></div><div style="position:absolute;left:10px;right:10px;bottom:16px;text-align:center;font-size:10px;font-weight:800;text-shadow:0 2px 8px #000"><span style="background:rgba(0,0,0,.45);padding:4px 7px;border-radius:4px">we found the quiet side of the city</span></div><span class="thumb-label">CAPTION · STYLE 04</span></div>'+
    '<div class="segmented" style="margin-top:10px"><button class="active">Auto</button><button>Manual</button><button>Style</button></div>'+
    '<div class="panel" style="margin-top:9px">'+backendNote('Transkripsi akurat multi-bahasa ditandai BUTUH BACKEND. Tombol lokal memakai SpeechRecognizer perangkat.')+'</div>'+
    '<button class="primary-wide" style="margin-top:9px" data-action="transcribe">'+ico('captions')+' Transcribe locally</button>'+
    '<div class="mini-label">Transcript · 00:24</div>'+
    '<div class="caption-line"><div class="caption-time">00:00</div><div class="caption-text">the city is loud at <mark>midnight</mark><span class="edit">tap to edit</span></div></div>'+
    '<div class="caption-line"><div class="caption-time">00:04</div><div class="caption-text">but the side streets go quiet<span class="edit">tap to edit</span></div></div>'+
    '<div class="caption-line"><div class="caption-time">00:09</div><div class="caption-text">we found a tiny ramen shop<span class="edit">tap to edit</span></div></div>'+
    '<div class="caption-line"><div class="caption-time">00:15</div><div class="caption-text">and stayed until the trains stopped<span class="edit">tap to edit</span></div></div>'+
    '<div class="grid-2" style="margin-top:9px"><button class="secondary-wide" data-action="translate">'+ico('globe')+' Translate</button><button class="secondary-wide" data-go="text">'+ico('text')+' Style</button></div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.audio = function(){
  const app = appbar('Audio',{back:'editor',sub:'Music + voiceover · local mix',actions:'<button class="accent-btn" data-go="editor">Done</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="segmented"><button class="active">Music</button><button>Voiceover</button><button>SFX</button></div>'+
    '<div class="panel" style="margin-top:9px"><div class="row-between"><div><b style="font-size:10px">midnight_drive.wav</b><span style="display:block;font-size:7px;color:var(--muted);margin-top:2px">Local music · 00:24 · 48 kHz</span></div><span class="badge lime">ON</span></div>'+waveBars(42,'cyan')+'<div class="row-between" style="font-size:7px;color:var(--muted-2)"><span>00:00</span><span>00:24</span></div></div>'+
    slider('Volume','72%',72)+slider('Fade in','0.8s',22)+slider('Fade out','1.2s',31,'coral')+
    '<div class="panel"><div class="row-between"><h4 style="margin:0">Beat sync</h4><span class="switch" data-action="toggle"></span></div><p style="margin-top:6px">Beat markers terdeteksi lokal dari waveform. Tidak ada upload audio.</p></div>'+
    '<button class="secondary-wide" style="margin-top:9px" data-action="record-voice">'+ico('mic')+' Record voiceover</button>'+
    '<div class="mini-label">Sound effects</div>'+
    '<div class="list-card" data-action="add-sfx"><div class="list-thumb" style="display:grid;place-items:center;background:var(--panel-3);color:var(--cyan)">'+ico('volume')+'</div><div class="list-info"><b>Whoosh</b><span>SFX · 0.8s</span></div><span class="badge">LOCAL</span></div>'+
    '<div class="panel" style="margin-top:9px">'+backendNote('Vocal separation dan dubbing ditandai BUTUH BACKEND. Mixing, fade, dan beat detection berjalan lokal.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.tts = function(){
  const app = appbar('Text to speech',{back:'editor',sub:'Android TTS · local voices',actions:'<span class="badge lime">LOCAL</span>'});
  const voices = [['Aria','en-US · warm','active'],['Kai','en-US · deep',''],['Nova','id-ID · clear',''],['Sol','en-GB · bright','']];
  const content = '<div class="screen-pad">'+
    '<div class="panel"><h4>Script</h4><textarea class="input textarea" aria-label="TTS script">The city is loud at midnight, but the side streets go quiet.</textarea><div class="row-between" style="font-size:7px;color:var(--muted-2);margin-top:5px"><span>62 characters</span><span>≈ 4.2s</span></div></div>'+
    '<div class="mini-label">Voice</div><div class="list">'+voices.map(function(v){
      return '<div class="list-card '+(v[2]?'':'')+'" data-action="pick-voice" style="'+(v[2]?'border-color:rgba(201,255,74,.35)':'')+'"><div class="list-thumb" style="display:grid;place-items:center;background:var(--panel-3);color:var(--lime)">'+ico('user')+'</div><div class="list-info"><b>'+v[0]+'</b><span>'+v[1]+'</span></div><button class="icon-btn" data-action="preview-voice">'+ico('play')+'</button></div>';
    }).join('')+'</div>'+
    slider('Rate','1.0×',50)+slider('Pitch','1.0×',50,'cyan')+
    '<button class="primary-wide" style="margin-top:10px" data-action="tts-generate">'+ico('sparkles')+' Generate voice</button>'+
    '<div id="ttsResult" class="panel" style="display:none;margin-top:10px"><div class="row-between"><b style="font-size:9px">Generated locally</b><span class="badge lime">READY</span></div>'+waveBars(36,'lime')+'<button class="secondary-wide" style="margin-top:6px" data-go="editor">'+ico('plus')+' Insert to timeline</button></div>'+
    '<div class="panel" style="margin-top:9px">'+backendNote('Voice clone membutuhkan model suara dan ditandai BUTUH BACKEND. TTS sistem tetap gratis dan lokal.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER['ai-music'] = function(){
  const app = appbar('AI Music',{back:'editor',sub:'Procedural local generator',actions:'<span class="badge lime">NO CREDITS</span>'});
  const content = '<div class="screen-pad">'+
    '<div class="ai-hero"><h2>Set the mood.</h2><p>Generate a local loop or bring your own track. No server, no license gate.</p></div>'+
    '<textarea class="input textarea" style="margin-top:10px" aria-label="Music prompt">warm lo-fi beat for a night drive, soft keys, vinyl texture</textarea>'+
    '<div class="mini-label">Mood</div><div class="chips"><button class="chip active">Lo-fi</button><button class="chip">Cinematic</button><button class="chip">Pop</button><button class="chip">Ambient</button><button class="chip">Tense</button></div>'+
    slider('Duration','24s',48)+
    '<div class="panel"><div class="kv"><span>Tempo</span><b>84 BPM</b></div><div class="kv"><span>Key</span><b>A minor</b></div><div class="kv"><span>Engine</span><b>Local procedural</b></div></div>'+
    '<button class="primary-wide" style="margin-top:10px" data-action="music-generate">'+ico('music')+' Generate music</button>'+
    '<div id="musicResult" class="panel" style="display:none;margin-top:10px"><div class="row-between"><b style="font-size:9px">night_drive_loop.wav</b><span class="badge lime">READY</span></div>'+waveBars(40,'cyan')+'<button class="secondary-wide" style="margin-top:6px" data-go="editor">'+ico('plus')+' Insert to timeline</button></div>'+
    '<div class="panel" style="margin-top:9px">'+backendNote('Model musik generatif penuh ditandai BUTUH BACKEND. Generator prosedural lokal menyediakan loop dasar.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.export = function(){
  const app = appbar('Export',{back:'editor',sub:'Local render · no watermark',actions:'<span class="badge lime">FREE</span>'});
  const content = '<div class="screen-pad">'+
    '<div class="export-preview"><div class="art tokyo"></div><div class="ep-overlay"><div><b>Midnight in Tokyo</b><span>9:16 · 24.0s · 30 fps</span></div></div></div>'+
    '<div class="panel"><div class="kv"><span>Resolution</span><b>1080p</b></div><div class="kv"><span>Frame rate</span><b>30 fps</b></div><div class="kv"><span>Format</span><b>MP4 · H.264</b></div><div class="kv"><span>Audio</span><b>AAC · 48 kHz</b></div><div class="kv"><span>Bitrate</span><b>Auto · 12 Mbps</b></div></div>'+
    '<div class="panel" style="margin-top:9px"><div class="row-between"><div><b style="font-size:9px">Smart cover</b><span style="display:block;font-size:7px;color:var(--muted);margin-top:2px">Pick a strong frame automatically</span></div><span class="switch" data-action="toggle"></span></div></div>'+
    '<div class="panel" style="margin-top:9px"><div class="row-between"><div><b style="font-size:9px">Watermark</b><span style="display:block;font-size:7px;color:var(--muted);margin-top:2px">Never added</span></div><span class="badge lime">OFF</span></div></div>'+
    '<div id="exportProgress" style="display:none;margin-top:12px"><div class="progress"><i id="exportBar" style="width:0%"></i></div><div class="export-row"><span id="exportStatus">Preparing timeline…</span><span id="exportPct">0%</span></div></div>'+
    '<button id="exportBtn" class="primary-wide" style="margin-top:12px" data-action="export-start">'+ico('download')+' Export video</button>'+
    '<div class="hint" style="margin-top:8px">Render berjalan lokal dengan Media3. Estimasi 38 MB · 00:24.</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER['export-done'] = function(){
  return screenScroll('<div class="screen-pad" style="padding-top:18px">'+
    '<div class="success-ring">'+ico('check')+'</div>'+
    '<div style="text-align:center"><div class="eyebrow">RENDER COMPLETE</div><h1 style="font-size:25px;letter-spacing:-.06em;margin:2px 0 5px">Your cut is ready.</h1><p style="font-size:9px;color:var(--muted);margin:0">Midnight in Tokyo · 1080p · 00:24</p></div>'+
    '<div class="export-preview" style="margin-top:14px;height:180px"><div class="art tokyo"></div><div class="ep-overlay"><div><b>Midnight in Tokyo.mp4</b><span>38.2 MB · saved to Movies/Zcut</span></div></div></div>'+
    '<div class="grid-2"><button class="primary-wide" data-go="share">'+ico('share')+' Share</button><button class="secondary-wide" data-action="save-gallery">'+ico('download')+' Save</button></div>'+
    '<button class="secondary-wide" style="margin-top:8px" data-go="home">'+ico('home')+' Back home</button>'+
    '<div class="panel" style="margin-top:12px"><div class="row-between"><span style="font-size:8px;color:var(--muted)">Watermark</span><span class="badge lime">NONE</span></div><div class="row-between" style="margin-top:7px"><span style="font-size:8px;color:var(--muted)">Paywall</span><span class="badge lime">NONE</span></div><div class="row-between" style="margin-top:7px"><span style="font-size:8px;color:var(--muted)">Upload</span><span class="badge lime">NONE</span></div></div>'+
  '</div>');
};

RENDER.drafts = function(){
  const app = appbar('Drafts',{back:'home',sub:'Local workspace · 12 projects',actions:'<button class="icon-btn" data-go="media-picker">'+ico('plus')+'</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="segmented"><button class="active">Local</button><button>Backup</button><button>Trash</button></div>'+
    '<div class="list" style="margin-top:10px">'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art tokyo"></div></div><div class="list-info"><b>Midnight in Tokyo</b><span>Edited 2h ago · 24s · 38 MB</span></div><span class="badge lime">AUTO</span></div>'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art beach"></div></div><div class="list-info"><b>Summer reel</b><span>Edited yesterday · 18s</span></div>'+ico('more','chev')+'</div>'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art portrait"></div></div><div class="list-info"><b>Studio portrait</b><span>Edited 3d ago · 9s</span></div>'+ico('more','chev')+'</div>'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art food"></div></div><div class="list-info"><b>Late night ramen</b><span>Edited 4d ago · 31s</span></div>'+ico('more','chev')+'</div>'+
    '</div>'+
    '<div class="panel" style="margin-top:12px"><div class="row-between"><div><b style="font-size:9px">Local backup</b><span style="display:block;font-size:7px;color:var(--muted);margin-top:2px">Export all drafts as a zip</span></div><button class="btn sm" data-action="backup">Create</button></div></div>'+
    '<div class="panel" style="margin-top:9px">'+backendNote('Cloud sync, shared draft, dan komentar ditandai BUTUH BACKEND. Draft lokal tetap bisa di-backup ke folder pilihanmu.')+'</div>'+
  '</div>';
  return screenScroll(app+content);
};
