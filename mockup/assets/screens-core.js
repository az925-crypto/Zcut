/* Zcut mockup — start, library, core editor and color screens */
RENDER.splash = function(){
  return '<div class="screen-scroll" style="padding-bottom:0"><div class="splash">'+
    '<div class="splash-mark">Z</div><div class="splash-word">zcut<span>.</span></div>'+
    '<div class="splash-tag">CUT DIFFERENT.</div>'+
    '<div class="splash-foot">on-device studio · v0.1</div></div></div>';
};

RENDER.onboarding = function(){
  return screenScroll('<div class="onboarding">'+
    '<div class="ob-badge">'+ico('unlock')+' ALL FEATURES UNLOCKED</div>'+
    '<div class="ob-art"><div class="ob-frame"></div><div class="ob-word">MIDNIGHT</div><div class="ob-sub">IN TOKYO</div><div class="ob-live"><i></i> PREVIEW · 9:16</div></div>'+
    '<h1>Your edit.<br><span>Your rules.</span></h1>'+
    '<p>A local-first video studio with every tool open. No subscription, no credits, no watermark. Your media never leaves the device.</p>'+
    '<div class="ob-points">'+
      '<div class="ob-point"><span class="obp-ic">'+ico('wand')+'</span><span><b>Pro tools, zero gates.</b> Effects, AI, export — all included.</span></div>'+
      '<div class="ob-point"><span class="obp-ic">'+ico('shield')+'</span><span><b>On-device by default.</b> No account, no upload, no tracking.</span></div>'+
      '<div class="ob-point"><span class="obp-ic">'+ico('sparkles')+'</span><span><b>Clean by design.</b> No CapCut servers, no copied assets.</span></div>'+
    '</div>'+
    '<button class="primary-wide" data-go="home">'+ico('play')+' Start creating</button>'+
    '<button class="ob-skip" data-go="home">I already have a project</button>'+
  '</div>');
};

RENDER.home = function(){
  const app = appbar('Good evening, Azka',{noBack:true,sub:'What are we making today?',actions:
    '<button class="icon-btn" data-go="settings">'+ico('bell')+'</button>'+
    '<button class="icon-btn" data-go="profile" style="padding:0"><span class="avatar" style="width:28px;height:28px;border-radius:9px;font-size:10px">A</span></button>'});
  const content = '<div class="screen-pad">'+
    '<div class="searchbar" data-go="projects">'+ico('search')+'<span>Search projects, templates, tools</span><span class="kbd">⌘ K</span></div>'+
    '<div class="hero" style="margin-top:12px"><div class="hero-chip">LOCAL ENGINE</div><h2>Start a new cut.</h2><p>Import clips, shape the story, export clean. Everything stays on your device.</p><button class="btn primary" data-go="media-picker">'+ico('plus')+' New project</button></div>'+
    '<div class="quick-row" style="margin-top:9px">'+
      '<button class="quick" data-go="media-picker"><span class="qicon">'+ico('video')+'</span>Video</button>'+
      '<button class="quick" data-go="photo-editor"><span class="qicon coral">'+ico('image')+'</span>Photo</button>'+
      '<button class="quick" data-go="ai-generate"><span class="qicon cyan">'+ico('sparkles')+'</span>AI video</button>'+
      '<button class="quick" data-go="templates"><span class="qicon violet">'+ico('grid')+'</span>Templates</button>'+
    '</div>'+
    sectionHead('Recent drafts','See all','projects')+
    '<div class="h-scroll">'+
      mediaCard('tokyo','Midnight in Tokyo','Edited 2h ago','00:24','editor')+
      mediaCard('beach','Summer reel','Edited yesterday','00:18','editor')+
      mediaCard('portrait','Studio portrait','Edited 3d ago','00:09','editor')+
    '</div>'+
    sectionHead('Trending templates','Browse','templates')+
    '<div class="h-scroll">'+
      '<div style="flex:0 0 132px">'+tile('night','Neon Nights','6 slots · 12s','template-detail','LOCAL')+'</div>'+
      '<div style="flex:0 0 132px">'+tile('gold','Golden Hour','4 slots · 15s','template-detail','LOCAL')+'</div>'+
      '<div style="flex:0 0 132px">'+tile('pink','Soft Pop','8 slots · 20s','template-detail','LOCAL')+'</div>'+
    '</div>'+
    '<div class="panel" style="margin-top:14px;display:flex;gap:9px;align-items:center"><div class="fr-icon">'+ico('sparkles')+'</div><div class="fr-text"><b>Zcut Agent</b><span>Describe the edit. I will build the first cut.</span></div><button class="btn sm primary" data-go="ai-agent">Try</button></div>'+
  '</div>'+bottomNav('home');
  return screenScroll(app+content);
};

