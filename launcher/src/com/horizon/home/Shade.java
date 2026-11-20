package com.horizon.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Random;

/**
 * Home's dark fades and artwork-colour glow, baked once into a dithered half-resolution image
 * (Veil's trick: full-screen gradient shaders are slow on TV GPUs; one image is as cheap as a photo).
 * Two layers crossfade when the glow colour changes.
 */
final class Shade extends FrameLayout {
    private final ImageView a, b;
    private boolean showingA = true;
    private Integer lastGlow = -1;
    private int lastBg;

    Shade(Context c) {
        super(c);
        a = new ImageView(c);
        b = new ImageView(c);
        for (ImageView iv : new ImageView[] {a, b}) {
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            addView(iv, new LayoutParams(U.MATCH, U.MATCH));
        }
        b.setAlpha(0f);
    }

    void set(final int bg, final Integer glow) {
        if (glow == null ? lastGlow == null : glow.equals(lastGlow)) { if (bg == lastBg) return; }
        lastGlow = glow;
        lastBg = bg;
        final int w = Math.max(320, U.W / 2), h = Math.max(180, U.H / 2);
        U.IO.execute(() -> {
            final Bitmap bm = bake(w, h, bg, glow);
            U.MAIN.post(() -> {
                ImageView in = showingA ? b : a, out = showingA ? a : b;
                in.setImageBitmap(bm);
                in.animate().cancel();
                in.setAlpha(0f);
                in.bringToFront();
                in.animate().alpha(1f).setDuration(500).withEndAction(() -> { out.setAlpha(0f); out.setImageDrawable(null); }).start();
                showingA = !showingA;
            });
        });
    }

    private static float ramp(float t, float[] stops, float[] values) {
        if (t <= stops[0]) return values[0];
        for (int i = 1; i < stops.length; i++) {
            if (t <= stops[i]) {
                float f = (t - stops[i - 1]) / (stops[i] - stops[i - 1]);
                return values[i - 1] + (values[i] - values[i - 1]) * f;
            }
        }
        return values[values.length - 1];
    }

    private static final float[] H_STOPS = {0f, 0.42f, 0.78f};
    private static final float[] H_ALPHA = {0.94f, 0.6f, 0.05f};
    private static final float[] V_STOPS = {0f, 0.22f, 0.55f, 0.8f, 1f};
    private static final float[] V_ALPHA = {0.5f, 0f, 0.5f, 0.97f, 1f};
    private static final float[] G_STOPS = {0f, 0.55f, 1f};
    private static final float[] G_ALPHA = {0.16f, 0.05f, 0f};

    static Bitmap bake(int w, int h, int bg, Integer glow) {
        int[] px = new int[w * h];
        Random rnd = new Random(42);
        float[] hx = new float[w];
        for (int x = 0; x < w; x++) hx[x] = ramp(x / (w - 1f), H_STOPS, H_ALPHA);
        float cx = w * 0.18f, cy = h * 1.02f, radius = w * 0.62f;
        float br = Color.red(bg), bgG = Color.green(bg), bb = Color.blue(bg);
        float gr = glow == null ? 0 : Color.red(glow), gg = glow == null ? 0 : Color.green(glow), gb = glow == null ? 0 : Color.blue(glow);
        for (int y = 0; y < h; y++) {
            float va = ramp(y / (h - 1f), V_STOPS, V_ALPHA);
            float dy = y - cy;
            for (int x = 0; x < w; x++) {
                float a = 1f - (1f - hx[x]) * (1f - va);
                float r = br, g = bgG, bl = bb;
                float outA = a;
                if (glow != null) {
                    float dx = x - cx;
                    float ga = ramp((float) Math.sqrt(dx * dx + dy * dy) / radius, G_STOPS, G_ALPHA);
                    // glow over the fade (source-over, premultiplied)
                    float na = ga + outA * (1f - ga);
                    if (na > 0) {
                        r = (gr * ga + r * outA * (1f - ga)) / na;
                        g = (gg * ga + g * outA * (1f - ga)) / na;
                        bl = (gb * ga + bl * outA * (1f - ga)) / na;
                    }
                    outA = na;
                }
                int A = Math.max(0, Math.min(255, (int) (outA * 255f + rnd.nextFloat() - 0.5f)));
                int R = Math.max(0, Math.min(255, (int) (r + rnd.nextFloat() - 0.5f)));
                int G = Math.max(0, Math.min(255, (int) (g + rnd.nextFloat() - 0.5f)));
                int B = Math.max(0, Math.min(255, (int) (bl + rnd.nextFloat() - 0.5f)));
                px[y * w + x] = (A << 24) | (R << 16) | (G << 8) | B;
            }
        }
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bm.setPixels(px, 0, w, 0, 0, w, h);
        return bm;
    }
}
