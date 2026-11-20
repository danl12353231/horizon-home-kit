package com.horizon.home;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Home settings: switch between Scenes and Nuvio Hero Wall, plus the details of each. */
public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout list;
    private final List<Runnable> refreshers = new ArrayList<>();
    private int accent = 0xFFFFB36B;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        U.init(this);
        V.init(this);
        prefs = U.prefs(this);
        accent = V.themeAccent;

        FrameLayout root = new FrameLayout(this);
        View dim = Ui.gradient(this, GradientDrawable.Orientation.RIGHT_LEFT, 0xCC000000, 0x8C000000, 0x33000000);
        root.addView(dim, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));

        FrameLayout panel = new FrameLayout(this);
        panel.setBackground(V.shape(U.alpha(U.mix(V.BG, 0xFFFFFFFF, 0.1f), 0.95f), 20, V.LINE));
        panel.setElevation(U.f(20));
        root.addView(panel, U.flp(U.s(600), U.MATCH, Gravity.END, 0, 24, 24, 24));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setVerticalFadingEdgeEnabled(true);
        sv.setFadingEdgeLength(U.s(40));
        sv.setClipToPadding(false);
        list = U.col(this);
        list.setPadding(U.s(34), U.s(30), U.s(34), U.s(40));
        sv.addView(list, new FrameLayout.LayoutParams(U.MATCH, U.WRAP));
        panel.addView(sv, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
        setContentView(root);
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        for (Runnable r : refreshers) r.run();
    }

    private void build() {
        list.removeAllViews();
        refreshers.clear();

        TextView h = V.text(this, "horizon", 26, 800, 0xFFFFFFFF);
        list.addView(h);
        TextView sub = U.text(this, "Ad-free by design · no tracking · your XGIMI home", 13, 0x99FFFFFF, U.REG);
        list.addView(sub, U.llp(U.WRAP, U.WRAP, 0, 8, 0, 6));

        section("ARTWORK");
        choice("Change title every", U.P_HERO_SECS, 12,
                new int[] {8, 12, 20, 30, 60}, new String[] {"8 seconds", "12 seconds", "20 seconds", "30 seconds", "1 minute"});
        toggle("Fill in from other apps", "If Nuvio shares only a few titles, add rows from YouTube, Prime Video and others", U.P_HERO_FILL, true);
        buildNuvioStatus();

        section("LOOK");
        choice("Screen edge margin", "edge", 64, new int[] {48, 64, 80, 96},
                new String[] {"Small", "Medium", "Large", "Extra large"});
        action("Theme", () -> themeName(), this::nextTheme);
        toggle("Colours from artwork", "The accent glows in the colours of what's on screen", "art_colours", true);
        toggle("Weather", "Temperature next to the clock (Open-Meteo, no account)", "weather", true);

        section("PROJECTOR CHANGES");
        action("Ad & tracker blocking", () -> dnsOn() ? "On \u00b7 NextDNS" : "Off", () -> {
            final boolean on = dnsOn();
            confirmRun(on ? "Turn off ad & tracker blocking?" : "Turn ad & tracker blocking back on?",
                    on ? "The projector goes back to its normal DNS. Ads and trackers will load again."
                       : "Lookups are encrypted and filtered by your NextDNS profile again.",
                    on ? "settings delete global private_dns_mode; settings delete global private_dns_specifier"
                       : "settings put global private_dns_specifier " + NextDnsStats.profile(this) + ".dns.nextdns.io; settings put global private_dns_mode hostname");
        });
        action("Power button", () -> fullOff() ? "Full shutdown \u00b7 logo on every start" : "Quick standby", () -> {
            final boolean full = fullOff();
            confirmRun(full ? "Switch Power Off to quick standby?" : "Make Power Off a full shutdown?",
                    full ? "Turning on takes about 2 seconds. The XGIMI logo only plays after a full start (XGIMI does one on every 15th power-off)."
                         : "Every Power Off uses XGIMI's own shutdown (cover closes). The XGIMI logo plays each time, but starting takes about 30\u201340 seconds.",
                    full ? "settings delete system power_key_definition" : "settings put system power_key_definition 1");
        });
        action("Animation speed", () -> fastAnim() ? "Fast (0.5\u00d7)" : "Normal", () -> {
            final boolean fast = fastAnim();
            confirmRun(fast ? "Back to normal animation speed?" : "Use fast animations?",
                    "Changes how quickly menus and screens animate in every app.",
                    fast ? "settings put global window_animation_scale 1.0; settings put global transition_animation_scale 1.0; settings delete global animator_duration_scale"
                         : "settings put global window_animation_scale 0.5; settings put global transition_animation_scale 0.5; settings put global animator_duration_scale 0.5");
        });
        action("Google TV recommendations", () -> enabled(TVREC) ? "On" : "Off", () -> {
            final boolean on = enabled(TVREC);
            confirmRun(on ? "Turn off Google TV recommendations?" : "Turn Google TV recommendations back on?",
                    "A background service only Google's home screen uses. Off saves memory.",
                    (on ? "pm disable-user --user 0 " : "pm enable ") + TVREC);
        });
        action("Home screen", () -> horizonIsHome() ? "Horizon" : "Google TV", () -> {
            if (horizonIsHome()) {
                confirmRun("Go back to the Google TV home screen?",
                        "Turns Google's home and its recommendations back on and makes it the home screen. "
                                + "Horizon stays installed: open it from your apps any time to switch back.",
                        "pm enable " + TVLAUNCHER + "; pm enable " + TVREC
                                + "; cmd package set-home-activity " + TVLAUNCHER + "/.MainActivity"
                                + "; settings delete secure enabled_accessibility_services; settings put secure accessibility_enabled 0"
                                + "; am start -a android.intent.action.MAIN -c android.intent.category.HOME");
            } else {
                confirmRun("Make Horizon your home screen?",
                        "Switches Google's home off (it stays installed) and makes the Home button open Horizon.",
                        "cmd package set-home-activity " + getPackageName() + "/.HomeActivity"
                                + "; settings put secure enabled_accessibility_services " + getPackageName() + "/" + getPackageName() + ".HomeButtonService"
                                + "; settings put secure accessibility_enabled 1"
                                + "; pm disable-user --user 0 " + TVREC + "; pm disable-user --user 0 " + TVLAUNCHER
                                + "; am start -n " + getPackageName() + "/.HomeActivity");
            }
        });

        section("LIVE TV");
        action("Open TV guide", () -> "Channels and listings from your Live TV addon", () -> startActivity(new Intent(this, GuideActivity.class)));
        action("Install Live TV in Nuvio", () -> "Adds the addon with the TV GUIDE link to the Nuvio profile in use", this::installLiveTv);

        section("GENERAL");
        choice("Ambient mode after", U.P_AMBIENT_MIN, 3,
                new int[] {0, 1, 3, 5, 10, 20}, new String[] {"Never", "1 minute", "3 minutes", "5 minutes", "10 minutes", "20 minutes"});
        toggle("Show greeting", null, U.P_GREET, true);
        action("Your name", () -> prefs.getString(U.P_NAME, ""), this::editName);
        action("Show hidden apps again", () -> hiddenCount() + " hidden", this::unhideAll);
        action("Projector settings", () -> "Picture, keystone, 3D, sound", () -> Apps.start(this, new Intent(Settings.ACTION_SETTINGS)));
        action("About", () -> "Version " + version() + "  ·  ad-free home for your XGIMI", null);
    }

    private void installLiveTv() {
        if (GuideActivity.src(this).isEmpty()) {
            android.widget.Toast.makeText(this, "Set your Live TV addon link first (see the setup guide)", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        String m = GuideActivity.src(this) + "/manifest.json";
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(m.replaceFirst("^https", "stremio")));
            i.setPackage(U.NUVIO);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Nuvio isn't installed", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------ pieces

    private void section(String title) {
        TextView t = U.text(this, title, 11.5f, accent, U.MED);
        t.setLetterSpacing(0.2f);
        list.addView(t, U.llp(U.WRAP, U.WRAP, 4, 28, 0, 10));
    }

    private LinearLayout rowBase(String title, String subtitle) {
        final LinearLayout row = U.row(this);
        row.setPadding(U.s(18), U.s(13), U.s(18), U.s(13));
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setBackground(U.round(0x0DFFFFFF, 14));
        LinearLayout texts = U.col(this);
        TextView tt = U.text(this, title, 16, 0xFFFFFFFF, U.MED);
        texts.addView(tt);
        if (!TextUtils.isEmpty(subtitle)) {
            TextView st = U.text(this, subtitle, 12, 0x99FFFFFF, U.REG);
            st.setLineSpacing(U.f(2), 1f);
            texts.addView(st, U.llp(U.WRAP, U.WRAP, 0, 5, 0, 0));
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, U.WRAP, 1f));
        list.addView(row, U.llp(U.MATCH, U.WRAP, 0, 0, 0, 8));
        return row;
    }

    private void focusStyle(final View row, final TextView value) {
        row.setOnFocusChangeListener((v, has) -> {
            v.setBackground(has ? U.ring(0x24FFFFFF, accent, 2, 14) : U.round(0x0DFFFFFF, 14));
            if (value != null) value.setTextColor(has ? 0xFFFFFFFF : 0xB3FFFFFF);
        });
    }

    private void choice(String title, final String key, final int def, final int[] values, final String[] labels) {
        final LinearLayout row = rowBase(title, null);
        final TextView val = U.text(this, "", 14, 0xB3FFFFFF, U.MED);
        row.addView(val);
        final Runnable refresh = () -> {
            int v = prefs.getInt(key, def);
            int idx = indexOf(values, v);
            val.setText((row.hasFocus() ? "‹  " : "") + labels[idx] + (row.hasFocus() ? "  ›" : ""));
        };
        refreshers.add(refresh);
        focusStyle(row, val);
        View.OnFocusChangeListener base = row.getOnFocusChangeListener();
        row.setOnFocusChangeListener((v, has) -> { base.onFocusChange(v, has); refresh.run(); });
        row.setOnKeyListener((v, code, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            int dir = 0;
            if (code == KeyEvent.KEYCODE_DPAD_RIGHT) dir = 1;
            else if (code == KeyEvent.KEYCODE_DPAD_LEFT) dir = -1;
            else if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) dir = 1;
            if (dir == 0) return false;
            int idx = indexOf(values, prefs.getInt(key, def));
            int n = (idx + dir + values.length) % values.length;
            prefs.edit().putInt(key, values[n]).apply();
            refresh.run();
            return true;
        });
        refresh.run();
    }

    private void toggle(String title, String subtitle, final String key, final boolean def) {
        final LinearLayout row = rowBase(title, subtitle);
        final View sw = new View(this);
        final View knob = new View(this);
        FrameLayout track = new FrameLayout(this);
        track.addView(sw, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
        track.addView(knob, U.flp(U.s(18), U.s(18), Gravity.CENTER_VERTICAL | Gravity.START, 3, 0, 3, 0));
        row.addView(track, U.llp(U.s(44), U.s(24), 16, 0, 0, 0));
        final Runnable refresh = () -> {
            boolean on = prefs.getBoolean(key, def);
            sw.setBackground(U.round(on ? accent : 0x40FFFFFF, 12));
            knob.setBackground(U.round(0xFFFFFFFF, 9));
            knob.setTranslationX(on ? U.f(20) : 0);
        };
        refreshers.add(refresh);
        focusStyle(row, null);
        row.setOnClickListener(v -> {
            prefs.edit().putBoolean(key, !prefs.getBoolean(key, def)).apply();
            refresh.run();
        });
        refresh.run();
    }

    interface Text { String get(); }

    private static final String TVLAUNCHER = "com.google.android.tvlauncher", TVREC = "com.google.android.tvrecommendations";

    private boolean dnsOn() {
        return "hostname".equals(Settings.Global.getString(getContentResolver(), "private_dns_mode"));
    }

    private boolean fullOff() {
        return Settings.System.getInt(getContentResolver(), "power_key_definition", 0) == 1;
    }

    private boolean fastAnim() {
        return Settings.Global.getFloat(getContentResolver(), "window_animation_scale", 1f) < 0.9f;
    }

    private boolean enabled(String pkg) {
        try {
            int st = getPackageManager().getApplicationEnabledSetting(pkg);
            return st == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT || st == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } catch (Throwable notInstalled) {
            return false;
        }
    }

    private boolean horizonIsHome() {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo r = getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
        return r != null && r.activityInfo != null && getPackageName().equals(r.activityInfo.packageName);
    }

    /** Asks first, then runs the shell command on the projector itself (through its network-debugging port). */
    private void confirmRun(String title, String message, final String cmd) {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (d, w) -> {
                    Toast.makeText(this, "Working\u2026", Toast.LENGTH_SHORT).show();
                    U.IO.execute(() -> {
                        final String out = AdbLocal.exec(getApplicationContext(), cmd);
                        U.MAIN.post(() -> {
                            if (out == null) {
                                Toast.makeText(this, "waiting".equals(AdbLocal.state)
                                        ? "Allow the network debugging prompt on screen, then try again"
                                        : "Couldn't reach the projector's debugging port. Turn on network debugging in Developer options.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
                            }
                            for (Runnable r : refreshers) r.run();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void action(String title, final Text value, final Runnable onClick) {
        final LinearLayout row = rowBase(title, null);
        final TextView val = U.text(this, "", 13, 0xB3FFFFFF, U.REG);
        val.setMaxWidth(U.s(260));
        val.setGravity(Gravity.END);
        row.addView(val, U.llp(U.WRAP, U.WRAP, 12, 0, 0, 0));
        final Runnable refresh = () -> val.setText(value == null ? "" : value.get());
        refreshers.add(refresh);
        focusStyle(row, val);
        if (onClick != null) row.setOnClickListener(v -> { onClick.run(); refresh.run(); });
        refresh.run();
    }

    private void buildModePicker() {
        LinearLayout pair = U.row(this);
        pair.setClipChildren(false);
        list.setClipChildren(false);
        final List<Runnable> paint = new ArrayList<>();
        String[] modes = {U.MODE_SCENES, U.MODE_NUVIO};
        String[] titles = {"Scenes", "Artwork"};
        String[] subs = {"XGIMI Wall videos, starting on Mars Base", "Full-screen artwork of what you're watching"};
        for (int i = 0; i < 2; i++) {
            final String m = modes[i];
            final FrameLayout card = new FrameLayout(this);
            card.setFocusable(true);
            card.setFocusableInTouchMode(true);
            Ui.rounded(card, 16);
            ImageView art = new ImageView(this);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap preview = preview(m);
            if (preview != null) art.setImageBitmap(preview);
            else art.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[] {0xFF151A3A, 0xFF3A1D3F, 0xFF8A4A2A}));
            card.addView(art, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
            card.addView(Ui.gradient(this, GradientDrawable.Orientation.BOTTOM_TOP, 0xE6000000, 0x33000000, 0x00000000),
                    new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
            LinearLayout txt = U.col(this);
            final TextView inUse = U.text(this, "✓  IN USE", 10, accent, U.MED);
            inUse.setLetterSpacing(0.18f);
            txt.addView(inUse);
            txt.addView(U.text(this, titles[i], 17, 0xFFFFFFFF, U.MED), U.llp(U.WRAP, U.WRAP, 0, 5, 0, 0));
            TextView s = U.text(this, subs[i], 11, 0xB3FFFFFF, U.REG);
            s.setMaxLines(2);
            txt.addView(s, U.llp(U.WRAP, U.WRAP, 0, 4, 0, 0));
            card.addView(txt, U.flp(U.MATCH, U.WRAP, Gravity.BOTTOM, 14, 0, 14, 12));
            final View ring = new View(this);
            card.addView(ring, new FrameLayout.LayoutParams(U.MATCH, U.MATCH));
            final Runnable p = () -> {
                boolean sel = m.equals(U.mode(this));
                boolean foc = card.hasFocus();
                inUse.setVisibility(sel ? View.VISIBLE : View.INVISIBLE);
                GradientDrawable g = new GradientDrawable();
                g.setCornerRadius(U.f(16));
                g.setColor(0);
                if (foc) g.setStroke(U.s(3), 0xFFFFFFFF);
                else if (sel) g.setStroke(U.s(2.5f), accent);
                else g.setStroke(U.s(1), 0x33FFFFFF);
                ring.setBackground(g);
            };
            paint.add(p);
            card.setOnFocusChangeListener((v, has) -> {
                v.animate().scaleX(has ? 1.04f : 1f).scaleY(has ? 1.04f : 1f).setDuration(150).start();
                for (Runnable r : paint) r.run();
            });
            card.setOnClickListener(v -> {
                prefs.edit().putString(U.P_MODE, m).apply();
                for (Runnable r : paint) r.run();
                Toast.makeText(this, titles[m.equals(U.MODE_SCENES) ? 0 : 1] + " is now your home style", Toast.LENGTH_SHORT).show();
            });
            pair.addView(card, U.llp(0, U.s(150), i == 0 ? 0 : 7, 0, i == 0 ? 7 : 0, 0));
            ((LinearLayout.LayoutParams) card.getLayoutParams()).weight = 1f;
        }
        refreshers.add(() -> { for (Runnable r : paint) r.run(); });
        for (Runnable r : paint) r.run();
        list.addView(pair, U.llp(U.MATCH, U.WRAP, 0, 0, 0, 4));
        pair.getChildAt(U.MODE_NUVIO.equals(U.mode(this)) ? 1 : 0).post(() -> pair.getChildAt(U.MODE_NUVIO.equals(U.mode(this)) ? 1 : 0).requestFocus());
    }

    private Bitmap preview(String m) {
        try {
            if (U.MODE_SCENES.equals(m)) {
                for (Scene s : Scene.scan(this)) {
                    if ("s11".equals(s.id) && s.poster != null) {
                        BitmapFactory.Options o = new BitmapFactory.Options();
                        o.inSampleSize = 4;
                        return BitmapFactory.decodeFile(s.poster.getAbsolutePath(), o);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void buildSceneList() {
        final Set<String> off = new HashSet<>(Arrays.asList(prefs.getString(U.P_SCENES_OFF, "").split(",")));
        int i = 0;
        for (final Scene s : Scene.scan(this)) {
            i++;
            final boolean has = s.video != null;
            final LinearLayout row = rowBase(s.name, has ? null : "Not copied to the projector yet");
            ImageView thumb = new ImageView(this);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Ui.rounded(thumb, 6);
            if (s.poster != null) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 16;
                thumb.setImageBitmap(BitmapFactory.decodeFile(s.poster.getAbsolutePath(), o));
            } else {
                thumb.setBackground(U.round(U.alpha(s.accent, 0.4f), 6));
            }
            row.addView(thumb, 0, U.llp(U.s(64), U.s(36), 0, 0, 14, 0));
            final TextView mark = U.text(this, "", 13, 0xFFFFFFFF, U.MED);
            row.addView(mark);
            final Runnable refresh = () -> {
                Set<String> o = new HashSet<>(Arrays.asList(prefs.getString(U.P_SCENES_OFF, "").split(",")));
                boolean on = has && !o.contains(s.id);
                mark.setText(on ? "On" : "Off");
                mark.setTextColor(on ? s.accent : 0x80FFFFFF);
            };
            refreshers.add(refresh);
            focusStyle(row, null);
            row.setOnClickListener(v -> {
                if (!has) return;
                Set<String> o = new HashSet<>(Arrays.asList(prefs.getString(U.P_SCENES_OFF, "").split(",")));
                o.remove("");
                if (o.contains(s.id)) o.remove(s.id); else o.add(s.id);
                prefs.edit().putString(U.P_SCENES_OFF, TextUtils.join(",", o)).apply();
                refresh.run();
            });
            refresh.run();
        }
    }

    private void buildNuvioStatus() {
        final LinearLayout row = rowBase("Nuvio connection", "Checking…");
        final TextView st = (TextView) ((LinearLayout) row.getChildAt(0)).getChildAt(1);
        focusStyle(row, null);
        final Runnable check = () -> U.IO.execute(() -> {
            final Nuvio.Result r = Nuvio.load(getApplicationContext(), false);
            U.MAIN.post(() -> {
                String msg;
                if ("permission".equals(r.error)) {
                    msg = "Needs permission to read TV home rows. Select to allow.";
                } else if (r.nuvioPicks + r.nuvioContinue == 0) {
                    msg = "No titles from Nuvio yet. In Nuvio's settings, switch on the Android TV home screen rows and Continue Watching.";
                } else {
                    msg = r.nuvioPicks + " titles and " + r.nuvioContinue + " in Continue Watching from Nuvio";
                }
                st.setText(msg);
            });
        });
        refreshers.add(check);
        row.setOnClickListener(v -> {
            if (checkSelfPermission("com.android.providers.tv.permission.READ_EPG_DATA") != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission("android.permission.READ_TV_LISTINGS") != PackageManager.PERMISSION_GRANTED) {
                try {
                    requestPermissions(new String[] {"com.android.providers.tv.permission.READ_EPG_DATA",
                            "android.permission.READ_TV_LISTINGS", Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                } catch (Throwable ignored) {}
            } else {
                Apps.launchPackage(this, U.NUVIO);
            }
        });
    }

    @Override public void onRequestPermissionsResult(int c, String[] p, int[] r) {
        super.onRequestPermissionsResult(c, p, r);
        for (Runnable x : refreshers) x.run();
    }

    private void editName() {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        et.setText(prefs.getString(U.P_NAME, ""));
        et.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Name for the greeting")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.edit().putString(U.P_NAME, et.getText().toString().trim()).apply();
                    for (Runnable r : refreshers) r.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String themeName() {
        String id = prefs.getString("theme", "ember");
        for (String[] t : V.THEMES) if (t[0].equals(id)) return t[1];
        return "Ember";
    }

    private void nextTheme() {
        String id = prefs.getString("theme", "ember");
        int i = 0;
        for (int k = 0; k < V.THEMES.length; k++) if (V.THEMES[k][0].equals(id)) i = k;
        prefs.edit().putString("theme", V.THEMES[(i + 1) % V.THEMES.length][0]).apply();
    }

    private int hiddenCount() {
        int n = 0;
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) if (e.getKey().startsWith("hide_")) n++;
        return n;
    }

    private void unhideAll() {
        SharedPreferences.Editor e = prefs.edit();
        for (String k : new ArrayList<>(prefs.getAll().keySet())) if (k.startsWith("hide_")) e.remove(k);
        e.apply();
        Toast.makeText(this, "All apps are back on Home", Toast.LENGTH_SHORT).show();
    }

    private void openStockHome() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(new ComponentName("com.google.android.tvlauncher", "com.google.android.tvlauncher.MainActivity"));
        if (!Apps.start(this, i)) Toast.makeText(this, "Google TV home isn't available", Toast.LENGTH_SHORT).show();
    }

    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "1.0";
        }
    }

    private static int indexOf(int[] a, int v) {
        for (int i = 0; i < a.length; i++) if (a[i] == v) return i;
        return 0;
    }
}
