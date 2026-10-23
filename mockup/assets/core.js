/* Zcut mockup — shared icons and phone components */
const ICONS = {
  home:'<path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/><path d="M9.5 21v-6h5v6"/>',
  grid:'<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  sparkles:'<path d="m12 3 1.7 4.3L18 9l-4.3 1.7L12 15l-1.7-4.3L6 9l4.3-1.7L12 3Z"/><path d="m19 14 .8 2.2L22 17l-2.2.8L19 20l-.8-2.2L16 17l2.2-.8L19 14Z"/>',
  folder:'<path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H10l2 2h6.5A2.5 2.5 0 0 1 21 9.5v8A2.5 2.5 0 0 1 18.5 20h-13A2.5 2.5 0 0 1 3 17.5v-10Z"/>',
  user:'<circle cx="12" cy="8" r="4"/><path d="M4.5 21a7.5 7.5 0 0 1 15 0"/>',
  play:'<path d="m9 6 9 6-9 6V6Z" fill="currentColor" stroke="none"/>',
  pause:'<path d="M8 6v12M16 6v12"/>',
  undo:'<path d="M9 8 4 12l5 4"/><path d="M4 12h10a6 6 0 0 1 0 12h-1"/>',
  redo:'<path d="m15 8 5 4-5 4"/><path d="M20 12H10a6 6 0 0 0 0 12h1"/>',
  download:'<path d="M12 3v12"/><path d="m7 10 5 5 5-5"/><path d="M4 20h16"/>',
  back:'<path d="m15 18-6-6 6-6"/>',
  forward:'<path d="m9 18 6-6-6-6"/>',
  more:'<circle cx="5" cy="12" r="1.2" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.2" fill="currentColor" stroke="none"/><circle cx="19" cy="12" r="1.2" fill="currentColor" stroke="none"/>',
  search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  bell:'<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/>',
  video:'<rect x="3" y="5" width="13" height="14" rx="2"/><path d="m16 10 5-3v10l-5-3"/>',
  image:'<rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/>',
  music:'<path d="M9 18V5l11-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="17" cy="16" r="3"/>',
  mic:'<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0"/><path d="M12 18v3M8 21h8"/>',
  text:'<path d="M5 5h14M12 5v14M8 19h8"/>',
  sticker:'<path d="M21 12a9 9 0 1 1-9-9h3v3a6 6 0 0 0 6 6v3Z"/><path d="M15 3v3a3 3 0 0 0 3 3h3"/>',
  wand:'<path d="m4 20 12-12"/><path d="m14 4 1 2 2 1-2 1-1 2-1-2-2-1 2-1 1-2Z"/><path d="m19 13 .7 1.3L21 15l-1.3.7L19 17l-.7-1.3L17 15l1.3-.7L19 13Z"/>',
  layers:'<path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="m3 12 9 5 9-5"/><path d="m3 16 9 5 9-5"/>',
  scissors:'<circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M20 4 8.5 15.5M8.5 8.5 20 20"/>',
  trash:'<path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14M10 11v6M14 11v6"/>',
  copy:'<rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/>',
  volume:'<path d="M11 5 6 9H3v6h3l5 4V5Z"/><path d="M15 9a4 4 0 0 1 0 6M18 6a8 8 0 0 1 0 12"/>',
  speed:'<path d="M4 18a8 8 0 1 1 16 0"/><path d="m12 14 4-4"/><path d="M4 18h16"/>',
  keyframe:'<path d="m12 3 4 4-4 4-4-4 4-4ZM12 13l4 4-4 4-4-4 4-4Z"/>',
  transition:'<path d="M4 5h7v14H4zM13 5h7v14h-7z"/><path d="m9 9 3 3-3 3M15 15l-3-3 3-3"/>',
  filter:'<circle cx="9" cy="9" r="5"/><circle cx="15" cy="15" r="5"/><path d="M9 4v10M4 9h10M15 10v10M10 15h10"/>',
  adjust:'<path d="M4 7h16M4 12h16M4 17h16"/><circle cx="8" cy="7" r="2" fill="var(--bg)"/><circle cx="16" cy="12" r="2" fill="var(--bg)"/><circle cx="10" cy="17" r="2" fill="var(--bg)"/>',
  mask:'<path d="M4 5h16v14H4z"/><circle cx="12" cy="12" r="4"/>',
  cutout:'<path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4z"/><path d="M14 14h6v6h-6z" stroke-dasharray="2 2"/>',
  beauty:'<circle cx="12" cy="9" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/><path d="m18 4 .6 1.4L20 6l-1.4.6L18 8l-.6-1.4L16 6l1.4-.6L18 4Z"/>',
  captions:'<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M7 11h4M13 11h4M7 15h6M15 15h2"/>',
  cloud:'<path d="M7 18h10a4 4 0 0 0 .7-7.9A6 6 0 0 0 6.1 9.2 4.5 4.5 0 0 0 7 18Z"/>',
  check:'<path d="m5 12 4 4L19 6"/>',
  x:'<path d="m6 6 12 12M18 6 6 18"/>',
  arrow:'<path d="M5 12h14M13 6l6 6-6 6"/>',
  share:'<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 10.5 6.8-4M8.6 13.5l6.8 4"/>',
  settings:'<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4v-.2a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.6-1H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-1.6v-.2h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z"/>',
  plus:'<path d="M12 5v14M5 12h14"/>',
  camera:'<path d="M4 7h4l2-3h4l2 3h4v13H4V7Z"/><circle cx="12" cy="13" r="4"/>',
  flash:'<path d="m13 2-8 12h6l-1 8 9-13h-6l1-7Z"/>',
  refresh:'<path d="M20 11a8 8 0 1 0-2.3 6.7"/><path d="M20 4v7h-7"/>',
  crop:'<path d="M6 3v15a2 2 0 0 0 2 2h13"/><path d="M3 6h15a2 2 0 0 1 2 2v13"/>',
  brush:'<path d="M14 4 20 10 11 19l-7 2 2-7L14 4Z"/><path d="m13 5 6 6"/>',
  globe:'<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 4 6 4 9s-1 6-4 9c-3-3-4-6-4-9s1-6 4-9Z"/>',
  shield:'<path d="M12 3 20 6v6c0 5-4 8-8 9-4-1-8-4-8-9V6l8-3Z"/><path d="m9 12 2 2 4-4"/>',
  info:'<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>',
  upload:'<path d="M12 20V8"/><path d="m7 13 5-5 5 5"/><path d="M4 4h16"/>',
  timer:'<circle cx="12" cy="13" r="8"/><path d="M12 9v4l3 2M9 2h6"/>',
  gallery:'<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8" cy="9" r="1.5"/><path d="m21 15-5-5L5 20"/>',
  lock:'<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
  unlock:'<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 7.5-2"/>',
  chart:'<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
  eye:'<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>',
  heart:'<path d="M20.8 5.6a5.5 5.5 0 0 0-7.8 0L12 6.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 22l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z"/>',
  star:'<path d="m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1L3.2 9.4l6.1-.9L12 3Z"/>'
};
function ico(name, cls){
  const p = ICONS[name] || ICONS.info;
  return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="'+(cls||'')+'">'+p+'</svg>';
}
function statusbar(){
  return '<div class="statusbar"><span>9:41</span><div class="sicons">'+
    '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 18h2v-5H4v5Zm4 0h2V9H8v9Zm4 0h2V6h-2v12Zm4 0h2V3h-2v15Z"/></svg>'+
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9a14 14 0 0 1 18 0M6 12a9.5 9.5 0 0 1 12 0M9.5 15a5 5 0 0 1 5 0"/><circle cx="12" cy="19" r="1" fill="currentColor" stroke="none"/></svg>'+
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="7" width="16" height="10" rx="2"/><path d="M21 10v4"/><rect x="5" y="9" width="11" height="6" rx="1" fill="currentColor" stroke="none"/></svg>'+
  '</div></div>';
}
function appbar(title, opts){
  opts = opts || {};
  const back = opts.back || 'home';
  const actions = opts.actions || '';
  const sub = opts.sub ? '<span class="ab-sub">'+opts.sub+'</span>' : '';
  return '<div class="appbar">'+
    (opts.noBack ? '' : '<button class="icon-btn" data-go="'+back+'" aria-label="Back">'+ico('back')+'</button>')+
    '<div class="ab-title">'+title+sub+'</div>'+actions+'</div>';
}
function bottomNav(active){
  const items = [
    ['home','Home','home'],['templates','Templates','grid'],['ai-studio','AI','sparkles'],['projects','Projects','folder'],['profile','Profile','user']
  ];
  return '<div class="bottomnav">'+items.map(function(it){
    return '<button class="bn-item '+(active===it[0]?'active':'')+'" data-go="'+it[0]+'">'+ico(it[2])+'<span>'+it[1]+'</span></button>';
  }).join('')+'</div>';
}
function art(kind){ return '<div class="art '+(kind||'tokyo')+'"></div>'; }
function screenScroll(html, cls){ return '<div class="screen-scroll '+(cls||'')+'">'+html+'</div>'; }
function sectionHead(title, actionLabel, go){
  return '<div class="section-head"><h3>'+title+'</h3>'+(actionLabel?'<button data-go="'+(go||'projects')+'">'+actionLabel+' '+ico('forward')+'</button>':'')+'</div>';
}
function slider(label, value, pct, color){
  return '<div class="slider-row '+(color||'')+'"><div class="slider-top"><span>'+label+'</span><b>'+value+'</b></div><div class="slider-track"><div class="slider-fill" style="width:'+pct+'%"></div></div></div>';
}
function mediaCard(kind, title, meta, dur, go){
  return '<div class="project-card" data-go="'+(go||'editor')+'"><div class="project-thumb"><div class="art '+kind+'"></div><span class="dur">'+dur+'</span><span class="more">'+ico('more')+'</span></div><div class="project-meta"><b>'+title+'</b><span>'+meta+'</span></div></div>';
}
function tile(kind, title, meta, go, badge){
  return '<div class="tile" data-go="'+(go||'template-detail')+'"><div class="tile-art"><div class="art '+kind+'"></div>'+(badge?'<span class="pro">'+badge+'</span>':'')+'</div><div class="tile-meta"><b>'+title+'</b><span>'+meta+'</span></div></div>';
}
function waveBars(n, cls){
  let out = '<div class="wave '+(cls||'')+'">';
  for(let i=0;i<n;i++){
    const h = 10 + Math.abs(Math.sin(i*1.7))*34 + (i%5===0?8:0);
    out += '<i style="height:'+h.toFixed(0)+'px"></i>';
  }
  return out+'</div>';
}
function backendNote(text){
  return '<div class="backend-note">'+ico('info')+'<span>'+text+'</span></div>';
}
function kv(label, value){ return '<div class="kv"><span>'+label+'</span><b>'+value+'</b></div>'; }
function featureRow(icon, title, sub, go, badge){
  return '<div class="feature-row" data-go="'+(go||'settings')+'"><div class="fr-icon">'+ico(icon)+'</div><div class="fr-text"><b>'+title+'</b><span>'+sub+'</span></div>'+(badge?'<span class="badge '+(badge.cls||'')+'">'+badge.text+'</span>':'')+ico('forward','chev')+'</div>';
}
