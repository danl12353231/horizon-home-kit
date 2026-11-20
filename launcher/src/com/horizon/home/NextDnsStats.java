package com.horizon.home;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Blocking stats from the NextDNS API (needs the profile id + an API key, kept in private prefs). */
final class NextDnsStats {
    static final class Stats {
        long blocked, total;
        final List<String[]> topBlocked = new ArrayList<>(); // {domain, count}
        long[] week = new long[7];
        long weekBlocked;
        String topWeek;
        String error;
    }

    private static Stats cached;
    private static long cachedAt;

    private NextDnsStats() {}

    static String profile(Context c) { return U.prefs(c).getString("nextdns_id", ""); }
    static String key(Context c) { return U.prefs(c).getString("nextdns_key", ""); }

    static boolean configured(Context c) { return !TextUtils.isEmpty(profile(c)) && !TextUtils.isEmpty(key(c)); }

    static Stats cached() { return cached; }

    static Stats load(Context c) {
        if (cached != null && System.currentTimeMillis() - cachedAt < 5 * 60_000L) return cached;
        Stats s = new Stats();
        String base = "https://api.nextdns.io/profiles/" + profile(c) + "/analytics/";
        try {
            JSONArray st = get(c, base + "status?from=-24h").getJSONArray("data");
            for (int i = 0; i < st.length(); i++) {
                JSONObject o = st.getJSONObject(i);
                long q = o.optLong("queries");
                s.total += q;
                if ("blocked".equals(o.optString("status"))) s.blocked += q;
            }
            JSONArray dom = get(c, base + "domains?status=blocked&from=-24h&limit=3").getJSONArray("data");
            for (int i = 0; i < dom.length(); i++) {
                JSONObject o = dom.getJSONObject(i);
                s.topBlocked.add(new String[] {o.optString("domain"), String.valueOf(o.optLong("queries"))});
            }
            JSONArray wk = get(c, base + "status;series?from=-7d&interval=1d").getJSONArray("data");
            for (int i = 0; i < wk.length(); i++) {
                JSONObject o = wk.getJSONObject(i);
                if (!"blocked".equals(o.optString("status"))) continue;
                JSONArray q = o.optJSONArray("queries");
                if (q == null) continue;
                int n = q.length();
                for (int k = 0; k < Math.min(7, n); k++) {
                    s.week[7 - Math.min(7, n) + k] = q.optLong(n - Math.min(7, n) + k);
                    s.weekBlocked += q.optLong(n - Math.min(7, n) + k);
                }
            }
            JSONArray top7 = get(c, base + "domains?status=blocked&from=-7d&limit=1").getJSONArray("data");
            if (top7.length() > 0) s.topWeek = top7.getJSONObject(0).optString("domain");
        } catch (Throwable t) {
            s.error = t.getMessage();
        }
        if (s.error == null) { cached = s; cachedAt = System.currentTimeMillis(); }
        return s;
    }

    private static JSONObject get(Context c, String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        try {
            con.setConnectTimeout(8000);
            con.setReadTimeout(12000);
            con.setRequestProperty("X-Api-Key", key(c));
            int code = con.getResponseCode();
            if (code != 200) throw new Exception("NextDNS " + code);
            try (InputStream in = con.getInputStream()) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) bo.write(b, 0, n);
                return new JSONObject(bo.toString("UTF-8"));
            }
        } finally {
            con.disconnect();
        }
    }
}
