package com.horizon.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Makes the remote's Home button open Horizon Home (after Veil's HomeButtonService). It only looks at
 * the Home key and at which app's window comes to the front; it never reads screen content.
 */
public class HomeButtonService extends AccessibilityService {
    @Override protected void onServiceConnected() {
        android.util.Log.i("HorizonHomeKey", "connected");
    }

    @Override public boolean onKeyEvent(KeyEvent e) {
        android.util.Log.i("HorizonHomeKey", "key " + e.getKeyCode() + " action " + e.getAction());
        if (e.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (e.getAction() == KeyEvent.ACTION_UP && !e.isCanceled()) goHome();
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence p = e.getPackageName(), c = e.getClassName();
        if (p == null || c == null) return;
        String pkg = p.toString(), cls = c.toString();
        android.util.Log.i("HorizonHomeKey", "window " + pkg + " / " + cls);
        // Only the stock home *screen* itself, not its other screens.
        if ((pkg.equals("com.google.android.tvlauncher") && cls.endsWith("MainActivity"))
                || (pkg.equals("com.google.android.apps.tv.launcherx") && cls.endsWith("HomeActivity"))) {
            goHome();
        }
    }

    @Override public void onInterrupt() {}

    private void goHome() {
        try {
            startActivity(new Intent(this, HomeActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (Throwable ignored) {
            // never crash; the stock home stays as a fallback
        }
    }
}
