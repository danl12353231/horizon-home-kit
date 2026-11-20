package com.horizon.home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The 22 streaming-service folders from the user's Nuvio Personalized setup (same order, same tile
 * art, title logos and backdrops). Titles come from TMDB (same lists and provider filters Nuvio uses)
 * when a TMDB key is set, otherwise from the public Streaming Catalogs addon.
 */
final class Streaming {
    private static final String TAG = "HorizonStreaming";
    static final String CATALOG = "https://7a82163c306e-stremio-netflix-catalog-addon.baby-beamup.club";
    private static final String K = "https://imkaptain.github.io/nuvio-assets/assets/images/";
    private static final String P = "https://raw.githubusercontent.com/bramst0ne/prism-wallpapers/main/collections/";
    private static final String A = "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/";
    private static final String Q = "https://raw.githubusercontent.com/ImKaptain/nuvio-assets/main/collections/";
    private static final String BD = "/backdrops/t2_flat_1080p.jpg";

    static final class Service {
        final String key, name, catalog, cover, logo, hero, providers, region;
        final long tmdbList;
        final int color;
        Service(String key, String name, String catalog, int color, String cover, String logo, String hero,
                String providers, String region, long tmdbList) {
            this.key = key; this.name = name; this.catalog = catalog; this.color = color;
            this.cover = cover; this.logo = logo; this.hero = hero;
            this.providers = providers; this.region = region; this.tmdbList = tmdbList;
        }
        boolean hasCatalog() { return catalog != null; }
    }

    static final class Row {
        final String title;
        final List<Nuvio.Item> items;
        Row(String title, List<Nuvio.Item> items) { this.title = title; this.items = items; }
    }

