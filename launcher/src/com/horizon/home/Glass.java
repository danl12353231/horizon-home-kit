package com.horizon.home;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Liquid glass" panels for Android 11, which has no live blur. A few times a second we take a
 * tiny copy of the background (video frame or artwork), blur it, and every glass panel paints the
 * part of it that sits behind the panel, then adds tint, sheen and a bright rim.
 */
final class Glass {
    interface Source {
        boolean capture(Bitmap dst, Canvas canvas);
    }

    static final int BW = 192, BH = 108;

    private static Source source;
    private static Bitmap work, blurred, spare;
    private static Canvas workCanvas;
    private static int[] px, tmp;
    private static boolean running;
    private static boolean enabled = true;
    private static final List<WeakReference<View>> hosts = new ArrayList<>();
    private static int periodMs = 200;

    private static final Runnable loop = new Runnable() {
        @Override public void run() {
            if (!running) return;
            update();
            U.MAIN.postDelayed(this, periodMs);
        }
    };

    static void setSource(Source s) { source = s; }

    static void setEnabled(boolean e) {
        enabled = e;
        invalidateAll();
    }

    static void start() {
        if (running) return;
        running = true;
        U.MAIN.post(loop);
    }

    static void stop() {
        running = false;
        U.MAIN.removeCallbacks(loop);
    }

    /** Slow down while nothing glassy is on screen (ambient mode). */
    static void setPeriod(int ms) { periodMs = ms; }

    static void update() {
        if (source == null || !enabled) return;
        if (work == null) {
            work = Bitmap.createBitmap(BW, BH, Bitmap.Config.ARGB_8888);
            workCanvas = new Canvas(work);
            px = new int[BW * BH];
            tmp = new int[BW * BH];
        }
        boolean ok;
        try {
            ok = source.capture(work, workCanvas);
        } catch (Throwable t) {
            ok = false;
        }
        if (!ok) return;
        work.getPixels(px, 0, BW, 0, 0, BW, BH);
        boxBlur(px, tmp, BW, BH, 7);
        boxBlur(px, tmp, BW, BH, 7);
        boxBlur(px, tmp, BW, BH, 6);
        Bitmap out = spare;
        if (out == null) out = Bitmap.createBitmap(BW, BH, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, BW, 0, 0, BW, BH);
        spare = blurred;
        blurred = out;
        invalidateAll();
    }

    static void register(View v) {
        hosts.add(new WeakReference<>(v));
    }

    static void invalidateAll() {
        Iterator<WeakReference<View>> it = hosts.iterator();
        while (it.hasNext()) {
            View v = it.next().get();
            if (v == null) { it.remove(); continue; }
            if (v.isShown() && v.getAlpha() > 0.01f) v.invalidate();
        }
    }

    static void clearHosts() { hosts.clear(); }

    /** Separable box blur on ARGB pixels, in place. */
    private static void boxBlur(int[] p, int[] t, int w, int h, int r) {
        int div = r * 2 + 1;
        for (int y = 0; y < h; y++) {
            int off = y * w;
            int ra = 0, rr = 0, rg = 0, rb = 0;
            for (int i = -r; i <= r; i++) {
                int c = p[off + Math.min(w - 1, Math.max(0, i))];
                ra += c >>> 24; rr += (c >> 16) & 255; rg += (c >> 8) & 255; rb += c & 255;
            }
            for (int x = 0; x < w; x++) {
                t[off + x] = ((ra / div) << 24) | ((rr / div) << 16) | ((rg / div) << 8) | (rb / div);
                int in = p[off + Math.min(w - 1, x + r + 1)];
                int outc = p[off + Math.max(0, x - r)];
                ra += (in >>> 24) - (outc >>> 24);
                rr += ((in >> 16) & 255) - ((outc >> 16) & 255);
                rg += ((in >> 8) & 255) - ((outc >> 8) & 255);
                rb += (in & 255) - (outc & 255);
            }
        }
        for (int x = 0; x < w; x++) {
            int ra = 0, rr = 0, rg = 0, rb = 0;
            for (int i = -r; i <= r; i++) {
                int c = t[Math.min(h - 1, Math.max(0, i)) * w + x];
                ra += c >>> 24; rr += (c >> 16) & 255; rg += (c >> 8) & 255; rb += c & 255;
            }
            for (int y = 0; y < h; y++) {
                p[y * w + x] = ((ra / div) << 24) | ((rr / div) << 16) | ((rg / div) << 8) | (rb / div);
                int in = t[Math.min(h - 1, y + r + 1) * w + x];
                int outc = t[Math.max(0, y - r) * w + x];
                ra += (in >>> 24) - (outc >>> 24);
                rr += ((in >> 16) & 255) - ((outc >> 16) & 255);
                rg += ((in >> 8) & 255) - ((outc >> 8) & 255);
                rb += (in & 255) - (outc & 255);
            }
        }
    }

