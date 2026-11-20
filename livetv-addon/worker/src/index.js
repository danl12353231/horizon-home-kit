// Nuvio Live TV — a Stremio-protocol addon for Nuvio (and Stremio) with live channels
// from every country and a matched TV guide.
//
// URL layout:
//   /configure                         configure page
//   /<config>/configure                configure page, pre-filled
//   /<config>/manifest.json            addon manifest for that configuration
//   /<config>/catalog/tv/<id>[/<extra>].json
//   /<config>/meta/tv/<id>.json
//   /<config>/stream/tv/<id>.json
//   /api/countries                     countries with channel and guide counts (for the page)
//
// <config> is base64url(JSON), see DEFAULTS.

import { configurePage } from "./page.js";

const DATA = "https://raw.githubusercontent.com/jyotiprakash101/nuvio-livetv/data/";
const VERSION = "1.0.0";
const PREFIX = "nltv";

const DEFAULTS = {
  cc: ["in"],          // countries
  cat: [],             // only these categories (empty = all)
  lang: [],            // only these languages (ISO 639-3, empty = all)
  nsfw: false,         // include adult channels
  guide: false,        // only channels that have a guide
  tz: "Asia/Kolkata",  // time zone for times and "today"
  name: "Live TV",     // catalog name
  shape: "landscape",  // landscape | poster | square
  art: "programme",    // programme | logo  (artwork for "On now")
  rows: ["now", "soon", "tonight", "channels"],
  catRows: [],         // extra "On now" rows per category, e.g. ["news","sports"]
  clean: true,         // tidy channel names
  days: 3,             // schedule days ahead
  maxStreams: 3,        // links per channel
  guideApp: false,      // add an "Open TV Guide" link that opens Horizon Home's guide
  working: false,       // only channels whose link passed the last test
  src: [],              // only these stream sources (empty = all)
};

const SOURCE_NAMES = {
  samsung: "Samsung TV Plus", pluto: "Pluto TV", roku: "Roku Channel", freetv: "Free-TV",
  validated: "Validated list", online: "Health-checked list", iptv: "iptv-org",
};

// ------------------------------------------------------------------ helpers

const json = (obj, maxAge = 300) =>
  new Response(JSON.stringify(obj), {
    headers: {
      "content-type": "application/json; charset=utf-8",
      "access-control-allow-origin": "*",
      "access-control-allow-headers": "*",
      "cache-control": `public, max-age=${maxAge}`,
    },
  });

function b64urlDecode(s) {
  s = s.replace(/-/g, "+").replace(/_/g, "/");
  while (s.length % 4) s += "=";
  const bin = atob(s);
  const bytes = Uint8Array.from(bin, (c) => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

function readConfig(seg) {
  try {
    const cfg = { ...DEFAULTS, ...JSON.parse(b64urlDecode(seg)) };
    cfg.cc = [].concat(cfg.cc || []).map((c) => String(c).toLowerCase()).filter((c) => /^[a-z]{2}$/.test(c)).slice(0, 20);
    if (!cfg.cc.length) cfg.cc = DEFAULTS.cc;
    cfg.days = Math.max(1, Math.min(4, Number(cfg.days) || 3));
    cfg.maxStreams = Math.max(1, Math.min(6, Number(cfg.maxStreams) || 3));
    try { new Intl.DateTimeFormat("en", { timeZone: cfg.tz }); } catch { cfg.tz = "UTC"; }
    return cfg;
  } catch {
    return null;
  }
}

// In-memory cache per isolate + the edge cache.
const mem = new Map();
async function data(path, ttl) {
  const hit = mem.get(path);
  const now = Date.now();
  if (hit && now - hit.at < ttl * 1000) return hit.value;
  const url = DATA + path;
  // one subrequest per file; the fetch itself is cached at the edge
  const res = await fetch(url, { cf: { cacheTtl: ttl, cacheEverything: true } }).catch(() => null);
  if (!res || !res.ok) return hit ? hit.value : null; // keep serving the last good copy
  const value = await res.json();
  mem.set(path, { at: now, value });
  if (mem.size > 300) mem.delete(mem.keys().next().value);
  return value;
}

const channelsOf = (cc) => data(`c/${cc}.json`, 3600);
const nowOf = (cc) => data(`now/${cc}.json`, 900);
const guideOf = (cc, id) => data(`g/${cc}/${encodeURIComponent(id)}.json`, 1800);
const countries = () => data("countries.json", 3600);
const categories = () => data("categories.json", 86400);

const CLEAN = /\s*(\((?:\d{3,4}p|geo-?blocked|not 24\/7)[^)]*\)|\[(?:geo-?blocked|not 24\/7)[^\]]*\])/gi;
const cleanName = (cfg, n) => (cfg.clean ? n.replace(CLEAN, "").replace(/\s{2,}/g, " ").trim() : n);

