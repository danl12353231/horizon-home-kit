package com.horizon.home;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Full-screen artwork that slowly drifts (Ken Burns) and cross-fades between titles. */
final class HeroWall extends FrameLayout implements Glass.Source {
    private final ImageView[] iv = new ImageView[2];
    private final ObjectAnimator[] drift = new ObjectAnimator[2];
    private int front = -1;
    private int flip;
    private final View fallback;
    private long driftMs = 14000;

    HeroWall(Context c) {
        super(c);
        setBackgroundColor(Color.BLACK);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {0xFF06070C, 0xFF151A3A, 0xFF3A1D3F, 0xFF8A4A2A});
        fallback = new View(c);
        fallback.setBackground(g);
        addView(fallback, new LayoutParams(U.MATCH, U.MATCH));
        for (int i = 0; i < 2; i++) {
            iv[i] = new ImageView(c);
            iv[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv[i].setAlpha(0f);
            iv[i].setPivotX(U.W / 2f);
            iv[i].setPivotY(U.H / 2f);
            addView(iv[i], new LayoutParams(U.MATCH, U.MATCH));
        }
    }

    void setDriftMs(long ms) { driftMs = ms; }

    boolean hasImage() { return front >= 0; }

    void show(Bitmap b) {
        if (b == null) return;
        int next = front < 0 ? 0 : 1 - front;
        final ImageView n = iv[next];
        n.animate().cancel();
        n.setImageBitmap(b);
        n.bringToFront();
        n.setAlpha(0f);
        n.animate().alpha(1f).setDuration(front < 0 ? 500 : 700).start();
        if (drift[next] != null) drift[next].cancel();
        flip++;
        n.setScaleX(1f);
        n.setScaleY(1f);
        n.setTranslationX(0);
        n.setTranslationY(0);
        driftView = n;
        driftStart = System.currentTimeMillis() + 3000; // rest on the picture a moment first
        removeCallbacks(driftTick);
        postDelayed(driftTick, 3000);
        final int old = front;
        front = next;
        if (old >= 0) {
            final ImageView o = iv[old];
            postDelayed(() -> {
                if (front != old) {
                    o.setAlpha(0f);
                    o.setImageDrawable(null); // let the old picture's memory go
                    if (drift[old] != null) drift[old].cancel();
                }
            }, 760);
        }
    }

    private ImageView driftView;
    private long driftStart;
    private boolean paused;

    /** Slow pan-and-zoom at 30 fps (it moves far less than a pixel a frame, so 30 looks the same). */
    private final Runnable driftTick = new Runnable() {
        @Override public void run() {
            if (paused || driftView == null) return;
            long cycle = Math.max(4000, driftMs * 2);
            float t = ((System.currentTimeMillis() - driftStart) % cycle) / (float) cycle;
            float d = t < 0.5f ? t * 2f : 2f - t * 2f;            // 0 -> 1 -> 0
            d = d * d * (3f - 2f * d);                              // ease in/out
            float z = 1f + 0.08f * d;
            driftView.setScaleX(z);
            driftView.setScaleY(z);
            driftView.setTranslationX(-getWidth() * 0.035f * d);
            driftView.setTranslationY(-getHeight() * 0.012f * d);
            postDelayed(this, 33);
        }
    };

    private long pausedAt;

    void pause() {
        if (!paused) pausedAt = System.currentTimeMillis();
        paused = true;
        removeCallbacks(driftTick);
    }

    void resume() {
        if (paused && pausedAt > 0) driftStart += System.currentTimeMillis() - pausedAt;
        pausedAt = 0;
        paused = false;
        removeCallbacks(driftTick);
        if (driftView != null) post(driftTick);
    }

    @Override
    public boolean capture(Bitmap dst, Canvas canvas) {
        if (getWidth() == 0) return false;
        canvas.drawColor(Color.BLACK);
        canvas.save();
        canvas.scale(dst.getWidth() / (float) getWidth(), dst.getHeight() / (float) getHeight());
        try {
            draw(canvas);
        } catch (Throwable t) {
            canvas.restore();
            return false;
        }
        canvas.restore();
        return true;
    }
}
