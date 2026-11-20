package com.horizon.home;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * One streaming service (e.g. Netflix) as it appears in Nuvio. The top half is a fixed spotlight:
 * the focused title's backdrop sits top-right and fades into the page, with its logo and details on
 * the left. Rows of posters scroll only inside the bottom half, so nothing moves behind them.
 */
public class ServiceActivity extends Activity {
    private static final float EDGE = 48, TOP_H = 292, BACK_W = 640;

    private Streaming.Service svc;
    private final ImageView[] back = new ImageView[2];
    private int backFront = -1;
    private LinearLayout info, grid;
    private ImageView titleLogo;
    private TextView kicker, titleText, meta, desc, status;
    private ScrollView scroll;
    private List<Nuvio.Item> items = new ArrayList<>();
    private List<Streaming.Row> rows = new ArrayList<>();
    private int shown = -1, pending = -1;
    private final Runnable showPending = () -> { if (pending >= 0) show(pending); };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        U.init(this);
        V.init(this);
        svc = Streaming.find(getIntent().getStringExtra("service"));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(V.BG);
        getWindow().setBackgroundDrawable(null);

        // Spotlight backdrop: fixed size, top-right, blended into the page on its left and bottom.
        FrameLayout backBox = new FrameLayout(this);
        for (int i = 0; i < 2; i++) {
            back[i] = new ImageView(this);
            back[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
            back[i].setAlpha(0f);
            backBox.addView(back[i], new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
        }
        View fadeL = new View(this);
        fadeL.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {V.BG, U.alpha(V.BG, 0.75f), U.alpha(V.BG, 0.25f), 0x00000000}));
        backBox.addView(fadeL, U.flp(V.dp(300), U.MATCH, Gravity.START));
        View fadeB = new View(this);
        fadeB.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {V.BG, U.alpha(V.BG, 0.6f), 0x00000000}));
        backBox.addView(fadeB, U.flp(U.MATCH, V.dp(110), Gravity.BOTTOM));
        View fadeT = new View(this);
        fadeT.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {U.alpha(V.BG, 0.55f), 0x00000000}));
        backBox.addView(fadeT, U.flp(U.MATCH, V.dp(70), Gravity.TOP));
        root.addView(backBox, U.flp(V.dp(BACK_W), V.dp(TOP_H), Gravity.TOP | Gravity.END));

        // Left: service badge, then the focused title's logo and details.
        info = U.col(this);
        LinearLayout badge = U.row(this);
        final ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackground(V.shape(svc.color, 6, 0));
        cover.setClipToOutline(true);
        badge.addView(cover, new LinearLayout.LayoutParams(V.dp(46), V.dp(26)));
        Img.load(this, svc.cover, V.dp(46), bm -> { if (bm != null) cover.setImageBitmap(bm); });
        LinearLayout bt = U.col(this);
        bt.addView(V.text(this, svc.name, 13, 800, V.TEXT));
        bt.addView(V.dim(V.caps(this, "On Nuvio", 7.5f, V.TEXT), 0.6f));
        badge.addView(bt, lpm(U.WRAP, U.WRAP, 10, 0, 0, 0));
        info.addView(badge);

        kicker = V.caps(this, "", 8.5f, V.accent());
        info.addView(kicker, lpm(U.WRAP, U.WRAP, 0, 26, 0, 0));
        titleLogo = new ImageView(this);
        titleLogo.setScaleType(ImageView.ScaleType.FIT_START);
        titleLogo.setVisibility(View.GONE);
        info.addView(titleLogo, lpm(V.dp(300), V.dp(78), 0, 8, 0, 0));
        titleText = V.text(this, "", 32, 800, V.TEXT);
        titleText.setLetterSpacing(-0.02f);
        titleText.setMaxLines(2);
        titleText.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(titleText, lpm(V.dp(400), U.WRAP, 0, 6, 0, 0));
        meta = V.dim(V.text(this, "", 10.5f, 500, V.TEXT), 0.8f);
        info.addView(meta, lpm(U.WRAP, U.WRAP, 0, 10, 0, 0));
        desc = V.dim(V.text(this, "", 10.5f, 400, V.TEXT), 0.75f);
        desc.setMaxLines(3);
        desc.setEllipsize(TextUtils.TruncateAt.END);
        desc.setLineSpacing(V.dpf(2.5f), 1f);
        info.addView(desc, lpm(V.dp(380), U.WRAP, 0, 8, 0, 0));
        root.addView(info, U.flp(U.WRAP, U.WRAP, Gravity.TOP | Gravity.START, 0, 0, 0, 0));
        FrameLayout.LayoutParams ip = (FrameLayout.LayoutParams) info.getLayoutParams();
        ip.leftMargin = V.dp(EDGE);
        ip.topMargin = V.dp(34);

        // Rows: their own fixed area below the spotlight.
        scroll = new AnchoredScroll(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setVerticalFadingEdgeEnabled(false);
        grid = U.col(this);
        grid.setPadding(0, V.dp(6), 0, V.dp(160));
        scroll.addView(grid, new FrameLayout.LayoutParams(U.MATCH, U.WRAP));
        FrameLayout.LayoutParams sp = U.flp(U.MATCH, U.MATCH, Gravity.TOP);
        sp.topMargin = V.dp(TOP_H);
        root.addView(scroll, sp);

        status = V.dim(V.text(this, "Loading " + svc.name + "…", 11, 500, V.TEXT), 0.7f);
        FrameLayout.LayoutParams stp = U.flp(U.WRAP, U.WRAP, Gravity.TOP | Gravity.START);
        stp.leftMargin = V.dp(EDGE);
        stp.topMargin = V.dp(TOP_H + 20);
        root.addView(status, stp);

        setContentView(root);
        // Only the rows may draw past their edges (focus outlines); the spotlight and the rows area
        // itself stay clipped so nothing slides over the top half.
        V.unclipAll(grid);

        // Start on the service's own backdrop and logo, like Nuvio's folder page.
        kicker.setText("Streaming on Nuvio");
        titleText.setText(svc.name);
        Img.load(this, svc.hero, HomeActivity.WALL_W, bm -> { if (bm != null && shown < 0) showBack(bm); });
        if (svc.logo != null) Img.load(this, svc.logo, V.dp(300), bm -> {
            if (bm != null && shown < 0) { titleLogo.setImageBitmap(bm); titleLogo.setVisibility(View.VISIBLE); titleText.setVisibility(View.GONE); }
        });
        Streaming.load(this, svc.key, rs -> {
            List<Nuvio.Item> list = new ArrayList<>();
            for (Streaming.Row r : rs) list.addAll(r.items);
            items = list;
            rows = rs;
            if (list.isEmpty()) {
                status.setText("Couldn't load " + svc.name + " right now. Check the internet connection.");
                return;
            }
            status.setVisibility(View.GONE);
            buildGrid();
        });
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_UI_HIDDEN) Img.clear();
    }

    private LinearLayout.LayoutParams lpm(int w, int h, float l, float t, float r, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(V.dp(l), V.dp(t), V.dp(r), V.dp(b));
        return p;
    }

    // ------------------------------------------------------------------ rows

    private void buildGrid() {
        grid.removeAllViews();
        View f = null;
        int base = 0;
        for (Streaming.Row r : rows) {
            List<Integer> idx = new ArrayList<>();
            for (int k = 0; k < r.items.size(); k++) idx.add(base + k);
            base += r.items.size();
            View first = section(r.title, idx);
            if (f == null) f = first;
        }
        if (f != null) { final View ff = f; ff.post(ff::requestFocus); }
    }

    private View section(String title, List<Integer> idx) {
        if (idx.isEmpty()) return null;
        TextView h = V.text(this, title, 12, 700, V.TEXT);
        LinearLayout.LayoutParams hp = lpm(U.WRAP, U.WRAP, 0, grid.getChildCount() == 0 ? 0 : 10, 0, 0);
        hp.leftMargin = V.dp(EDGE);
        grid.addView(h, hp);
        HorizontalScrollView hs = new HomeActivity.AnchoredRow(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setHorizontalFadingEdgeEnabled(false);
        hs.setFocusable(false);
        LinearLayout row = U.row(this);
        row.setPadding(V.dp(EDGE), V.dp(8), V.dp(EDGE), V.dp(10));
        hs.addView(row);
        grid.addView(hs, new LinearLayout.LayoutParams(U.MATCH, U.WRAP));
        int ph = V.dp(132), pw = Math.round(ph * 2f / 3f);
        View first = null;
        for (int k = 0; k < idx.size(); k++) {
            final int i = idx.get(k);
            final Nuvio.Item it = items.get(i);
            View card = posterCard(it, pw, ph, () -> {
                pending = i;
                U.MAIN.removeCallbacks(showPending);
                U.MAIN.postDelayed(showPending, 220);
            });
            card.setOnClickListener(v -> Streaming.open(this, it));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(pw, ph);
            cp.rightMargin = k == idx.size() - 1 ? 0 : V.dp(10);
            row.addView(card, cp);
            if (first == null) first = card;
        }
        return first;
    }

    /** Tall poster card; focus lifts it slightly and draws a white outline. */
    private View posterCard(Nuvio.Item it, int w, int h, Runnable onFocus) {
        final FrameLayout card = new FrameLayout(this);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setBackground(V.shape(0xFF181A20, 8, 0));
        card.setClipToOutline(true);
        final ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(iv, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
        final TextView t = V.text(this, it.title, 10, 600, U.alpha(V.TEXT, 0.8f));
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(3);
        card.addView(t, U.flp(U.MATCH, U.WRAP, Gravity.CENTER, 6, 0, 6, 0));
        String art = it.posterTall != null ? it.posterTall : it.cardArt();
        Img.load(this, art, w, b -> { if (b != null) { iv.setImageBitmap(b); t.setVisibility(View.GONE); } });
        final View ring = new View(this);
        ring.setBackground(V.shape(0x00000000, 8, 0));
        ((GradientDrawable) ring.getBackground()).setStroke(V.dp(1.5f), V.TEXT);
        ring.setAlpha(0f);
        card.addView(ring, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
        card.setContentDescription(it.title);
        card.setOnFocusChangeListener((v, has) -> {
            v.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(160)
                    .setInterpolator(new DecelerateInterpolator()).start();
            ring.setAlpha(has ? 1f : 0f);
            if (has) onFocus.run();
        });
        return card;
    }

    // ------------------------------------------------------------------ spotlight

    private void show(final int i) {
        if (i < 0 || i >= items.size() || i == shown) return;
        shown = i;
        final Nuvio.Item it = items.get(i);
        kicker.setText("series".equals(it.type) ? "Series" : "Film");
        titleText.setText(it.title);
        titleText.setVisibility(View.VISIBLE);
        titleLogo.setVisibility(View.GONE);
        titleLogo.setImageDrawable(null);
        final Img.Cb logoCb = bm -> {
            if (shown != i || bm == null) return;
            titleLogo.setImageBitmap(bm);
            titleLogo.setVisibility(View.VISIBLE);
            titleText.setVisibility(View.GONE);
        };
        if (!TextUtils.isEmpty(it.logo)) Img.load(this, it.logo, V.dp(300), logoCb);
        else if (it.tmdbId > 0) U.IO.execute(() -> {
            String l = Streaming.tmdbLogo(getApplicationContext(), it);
            if (l != null) U.MAIN.post(() -> { it.logo = l; Img.load(this, l, V.dp(300), logoCb); });
        });
        String m = it.meta();
        meta.setText(TextUtils.isEmpty(m) ? "" : m);
        U.gone(meta, TextUtils.isEmpty(m));
        desc.setText(it.desc == null ? "" : it.desc);
        info.setAlpha(0.35f);
        info.animate().alpha(1f).setDuration(260).setInterpolator(new DecelerateInterpolator()).withLayer().start();
        Img.load(this, Img.sized(it.art(), "w1280"), HomeActivity.WALL_W, bm -> { if (shown == i && bm != null) showBack(bm); });
    }

    private void showBack(android.graphics.Bitmap bm) {
        final int next = backFront < 0 ? 0 : 1 - backFront;
        final int old = backFront;
        ImageView n = back[next];
        n.animate().cancel();
        n.setImageBitmap(bm);
        n.setAlpha(0f);
        n.animate().alpha(1f).setDuration(320).start();
        if (old >= 0) {
            final ImageView o = back[old];
            o.animate().cancel();
            o.animate().alpha(0f).setDuration(320).withEndAction(() -> { if (backFront != old) o.setImageDrawable(null); }).start();
        }
        backFront = next;
    }

    /** Vertical scroller that keeps the focused row's title at the top of the rows area. */
    static final class AnchoredScroll extends ScrollView {
        AnchoredScroll(Context c) { super(c); }
        @Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect r) {
            if (getChildCount() == 0) return 0;
            int max = Math.max(0, getChildAt(0).getHeight() - getHeight());
            int target = Math.max(0, Math.min(max, r.top - V.dp(30)));
            return target - getScrollY();
        }
    }
}