function fmtTime(cfg, ts) {
  return new Intl.DateTimeFormat("en-GB", { timeZone: cfg.tz, hour: "2-digit", minute: "2-digit" }).format(new Date(ts * 1000));
}
function dayKey(cfg, ts) {
  return new Intl.DateTimeFormat("en-CA", { timeZone: cfg.tz, year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date(ts * 1000));
}
function dayLabel(cfg, ts) {
  return new Intl.DateTimeFormat("en-GB", { timeZone: cfg.tz, weekday: "short", day: "numeric", month: "short" }).format(new Date(ts * 1000));
}
function localHour(cfg, ts) {
  return Number(new Intl.DateTimeFormat("en-GB", { timeZone: cfg.tz, hour: "2-digit", hourCycle: "h23" }).format(new Date(ts * 1000)));
}
const mins = (s) => Math.max(1, Math.round(s / 60));

// All channels for the configuration, filtered.
async function allChannels(cfg) {
  const lists = await Promise.all(cfg.cc.map(async (cc) => [cc, (await channelsOf(cc)) || []]));
  const out = [];
  const cats = new Set(cfg.cat);
  const langs = new Set(cfg.lang);
  const srcs = new Set(cfg.src || []);
  for (const [cc, list] of lists) {
    for (const ch of list) {
      if (ch.x && !cfg.nsfw) continue;
      if (cfg.guide && !ch.g) continue;
      if (cats.size && !(ch.c || []).some((c) => cats.has(c))) continue;
      if (langs.size && !(ch.la || []).some((l) => langs.has(l))) continue;
      if (cfg.working && !ch.v) continue;
      if (srcs.size && !(ch.o || ["iptv"]).some((o) => srcs.has(o))) continue;
      out.push({ ...ch, cc });
    }
  }
  return out;
}

async function nowMap(cfg) {
  const maps = await Promise.all(cfg.cc.map(async (cc) => [cc, (await nowOf(cc)) || { p: {} }]));
  const m = new Map();
  for (const [cc, d] of maps) for (const [id, progs] of Object.entries(d.p || {})) m.set(`${cc}:${id}`, progs);
  return m;
}

const metaId = (cc, id) => `${PREFIX}:${cc}:${id}`;
function parseId(id) {
  const parts = id.split(":");
  if (parts[0] !== PREFIX || parts.length < 3) return null;
  return { cc: parts[1], id: parts[2], season: parts[3], episode: parts[4] };
}

function current(progs, t) {
  if (!progs) return [null, null];
  let cur = null, next = null;
  for (const p of progs) {
    if (p.s <= t && p.e > t) cur = p;
    else if (p.s > t && !next) next = p;
  }
  return [cur, next];
}

// ------------------------------------------------------------------ manifest

async function manifest(cfg, cfgSeg, origin) {
  const cats = (await categories()) || {};
  const cnt = (await countries()) || {};
  const genreOptions = [
    ...Object.entries(cats).map(([, n]) => n),
    ...(cfg.cc.length > 1 ? cfg.cc.map((cc) => (cnt[cc] ? `${cnt[cc].flag} ${cnt[cc].name}` : cc.toUpperCase())) : []),
  ];
  const catalogs = [];
  const add = (id, name, extra = []) => catalogs.push({ type: "tv", id, name, extra });
  const genre = { name: "genre", options: genreOptions, isRequired: false };
  const skip = { name: "skip", isRequired: false };
  if (cfg.rows.includes("now")) add("nltv_now", `${cfg.name} · On now`, [genre, skip]);
  for (const c of cfg.catRows) add(`nltv_now_${c}`, `${cfg.name} · ${(cats[c] || c)} on now`, [skip]);
  if (cfg.rows.includes("soon")) add("nltv_soon", `${cfg.name} · Starting soon`, [genre, skip]);
  if (cfg.rows.includes("tonight")) add("nltv_tonight", `${cfg.name} · Tonight`, [genre, skip]);
  if (cfg.rows.includes("channels") || !catalogs.length)
    add("nltv_channels", `${cfg.name} · Channels`, [genre, { name: "search", isRequired: false }, skip]);
  // search across programmes too
  add("nltv_search", `${cfg.name} · Programmes`, [{ name: "search", isRequired: true }]);

  return {
    id: `community.nuvio.livetv.${cfgSeg.slice(0, 12)}`,
    version: VERSION,
    name: cfg.name,
    description: `Live channels from ${cfg.cc.map((c) => c.toUpperCase()).join(", ")} with a matched TV guide. Free public streams from iptv-org; listings from epg.pw.`,
    logo: `${origin}/logo.png`,
    types: ["tv"],
    resources: ["catalog", { name: "meta", types: ["tv", "series"], idPrefixes: [PREFIX + ":"] }, { name: "stream", types: ["tv", "series"], idPrefixes: [PREFIX + ":"] }],
    idPrefixes: [PREFIX + ":"],
    catalogs,
    behaviorHints: { configurable: true, configurationRequired: false },
  };
}

// ------------------------------------------------------------------ catalogs

function parseExtra(extraSeg) {
  const out = {};
  if (!extraSeg) return out;
  for (const kv of extraSeg.split("&")) {
    const [k, v = ""] = kv.split("=");
    out[decodeURIComponent(k)] = decodeURIComponent(v.replace(/\+/g, " "));
  }
  return out;
}

async function genreFilter(cfg, extra) {
  if (!extra.genre) return () => true;
  const cats = (await categories()) || {};
  const catId = Object.keys(cats).find((k) => cats[k] === extra.genre);
  if (catId) return (ch) => (ch.c || []).includes(catId);
  const cnt = (await countries()) || {};
  const cc = Object.keys(cnt).find((k) => `${cnt[k].flag} ${cnt[k].name}` === extra.genre);
  if (cc) return (ch) => ch.cc === cc;
  return () => true;
}

function programmeItem(cfg, ch, p, label) {
  const art = cfg.art === "programme" && p.i ? p.i : ch.l;
  return {
    id: metaId(ch.cc, ch.id),
    type: "tv",
    name: p.t,
    poster: art,
    posterShape: cfg.shape,
    logo: ch.l,
    background: p.i || undefined,
    description: `${label} · ${cleanName(cfg, ch.n)}`,
    releaseInfo: `${fmtTime(cfg, p.s)}–${fmtTime(cfg, p.e)}`,
    genres: p.c,
  };
}

function channelItem(cfg, ch, now) {
  const [cur, next] = current(now.get(`${ch.cc}:${ch.id}`), Date.now() / 1000);
  let desc = "";
  if (cur) desc = `Now: ${cur.t} (until ${fmtTime(cfg, cur.e)})`;
  if (next) desc += `${desc ? "\n" : ""}Next: ${next.t} at ${fmtTime(cfg, next.s)}`;
  return {
    id: metaId(ch.cc, ch.id),
    type: "tv",
    name: cleanName(cfg, ch.n),
    poster: ch.l,
    posterShape: cfg.shape === "poster" ? "square" : cfg.shape,
    logo: ch.l,
    description: desc || "Live channel",
    releaseInfo: cur ? `Now · ${cur.t}` : "Live",
  };
}

async function catalog(cfg, id, extra) {
  const skip = Number(extra.skip) || 0;
  const page = 100;
  const t = Date.now() / 1000;
  const [chans, now] = await Promise.all([allChannels(cfg), nowMap(cfg)]);
  const gf = await genreFilter(cfg, extra);
  let items = [];

  if (id === "nltv_channels") {
    const q = (extra.search || "").toLowerCase().trim();
    items = chans
      .filter(gf)
      .filter((ch) => !q || ch.n.toLowerCase().includes(q))
      .map((ch) => channelItem(cfg, ch, now));
  } else if (id === "nltv_search") {
    const q = (extra.search || "").toLowerCase().trim();
    if (!q) return { metas: [] };
    const seen = new Set();
    const hits = [];
    for (const ch of chans) {
      const progs = now.get(`${ch.cc}:${ch.id}`) || [];
      for (const p of progs) {
        if (p.e < t || !p.t.toLowerCase().includes(q)) continue;
        if (seen.has(ch.id)) break;
        seen.add(ch.id);
        hits.push([p, ch]);
        break;
      }
      if (!seen.has(ch.id) && ch.n.toLowerCase().includes(q)) hits.push([null, ch]);
    }
    hits.sort((a, b) => (a[0]?.s ?? 1e12) - (b[0]?.s ?? 1e12));
    items = hits.map(([p, ch]) =>
      p ? programmeItem(cfg, ch, p, p.s <= t ? `On now until ${fmtTime(cfg, p.e)}` : `Starts ${fmtTime(cfg, p.s)}`) : channelItem(cfg, ch, now));
  } else if (id === "nltv_now" || id.startsWith("nltv_now_")) {
    const only = id.startsWith("nltv_now_") ? id.slice(9) : null;
    for (const ch of chans) {
      if (!gf(ch) || (only && !(ch.c || []).includes(only))) continue;
      const [cur] = current(now.get(`${ch.cc}:${ch.id}`), t);
      if (cur) items.push([cur, ch]);
    }
    // programmes that just started first, then by time left
    items.sort((a, b) => (t - a[0].s) / (a[0].e - a[0].s) - (t - b[0].s) / (b[0].e - b[0].s));
    items = items.map(([p, ch]) => programmeItem(cfg, ch, p, `${mins(p.e - t)} min left`));
  } else if (id === "nltv_soon") {
    for (const ch of chans) {
      if (!gf(ch)) continue;
      const [, next] = current(now.get(`${ch.cc}:${ch.id}`), t);
      if (next && next.s - t < 3 * 3600) items.push([next, ch]);
    }
    items.sort((a, b) => a[0].s - b[0].s);
    items = items.map(([p, ch]) => programmeItem(cfg, ch, p, `Starts ${fmtTime(cfg, p.s)} · in ${mins(p.s - t)} min`));
  } else if (id === "nltv_tonight") {
    const today = dayKey(cfg, t);
    for (const ch of chans) {
      if (!gf(ch)) continue;
      for (const p of now.get(`${ch.cc}:${ch.id}`) || []) {
        if (p.e < t) continue;
        const h = localHour(cfg, p.s);
        if (dayKey(cfg, p.s) === today && h >= 19 && h <= 23) {
          items.push([p, ch]);
          break;
        }
      }
    }
    items.sort((a, b) => a[0].s - b[0].s);
    items = items.map(([p, ch]) => programmeItem(cfg, ch, p, `Tonight ${fmtTime(cfg, p.s)}`));
  }
  return { metas: items.slice(skip, skip + page) };
}

// ------------------------------------------------------------------ meta

async function meta(cfg, idStr) {
  const id = parseId(idStr);
  if (!id) return { meta: null };
  const list = (await channelsOf(id.cc)) || [];
  const ch = list.find((c) => c.id === id.id);
  if (!ch) return { meta: null };
  const t = Date.now() / 1000;
  const progs = ch.g ? (await guideOf(id.cc, ch.id)) || [] : [];
  const today = dayKey(cfg, t);
  const dayDiff = (dk) => Math.round((Date.parse(dk) - Date.parse(today)) / 86400000);
  const videos = [];
  let defaultVideoId;
  let cur = null, next = null;
  for (const p of progs) {
    if (p.e < t - 6 * 3600) continue; // keep the last few hours of today for browsing
    const dk = dayKey(cfg, p.s);
    if (dk < today) continue;
    const season = dayDiff(dk) + 1; // season 1 = today, 2 = tomorrow, …
    if (season > cfg.days) break;
    const episode = videos.filter((v) => v.season === season).length + 1;
    const live = p.s <= t && p.e > t;
    if (live) cur = p;
    else if (p.s > t && !next) next = p;
    const vid = `${metaId(id.cc, ch.id)}:${season}:${episode}`;
    if (live) defaultVideoId = vid;
    const when = `${dayLabel(cfg, p.s)} · ${fmtTime(cfg, p.s)}–${fmtTime(cfg, p.e)}`;
    videos.push({
      id: vid,
      title: `${live ? "● LIVE · " : ""}${fmtTime(cfg, p.s)} · ${p.t}${p.st ? ` — ${p.st}` : ""}`,
      season,
      episode,
      released: new Date(p.s * 1000).toISOString(),
      startTime: new Date(p.s * 1000).toISOString(),
      endTime: new Date(p.e * 1000).toISOString(),
      thumbnail: p.i || ch.l,
      overview: [when, p.c ? p.c.join(", ") : "", p.d || ""].filter(Boolean).join("\n"),
      runtime: `${mins(p.e - p.s)} min`,
      available: true,
    });
  }
  if (!videos.length) {
    videos.push({
      id: `${metaId(id.cc, ch.id)}:1:1`, title: "● LIVE", season: 1, episode: 1,
      released: new Date().toISOString(), thumbnail: ch.l, overview: "Guide not available for this channel.", available: true,
    });
    defaultVideoId = videos[0].id;
  }
  const cats = (await categories()) || {};
  let description = "";
  if (cur) description += `On now: ${cur.t} (${fmtTime(cfg, cur.s)}–${fmtTime(cfg, cur.e)})${cur.d ? `\n${cur.d}` : ""}`;
  if (next) description += `${description ? "\n\n" : ""}Next: ${next.t} at ${fmtTime(cfg, next.s)}`;
  if (!description) description = "Live channel. Pick a programme from the schedule or press Play to watch live.";
  return {
    meta: {
      id: idStr.split(":").slice(0, 3).join(":"),
      type: "tv",
      name: cleanName(cfg, ch.n),
      poster: ch.l,
      posterShape: "square",
      logo: ch.l,
      background: cur?.i || undefined,
      description,
      genres: (ch.c || []).map((c) => cats[c] || c),
      releaseInfo: cur ? "Live now" : "Live",
      language: (ch.la || []).join(", ") || undefined,
      country: id.cc.toUpperCase(),
      website: ch.w,
      videos,
      behaviorHints: { defaultVideoId, isLive: true, hasScheduledVideos: true },
    },
  };
}

// ------------------------------------------------------------------ streams

async function stream(cfg, idStr, cfgStr, origin) {
  const id = parseId(idStr);
  if (!id) return { streams: [] };
  const list = (await channelsOf(id.cc)) || [];
  const ch = list.find((c) => c.id === id.id);
  if (!ch) return { streams: [] };
  const t = Date.now() / 1000;
  let note = "";
  if (id.season && ch.g) {
    const now = await nowOf(id.cc);
    const [cur] = current(now?.p?.[ch.id], t);
    if (cur) note = `Now: ${cur.t}`;
  }
  const extra = cfg.guideApp
    ? [{
        externalUrl: `horizon://guide?cc=${id.cc}&ch=${encodeURIComponent(ch.id)}&src=${encodeURIComponent(`${origin}/${cfgStr}`)}`,
        name: "📺 TV GUIDE",
        description: "Open the full TV guide on Horizon Home",
      }]
    : [];
  return {
    streams: [...(ch.s || []).filter((s) => !(cfg.src || []).length || cfg.src.includes(s.o || "iptv")).slice(0, cfg.maxStreams).map((s, i) => {
      const headers = {};
      if (s.a) headers["User-Agent"] = s.a;
      if (s.r) headers["Referer"] = s.r;
      const hints = { notWebReady: true, bingeGroup: `nltv-${ch.id}` };
      if (Object.keys(headers).length) hints.proxyHeaders = { request: headers };
      return {
        url: s.u,
        name: `● LIVE${s.q ? `\n${s.q}` : ""}`,
        description: [cleanName(cfg, ch.n), note,
          `${SOURCE_NAMES[s.o] || "iptv-org"} · ${s.k ? "✓ tested working" : s.k === 0 ? "failed last test" : "not tested"}`,
          i ? `Backup link ${i}` : "Main link"].filter(Boolean).join("\n"),
        behaviorHints: hints,
      };
    }), ...extra],
  };
}

// ------------------------------------------------------------------ playlist for IPTV apps

const RAW = "https://raw.githubusercontent.com/jyotiprakash101/nuvio-livetv/data/";

async function playlist(cfg) {
  const chans = await allChannels(cfg);
  const cnt = (await countries()) || {};
  const tvg = cfg.cc.map((cc) => `${RAW}x/${cc}.xml.gz`).join(",");
  const lines = [`#EXTM3U url-tvg="${tvg}" x-tvg-url="${tvg}"`];
  for (const ch of chans) {
    const s = (ch.s || [])[0];
    if (!s) continue;
    const group = cfg.cc.length > 1 ? (cnt[ch.cc]?.name || ch.cc.toUpperCase()) : ((ch.c || [])[0] || "general");
    const name = cleanName(cfg, ch.n).replace(/,/g, " ");
    lines.push(`#EXTINF:-1 tvg-id="${ch.id}" tvg-name="${name}"${ch.l ? ` tvg-logo="${ch.l}"` : ""} group-title="${group}",${name}`);
    if (s.a) lines.push(`#EXTVLCOPT:http-user-agent=${s.a}`);
    if (s.r) lines.push(`#EXTVLCOPT:http-referrer=${s.r}`);
    lines.push(s.u);
  }
  return new Response(lines.join("\n") + "\n", {
    headers: { "content-type": "audio/x-mpegurl; charset=utf-8", "access-control-allow-origin": "*", "cache-control": "public, max-age=900" },
  });
}

// ------------------------------------------------------------------ router

export default {
  async fetch(request) {
    const url = new URL(request.url);
    const origin = url.origin;
    if (request.method === "OPTIONS") return json({}, 86400);
    const parts = url.pathname.split("/").filter(Boolean);

    try {
      if (!parts.length) return Response.redirect(`${origin}/configure`, 302);
      if (parts[0] === "configure") return configurePage(origin, null, DEFAULTS);
      if (parts[0] === "api" && parts[1] === "countries") return json((await countries()) || {}, 3600);
      if (parts[0] === "api" && parts[1] === "categories") return json((await categories()) || {}, 86400);
      if (parts[0] === "api" && parts[1] === "report") return json((await data("report.json", 900)) || {}, 900);
      if (parts[0] === "logo.png") return Response.redirect("https://raw.githubusercontent.com/jyotiprakash101/nuvio-livetv/main/worker/logo.png", 302);

      const cfg = readConfig(parts[0]);
      if (!cfg) return json({ error: "bad configuration" }, 60);
      const rest = parts.slice(1);
      if (rest[0] === "configure") return configurePage(origin, cfg, DEFAULTS);
      if (rest[0] === "manifest.json") return json(await manifest(cfg, parts[0], origin), 3600);

      const [resource, type, id, extraSeg] = rest;
      const strip = (s) => decodeURIComponent((s || "").replace(/\.json$/, ""));
      if (rest[0] === "playlist.m3u" || rest[0] === "playlist.m3u8") return playlist(cfg);
      if (rest[0] === "epg.xml.gz" && cfg.cc.length) return Response.redirect(`${RAW}x/${cfg.cc[0]}.xml.gz`, 302);
      if (rest[0] === "channels.json") return json(await allChannels(cfg), 600);
      if (rest[0] === "now.json") return json(Object.fromEntries(await nowMap(cfg)), 300);
      if (type !== "tv" && type !== "series") return json({ metas: [], streams: [] }, 3600);
      if (resource === "catalog") return json(await catalog(cfg, strip(id), parseExtra(strip(extraSeg))), 300);
      if (resource === "meta") return json(await meta(cfg, strip(id)), 600);
      if (resource === "stream") return json(await stream(cfg, strip(id), parts[0], origin), 600);
      return json({ error: "not found" }, 60);
    } catch (e) {
      return json({ error: String(e && e.message || e) }, 10);
    }
  },
};
