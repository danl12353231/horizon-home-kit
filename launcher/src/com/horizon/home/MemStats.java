package com.horizon.home;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What is using the projector's memory, from "dumpsys meminfo" (needs the DUMP permission,
 * granted once over adb). Processes of one app are added together. Cached for 20 s.
 */
final class MemStats {
    static final class App {
        String pkg, label;
        long kb;
        boolean system, self;
    }

    static final class Stats {
        long totalKb, availKb;
        final List<App> apps = new ArrayList<>();
        boolean noPermission;
    }

    private static Stats cache;
    private static long cacheAt;

    static Stats cached() { return cache; }

    static synchronized Stats load(Context c, boolean force) {
        if (!force && cache != null && System.currentTimeMillis() - cacheAt < 20000) return cache;
        Stats s = new Stats();
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        s.totalKb = mi.totalMem / 1024;
        s.availKb = mi.availMem / 1024;
        Map<String, Long> by = new LinkedHashMap<>();
        String text = null;
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"dumpsys", "meminfo"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            p.destroy();
            text = sb.toString();
        } catch (Throwable ignored) {}
        parse(text, by);
        if (by.isEmpty()) {
            viaAdb = true;
            parse(AdbLocal.exec(c, "dumpsys meminfo"), by);
        } else {
            viaAdb = false;
        }
        if (by.isEmpty()) s.noPermission = true;
        PackageManager pm = c.getPackageManager();
        for (Map.Entry<String, Long> e : by.entrySet()) {
            App a = new App();
            a.pkg = e.getKey();
            a.kb = e.getValue();
            a.self = a.pkg.equals(c.getPackageName());
            a.label = a.pkg;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(a.pkg, 0);
                a.label = String.valueOf(pm.getApplicationLabel(ai));
                a.system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && pm.getLaunchIntentForPackage(a.pkg) == null
                        && pm.getLeanbackLaunchIntentForPackage(a.pkg) == null;
            } catch (Throwable notAnApp) {
                a.system = true; // system_server, surfaceflinger and other native processes
                if ("system".equals(a.pkg)) a.label = "Android system";
            }
            s.apps.add(a);
        }
        Collections.sort(s.apps, (x, y) -> Long.compare(y.kb, x.kb));
        cache = s;
        cacheAt = System.currentTimeMillis();
        return s;
    }

    /** Closes background processes of apps you aren't using (never the home screen or Android itself). */
    static long freeUp(Context c) {
        Stats before = load(c, true);
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        for (App a : before.apps) {
            if (a.self || a.pkg.indexOf('.') < 0) continue;
            if (a.pkg.startsWith("com.android.systemui") || a.pkg.equals("android")) continue;
            try { am.killBackgroundProcesses(a.pkg); } catch (Throwable ignored) {}
        }
        if (viaAdb) {
            StringBuilder cmd = new StringBuilder();
            for (App a : before.apps) {
                if (a.self || a.system || a.pkg.indexOf('.') < 0) continue;
                if (cmd.length() > 0) cmd.append(';');
                cmd.append("am kill ").append(a.pkg);
            }
            if (cmd.length() > 0) AdbLocal.exec(c, cmd.toString());
        }
        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
        Stats after = load(c, true);
        return Math.max(0, after.availKb - before.availKb);
    }

    static boolean viaAdb;

    private static void parse(String text, Map<String, Long> by) {
        if (text == null) return;
        boolean in = false;
        for (String line : text.split("\n")) {
            if (line.startsWith("Total PSS by process")) { in = true; continue; }
            if (!in) continue;
            String t = line.trim();
            if (t.isEmpty()) break;
            int colon = t.indexOf("K: ");
            if (colon < 0) continue;
            long kb;
            try { kb = Long.parseLong(t.substring(0, colon).replace(",", "").trim()); } catch (Throwable e) { continue; }
            String name = t.substring(colon + 3);
            int sp = name.indexOf(" (pid");
            if (sp > 0) name = name.substring(0, sp);
            int sub = name.indexOf(':');
            if (sub > 0) name = name.substring(0, sub);
            Long prev = by.get(name);
            by.put(name, (prev == null ? 0 : prev) + kb);
        }
    }

    static String mb(long kb) {
        if (kb >= 1024 * 1024) return String.format(java.util.Locale.US, "%.1f GB", kb / 1048576f);
        return (kb / 1024) + " MB";
    }
}
