package com.horizon.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Full-screen artwork rendered in one canvas.
 *
 * Keeping both pictures inside a single View avoids the brief texture/layer replacement flash that
 * some Android TV compositors produce when multiple transparent ImageViews overlap or are recycled.
 */
final class HeroWall extends View implements Glass.Source {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private final GradientDrawable fallback;

    private Bitmap current;
    private Bitmap incoming;
    private Bitmap pending;
    private ValueAnimator fade;
    private float mix;

    HeroWall(Context c) {
        super(c);
        setBackgroundColor(Color.BLACK);
        fallback = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {0xFF06070C, 0xFF151A3A, 0xFF3A1D3F, 0xFF8A4A2A});
    }

    boolean hasImage() { return current != null || incoming != null; }

    void show(Bitmap b) {
        if (b == null || b.isRecycled()) return;
        if (b == current || b == incoming || b == pending) return;

        if (current == null && incoming == null) {
            current = b;
            invalidate();
            return;
        }

        // Never replace a bitmap that is currently being drawn. During fast navigation retain only
        // the latest requested picture, then blend to it after the active fade finishes.
        if (fade != null && fade.isRunning()) {
            pending = b;
            return;
        }
        beginFade(b);
    }

    private void beginFade(Bitmap b) {
        incoming = b;
        mix = 0f;
        invalidate(); // Upload/draw the new texture invisibly while the current image remains intact.

        fade = ValueAnimator.ofFloat(0f, 1f);
        fade.setDuration(520);
        fade.setInterpolator(new DecelerateInterpolator(1.4f));
        fade.addUpdateListener(a -> {
            mix = (Float) a.getAnimatedValue();
            invalidate();
        });
        fade.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override public void onAnimationCancel(Animator animation) { cancelled = true; }

            @Override public void onAnimationEnd(Animator animation) {
                if (cancelled) return;
                current = incoming;
                incoming = null;
                mix = 0f;
                fade = null;
                invalidate();

                Bitmap next = pending;
                pending = null;
                if (next != null && next != current && !next.isRecycled()) {
                    // Starting on the following frame gives the completed image a stable presented
                    // frame and avoids back-to-back texture uploads on slower TV GPUs.
                    postOnAnimation(() -> beginFade(next));
                }
            }
        });
        // Let the zero-alpha invalidation reach the render thread before animation values advance.
        postOnAnimation(() -> {
            if (fade != null && !fade.isStarted()) fade.start();
        });
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        fallback.setBounds(0, 0, getWidth(), getHeight());
        fallback.draw(canvas);
        drawCover(canvas, current, 255);
        if (incoming != null) drawCover(canvas, incoming, Math.round(255f * mix));
    }

    private void drawCover(Canvas canvas, Bitmap bitmap, int alpha) {
        if (bitmap == null || bitmap.isRecycled() || alpha <= 0 || getWidth() <= 0 || getHeight() <= 0) return;
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        float scale = Math.max(getWidth() / (float) bw, getHeight() / (float) bh);
        float shownW = getWidth() / scale;
        float shownH = getHeight() / scale;
        int left = Math.max(0, Math.round((bw - shownW) * 0.5f));
        int top = Math.max(0, Math.round((bh - shownH) * 0.5f));
        int right = Math.min(bw, Math.round(left + shownW));
        int bottom = Math.min(bh, Math.round(top + shownH));
        src.set(left, top, right, bottom);
        dst.set(0, 0, getWidth(), getHeight());
        paint.setAlpha(alpha);
        canvas.drawBitmap(bitmap, src, dst, paint);
    }

    void pause() {}
    void resume() {}

    @Override protected void onDetachedFromWindow() {
        if (fade != null) fade.cancel();
        fade = null;
        super.onDetachedFromWindow();
    }

    @Override
    public boolean capture(Bitmap bitmap, Canvas canvas) {
        if (getWidth() == 0) return false;
        canvas.drawColor(Color.BLACK);
        canvas.save();
        canvas.scale(bitmap.getWidth() / (float) getWidth(), bitmap.getHeight() / (float) getHeight());
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
