package com.horizon.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/** Tiny image loader: memory cache + disk cache, background threads, no libraries. */
final class Img {
    interface Cb { void done(Bitmap b); }

    private static final String TAG = "HorizonImg";
    private static final LruCache<String, Bitmap> mem = new LruCache<String, Bitmap>(
            (int) Math.min(20 * 1024, Runtime.getRuntime().maxMemory() / 1024 / 8)) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount() / 1024; }
    };

    private Img() {}

    /** TMDB serves several sizes; ask for one that suits the place it's shown. */
    static String sized(String uri, String size) {
        if (uri == null) return null;
        int i = uri.indexOf("image.tmdb.org/t/p/");
        if (i < 0) return uri;
        int start = i + "image.tmdb.org/t/p/".length();
        int end = uri.indexOf('/', start);
        if (end < 0) return uri;
        return uri.substring(0, start) + size + uri.substring(end);
    }

    static void clear() { mem.evictAll(); }

    static Bitmap cached(String uri, int reqW) {
        return uri == null ? null : mem.get(uri + "@" + reqW);
    }

    static void load(Context c, String uri, int reqW, Cb cb) {
        if (TextUtils.isEmpty(uri)) { cb.done(null); return; }
        Bitmap hit = cached(uri, reqW);
        if (hit != null) { cb.done(hit); return; }
        final Context app = c.getApplicationContext();
        U.IO.execute(() -> {
            Bitmap b = loadSync(app, uri, reqW);
            U.MAIN.post(() -> cb.done(b));
        });
    }

    static Bitmap loadSync(Context c, String uri, int reqW) {
        if (TextUtils.isEmpty(uri)) return null;
        String key = uri + "@" + reqW;
        Bitmap hit = mem.get(key);
        if (hit != null) return hit;
        try {
            File f = file(c, uri);
            if (!f.exists() || f.length() == 0) fetch(c, uri, f);
            if (!f.exists()) return null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int sample = 1;
            while (o.outWidth / (sample * 2) >= reqW) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            // Big pictures (backdrops) are opaque: RGB_565 halves memory and upload time; logos keep alpha.
            o.inPreferredConfig = reqW >= 900 && !uri.endsWith(".png") ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888;
            Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            // Power-of-two sampling can leave a picture far bigger than needed (a 2000 px poster for a
            // 160 px card); scale it to the exact size so memory matches what's on screen.
            if (b != null && b.getWidth() > reqW * 1.2f) {
                int h = Math.max(1, Math.round(b.getHeight() * reqW / (float) b.getWidth()));
                Bitmap s = Bitmap.createScaledBitmap(b, reqW, h, true);
                if (s != b) b.recycle();
                b = s;
            }
            if (b != null && reqW >= 900) b.prepareToDraw();
            if (b != null) mem.put(key, b);
            else f.delete();
            return b;
        } catch (Throwable t) {
            Log.w(TAG, "load " + uri, t);
            return null;
        }
    }

    static File file(Context c, String uri) throws Exception {
        File dir = new File(c.getCacheDir(), "img");
        if (!dir.exists()) dir.mkdirs();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] d = md.digest(uri.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte x : d) sb.append(String.format("%02x", x));
        return new File(dir, sb.toString());
    }

    static void fetchTo(Context c, String uri, File out) throws Exception { fetch(c, uri, out); }

    private static void fetch(Context c, String uri, File out) throws Exception {
        File tmp = new File(out.getPath() + ".part");
        InputStream in = null;
        HttpURLConnection con = null;
        try {
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                URL url = new URL(uri);
                for (int hop = 0; hop < 4; hop++) {
                    con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(7000);
                    con.setReadTimeout(12000);
                    con.setInstanceFollowRedirects(true);
                    con.setRequestProperty("User-Agent", "HorizonHome/1.0 (Android TV)");
                    int code = con.getResponseCode();
                    if (code >= 300 && code < 400 && con.getHeaderField("Location") != null) {
                        url = new URL(url, con.getHeaderField("Location"));
                        con.disconnect();
                        continue;
                    }
                    if (code != 200) throw new Exception("HTTP " + code);
                    break;
                }
                in = con.getInputStream();
            } else {
                in = c.getContentResolver().openInputStream(Uri.parse(uri));
            }
            if (in == null) return;
            try (OutputStream os = new FileOutputStream(tmp)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            }
            if (!tmp.renameTo(out)) tmp.delete();
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (con != null) con.disconnect();
        }
    }

    /** Remove disk-cached art older than two weeks so the cache doesn't grow forever. */
    static void trim(Context c) {
        U.IO.execute(() -> {
            File dir = new File(c.getCacheDir(), "img");
            File[] fs = dir.listFiles();
            if (fs == null) return;
            long cutoff = System.currentTimeMillis() - 14L * 24 * 3600 * 1000;
            for (File f : fs) if (f.lastModified() < cutoff) f.delete();
        });
    }
}
