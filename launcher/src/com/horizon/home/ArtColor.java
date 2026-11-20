package com.horizon.home;

import android.graphics.Bitmap;
import android.graphics.Color;

/** Picks the most characterful colour out of artwork (after Veil's ArtColor). */
final class ArtColor {
    private ArtColor() {}

    /** Strongest saturated colour, or null for grey artwork. */
    static Integer extract(Bitmap bmp) {
        if (bmp == null) return null;
        try {
            int w = 64, h = Math.max(1, Math.round(64f * bmp.getHeight() / bmp.getWidth()));
            Bitmap small = Bitmap.createScaledBitmap(bmp, w, h, true);
            int[] px = new int[w * h];
            small.getPixels(px, 0, w, 0, 0, w, h);
            int bins = 24;
            float[] weight = new float[bins], r = new float[bins], g = new float[bins], b = new float[bins];
            float[] hsv = new float[3];
            for (int c : px) {
                Color.colorToHSV(c, hsv);
                if (hsv[1] < 0.18f || hsv[2] < 0.15f) continue;
                float wgt = hsv[1] * hsv[1] * hsv[2];
                int bin = Math.min(bins - 1, (int) (hsv[0] / 360f * bins));
                weight[bin] += wgt;
                r[bin] += wgt * Color.red(c); g[bin] += wgt * Color.green(c); b[bin] += wgt * Color.blue(c);
            }
            int best = -1;
            for (int i = 0; i < bins; i++) if (weight[i] > 0.4f && (best < 0 || weight[i] > weight[best])) best = i;
            if (best < 0) return null;
            return Color.rgb((int) (r[best] / weight[best]), (int) (g[best] / weight[best]), (int) (b[best] / weight[best]));
        } catch (Throwable t) {
            return null;
        }
    }

    /** Light enough for dark text on it, never neon. */
    static int asAccent(int c) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[1] = Math.max(0.35f, Math.min(0.7f, hsv[1]));
        hsv[2] = Math.max(0.92f, hsv[2]);
        return Color.HSVToColor(hsv);
    }

    /** For the edge glow: full brightness, a bit more saturation. */
    static int asGlow(int c) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[1] = Math.max(0.45f, Math.min(0.9f, hsv[1]));
        hsv[2] = 1f;
        return Color.HSVToColor(hsv);
    }
}