RENDER.projects = function(){
  const app = appbar('Projects',{noBack:true,sub:'12 local drafts · 3 exports',actions:'<button class="icon-btn" data-go="media-picker">'+ico('plus')+'</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="searchbar">'+ico('search')+'<span>Find a project</span></div>'+
    '<div class="chips" style="margin-top:10px"><button class="chip active">All</button><button class="chip">Drafts</button><button class="chip">Exports</button><button class="chip">Templates</button></div>'+
    '<div class="list" style="margin-top:12px">'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art tokyo"></div></div><div class="list-info"><b>Midnight in Tokyo</b><span>9:16 · 00:24 · 1080p</span></div><span class="badge lime">DRAFT</span></div>'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art beach"></div></div><div class="list-info"><b>Summer reel</b><span>9:16 · 00:18 · 1080p</span></div><span class="badge">DRAFT</span></div>'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art portrait"></div></div><div class="list-info"><b>Studio portrait</b><span>4:5 · 00:09 · 1440p</span></div><span class="badge cyan">EXPORT</span></div>'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art city"></div></div><div class="list-info"><b>City walk</b><span>16:9 · 01:12 · 1080p</span></div><span class="badge">DRAFT</span></div>'+
      '<div class="list-card" data-go="editor"><div class="list-thumb"><div class="art food"></div></div><div class="list-info"><b>Late night ramen</b><span>9:16 · 00:31 · 1080p</span></div><span class="badge">DRAFT</span></div>'+
    '</div>'+
    '<div class="panel" style="margin-top:12px;display:flex;align-items:center;gap:9px"><div class="fr-icon">'+ico('cloud')+'</div><div class="fr-text"><b>Local backup</b><span>Export all drafts as a zip. No cloud required.</span></div><button class="btn sm" data-action="backup">Create</button></div>'+
  '</div>'+bottomNav('projects');
  return screenScroll(app+content);
};

RENDER.templates = function(){
  const app = appbar('Templates',{noBack:true,sub:'Local packs · no marketplace lock',actions:'<button class="icon-btn" data-go="discover">'+ico('search')+'</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="chips"><button class="chip active">For you</button><button class="chip">Trending</button><button class="chip">Vlog</button><button class="chip">Photo</button><button class="chip">Business</button></div>'+
    '<div class="grid-2" style="margin-top:10px">'+
      tile('night','Neon Nights','6 slots · 12s','template-detail','LOCAL')+
      tile('gold','Golden Hour','4 slots · 15s','template-detail','LOCAL')+
      tile('pink','Soft Pop','8 slots · 20s','template-detail','LOCAL')+
      tile('green','Match Cut','5 slots · 10s','template-detail','LOCAL')+
      tile('blue','Clean Product','3 slots · 08s','template-detail','LOCAL')+
      tile('city','City Walk','7 slots · 18s','template-detail','LOCAL')+
    '</div>'+
    '<div class="panel" style="margin-top:12px">'+backendNote('Marketplace dan AI template tidak memanggil server asli. Template bawaan tetap lokal; publikasi komunitas ditandai BUTUH BACKEND.')+'</div>'+
  '</div>'+bottomNav('templates');
  return screenScroll(app+content);
};

RENDER['template-detail'] = function(){
  const app = appbar('Template',{back:'templates',actions:'<button class="icon-btn" data-action="favorite">'+ico('heart')+'</button>'});
  const content = '<div class="template-hero"><div class="art night"></div><div class="th-badge badge lime">LOCAL TEMPLATE</div><div class="th-overlay"><div><h2>Neon Nights</h2><p>6 media slots · 12 seconds · 9:16</p></div></div></div>'+
    '<div class="screen-pad">'+
      '<div class="template-meta"><span class="badge lime">NO WATERMARK</span><span class="badge">6 SLOTS</span><span class="badge">BEAT SYNC</span><span class="badge">CAPTIONS</span></div>'+
      '<p style="font-size:9px;color:var(--muted);line-height:1.6;margin:0 0 12px">A high-contrast night template with beat-synced cuts, glow titles, and a local caption style. Replace each slot with your own clips.</p>'+
      '<div class="panel"><h4>What is included</h4>'+
        featureRow('layers','6 media slots','Auto-trim + center crop','editor')+
        featureRow('captions','Caption style','Editable, local font','captions')+
        featureRow('music','Beat markers','Use your own audio','audio')+
      '</div>'+
      '<button class="primary-wide" style="margin-top:12px" data-go="editor">'+ico('play')+' Use template</button>'+
      '<div class="hint" style="margin-top:8px">Template ini dibuat untuk Zcut. Tidak ada aset CapCut yang dipakai.</div>'+
    '</div>';
  return screenScroll(app+content);
};

RENDER.discover = function(){
  const app = appbar('Discover',{back:'templates',sub:'Made locally, shared freely',actions:'<button class="icon-btn" data-go="search">'+ico('search')+'</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="discover-hero"><div class="art city"></div><div class="dh-overlay"><div><h2>Small city, big cut.</h2><p>@localcreator · 24s · made with Zcut</p></div></div></div>'+
    '<div class="chips" style="margin-top:10px"><button class="chip active">For you</button><button class="chip">Minimal</button><button class="chip">Cinematic</button><button class="chip">Tutorial</button></div>'+
    '<div class="discover-grid">'+
      '<div class="discover-card" data-go="template-detail"><div class="dc-art"><div class="art beach"></div><span class="thumb-label">TEMPLATE</span></div><div class="dc-meta"><b>Sunset loop</b><span>@maya · 18s</span></div></div>'+
      '<div class="discover-card" data-go="template-detail"><div class="dc-art"><div class="art portrait"></div><span class="thumb-label">TEMPLATE</span></div><div class="dc-meta"><b>Soft focus</b><span>@ren · 12s</span></div></div>'+
      '<div class="discover-card" data-go="template-detail"><div class="dc-art"><div class="art green"></div><span class="thumb-label">TEMPLATE</span></div><div class="dc-meta"><b>Match cut</b><span>@studio · 10s</span></div></div>'+
      '<div class="discover-card" data-go="template-detail"><div class="dc-art"><div class="art gold"></div><span class="thumb-label">TEMPLATE</span></div><div class="dc-meta"><b>Golden hour</b><span>@nina · 15s</span></div></div>'+
    '</div>'+
    '<div class="panel" style="margin-top:12px">'+backendNote('Feed ini adalah contoh lokal. Zcut tidak menghubungi server CapCut dan tidak mengirim data penggunaan.')+'</div>'+
  '</div>'+bottomNav('templates');
  return screenScroll(app+content);
};

RENDER['ai-studio'] = function(){
  const app = appbar('AI Studio',{noBack:true,sub:'Local tools first · backend only when needed',actions:'<span class="badge lime">NO CREDITS</span>'});
  const content = '<div class="screen-pad">'+
    '<div class="ai-hero"><h2>Make the impossible cut.</h2><p>Start with on-device helpers. Generative models are optional and never use CapCut servers.</p><button class="btn primary" data-go="ai-agent">'+ico('sparkles')+' Ask Zcut Agent</button></div>'+
    '<div class="ai-grid" style="margin-top:9px">'+
      '<div class="ai-card" data-go="ai-clipper"><span class="ai-ic">'+ico('scissors')+'</span><span class="badge lime">LOCAL</span><b>AI Clipper</b><span>Find short moments in long video.</span></div>'+
      '<div class="ai-card" data-go="ai-agent"><span class="ai-ic">'+ico('wand')+'</span><span class="badge lime">LOCAL</span><b>Agent</b><span>Describe an edit in plain language.</span></div>'+
      '<div class="ai-card" data-go="ai-generate"><span class="ai-ic">'+ico('image')+'</span><span class="badge coral">BACKEND</span><b>Generate</b><span>Text to image or video.</span></div>'+
      '<div class="ai-card" data-go="digital-human"><span class="ai-ic">'+ico('user')+'</span><span class="badge coral">BACKEND</span><b>Digital Human</b><span>Avatar presenter + lip sync.</span></div>'+
      '<div class="ai-card" data-go="ai-music"><span class="ai-ic">'+ico('music')+'</span><span class="badge lime">LOCAL*</span><b>AI Music</b><span>Procedural loops on device.</span></div>'+
      '<div class="ai-card" data-go="ad-maker"><span class="ai-ic">'+ico('chart')+'</span><span class="badge lime">LOCAL*</span><b>Ad Maker</b><span>Script to a first ad cut.</span></div>'+
    '</div>'+
    '<div class="panel" style="margin-top:12px">'+backendNote('Model generatif, voice clone, dan lip sync membutuhkan backend/model sendiri. Tool tetap terbuka; tidak ada paywall atau kredit.')+'</div>'+
    '<div class="mini-label">Recently used</div>'+
    '<div class="list-card" data-go="ai-clipper"><div class="list-thumb"><div class="art city"></div></div><div class="list-info"><b>Long vlog → 3 shorts</b><span>AI Clipper · local heuristic</span></div><span class="badge lime">DONE</span></div>'+
  '</div>'+bottomNav('ai-studio');
  return screenScroll(app+content);
};

RENDER.editor = function(){
  const tools = [
    ['trim','scissors','Edit'],['audio','music','Audio'],['text','text','Text'],['stickers','sticker','Stickers'],
    ['effects','wand','Effects'],['filters','filter','Filters'],['adjust','adjust','Adjust'],['mask','mask','Mask']
  ];
  const content = '<div class="editor-screen">'+
    '<div class="editor-top"><button class="icon-btn" data-go="home">'+ico('back')+'</button><div class="proj"><b>Midnight in Tokyo</b><span>1080 × 1920 · 30 fps · local draft</span></div><button class="icon-btn" data-action="undo">'+ico('undo')+'</button><button class="icon-btn" data-action="redo">'+ico('redo')+'</button><button class="export" data-go="export">'+ico('download')+' Export</button></div>'+
    '<div class="preview-box"><div class="preview-frame"><div class="art tokyo"></div><div class="frame-text">MIDNIGHT</div><div class="frame-sub">IN TOKYO</div><div class="frame-badge">9:16 · 24.0s</div><button class="play-badge" data-action="play">'+ico('play')+'</button><div class="preview-controls"><span class="pc-time">00:08.12</span><div class="pc-spacer"></div><button class="pc-btn" data-action="mute">'+ico('volume')+'</button><button class="pc-btn" data-action="fullscreen">'+ico('eye')+'</button></div></div></div>'+
    '<div class="editor-tools">'+tools.map(function(t){return '<button class="editor-tool" data-go="'+t[0]+'"><span class="et-icon">'+ico(t[1])+'</span><span>'+t[2]+'</span></button>';}).join('')+'</div>'+
    '<div class="timeline">'+
      '<div class="tl-ruler"><span>0s</span><span>2s</span><span>4s</span><span>6s</span><span>8s</span><span>10s</span><span>12s</span><span>14s</span><span>16s</span><span>18s</span><span>20s</span><span>24s</span></div>'+
      '<div class="tracks">'+
        '<div class="track"><div class="track-label">'+ico('video')+' V1</div><div class="track-lane"><div class="clip v1" data-go="trim">tokyo_01</div><div class="clip v2" data-go="trim">street_02</div><div class="clip v3" data-go="trim">neon_03</div></div></div>'+
        '<div class="track"><div class="track-label">'+ico('layers')+' OV</div><div class="track-lane"><div class="clip ov" data-go="stickers">glow title</div></div></div>'+
        '<div class="track"><div class="track-label">'+ico('text')+' T1</div><div class="track-lane"><div class="clip tx" data-go="text">MIDNIGHT</div></div></div>'+
        '<div class="track"><div class="track-label">'+ico('music')+' A1</div><div class="track-lane"><div class="clip audio" data-go="audio"></div></div></div>'+
      '</div><div class="playhead"></div></div>'+
    '<div class="tl-footer"><button data-go="trim">'+ico('scissors')+'Split</button><button data-action="delete">'+ico('trash')+'Delete</button><button data-action="duplicate">'+ico('copy')+'Duplicate</button><button data-go="audio">'+ico('volume')+'Volume</button><button data-go="speed">'+ico('speed')+'Speed</button></div>'+
  '</div>';
  return screenScroll(content, 'editor-scroll');
};

RENDER['media-picker'] = function(){
  const app = appbar('Add media',{back:'editor',sub:'3 selected · local workspace',actions:'<button class="accent-btn" data-action="import">'+ico('upload')+' Import</button>'});
  const content = '<div class="screen-pad">'+
    '<div class="segmented"><button class="active">Photos</button><button>Videos</button><button>Audio</button></div>'+
    '<div class="chips" style="margin-top:9px"><button class="chip active">Recent</button><button class="chip">Camera</button><button class="chip">Downloads</button><button class="chip">Favorites</button></div>'+
    '<div class="grid-3" style="margin-top:9px">'+
      '<div class="tile" data-action="select-media"><div class="tile-art"><div class="art tokyo"></div><span class="badge lime" style="position:absolute;right:4px;top:4px">1</span></div></div>'+
      '<div class="tile" data-action="select-media"><div class="tile-art"><div class="art beach"></div><span class="badge lime" style="position:absolute;right:4px;top:4px">2</span></div></div>'+
      '<div class="tile" data-action="select-media"><div class="tile-art"><div class="art portrait"></div><span class="badge lime" style="position:absolute;right:4px;top:4px">3</span></div></div>'+
      '<div class="tile" data-action="select-media"><div class="tile-art"><div class="art street"></div></div></div>'+
      '<div class="tile" data-action="select-media"><div class="tile-art"><div class="art food"></div></div></div>'+
      '<div class="tile" data-action="select-media"><div class="tile-art"><div class="art city"></div></div></div>'+
      '<div class="tile" data-action="select-media"><div class="tile-art"><div class="art green"></div></div></div>'+
      '<div class="tile" data-action="select-media"><div class="tile-art"><div class="art gold"></div></div></div>'+
      '<div class="tile" data-action="select-media"><div class="tile-art"><div class="art night"></div></div></div>'+
    '</div>'+
    '<button class="secondary-wide" style="margin-top:10px" data-action="browse">'+ico('folder')+' Browse files</button>'+
    '<div class="hint" style="margin-top:8px">Media disalin ke workspace privat aplikasi. Tidak ada upload.</div>'+
  '</div>'+
  '<div style="position:sticky;bottom:0;background:rgba(8,9,13,.94);border-top:1px solid var(--line);padding:10px 18px 14px;backdrop-filter:blur(14px)"><button class="primary-wide" data-go="editor">'+ico('plus')+' Add 3 items to timeline</button></div>';
  return screenScroll(app+content);
};

RENDER.trim = function(){
  const app = appbar('Trim',{back:'editor',sub:'tokyo_01 · 00:12.480',actions:'<button class="accent-btn" data-go="editor">Done</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:176px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative;margin-top:4px"><div class="art tokyo"></div><button class="play-badge" data-action="play">'+ico('play')+'</button><span class="thumb-label">SOURCE · 00:12.480</span></div>'+
    '<div class="trim-track"><div class="trim-selection"><div class="trim-handle left"></div><div class="trim-handle right"></div><span>00:02.140</span><span>00:10.620</span></div></div>'+
    '<div class="field-row"><div class="field"><span>START</span><b>00:02.140</b></div><div class="field"><span>END</span><b>00:10.620</b></div><div class="field"><span>DURATION</span><b>00:08.480</b></div></div>'+
    '<div class="grid-3" style="margin-top:10px"><button class="secondary-wide" data-action="split">'+ico('scissors')+' Split</button><button class="secondary-wide" data-action="duplicate">'+ico('copy')+' Duplicate</button><button class="secondary-wide" data-action="delete">'+ico('trash')+' Delete</button></div>'+
    '<div class="panel" style="margin-top:12px"><h4>Frame accurate</h4><p>Handle trim memakai source time range. Perubahan tidak menggeser clip lain sampai kamu memilih ripple.</p></div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.speed = function(){
  const app = appbar('Speed',{back:'editor',sub:'Retime clip · audio pitch aware',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:152px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art street"></div><span class="thumb-label">PREVIEW · 1.0×</span></div>'+
    '<div class="chips" style="margin-top:10px"><button class="chip">0.25×</button><button class="chip">0.5×</button><button class="chip">0.75×</button><button class="chip active">1.0×</button><button class="chip">1.5×</button><button class="chip">2×</button><button class="chip">4×</button><button class="chip">Reverse</button></div>'+
    '<div class="panel" style="margin-top:10px"><div class="row-between"><h4 style="margin:0">Speed curve</h4><span class="badge lime">CUSTOM</span></div>'+
      '<svg viewBox="0 0 300 88" style="width:100%;height:78px;margin-top:6px"><path d="M8 70H292M8 48H292M8 26H292" stroke="rgba(255,255,255,.08)" stroke-width="1"/><path d="M8 66C40 66 52 18 92 18S150 70 190 62 250 26 292 26" fill="none" stroke="#c9ff4a" stroke-width="2"/><circle cx="8" cy="66" r="3" fill="#c9ff4a"/><circle cx="92" cy="18" r="3" fill="#c9ff4a"/><circle cx="190" cy="62" r="3" fill="#c9ff4a"/><circle cx="292" cy="26" r="3" fill="#c9ff4a"/></svg>'+
      '<div class="row-between" style="font-size:7px;color:var(--muted-2)"><span>START</span><span>MID</span><span>END</span></div>'+
    '</div>'+
    slider('Smoothness','72%',72)+
    '<div class="panel"><div class="kv"><span>Pitch audio</span><b>Preserve</b></div><div class="kv"><span>Interpolation</span><b>Optical flow</b></div><div class="kv"><span>Slow-motion</span><b>On</b></div></div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.keyframe = function(){
  const app = appbar('Keyframes',{back:'editor',sub:'Overlay · glow title',actions:'<button class="accent-btn" data-action="add-keyframe">'+ico('plus')+' Add</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:168px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art night"></div><div class="bounding-box"></div><span class="thumb-label">POSITION · 42, 28</span></div>'+
    '<div class="keyframe-track"><div class="kf-diamond" style="left:12%"></div><div class="kf-diamond off" style="left:38%"></div><div class="kf-diamond" style="left:64%"></div><div class="kf-diamond off" style="left:88%"></div></div>'+
    '<div class="panel"><h4>Properties</h4>'+
      '<div class="kv"><span>Position</span><b>42, 28</b></div>'+
      '<div class="kv"><span>Scale</span><b>118%</b></div>'+
      '<div class="kv"><span>Rotation</span><b>-4.5°</b></div>'+
      '<div class="kv"><span>Opacity</span><b>86%</b></div>'+
    '</div>'+
    '<div class="mini-label">Easing</div><div class="chips"><button class="chip">Linear</button><button class="chip active">Ease out</button><button class="chip">Ease in</button><button class="chip">Bezier</button></div>'+
    '<div class="panel" style="margin-top:10px"><h4>Curve editor</h4><svg viewBox="0 0 280 70" style="width:100%;height:64px"><path d="M8 62H272M8 36H272M8 10H272" stroke="rgba(255,255,255,.08)"/><path d="M8 62C90 62 100 10 272 10" fill="none" stroke="#59d9ff" stroke-width="2"/><circle cx="8" cy="62" r="3" fill="#59d9ff"/><circle cx="272" cy="10" r="3" fill="#59d9ff"/></svg></div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.transition = function(){
  const app = appbar('Transitions',{back:'editor',sub:'Between tokyo_01 and street_02',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:160px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative;display:flex"><div class="art tokyo" style="flex:1;position:relative"></div><div class="art street" style="flex:1;position:relative"></div><div style="position:absolute;inset:0;display:grid;place-items:center;background:rgba(8,9,13,.25)"><span class="badge lime" style="font-size:8px">DISSOLVE · 0.8s</span></div></div>'+
    slider('Duration','0.8s',38)+
    '<div class="mini-label">Transition library</div>'+
    '<div class="grid-2">'+
      '<div class="tile" data-action="pick-transition"><div class="tile-art" style="height:62px"><div class="art tokyo"></div></div><div class="tile-meta"><b>Dissolve</b><span>0.8s · smooth</span></div></div>'+
      '<div class="tile" data-action="pick-transition"><div class="tile-art" style="height:62px"><div class="art night"></div></div><div class="tile-meta"><b>Fade</b><span>0.5s · black</span></div></div>'+
      '<div class="tile" data-action="pick-transition"><div class="tile-art" style="height:62px"><div class="art blue"></div></div><div class="tile-meta"><b>Slide</b><span>0.4s · left</span></div></div>'+
      '<div class="tile" data-action="pick-transition"><div class="tile-art" style="height:62px"><div class="art pink"></div></div><div class="tile-meta"><b>Zoom</b><span>0.6s · punch</span></div></div>'+
      '<div class="tile" data-action="pick-transition"><div class="tile-art" style="height:62px"><div class="art gold"></div></div><div class="tile-meta"><b>Whip</b><span>0.3s · fast</span></div></div>'+
      '<div class="tile" data-action="pick-transition"><div class="tile-art" style="height:62px"><div class="art green"></div></div><div class="tile-meta"><b>Flash</b><span>0.2s · white</span></div></div>'+
    '</div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.filters = function(){
  const app = appbar('Filters',{back:'editor',sub:'Local LUT · real-time preview',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const filters = [
    ['Original','none'],['Tokyo','contrast(1.2) saturate(1.25) hue-rotate(-8deg)'],['Film','sepia(.28) contrast(1.15) saturate(.85)'],
    ['Mono','grayscale(1) contrast(1.1)'],['Warm','sepia(.18) saturate(1.3) brightness(1.05)'],['Cool','hue-rotate(18deg) saturate(1.1)'],
    ['Neon','saturate(1.8) contrast(1.3) hue-rotate(-18deg)'],['Fade','contrast(.85) brightness(1.12) saturate(.8)']
  ];
  const content = '<div class="screen-pad">'+
    '<div style="height:184px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art tokyo" style="filter:contrast(1.2) saturate(1.25) hue-rotate(-8deg)"></div><span class="thumb-label">TOKYO · 100%</span></div>'+
    slider('Intensity','100%',100)+
    '<div class="filter-grid" style="margin-top:4px">'+filters.map(function(f,i){
      return '<div class="filter-item '+(i===1?'active':'')+'" data-action="pick-filter"><div class="filter-thumb"><div class="art '+(i===0?'tokyo':i===1?'tokyo':i===2?'street':i===3?'night':i===4?'beach':i===5?'blue':i===6?'pink':'green')+'" style="filter:'+f[1]+'"></div></div><span>'+f[0]+'</span></div>';
    }).join('')+'</div>'+
    '<div class="panel" style="margin-top:12px"><h4>Looks are local</h4><p>Filter memakai LUT/shader milik Zcut. Tidak ada effect package CapCut yang disalin.</p></div>'+
  '</div>';
  return screenScroll(app+content);
};

RENDER.adjust = function(){
  const app = appbar('Adjust',{back:'editor',sub:'Color workspace · local',actions:'<button class="accent-btn" data-go="editor">Apply</button>'});
  const content = '<div class="screen-pad">'+
    '<div style="height:168px;border-radius:14px;overflow:hidden;border:1px solid var(--line);position:relative"><div class="art beach"></div><span class="thumb-label">COLOR · REC.709</span></div>'+
    '<div class="slider-stack" style="margin-top:10px">'+
      slider('Brightness','+6',56)+slider('Contrast','+12',62,'coral')+slider('Saturation','+8',58,'cyan')+
      slider('Temperature','+3',54)+slider('Tint','-2',47,'coral')+slider('Highlights','-14',38,'cyan')+
      slider('Shadows','+9',59)+slider('Vignette','12',24,'coral')+
    '</div>'+
    '<div class="grid-2"><button class="secondary-wide" data-action="reset">'+ico('refresh')+' Reset</button><button class="secondary-wide" data-go="filters">'+ico('filter')+' Filters</button></div>'+
    '<div class="panel" style="margin-top:10px"><h4>Advanced</h4><div class="kv"><span>HSL</span><b>8 bands</b></div><div class="kv"><span>Curves</span><b>RGB + luma</b></div><div class="kv"><span>Color wheels</span><b>Lift / gamma / gain</b></div></div>'+
  '</div>';
  return screenScroll(app+content);
};
