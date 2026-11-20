package com.horizon.home;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads the rows Nuvio publishes for the Android TV home screen (its channels and its Continue
 * Watching list) through the system TV provider, and turns them into hero-wall items.
 */
final class Nuvio {
    private static final String TAG = "HorizonNuvio";

    static final class Item {
        String pkg, title, episode, desc, longDesc, poster, thumb, logo, intentUri, channel;
        String genre, rating, year, contentRating;
        String type, imdbId, posterTall, runtime;
        long tmdbId;
        long durationMs, positionMs, engaged;
        float posterAspect;
        boolean watchNext;

        boolean isNuvio() { return U.NUVIO.equals(pkg); }

        String art() { return !TextUtils.isEmpty(poster) ? poster : thumb; }

        String cardArt() { return !TextUtils.isEmpty(thumb) ? thumb : poster; }

        String key() { return pkg + "|" + title + "|" + episode; }

        String meta() {
            List<String> parts = new ArrayList<>();
            if (!TextUtils.isEmpty(year)) parts.add(year);
            if (!TextUtils.isEmpty(genre)) parts.add(genre);
            if (durationMs > 60000) parts.add(duration(durationMs));
            else if (!TextUtils.isEmpty(runtime)) parts.add(runtime);
            if (!TextUtils.isEmpty(contentRating)) parts.add(contentRating);
            if (!TextUtils.isEmpty(rating)) parts.add(rating);
            return TextUtils.join("  ·  ", parts);
        }

        float progress() {
            if (durationMs <= 0 || positionMs <= 0) return 0f;
            return Math.min(1f, positionMs / (float) durationMs);
        }
    }

    static final class Result {
        final List<Item> picks = new ArrayList<>();
        final List<Item> continueWatching = new ArrayList<>();
        int nuvioPicks, nuvioContinue, otherPicks;
        String error;
        final Set<String> channels = new HashSet<>();
    }

    private Nuvio() {}

    static Result load(Context c, boolean fillFromOthers) {
        Result r = new Result();
        Map<Long, String> channelNames = new HashMap<>();
        Cursor ch = null;
        try {
            ch = c.getContentResolver().query(TvContract.Channels.CONTENT_URI,
                    new String[] {TvContract.Channels._ID, TvContract.Channels.COLUMN_DISPLAY_NAME}, null, null, null);
            if (ch != null) while (ch.moveToNext()) channelNames.put(ch.getLong(0), ch.getString(1));
        } catch (Throwable t) {
            Log.w(TAG, "channels", t);
        } finally {
            if (ch != null) ch.close();
        }

        List<Item> nuvio = new ArrayList<>(), others = new ArrayList<>();
        Cursor cur = null;
        try {
            cur = c.getContentResolver().query(TvContract.PreviewPrograms.CONTENT_URI, null, null, null, null);
            if (cur != null) {
                while (cur.moveToNext()) {
                    Item it = read(cur, false);
                    if (it == null || TextUtils.isEmpty(it.art())) continue;
                    long cid = getLong(cur, "channel_id");
                    it.channel = channelNames.get(cid);
                    if (it.channel != null) r.channels.add(it.channel);
                    if (it.isNuvio()) nuvio.add(it); else others.add(it);
                }
            }
        } catch (SecurityException se) {
            r.error = "permission";
            Log.w(TAG, "preview programs: no permission", se);
        } catch (Throwable t) {
            r.error = t.getClass().getSimpleName();
            Log.w(TAG, "preview programs", t);
        } finally {
            if (cur != null) cur.close();
        }

        List<Item> nuvioNext = new ArrayList<>(), otherNext = new ArrayList<>();
        try {
            cur = c.getContentResolver().query(TvContract.WatchNextPrograms.CONTENT_URI, null, null, null, null);
            if (cur != null) {
                while (cur.moveToNext()) {
                    Item it = read(cur, true);
                    if (it == null) continue;
                    it.engaged = getLong(cur, "last_engagement_time_utc_millis");
                    if (it.isNuvio()) nuvioNext.add(it); else otherNext.add(it);
                }
            }
        } catch (SecurityException se) {
            if (r.error == null) r.error = "permission";
        } catch (Throwable t) {
            Log.w(TAG, "watch next", t);
        } finally {
            if (cur != null) cur.close();
        }
        Collections.sort(nuvioNext, (a, b) -> Long.compare(b.engaged, a.engaged));
        Collections.sort(otherNext, (a, b) -> Long.compare(b.engaged, a.engaged));

        dedupe(nuvio);
        r.picks.addAll(nuvio);
        r.nuvioPicks = nuvio.size();
        r.nuvioContinue = nuvioNext.size();
        r.continueWatching.addAll(nuvioNext);
        if (fillFromOthers) {
            if (r.picks.size() < 6) {
                dedupe(others);
                for (Item it : others) {
                    if (r.picks.size() >= 24) break;
                    r.picks.add(it);
                    r.otherPicks++;
                }
            }
            r.continueWatching.addAll(otherNext);
        }
        return r;
    }