    /** Background drawable for a glass panel. */
    static final class Panel extends Drawable {
        private final View host;
        private final float radius;
        private int tint;
        private float rimStrength = 1f;
        private final Paint glassPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Matrix m = new Matrix();
        private final int[] loc = new int[2];
        private Bitmap shaderBitmap;
        private BitmapShader shader;
        private int lastH = -1;

        Panel(View host, float radiusUnits, int tint) {
            this.host = host;
            this.radius = U.f(radiusUnits);
            this.tint = tint;
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(1.35f);
            ColorMatrix bright = new ColorMatrix(new float[] {
                    0.92f, 0, 0, 0, 6,
                    0, 0.92f, 0, 0, 6,
                    0, 0, 0.92f, 0, 8,
                    0, 0, 0, 1, 0});
            cm.postConcat(bright);
            glassPaint.setColorFilter(new ColorMatrixColorFilter(cm));
            rimPaint.setStyle(Paint.Style.STROKE);
            rimPaint.setStrokeWidth(Math.max(1f, U.f(1.1f)));
            register(host);
        }

        void setGlassTint(int t) { tint = t; invalidateSelf(); }
        void setRim(float s) { rimStrength = s; lastH = -1; invalidateSelf(); }

        @Override public void draw(Canvas c) {
            Rect b = getBounds();
            rect.set(b);
            if (rect.width() <= 0 || rect.height() <= 0) return;
            Bitmap bl = blurred;
            if (enabled && bl != null) {
                if (shaderBitmap != bl) {
                    shaderBitmap = bl;
                    shader = new BitmapShader(bl, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    glassPaint.setShader(shader);
                }
                host.getLocationInWindow(loc);
                float hs = host.getScaleX() == 0 ? 1 : host.getScaleX();
                float vs = host.getScaleY() == 0 ? 1 : host.getScaleY();
                m.setScale(U.W / (float) BW, U.H / (float) BH);
                m.postTranslate(-loc[0], -loc[1]);
                m.postScale(1f / hs, 1f / vs);
                shader.setLocalMatrix(m);
                c.drawRoundRect(rect, radius, radius, glassPaint);
            }
            tintPaint.setColor(enabled && bl != null ? tint : U.mix(tint, 0xE0101216, 0.6f));
            c.drawRoundRect(rect, radius, radius, tintPaint);
            if (lastH != b.height()) {
                lastH = b.height();
                sheenPaint.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                        new int[] {0x24FFFFFF, 0x08FFFFFF, 0x00FFFFFF, 0x0AFFFFFF},
                        new float[] {0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
                rimPaint.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                        new int[] {U.alpha(0xFFFFFFFF, 0.55f * rimStrength), U.alpha(0xFFFFFFFF, 0.12f * rimStrength), U.alpha(0xFFFFFFFF, 0.22f * rimStrength)},
                        new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            }
            c.drawRoundRect(rect, radius, radius, sheenPaint);
            float inset = rimPaint.getStrokeWidth() / 2f;
            rect.inset(inset, inset);
            c.drawRoundRect(rect, radius - inset, radius - inset, rimPaint);
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
