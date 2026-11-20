package com.horizon.home;

import android.content.Context;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.File;

/** Focus animations (GIF / animated WebP) for service tiles, decoded frame by frame (light on memory). */
final class Anim {
    interface Cb { void done(Drawable d); }

    private Anim() {}

    static void load(Context c, String url, int w, int h, Cb cb) {
        if (url == null) { cb.done(null); return; }
        final Context app = c.getApplicationContext();
        U.IO.execute(() -> {
            Drawable d = null;
            for (String u : variants(url)) {
                d = decode(app, u, w, h);
                if (d != null) break;
            }
            final Drawable out = d;
            U.MAIN.post(() -> cb.done(out));
        });
    }

    /** Tumblr ".gifv" links are web pages; the same path as .gif / .webp is the image itself. */
    private static String[] variants(String url) {
        if (url.endsWith(".gifv")) {
            String base = url.substring(0, url.length() - 5);
            return new String[] {base + ".gif", base + ".webp", url};
        }
        return new String[] {url};
    }

    private static Drawable decode(Context c, String url, int w, int h) {
        try {
            File f = Img.file(c, "anim:" + url);
            if (!f.exists() || f.length() == 0) {
                Img.fetchTo(c, url, f);
            }
            if (!f.exists() || f.length() < 100) { Log.w("HorizonAnim", "no file for " + url); return null; }
            ImageDecoder.Source src = ImageDecoder.createSource(f);
            Drawable d = ImageDecoder.decodeDrawable(src, (decoder, info, s) -> {
                int iw = info.getSize().getWidth(), ih = info.getSize().getHeight();
                float k = Math.min(1f, Math.max(w / (float) iw, h / (float) ih) * 1.1f);
                decoder.setTargetSize(Math.max(1, Math.round(iw * k)), Math.max(1, Math.round(ih * k)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            if (d instanceof AnimatedImageDrawable) ((AnimatedImageDrawable) d).setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
            Log.i("HorizonAnim", "ok " + url + " " + f.length() + " bytes " + d.getClass().getSimpleName());
            return d;
        } catch (Throwable t) {
            Log.w("HorizonAnim", "decode " + url + ": " + t);
            return null;
        }
    }
}
