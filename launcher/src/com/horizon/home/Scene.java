package com.horizon.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One XGIMI Wall scene, stored on the projector as sNN.mp4 (+ sNN.jpg poster). */
final class Scene {
    final String id;
    final String name;
    final int accent;
    /** How far to lift the picture (fraction of screen height) so the subject sits above the dock. */
    final float lift;
    final float zoom;
    File video;
    File poster;

    Scene(String id, String name, int accent, float lift, float zoom) {
        this.id = id;
        this.name = name;
        this.accent = accent;
        this.lift = lift;
        this.zoom = zoom;
    }

    static Scene[] all() {
        return new Scene[] {
                new Scene("s11", "Mars Base", 0xFFFFB36B, 0.122f, 1.06f),
                new Scene("s01", "Starry Sky", 0xFFA9BCFF, 0f, 1f),
                new Scene("s02", "Rain on Glass", 0xFFA9E0CC, 0f, 1f),
                new Scene("s03", "Sleeping Cat", 0xFFFFCF8A, 0f, 1f),
                new Scene("s04", "Starry Sky II", 0xFFC2B3FF, 0f, 1f),
                new Scene("s05", "Palm Beach", 0xFF5FE0D0, 0f, 1f),
                new Scene("s06", "Lakeside Camping", 0xFFFFC27A, 0f, 1f),
                new Scene("s07", "Above the Clouds", 0xFFFFB89A, 0f, 1f),
                new Scene("s08", "Underwater", 0xFF7FF6FF, 0f, 1f),
                new Scene("s10", "Ocean Waves", 0xFF8FD8FF, 0f, 1f),
        };
    }

    static List<File> dirs(Context c) {
        List<File> d = new ArrayList<>();
        d.add(new File(Environment.getExternalStorageDirectory(), "HorizonHome/scenes"));
        File own = c.getExternalFilesDir("scenes");
        if (own != null) d.add(own);
        d.add(new File(c.getFilesDir(), "scenes"));
        return d;
    }

    /** Every known scene, with files attached where they exist on disk. */
    static List<Scene> scan(Context c) {
        List<Scene> out = new ArrayList<>();
        List<File> dirs = dirs(c);
        for (Scene s : all()) {
            for (File dir : dirs) {
                File v = new File(dir, s.id + ".mp4");
                if (v.canRead() && v.length() > 1000) {
                    s.video = v;
                    File p = new File(dir, s.id + ".jpg");
                    s.poster = p.canRead() ? p : null;
                    break;
                }
            }
            out.add(s);
        }
        return out;
    }

    /** Scenes that have a video and are switched on, Mars first. */
    static List<Scene> rotation(Context c) {
        SharedPreferences p = U.prefs(c);
        Set<String> off = new HashSet<>(Arrays.asList(p.getString(U.P_SCENES_OFF, "").split(",")));
        List<Scene> out = new ArrayList<>();
        for (Scene s : scan(c)) {
            if (s.video != null && !off.contains(s.id)) out.add(s);
        }
        if (out.isEmpty()) {
            for (Scene s : scan(c)) if (s.video != null) out.add(s);
        }
        return out;
    }

    float liftFor(boolean enabled) { return enabled ? lift : 0f; }
    float zoomFor(boolean enabled) { return enabled ? zoom : 1f; }
}
