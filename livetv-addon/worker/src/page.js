// Configure page: pick countries and options, then install the addon in Nuvio / Stremio.

export function configurePage(origin, cfg, defaults) {
  const initial = JSON.stringify(cfg || defaults).replace(/</g, "\\u003c");
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Live TV for Nuvio · Configure</title>
<style>
  :root { --bg:#0e0f13; --card:#171920; --line:#262a35; --text:#eef0f4; --dim:#9aa1b2; --accent:#ffa45c; --ok:#4ade80; }
  * { box-sizing:border-box }
  body { margin:0; background:var(--bg); color:var(--text); font:15px/1.5 system-ui,-apple-system,Segoe UI,Roboto,sans-serif }
  .wrap { max-width:980px; margin:0 auto; padding:32px 20px 80px }
  h1 { font-size:28px; margin:0 0 4px; letter-spacing:-.02em }
  .sub { color:var(--dim); margin:0 0 28px }
  .card { background:var(--card); border:1px solid var(--line); border-radius:16px; padding:20px; margin-bottom:16px }
  .card h2 { font-size:15px; margin:0 0 4px; }
  .card p.h { color:var(--dim); margin:0 0 14px; font-size:13px }
  .grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(210px,1fr)); gap:6px; max-height:300px; overflow:auto; padding-right:4px }
  label.opt { display:flex; align-items:center; gap:8px; padding:7px 10px; border-radius:10px; cursor:pointer; border:1px solid transparent; font-size:14px }
  label.opt:hover { background:#1e2129 }
  label.opt.on { border-color:var(--accent); background:#241c14 }
  label.opt input { accent-color:var(--accent) }
  .n { margin-left:auto; color:var(--dim); font-size:12px; white-space:nowrap }
  input[type=text], select, input[type=number] { width:100%; background:#0f1116; color:var(--text); border:1px solid var(--line); border-radius:10px; padding:10px 12px; font:inherit }
  .row { display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:14px }
  .field span { display:block; font-size:13px; color:var(--dim); margin-bottom:6px }
  .chips { display:flex; flex-wrap:wrap; gap:8px }
  .chip { padding:7px 12px; border:1px solid var(--line); border-radius:999px; cursor:pointer; font-size:13px; user-select:none }
  .chip.on { background:var(--accent); color:#111; border-color:var(--accent); font-weight:600 }
  .toggles label { display:flex; gap:10px; align-items:flex-start; padding:8px 0; cursor:pointer }
  .toggles small { display:block; color:var(--dim) }
  .stats { display:flex; gap:12px; flex-wrap:wrap; margin-bottom:14px }
  .stat { flex:1; min-width:140px; background:#0f1116; border:1px solid var(--line); border-radius:12px; padding:12px 14px }
  .stat b { font-size:22px; display:block }
  .stat span { color:var(--dim); font-size:12px }
  .bar { height:6px; background:#262a35; border-radius:3px; overflow:hidden; margin-top:6px }
  .bar i { display:block; height:100%; background:var(--ok) }
  .actions { display:flex; gap:10px; flex-wrap:wrap; margin-top:6px }
  button, a.btn { background:var(--accent); color:#111; border:0; border-radius:12px; padding:12px 18px; font:600 15px system-ui; cursor:pointer; text-decoration:none }
  .ghost { background:transparent !important; color:var(--text) !important; border:1px solid var(--line) !important }
  code.url { display:block; word-break:break-all; background:#0f1116; border:1px solid var(--line); border-radius:10px; padding:10px 12px; font-size:12px; color:var(--dim); margin:12px 0 }
  .presets { display:flex; gap:8px; flex-wrap:wrap; margin-bottom:12px }
  .qr { display:flex; gap:16px; align-items:center; margin-top:12px }
  .qr img { width:120px; height:120px; border-radius:10px; background:#fff; padding:6px }
  .muted { color:var(--dim); font-size:13px }
  .search { margin-bottom:10px }
</style>
</head>
<body>
<div class="wrap">
  <h1>📺 Live TV for Nuvio</h1>
  <p class="sub">Free public live channels from every country, with a TV guide matched by name. Pick what you want, then install.</p>

  <div class="card">
    <div class="presets">
      <span class="chip" data-preset="mine">My country</span>
      <span class="chip" data-preset="news">News only</span>
      <span class="chip" data-preset="guide">Only channels with a guide</span>
      <span class="chip" data-preset="everything">Everything</span>
    </div>
    <div class="stats">
      <div class="stat"><b id="sCh">–</b><span>channels</span></div>
      <div class="stat"><b id="sG">–</b><span>with a TV guide</span><div class="bar"><i id="sBar" style="width:0"></i></div></div>
      <div class="stat"><b id="sC">–</b><span>countries selected</span></div>
    </div>
    <p class="muted" id="built"></p>
  </div>

  <div class="card">
    <h2>Countries</h2>
    <p class="h">Pick up to 20. Numbers show channels and how many have a guide.</p>
    <input type="text" class="search" id="cSearch" placeholder="Search countries…">
    <div class="grid" id="countries"></div>
  </div>

  <div class="card">
    <h2>Categories</h2>
    <p class="h">Leave all off to include every category.</p>
    <div class="chips" id="cats"></div>
  </div>

  <div class="card">
    <h2>Languages</h2>
    <p class="h">Leave all off to include every language.</p>
    <div class="chips" id="langs"></div>
  </div>

  <div class="card">
    <h2>Sources</h2>
    <p class="h">Where the links come from. Leave all off to use every source.</p>
    <div class="chips" id="srcs"></div>
  </div>

  <div class="card">
    <h2>Rows in Nuvio</h2>
    <p class="h">Each becomes a row you can place on Nuvio's home screen.</p>
    <div class="chips" id="rows"></div>
    <p class="h" style="margin-top:16px">Extra "on now" rows for categories:</p>
    <div class="chips" id="catRows"></div>
  </div>

  <div class="card">
    <h2>Display</h2>
    <div class="row">
      <label class="field"><span>Catalog name</span><input type="text" id="name"></label>
      <label class="field"><span>Your time zone</span><select id="tz"></select></label>
      <label class="field"><span>Card shape</span>
        <select id="shape"><option value="landscape">Landscape (16:9)</option><option value="poster">Poster</option><option value="square">Square</option></select></label>
      <label class="field"><span>"On now" artwork</span>
        <select id="art"><option value="programme">Programme picture (falls back to logo)</option><option value="logo">Channel logo</option></select></label>
      <label class="field"><span>Schedule days</span>
        <select id="days"><option>1</option><option>2</option><option selected>3</option><option>4</option></select></label>
      <label class="field"><span>Links per channel</span>
        <select id="maxStreams"><option>1</option><option>2</option><option selected>3</option><option>4</option><option>6</option></select></label>
    </div>
    <div class="toggles" style="margin-top:10px">
      <label><input type="checkbox" id="guide"><span>Only channels with a TV guide<small>Hides channels without listings.</small></span></label>
      <label><input type="checkbox" id="clean"><span>Tidy channel names<small>Removes tags like (1080p) and [Geo-blocked].</small></span></label>
      <label><input type="checkbox" id="working"><span>Only channels that work<small>Hides channels whose links failed the last test (tests run from the US, so some India-only channels may be hidden).</small></span></label>
      <label><input type="checkbox" id="guideApp"><span>Add "Open TV Guide" link<small>Opens the full grid guide in Horizon Home (Android TV launcher). Leave off on other devices.</small></span></label>
      <label><input type="checkbox" id="nsfw"><span>Include adult channels<small>Off by default.</small></span></label>
    </div>
  </div>

  <div class="card">
    <h2>Install</h2>
    <p class="h">In Nuvio: Settings → Media → Content &amp; Discovery → Addons → paste the link → Install Addon.</p>
    <code class="url" id="url"></code>
    <div class="actions">
      <button id="copy">Copy addon link</button>
      <a class="btn ghost" id="stremio" href="#">Open in Stremio / Nuvio</a>
    </div>
    <p class="h" style="margin-top:18px">For TiviMate, OTT Navigator, Televizo or any IPTV app — the same channels with the guide filled in:</p>
    <code class="url" id="m3u"></code>
    <div class="actions"><button class="ghost" id="copyM3u">Copy playlist link</button></div>
    <div class="qr"><img id="qr" alt="QR code of the addon link"><span class="muted">Scan with your phone to copy the link onto it.</span></div>
  </div>
  <p class="muted">Channels: public streams listed by <a style="color:var(--dim)" href="https://github.com/iptv-org/iptv">iptv-org</a>. Guide: <a style="color:var(--dim)" href="https://epg.pw">epg.pw</a>. This addon hosts no video. Source: <a style="color:var(--dim)" href="https://github.com/jyotiprakash101/nuvio-livetv">GitHub</a>.</p>
</div>
<script>
const ORIGIN = ${JSON.stringify(origin)};
const cfg = ${initial};
const LANGS = {hin:"Hindi",eng:"English",ben:"Bengali",tam:"Tamil",tel:"Telugu",mar:"Marathi",kan:"Kannada",mal:"Malayalam",guj:"Gujarati",pan:"Punjabi",urd:"Urdu",ori:"Odia",asm:"Assamese",spa:"Spanish",por:"Portuguese",fra:"French",deu:"German",ita:"Italian",ara:"Arabic",tur:"Turkish",rus:"Russian",zho:"Chinese",jpn:"Japanese",kor:"Korean",ind:"Indonesian",nep:"Nepali",sin:"Sinhala",tha:"Thai",vie:"Vietnamese",pol:"Polish",nld:"Dutch",fas:"Persian"};
const ROWS = {now:"On now", soon:"Starting soon", tonight:"Tonight (7–11 pm)", channels:"All channels"};
let C = {}, CATS = {};
const $ = (id) => document.getElementById(id);

function b64url(s){ return btoa(String.fromCharCode(...new TextEncoder().encode(s))).replace(/\\+/g,"-").replace(/\\//g,"_").replace(/=+$/,""); }

function chips(el, entries, list){
  el.innerHTML = "";
  for (const [id, label] of entries){
    const c = document.createElement("span");
    c.className = "chip" + (list.includes(id) ? " on" : "");
    c.textContent = label;
    c.onclick = () => { const i = list.indexOf(id); if (i<0) list.push(id); else list.splice(i,1); c.classList.toggle("on"); update(); };
    el.appendChild(c);
  }
}

function drawCountries(){
  const q = $("cSearch").value.toLowerCase();
  const el = $("countries"); el.innerHTML = "";
  const list = Object.entries(C).sort((a,b)=> (cfg.cc.includes(b[0]) - cfg.cc.includes(a[0])) || b[1].channels - a[1].channels);
  for (const [cc, c] of list){
    if (q && !c.name.toLowerCase().includes(q) && cc !== q) continue;
    const l = document.createElement("label");
    const on = cfg.cc.includes(cc);
    l.className = "opt" + (on ? " on" : "");
    l.innerHTML = '<input type="checkbox"'+(on?" checked":"")+'> '+c.flag+' '+c.name+'<span class="n">'+c.channels+' · '+c.guide+' guide</span>';
    l.querySelector("input").onchange = (e) => { if (e.target.checked && cfg.cc.length >= 20) { e.target.checked = false; alert("Up to 20 countries"); return; } if (e.target.checked) cfg.cc.push(cc); else cfg.cc = cfg.cc.filter(x=>x!==cc); l.classList.toggle("on", e.target.checked); update(); };
    el.appendChild(l);
  }
}

function update(){
  for (const k of ["name","tz","shape","art"]) cfg[k] = $(k).value;
  cfg.days = +$("days").value; cfg.maxStreams = +$("maxStreams").value;
  for (const k of ["guide","clean","nsfw","guideApp","working"]) cfg[k] = $(k).checked;
  let ch = 0, g = 0;
  for (const cc of cfg.cc){ if (C[cc]) { ch += C[cc].channels; g += C[cc].guide; } }
  $("sCh").textContent = ch.toLocaleString(); $("sG").textContent = g.toLocaleString();
  $("sBar").style.width = ch ? Math.round(100*g/ch)+"%" : "0";
  $("sC").textContent = cfg.cc.length;
  const url = ORIGIN + "/" + b64url(JSON.stringify(cfg)) + "/manifest.json";
  $("url").textContent = url;
  $("m3u").textContent = url.replace(/manifest\.json$/, "playlist.m3u");
  $("stremio").href = url.replace(/^https?:/, "stremio:");
  $("qr").src = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=" + encodeURIComponent(url);
}

function presets(){
  document.querySelectorAll("[data-preset]").forEach(p => p.onclick = () => {
    const k = p.dataset.preset;
    if (k === "news"){ cfg.cat = ["news"]; cfg.catRows = []; }
    if (k === "guide"){ $("guide").checked = true; }
    if (k === "everything"){ cfg.cat = []; cfg.lang = []; $("guide").checked = false; }
    if (k === "mine"){ guessCountry(); }
    chips($("cats"), Object.entries(CATS), cfg.cat); drawCountries(); update();
  });
}

function guessCountry(){
  const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || "";
  const map = {"Asia/Kolkata":"in","Asia/Calcutta":"in","Europe/London":"uk","America/New_York":"us","America/Chicago":"us","America/Los_Angeles":"us","Asia/Karachi":"pk","Asia/Dhaka":"bd","Asia/Kathmandu":"np","Asia/Colombo":"lk","Asia/Dubai":"ae","Europe/Paris":"fr","Europe/Berlin":"de","Europe/Madrid":"es","Europe/Rome":"it","America/Sao_Paulo":"br","America/Mexico_City":"mx","Asia/Tokyo":"jp","Asia/Seoul":"kr","Australia/Sydney":"au","America/Toronto":"ca"};
  const cc = map[tz]; if (cc) { cfg.cc = [cc]; }
}

async function init(){
  for (const k of ["name","shape","art"]) $(k).value = cfg[k];
  $("days").value = cfg.days; $("maxStreams").value = cfg.maxStreams;
  for (const k of ["guide","clean","nsfw","guideApp","working"]) $(k).checked = !!cfg[k];
  const zones = (Intl.supportedValuesOf ? Intl.supportedValuesOf("timeZone") : [cfg.tz]);
  const mine = Intl.DateTimeFormat().resolvedOptions().timeZone;
  if (!${JSON.stringify(!!cfg)} && mine) cfg.tz = mine;
  $("tz").innerHTML = zones.map(z=>'<option'+(z===cfg.tz?" selected":"")+'>'+z+'</option>').join("");
  for (const id of ["name","tz","shape","art","days","maxStreams","guide","clean","nsfw","guideApp","working"]) $(id).onchange = update;
  $("name").oninput = update;
  $("cSearch").oninput = drawCountries;
  $("copy").onclick = async () => { await navigator.clipboard.writeText($("url").textContent); $("copy").textContent = "Copied ✓"; setTimeout(()=>$("copy").textContent="Copy addon link",1500); };
  $("copyM3u").onclick = async () => { await navigator.clipboard.writeText($("m3u").textContent); $("copyM3u").textContent = "Copied ✓"; setTimeout(()=>$("copyM3u").textContent="Copy playlist link",1500); };
  chips($("rows"), Object.entries(ROWS), cfg.rows);
  chips($("langs"), Object.entries(LANGS), cfg.lang);
  cfg.src = cfg.src || [];
  chips($("srcs"), Object.entries({samsung:"Samsung TV Plus",pluto:"Pluto TV",roku:"Roku Channel",freetv:"Free-TV",validated:"Validated list",online:"Health-checked list",iptv:"iptv-org"}), cfg.src);
  presets();
  const [c, cats, rep] = await Promise.all([fetch("/api/countries").then(r=>r.json()), fetch("/api/categories").then(r=>r.json()), fetch("/api/report").then(r=>r.json()).catch(()=>({}))]);
  C = c; CATS = cats;
  chips($("cats"), Object.entries(cats), cfg.cat);
  chips($("catRows"), Object.entries(cats), cfg.catRows);
  if (rep.built) $("built").textContent = "Data updated " + new Date(rep.built*1000).toLocaleString() + " · " + rep.channels.toLocaleString() + " channels worldwide, " + rep.withGuide.toLocaleString() + " with a guide. Refreshed every 3 hours.";
  drawCountries(); update();
}
init();
</script>
</body>
</html>`;
  return new Response(html, { headers: { "content-type": "text/html; charset=utf-8", "cache-control": "no-cache" } });
}
