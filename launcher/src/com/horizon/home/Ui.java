package com.horizon.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Cards, pills and buttons with TV focus behaviour. */
final class Ui {
    private Ui() {}

    static int accent = 0xFFFFB36B;

    static void rounded(View v, final float radiusUnits) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), U.f(radiusUnits));
            }
        });
        v.setClipToOutline(true);
    }

    /** Grow + ring + shadow when focused. */
    static void focusCard(final View card, final View ring, final float scale, final Runnable onFocus) {
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        try { card.setOutlineSpotShadowColor(accent); card.setOutlineAmbientShadowColor(accent); } catch (Throwable ignored) {}
        card.setOnFocusChangeListener((v, has) -> {
            v.animate().scaleX(has ? scale : 1f).scaleY(has ? scale : 1f)
                    .translationZ(has ? U.f(14) : 0f)
                    .setDuration(has ? 180 : 150).setInterpolator(new DecelerateInterpolator()).start();
            if (ring != null) ring.animate().alpha(has ? 1f : 0f).setDuration(150).start();
            if (has && onFocus != null) onFocus.run();
        });
    }

    static View ring(Context c, float radiusUnits) {
        View r = new View(c);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.TRANSPARENT);
        g.setCornerRadius(U.f(radiusUnits));
        g.setStroke(Math.max(2, U.s(2.6f)), 0xFFFFFFFF);
        r.setBackground(g);
        r.setAlpha(0f);
        r.setDuplicateParentStateEnabled(false);
        return r;
    }

    /** 16:9 app card: the app's own TV banner, or its icon on a colour wash with the name. */
    static FrameLayout appCard(Context c, Apps.App a, int w, int h) {
        FrameLayout card = new FrameLayout(c);
        card.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        rounded(card, 12);
        card.setElevation(U.f(2));
        if (a.banner != null) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageDrawable(a.banner);
            card.setBackgroundColor(0xFF15171C);
            card.addView(iv, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
        } else {
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[] {U.mix(a.color, 0xFFFFFFFF, 0.12f), U.mix(a.color, 0xFF000000, 0.45f)});
            card.setBackground(bg);
            ImageView iv = new ImageView(c);
            iv.setImageDrawable(a.icon);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int is = Math.round(h * 0.46f);
            card.addView(iv, U.flp(is, is, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, h * 0.13f / U.k, 0, 0));
            TextView t = U.text(c, a.label, 11.5f, 0xEEFFFFFF, U.MED);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setGravity(Gravity.CENTER);
            card.addView(t, U.flp(U.MATCH, U.WRAP, Gravity.BOTTOM, 8, 0, 8, 9));
        }
        View ring = ring(c, 12);
        card.addView(ring, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
        card.setTag(ring);
        card.setContentDescription(a.label);
        return card;
    }

    /** Card for a film or show (artwork with title, optional progress). */
    static FrameLayout mediaCard(Context c, Nuvio.Item it, int w, int h, boolean showTitle) {
        final FrameLayout card = new FrameLayout(c);
        card.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        rounded(card, 12);
        card.setBackgroundColor(0xFF1A1C22);
        card.setElevation(U.f(2));
        final ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(iv, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
        String art = Img.sized(it.cardArt(), "w500");
        Img.load(c, art, Math.max(w, 320), b -> { if (b != null) { iv.setImageBitmap(b); iv.setAlpha(0f); iv.animate().alpha(1f).setDuration(250).start(); } });
        if (showTitle) {
            View shade = new View(c);
            shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {0x00000000, 0x00000000, 0xCC000000}));
            card.addView(shade, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
            TextView t = U.text(c, it.title, 11.5f, 0xFFFFFFFF, U.MED);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(t, U.flp(U.MATCH, U.WRAP, Gravity.BOTTOM, 10, 0, 10, it.progress() > 0 ? 14 : 9));
        }
        if (it.progress() > 0) {
            View track = new View(c);
            track.setBackground(U.round(0x55FFFFFF, 2));
            card.addView(track, U.flp(U.MATCH, U.s(3), Gravity.BOTTOM, 10, 0, 10, 7));
            View bar = new View(c);
            bar.setBackground(U.round(accent, 2));
            int bw = Math.round((w - U.s(20)) * it.progress());
            card.addView(bar, U.flp(Math.max(U.s(4), bw), U.s(3), Gravity.BOTTOM | Gravity.START, 10, 0, 0, 7));
        }
        View ring = ring(c, 12);
        card.addView(ring, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
        card.setTag(ring);
        card.setContentDescription(it.title);
        return card;
    }

    /** Tab / chip that turns solid white when focused. */
    static TextView pill(Context c, String label, float size) {
        final TextView t = U.text(c, label, size, 0xCCFFFFFF, U.MED);
        t.setGravity(Gravity.CENTER);
        t.setPadding(U.s(16), 0, U.s(16), 0);
        t.setFocusable(true);
        t.setFocusableInTouchMode(true);
        t.setBackground(new ColorDrawable(Color.TRANSPARENT));
        return t;
    }

    static void stylePill(TextView t, boolean focused, boolean selected) {
        if (focused) {
            t.setBackground(U.round(0xFFFFFFFF, 18));
            t.setTextColor(0xFF111216);
        } else if (selected) {
            t.setBackground(U.ring(0x26FFFFFF, 0x40FFFFFF, 1, 18));
            t.setTextColor(0xFFFFFFFF);
        } else {
            t.setBackground(new ColorDrawable(Color.TRANSPARENT));
            t.setTextColor(0xB3FFFFFF);
        }
    }

    /** Round icon button for the top capsule. */
    static FrameLayout iconButton(Context c, Icon icon, float sizeUnits, String label) {
        final FrameLayout b = new FrameLayout(c);
        int s = U.s(sizeUnits);
        b.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        b.setContentDescription(label);
        int is = Math.round(s * 0.52f);
        b.addView(icon, U.flp(is, is, Gravity.CENTER));
        b.setOnFocusChangeListener((v, has) -> {
            v.setBackground(has ? U.round(0xFFFFFFFF, sizeUnits / 2f) : null);
            icon.setColor(has ? 0xFF111216 : 0xFFFFFFFF);
            v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(140).start();
        });
        return b;
    }

    /** Big rounded button ("Play in Nuvio"). */
    static LinearLayout button(Context c, Icon icon, String label, boolean primary) {
        final LinearLayout b = U.row(c);
        b.setPadding(U.s(primary ? 20 : 18), 0, U.s(22), 0);
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        final TextView t = U.text(c, label, 15, 0xFFFFFFFF, U.MED);
        if (icon != null) b.addView(icon, U.llp(U.s(18), U.s(18), 0, 0, 10, 0));
        b.addView(t);
        final Drawable normal = primary ? U.round(0xF2FFFFFF, 24) : U.ring(0x33FFFFFF, 0x55FFFFFF, 1, 24);
        b.setBackground(normal);
        t.setTextColor(primary ? 0xFF101114 : 0xFFFFFFFF);
        if (icon != null) icon.setColor(primary ? 0xFF101114 : 0xFFFFFFFF);
        b.setOnFocusChangeListener((v, has) -> {
            if (has) {
                v.setBackground(U.ring(primary ? 0xFFFFFFFF : 0xFFFFFFFF, accent, 2.5f, 24));
                t.setTextColor(0xFF101114);
                if (icon != null) icon.setColor(0xFF101114);
            } else {
                v.setBackground(normal);
                t.setTextColor(primary ? 0xFF101114 : 0xFFFFFFFF);
                if (icon != null) icon.setColor(primary ? 0xFF101114 : 0xFFFFFFFF);
            }
            v.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(140).start();
        });
        return b;
    }

    static View gradient(Context c, GradientDrawable.Orientation o, int... colors) {
        View v = new View(c);
        v.setBackground(new GradientDrawable(o, colors));
        return v;
    }

    static ImageView image(Context c, Bitmap b) {
        ImageView iv = new ImageView(c);
        iv.setImageBitmap(b);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return iv;
    }
}
