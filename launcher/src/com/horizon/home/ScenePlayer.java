package com.horizon.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays the XGIMI Wall scenes full screen on a SurfaceView, so the video goes straight to the display's
 * video plane and the launcher UI is not redrawn for every video frame (much smoother on TV chips).
 * Scene changes cross through the next scene's poster: poster fades in, video swaps underneath,
 * poster fades out once the new video is rendering.
 */
final class ScenePlayer extends FrameLayout implements SurfaceHolder.Callback, Glass.Source {
    interface Listener {
        void onScene(Scene scene, int index, int count);
        void onProgress(float fraction);
    }

    private static final String TAG = "HorizonScenes";

    private final SurfaceView surface;
    private final ImageView poster;
    private MediaPlayer mp;
    private boolean surfaceReady, started, shown;
    private List<Scene> list = new ArrayList<>();
    private int index = -1;
    private Listener listener;
    private int intervalMs = 30000;
    private long shownAt;
    private boolean liftEnabled = true;
    private float liftAmount = 1f;
    private ValueAnimator liftAnim;
    private boolean sound;
    private int failures;
    private Scene playing;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!started) return;
            if (list.size() > 1 && shown) {
                float p = (System.currentTimeMillis() - shownAt) / (float) intervalMs;
                if (listener != null) listener.onProgress(Math.min(1f, p));
                if (p >= 1f) next();
            }
            U.MAIN.postDelayed(this, 500);
        }
    };

    ScenePlayer(Context c) {
        super(c);
        setBackgroundColor(Color.BLACK);
        setClipChildren(true);
        surface = new SurfaceView(c);
        surface.getHolder().addCallback(this);
        addView(surface, new LayoutParams(U.W, U.H));
        poster = new ImageView(c);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(poster, new LayoutParams(U.W, U.H));
    }

    void setListener(Listener l) { listener = l; }

    void configure(List<Scene> scenes, int intervalSecs, boolean lift, boolean withSound) {
        String curId = currentScene() != null ? currentScene().id : null;
        list = new ArrayList<>(scenes);
        intervalMs = Math.max(5, intervalSecs) * 1000;
        liftEnabled = lift;
        sound = withSound;
        index = -1;
        if (curId != null) for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(curId)) index = i;
    }

    Scene currentScene() {
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    int count() { return list.size(); }

    void start(boolean fromFirst) {
        if (list.isEmpty()) return;
        started = true;
        failures = 0;
        if (fromFirst || index < 0 || index >= list.size()) index = 0;
        show(index, false);
        U.MAIN.removeCallbacks(tick);
        U.MAIN.postDelayed(tick, 500);
    }

    void stop() {
        started = false;
        U.MAIN.removeCallbacks(tick);
        release();
        playing = null;
        poster.animate().cancel();
        poster.setAlpha(1f);
    }

    void next() { jump(1); }
    void prev() { jump(-1); }

    private void jump(int d) {
        if (list.size() < 2) return;
        index = ((index + d) % list.size() + list.size()) % list.size();
        show(index, true);
    }

    /** 1 = framing that lifts the subject above the rows, 0 = full frame (ambient). */
    void animateLift(float target) {
        if (liftAnim != null) liftAnim.cancel();
        liftAnim = ValueAnimator.ofFloat(liftAmount, target);
        liftAnim.setDuration(900);
        liftAnim.setInterpolator(new DecelerateInterpolator(1.6f));
        liftAnim.addUpdateListener(a -> { liftAmount = (float) a.getAnimatedValue(); applyLift(); });
        liftAnim.start();
    }

    private void show(int i, boolean transition) {
        if (!started || list.isEmpty()) return;
        final Scene s = list.get(i);
        if (listener != null) listener.onScene(s, i, list.size());
        Bitmap pb = loadPoster(s);
        if (pb != null) poster.setImageBitmap(pb);
        layoutFor(s);
        if (transition && shown) {
            // Poster of the next scene fades in over the current video, then the video swaps underneath.
            poster.setAlpha(0f);
            poster.animate().alpha(1f).setDuration(700).withEndAction(() -> play(s)).start();
        } else {
            poster.setAlpha(1f);
            play(s);
        }
        shownAt = System.currentTimeMillis();
    }

    private Bitmap loadPoster(Scene s) {
        if (s.poster == null) return null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            o.inSampleSize = U.W >= 1900 ? 1 : 2;
            return BitmapFactory.decodeFile(s.poster.getAbsolutePath(), o);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Size the surface for the scene's zoom; the lift itself is a cheap translation. */
    private void layoutFor(Scene s) {
        float z = s.zoomFor(liftEnabled);
        int w = Math.round(U.W * z), h = Math.round(U.H * z);
        LayoutParams lp = new LayoutParams(w, h);
        lp.leftMargin = -(w - U.W) / 2;
        lp.topMargin = -(h - U.H) / 2;
        surface.setLayoutParams(lp);
        poster.setLayoutParams(new LayoutParams(w, h));
        ((LayoutParams) poster.getLayoutParams()).leftMargin = lp.leftMargin;
        ((LayoutParams) poster.getLayoutParams()).topMargin = lp.topMargin;
        playing = s;
        applyLift();
    }

    private void applyLift() {
        Scene s = playing;
        float dy = s == null ? 0 : -s.liftFor(liftEnabled) * liftAmount * U.H;
        surface.setTranslationY(dy);
        poster.setTranslationY(dy);
    }

    private void play(Scene s) {
        playing = s;
        shown = false;
        release();
        if (!surfaceReady || s.video == null) return;
        try {
            mp = new MediaPlayer();
            mp.setDisplay(surface.getHolder());
            mp.setDataSource(s.video.getAbsolutePath());
            mp.setLooping(true);
            float v = sound ? 0.5f : 0f;
            mp.setVolume(v, v);
            final MediaPlayer m = mp;
            mp.setOnPreparedListener(p -> { if (p == mp) { p.start(); U.MAIN.postDelayed(() -> onRendering(m), 1200); } });
            mp.setOnInfoListener((p, what, extra) -> {
                if (p == mp && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) onRendering(p);
                return false;
            });
            mp.setOnErrorListener((p, what, extra) -> { Log.w(TAG, "error " + what + "/" + extra); onFailed(); return true; });
            mp.prepareAsync();
        } catch (Throwable t) {
            Log.w(TAG, "play " + s.id, t);
            onFailed();
        }
    }

    private void onRendering(MediaPlayer p) {
        if (p != mp || shown) return;
        shown = true;
        failures = 0;
        shownAt = System.currentTimeMillis();
        poster.animate().cancel();
        poster.animate().alpha(0f).setDuration(600).start();
    }

    private void onFailed() {
        release();
        failures++;
        if (started && failures < list.size()) U.MAIN.postDelayed(this::next, 400);
    }

    private void release() {
        if (mp != null) {
            try { mp.setOnInfoListener(null); mp.setOnErrorListener(null); mp.setOnPreparedListener(null); } catch (Throwable ignored) {}
            try { mp.reset(); } catch (Throwable ignored) {}
            try { mp.release(); } catch (Throwable ignored) {}
            mp = null;
        }
        shown = false;
    }

    @Override public void surfaceCreated(SurfaceHolder h) {
        surfaceReady = true;
        if (started && playing != null && mp == null) play(playing);
    }

    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}

    @Override public void surfaceDestroyed(SurfaceHolder h) {
        surfaceReady = false;
        release();
        poster.setAlpha(1f);
    }

    @Override public boolean capture(Bitmap dst, Canvas canvas) { return false; }
}