    private static void dedupe(List<Item> list) {
        Set<String> seen = new HashSet<>();
        List<Item> out = new ArrayList<>();
        for (Item it : list) if (seen.add(it.key())) out.add(it);
        list.clear();
        list.addAll(out);
    }

    private static Item read(Cursor c, boolean watchNext) {
        Item it = new Item();
        it.watchNext = watchNext;
        it.pkg = get(c, "package_name");
        it.title = get(c, "title");
        if (TextUtils.isEmpty(it.title)) return null;
        String ep = get(c, "episode_title");
        String sn = get(c, "season_display_number"), en = get(c, "episode_display_number");
        if (!TextUtils.isEmpty(sn) && !TextUtils.isEmpty(en)) {
            it.episode = "S" + sn + " E" + en + (TextUtils.isEmpty(ep) ? "" : "  " + ep);
        } else {
            it.episode = ep;
        }
        it.desc = get(c, "short_description");
        it.longDesc = get(c, "long_description");
        it.poster = get(c, "poster_art_uri");
        it.thumb = get(c, "thumbnail_uri");
        it.logo = get(c, "logo_uri");
        it.intentUri = get(c, "intent_uri");
        it.durationMs = getLong(c, "duration_millis");
        it.positionMs = getLong(c, "last_playback_position_millis");
        it.posterAspect = getLong(c, "poster_art_aspect_ratio");
        String rd = get(c, "release_date");
        if (rd != null && rd.length() >= 4) it.year = rd.substring(0, 4);
        String g = get(c, "canonical_genre");
        if (!TextUtils.isEmpty(g)) {
            try {
                String[] gs = TvContract.Programs.Genres.decode(g);
                List<String> nice = new ArrayList<>();
                for (String s : gs) {
                    if (nice.size() >= 2) break;
                    String t = s.replace('_', ' ').toLowerCase(Locale.ROOT);
                    if (t.isEmpty()) continue;
                    nice.add(Character.toUpperCase(t.charAt(0)) + t.substring(1));
                }
                it.genre = TextUtils.join(", ", nice);
            } catch (Throwable ignored) {}
        }
        String cr = get(c, "content_rating");
        if (!TextUtils.isEmpty(cr)) {
            String last = cr.substring(cr.lastIndexOf('/') + 1);
            int us = last.lastIndexOf('_');
            it.contentRating = us >= 0 ? last.substring(us + 1) : last;
            if (it.contentRating.contains(",")) it.contentRating = null;
        }
        String rv = get(c, "review_rating");
        if (!TextUtils.isEmpty(rv)) {
            long style = getLong(c, "review_rating_style");
            // 0 = stars, 1 = thumbs, 2 = percentage
            if (style == 2) it.rating = rv + "%";
            else if (style == 0) it.rating = "★ " + rv;
        }
        return it;
    }

    private static String get(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0) return null;
        try { return c.getString(i); } catch (Throwable t) { return null; }
    }

    private static long getLong(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0) return 0;
        try { return c.isNull(i) ? 0 : c.getLong(i); } catch (Throwable t) { return 0; }
    }

    static String duration(long ms) {
        long m = ms / 60000;
        if (m < 60) return m + "m";
        return (m / 60) + "h " + (m % 60) + "m";
    }

    /** Open the title in its app (Nuvio), falling back to just opening the app. */
    static boolean open(Context c, Item it) {
        if (it != null && it.imdbId != null) return Streaming.open(c, it);
        if (it != null && !TextUtils.isEmpty(it.intentUri)) {
            try {
                Intent i = Intent.parseUri(it.intentUri, Intent.URI_INTENT_SCHEME);
                if (i.getPackage() == null && i.getComponent() == null && it.pkg != null) {
                    // Keep plain links inside the app that published them.
                    Intent probe = new Intent(i).setPackage(it.pkg);
                    if (!c.getPackageManager().queryIntentActivities(probe, 0).isEmpty()) i = probe;
                }
                if (Apps.start(c, i)) {
                    Apps.noteLaunch(c, it.pkg);
                    return true;
                }
            } catch (Throwable t) {
                Log.w(TAG, "open", t);
            }
            try {
                Intent v = new Intent(Intent.ACTION_VIEW, Uri.parse(it.intentUri));
                if (it.pkg != null) v.setPackage(it.pkg);
                if (Apps.start(c, v)) return true;
            } catch (Throwable ignored) {}
        }
        return Apps.launchPackage(c, it != null && it.pkg != null ? it.pkg : U.NUVIO);
    }
}