    static final Service[] ALL = {
            new Service("netflix", "Netflix", "nfx", 0xFFE50914, K + "49571953.webp", P + "networks/213-netflix/logos/color/213_color_1.png", P + "networks/213-netflix" + BD, "8|1796", "US", 8679739),
            new Service("appletv", "Apple TV+", "atp", 0xFF9A9AA2, K + "4d8f0a8f.webp", P + "networks/2552-apple-tv/logos/color/2552_color_1.png", P + "networks/2552-apple-tv" + BD, "350", "US", 8679741),
            new Service("hulu", "Hulu", "hlu", 0xFF1CE783, K + "3a4d1b46.webp", P + "networks/453-hulu/logos/color/453_color_1.png", P + "providers/15-hulu" + BD, "15", "US", 8679743),
            new Service("prime", "Prime Video", "amp", 0xFF1A98FF, K + "5c86c888.webp", K + "f9b1d40a.webp", P + "providers/9-amazon-prime-video" + BD, "9|119|2100", "US", 8679742),
            new Service("amc", "AMC+", null, 0xFF16B3A6, A + "amcplus/amcplus-cover.png", Q + "providers/526-amc/logos/color/4661_amc_1.png", Q + "providers/526-amc" + BD, "526", "US", 8679755),
            new Service("crave", "Crave", null, 0xFF1E8CFF, A + "crave/crave-cover.png", null, A + "crave/crave-backdrop.jpg", "230", "CA", 0),
            new Service("criterion", "Criterion Channel", "crc", 0xFF8A8A8A, A + "criterion-channel/criterion-channel-cover.png", P + "providers/258-criterion-channel/logos/white/258_white_1.png", P + "providers/258-criterion-channel" + BD, "258", "US", 0),
            new Service("crunchyroll", "Crunchyroll", "cru", 0xFFF47521, K + "129a4038.webp", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/de/Crunchyroll_logo_2024.png/960px-Crunchyroll_logo_2024.png", P + "providers/283-crunchyroll" + BD, "283", "US", 8679744),
            new Service("curiosity", "Curiosity Stream", "cts", 0xFFF2B33D, A + "curiosity-stream/curiosity-stream-cover.png", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/37/CuriosityStream.svg/960px-CuriosityStream.svg.png", K + "dce3256c.webp", "190", "US", 0),
            new Service("discovery", "Discovery+", "dpe", 0xFF2175D9, K + "dbed63f2.webp", K + "4676e4ff.webp", K + "8815e841.webp", "520", "US", 8679745),
            new Service("disney", "Disney+", "dnp", 0xFF1A3DBE, K + "b8c0d25b.webp", P + "networks/2739-disney/logos/color/2739_color_4.png", P + "providers/337-disney-plus" + BD, "337", "US", 8679746),
            new Service("hayu", "Hayu", "hay", 0xFFFF2D6F, A + "hayu/hayu-cover.png", null, A + "hayu/hayu-backdrop.jpg", "223", "CA", 0),
            new Service("max", "HBO Max", "hbm", 0xFF2B3DF5, K + "b59babf1.webp", K + "6e4c10d3.webp", P + "providers/1899-hbo-max" + BD, "1899|1825", "US", 8679740),
            new Service("magellan", "MagellanTV", "mgl", 0xFF35C2E8, A + "magellan/magellan-cover.png", null, A + "magellan/magellan-backdrop.jpg", "551", "US", 0),
            new Service("mgm", "MGM+", null, 0xFFC8A55A, K + "d490e713.webp", P + "networks/6219-mgm/logos/color/6219_color_1.png", P + "providers/34-mgm-plus" + BD, "34", "US", 8679747),
            new Service("mubi", "MUBI", "mbi", 0xFF1032D8, K + "7c4bd708.webp", P + "companies/204957-mubi/logos/white/204957_white_2.png", P + "companies/204957-mubi" + BD, "11", "US", 8679748),
            new Service("paramount", "Paramount+", "pmp", 0xFF0064FF, A + "paramount-plus/paramount-plus-cover.png", P + "networks/4330-paramount/logos/color/4330_color_2.png", P + "providers/2303-paramount-plus-premium" + BD, "2303", "US", 8679749),
            new Service("peacock", "Peacock", "pcp", 0xFFE8E8E8, K + "970b675f.webp", P + "networks/3353-peacock/logos/color/3353_color_1.png", P + "providers/386-peacock-premium" + BD, "387", "US", 8679750),
            new Service("pluto", "Pluto TV", null, 0xFFFF3EA5, A + "pluto-tv/pluto-tv-cover.png", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c3/Pluto_TV_logo_2024.svg/500px-Pluto_TV_logo_2024.svg.png", Q + "providers/300-pluto-tv" + BD, "300", "US", 8679754),
            new Service("shudder", "Shudder", "shd", 0xFFE01C1C, K + "99e24ce1.webp", P + "companies/142877-shudder/logos/color/142877_color_1.png", P + "providers/99-shudder" + BD, "99", "US", 8679751),
            new Service("starz", "Starz", "stz", 0xFF8BE04F, K + "9e92f9b6.webp", K + "262762fd.webp", P + "providers/43-starz" + BD, "43|318", "US", 8679752),
            new Service("tubi", "Tubi", null, 0xFF7A00F5, A + "tubi/tubi-cover.png", Q + "companies/153734-tubi/logos/color/153734_color_1.png", Q + "companies/153734-tubi" + BD, "73", "US", 8679753),
    };

    /** Focus animations from the Nuvio Personalized folders. */
    private static final String[][] GIFS = {
            {"netflix", "https://64.media.tumblr.com/9f93a9fc2e02fb466eb02a7d2247cb6e/a5b604d3737fc559-49/s500x750/403804744183922f6091d48139021fc3da22f786.gifv"},
            {"appletv", "https://64.media.tumblr.com/d717319220a7d26bdaa88e72f6f76889/d9a7a808f588d8f4-63/s500x750/959b0ca57f53153b2ca9adaf414859e45e3734e6.gifv"},
            {"hulu", "https://64.media.tumblr.com/3a7015de27b570e421a0182a66d0a69b/0ec3a7379449b39d-2d/s1280x1920/962f0dcfa0a2458bd6bf923ff47776f2404bf43a.gifv"},
            {"prime", "https://64.media.tumblr.com/5c5ed8bf948c5b3ca63a11544b94c720/55dbe5d6db4b66a5-bf/s500x750/d6686beaab57aa152f1b92a4562f757a1c522a2f.gifv"},
            {"amc", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/amcplus/amcplus-hover.gif"},
            {"crave", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/crave/crave-hover.gif"},
            {"criterion", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/criterion-channel/criterion-channel-hover.gif"},
            {"crunchyroll", "https://cdn.dribbble.com/userupload/42119533/file/original-3c24d9649543096949cc4262451501b7.gif"},
            {"curiosity", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/curiosity-stream/curiosity-stream-hover.gif"},
            {"discovery", "https://max-streams.gleeze.com/images/Discovery.gif"},
            {"disney", "https://64.media.tumblr.com/ca6dc6d4e8a260c0c5f40c47c7334e57/eb71329b4dc482d5-50/s500x750/e9e82c5bca839be289f3d87863141ebf6d7fca28.gifv"},
            {"hayu", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/hayu/hayu-hover.gif"},
            {"max", "https://64.media.tumblr.com/cca7a86d443a0bc88536a2ad6ce72aec/b495f88d5c1df470-a2/s640x960/4cb9b614191d17d02c946b4ca59548cd333c06fd.gifv"},
            {"magellan", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/magellan/magellan-hover.gif"},
            {"mgm", "https://64.media.tumblr.com/9f43b185411e18d71444c9a5a5a79632/5d68ae94e9917470-70/s500x750/0871e87c17045759735ef64c26703c71b585bfb0.gifv"},
            {"mubi", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/mubi/mubi-hover.gif"},
            {"paramount", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/paramount-plus/paramount-plus-hover.gif"},
            {"peacock", "https://64.media.tumblr.com/a378bf124ed7d00c8d7430cb8a9ae0cc/9e0da9dbc32641f6-b2/s1280x1920/e8c0ba2f6aadae5b6f532a177926528e94b04d68.gifv"},
            {"pluto", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/pluto-tv/pluto-tv-hover.gif"},
            {"shudder", "https://raw.githubusercontent.com/luckynumb3rs/stremio-perfect-setup/refs/heads/main/collections/streaming/focused/shudder.gif"},
            {"starz", "https://64.media.tumblr.com/81fe52878aad139bc7b4ff5db7efc766/1e7a62d9e4b13b7a-a2/s500x750/740d23b1e14a96d02282d62190ee61e2a30aed4d.gifv"},
            {"tubi", "https://raw.githubusercontent.com/ImKaptain/nuvio-art/main/art/streaming-services/tubi/tubi-hover.gif"}
    };

    static String gif(String key) {
        for (String[] g : GIFS) if (g[0].equals(key)) return g[1];
        return null;
    }

    private static final Map<String, List<Row>> mem = new HashMap<>();
    private static final Map<String, Long> memAt = new HashMap<>();

    private Streaming() {}

    static Service find(String key) {
        for (Service s : ALL) if (s.key.equals(key)) return s;
        return ALL[0];
    }

    static String tmdbKey(Context c) { return U.prefs(c).getString("tmdb_key", ""); }

    /** Services we can show titles for right now. */
    static boolean canBrowse(Context c, Service s) {
        return s.hasCatalog() || !TextUtils.isEmpty(tmdbKey(c));
    }

    interface Cb { void done(List<Row> rows); }

    /** Top picks for the home row: the first row's titles. */
    static List<Nuvio.Item> cached(String key) {
        List<Row> r = mem.get(key);
        if (r == null) return null;
        List<Nuvio.Item> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Row row : r) for (Nuvio.Item it : row.items) {
            if (out.size() >= 16) return out;
            if (seen.add(it.type + it.imdbId)) out.add(it);
            if (out.size() >= 8 && row == r.get(0)) break;
        }
        return out;
    }

    static void load(Context c, final String key, final Cb cb) {
        List<Row> hit = mem.get(key);
        Long at = memAt.get(key);
        if (hit != null && at != null && System.currentTimeMillis() - at < 6 * 3600_000L) { cb.done(hit); return; }
        final Context app = c.getApplicationContext();
        U.IO.execute(() -> {
            List<Row> rows = loadSync(app, key);
            U.MAIN.post(() -> cb.done(rows));
        });
    }

    static synchronized List<Row> loadSync(Context c, String key) {
        Service s = find(key);
        String tk = tmdbKey(c);
        List<Row> rows = new ArrayList<>();
        if (!TextUtils.isEmpty(tk)) rows = tmdbRows(c, s, tk);
        if (rows.isEmpty() && s.hasCatalog()) rows = catalogRows(c, s);
        if (!rows.isEmpty()) {
            mem.put(key, rows);
            memAt.put(key, System.currentTimeMillis());
        }
        return rows;
    }

    // ---------------------------------------------------------------- TMDB (same sources as Nuvio)

    private static List<Row> tmdbRows(Context c, Service s, String k) {
        List<Row> rows = new ArrayList<>();
        String base = "https://api.themoviedb.org/3";
        String prov = "&with_watch_providers=" + Uri.encode(s.providers) + "&watch_region=" + s.region;
        if (s.tmdbList > 0) add(rows, "Top 10 Movies", parseTmdb(cachedGet(c, base + "/list/" + s.tmdbList + "?api_key=" + k, "items"), "movie", s));
        add(rows, "Popular Series", parseTmdb(cachedGet(c, base + "/discover/tv?api_key=" + k + prov + "&sort_by=popularity.desc&vote_count.gte=25", "results"), "series", s));
        add(rows, "Popular Movies", parseTmdb(cachedGet(c, base + "/discover/movie?api_key=" + k + prov + "&sort_by=popularity.desc&vote_count.gte=25", "results"), "movie", s));
        add(rows, "New Series", parseTmdb(cachedGet(c, base + "/discover/tv?api_key=" + k + prov + "&sort_by=first_air_date.desc&vote_count.gte=5", "results"), "series", s));
        add(rows, "New Movies", parseTmdb(cachedGet(c, base + "/discover/movie?api_key=" + k + prov + "&sort_by=primary_release_date.desc&vote_count.gte=5&without_genres=99", "results"), "movie", s));
        add(rows, "Top Rated", parseTmdb(cachedGet(c, base + "/discover/movie?api_key=" + k + prov + "&sort_by=vote_average.desc&vote_count.gte=300", "results"), "movie", s));
        return rows;
    }

    private static void add(List<Row> rows, String title, List<Nuvio.Item> items) {
        if (!items.isEmpty()) rows.add(new Row(title, items));
    }

    private static List<Nuvio.Item> parseTmdb(JSONArray arr, String fallbackType, Service s) {
        List<Nuvio.Item> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String mt = o.optString("media_type", "");
            String type = "tv".equals(mt) ? "series" : "movie".equals(mt) ? "movie" : fallbackType;
            Nuvio.Item it = new Nuvio.Item();
            it.pkg = U.NUVIO;
            it.type = type;
            it.tmdbId = o.optLong("id");
            it.imdbId = "tmdb:" + it.tmdbId;
            it.title = o.optString("movie".equals(type) ? "title" : "name", o.optString("title", o.optString("name")));
            if (TextUtils.isEmpty(it.title) || it.tmdbId == 0) continue;
            it.desc = o.optString("overview", null);
            String bd = o.optString("backdrop_path", null), ps = o.optString("poster_path", null);
            if (bd != null && !"null".equals(bd)) { it.poster = "https://image.tmdb.org/t/p/original" + bd; it.thumb = "https://image.tmdb.org/t/p/w780" + bd; }
            if (ps != null && !"null".equals(ps)) it.posterTall = "https://image.tmdb.org/t/p/w342" + ps;
            String date = o.optString("movie".equals(type) ? "release_date" : "first_air_date", "");
            if (date.length() >= 4) it.year = date.substring(0, 4);
            double v = o.optDouble("vote_average", 0);
            if (v > 0) it.rating = "★ " + String.format(java.util.Locale.ROOT, "%.1f", v);
            it.channel = s.name;
            out.add(it);
        }
        return out;
    }

    /** Title logo (the film's own wordmark) from TMDB, if there is one. */
    static String tmdbLogo(Context c, Nuvio.Item it) {
        String k = tmdbKey(c);
        if (TextUtils.isEmpty(k) || it.tmdbId == 0) return null;
        JSONArray logos = cachedGet(c, "https://api.themoviedb.org/3/" + ("series".equals(it.type) ? "tv" : "movie") + "/" + it.tmdbId
                + "/images?api_key=" + k + "&include_image_language=en,null", "logos");
        if (logos == null || logos.length() == 0) return null;
        JSONObject best = logos.optJSONObject(0);
        return best == null ? null : "https://image.tmdb.org/t/p/w500" + best.optString("file_path");
    }

    // ---------------------------------------------------------------- public catalog fallback

    private static List<Row> catalogRows(Context c, Service s) {
        List<Row> rows = new ArrayList<>();
        add(rows, "Top Movies", parseCatalog(cachedGet(c, CATALOG + "/catalog/movie/" + s.catalog + ".json", "metas"), "movie", s));
        add(rows, "Top Series", parseCatalog(cachedGet(c, CATALOG + "/catalog/series/" + s.catalog + ".json", "metas"), "series", s));
        return rows;
    }

    private static List<Nuvio.Item> parseCatalog(JSONArray metas, String type, Service s) {
        List<Nuvio.Item> out = new ArrayList<>();
        if (metas == null) return out;
        for (int k = 0; k < metas.length(); k++) {
            JSONObject o = metas.optJSONObject(k);
            if (o == null) continue;
            Nuvio.Item it = new Nuvio.Item();
            it.pkg = U.NUVIO;
            it.type = type;
            it.imdbId = o.optString("imdb_id", o.optString("id"));
            it.title = o.optString("name");
            if (TextUtils.isEmpty(it.title) || TextUtils.isEmpty(it.imdbId)) continue;
            it.desc = o.optString("description", null);
            it.posterTall = o.optString("poster", null);
            it.poster = o.optString("background", null);
            it.thumb = it.poster;
            it.logo = o.optString("logo", null);
            it.year = o.optString("releaseInfo", null);
            if (it.year != null && it.year.length() > 9) it.year = it.year.substring(0, 9);
            String r = o.optString("imdbRating", null);
            if (!TextUtils.isEmpty(r)) it.rating = "IMDb " + r;
            JSONArray g = o.optJSONArray("genres");
            if (g != null && g.length() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < Math.min(2, g.length()); x++) { if (x > 0) sb.append(", "); sb.append(g.optString(x)); }
                it.genre = sb.toString();
            }
            it.channel = s.name;
            out.add(it);
        }
        return out;
    }

    // ---------------------------------------------------------------- open in Nuvio

    static boolean open(Context c, Nuvio.Item it) {
        if (it == null) return false;
        List<String> links = new ArrayList<>();
        if (it.tmdbId > 0) {
            links.add("nuvio://tmdb/" + it.type + "/" + it.tmdbId);
        } else {
            links.add("nuvio://" + it.type + "/" + it.imdbId);
            links.add("nuvio://detail/" + it.type + "/" + it.imdbId);
        }
        for (String l : links) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(l)).setPackage(U.NUVIO);
            if (Apps.start(c, i)) { Apps.noteLaunch(c, U.NUVIO); return true; }
        }
        return Apps.launchPackage(c, U.NUVIO);
    }

    // ---------------------------------------------------------------- http + disk cache

    private static JSONArray cachedGet(Context c, String url, String arrayKey) {
        try {
            File f = Img.file(c, "json:" + url);
            String body = null;
            boolean fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < 6 * 3600_000L;
            if (!fresh) {
                body = get(url);
                if (body != null) try (FileOutputStream o = new FileOutputStream(f)) { o.write(body.getBytes("UTF-8")); }
            }
            if (body == null && f.exists()) body = read(f);
            if (body == null) return null;
            return new JSONObject(body).optJSONArray(arrayKey);
        } catch (Throwable t) {
            Log.w(TAG, "get " + url.replaceAll("api_key=[^&]+", "api_key=…"), t);
            return null;
        }
    }

    private static String get(String url) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(15000);
            con.setRequestProperty("User-Agent", "HorizonHome/1.2 (Android TV)");
            if (con.getResponseCode() != 200) return null;
            try (InputStream in = con.getInputStream()) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] b = new byte[16384];
                int n;
                while ((n = in.read(b)) > 0) bo.write(b, 0, n);
                return bo.toString("UTF-8");
            }
        } catch (Throwable t) {
            return null;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String read(File f) throws Exception {
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) bo.write(b, 0, n);
            return bo.toString("UTF-8");
        }
    }
}
