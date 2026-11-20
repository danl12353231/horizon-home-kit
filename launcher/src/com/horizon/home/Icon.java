package com.horizon.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Line icons drawn in code on a 24x24 grid, so the APK needs no icon assets. */
final class Icon extends View {
    static final int WIFI = 1, WIFI_OFF = 2, ETHERNET = 3, HDMI = 4, GEAR = 5, SLIDERS = 6, WALL = 7,
            SEARCH = 8, PLAY = 9, INFO = 10, PREV = 11, NEXT = 12, EYE = 13, APPS = 14, CHECK = 15, NUVIO = 16,
            SUN = 17, MOON = 18, CLOUD = 19, RAIN = 20, SHIELD = 21, SHIELD_OFF = 22, CHEVRON_DOWN = 23, GRID4 = 24, ORBIT = 25;

    private int type;
    private int level = 4;
    private int color = 0xFFFFFFFF;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();

    Icon(Context c, int type) {
        super(c);
        this.type = type;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
    }

    void setType(int t) { type = t; invalidate(); }
    void setLevel(int l) { level = l; invalidate(); }
    void setColor(int c) { color = c; invalidate(); }

    @Override protected void onDraw(Canvas c) {
        float sz = Math.min(getWidth(), getHeight());
        float u = sz / 24f;
        c.save();
        c.translate((getWidth() - sz) / 2f, (getHeight() - sz) / 2f);
        c.scale(u, u);
        p.setStrokeWidth(1.8f);
        p.setColor(color);
        fill.setColor(color);
        switch (type) {
            case WIFI: case WIFI_OFF: {
                int dim = U.alpha(color, 0.28f);
                for (int i = 0; i < 3; i++) {
                    float rad = 4f + i * 4.2f;
                    r.set(12 - rad, 19 - rad, 12 + rad, 19 + rad);
                    p.setColor(level > i + 1 && type == WIFI ? color : dim);
                    c.drawArc(r, 225, 90, false, p);
                }
                fill.setColor(level > 0 && type == WIFI ? color : dim);
                c.drawCircle(12, 19, 1.6f, fill);
                if (type == WIFI_OFF) { p.setColor(color); c.drawLine(4, 4, 20, 20, p); }
                break;
            }
            case ETHERNET:
                r.set(4, 6, 20, 18);
                c.drawRoundRect(r, 2, 2, p);
                c.drawLine(8, 18, 8, 14, p); c.drawLine(12, 18, 12, 14, p); c.drawLine(16, 18, 16, 14, p);
                break;
            case HDMI:
                path.reset();
                path.moveTo(3, 8); path.lineTo(21, 8); path.lineTo(21, 13); path.lineTo(18, 16);
                path.lineTo(6, 16); path.lineTo(3, 13); path.close();
                c.drawPath(path, p);
                c.drawLine(7, 11.5f, 17, 11.5f, p);
                break;
            case GEAR: {
                c.drawCircle(12, 12, 3.2f, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    float x1 = 12 + (float) Math.cos(a) * 6.4f, y1 = 12 + (float) Math.sin(a) * 6.4f;
                    float x2 = 12 + (float) Math.cos(a) * 9f, y2 = 12 + (float) Math.sin(a) * 9f;
                    c.drawLine(x1, y1, x2, y2, p);
                }
                c.drawCircle(12, 12, 6.4f, p);
                break;
            }
            case SLIDERS:
                c.drawLine(4, 7, 20, 7, p); c.drawLine(4, 12, 20, 12, p); c.drawLine(4, 17, 20, 17, p);
                fill.setColor(color);
                c.drawCircle(15, 7, 2.3f, fill); c.drawCircle(8, 12, 2.3f, fill); c.drawCircle(13, 17, 2.3f, fill);
                break;
            case WALL:
                r.set(3, 5, 21, 19);
                c.drawRoundRect(r, 2.5f, 2.5f, p);
                path.reset();
                path.moveTo(5, 17); path.lineTo(10, 11); path.lineTo(13.5f, 14.5f); path.lineTo(15.5f, 12.5f); path.lineTo(19, 17);
                c.drawPath(path, p);
                c.drawCircle(16, 9, 1.4f, fill);
                break;
            case SEARCH:
                c.drawCircle(10.5f, 10.5f, 6, p);
                c.drawLine(15, 15, 20, 20, p);
                break;
            case PLAY:
                path.reset();
                path.moveTo(7, 4.5f); path.lineTo(19.5f, 12); path.lineTo(7, 19.5f); path.close();
                c.drawPath(path, fill);
                break;
            case INFO:
                c.drawCircle(12, 12, 9, p);
                c.drawLine(12, 11, 12, 16.5f, p);
                c.drawCircle(12, 7.6f, 1.1f, fill);
                break;
            case PREV:
                path.reset(); path.moveTo(14.5f, 6); path.lineTo(8.5f, 12); path.lineTo(14.5f, 18);
                c.drawPath(path, p);
                break;
            case NEXT:
                path.reset(); path.moveTo(9.5f, 6); path.lineTo(15.5f, 12); path.lineTo(9.5f, 18);
                c.drawPath(path, p);
                break;
            case EYE:
                path.reset();
                path.moveTo(2.5f, 12); path.quadTo(12, 3, 21.5f, 12); path.quadTo(12, 21, 2.5f, 12); path.close();
                c.drawPath(path, p);
                c.drawCircle(12, 12, 3, p);
                break;
            case APPS:
                for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) c.drawCircle(6 + x * 6, 6 + y * 6, 1.7f, fill);
                break;
            case CHECK:
                path.reset(); path.moveTo(5, 12.5f); path.lineTo(10, 17.5f); path.lineTo(19, 7);
                c.drawPath(path, p);
                break;
            case SUN:
                c.drawCircle(12, 12, 4, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    c.drawLine(12 + (float) Math.cos(a) * 7, 12 + (float) Math.sin(a) * 7, 12 + (float) Math.cos(a) * 9.5f, 12 + (float) Math.sin(a) * 9.5f, p);
                }
                break;
            case MOON:
                path.reset();
                path.moveTo(20.5f, 13.5f);
                path.cubicTo(19.5f, 18f, 15f, 21f, 10.5f, 20.5f);
                path.cubicTo(5f, 19.8f, 2f, 14f, 3.8f, 9f);
                path.cubicTo(5f, 5.6f, 8f, 3.4f, 11.2f, 3f);
                path.cubicTo(8.5f, 6.5f, 9.5f, 12f, 13.5f, 14.5f);
                path.cubicTo(15.8f, 15.8f, 18.5f, 15.6f, 20.5f, 13.5f);
                c.drawPath(path, p);
                break;
            case CLOUD: case RAIN:
                path.reset();
                path.moveTo(7, 17); path.cubicTo(3.5f, 17, 3, 12, 6.5f, 11.2f);
                path.cubicTo(7, 6.5f, 13.5f, 5.5f, 15.2f, 9.6f);
                path.cubicTo(19.5f, 9.2f, 21.5f, 14.5f, 18, 17); path.close();
                c.drawPath(path, p);
                if (type == RAIN) { c.drawLine(8, 19.5f, 7.2f, 21.5f, p); c.drawLine(12, 19.5f, 11.2f, 21.5f, p); c.drawLine(16, 19.5f, 15.2f, 21.5f, p); }
                break;
            case SHIELD: case SHIELD_OFF:
                path.reset();
                path.moveTo(12, 3); path.lineTo(19, 6); path.lineTo(19, 12);
                path.cubicTo(19, 16.5f, 16, 19.5f, 12, 21); path.cubicTo(8, 19.5f, 5, 16.5f, 5, 12);
                path.lineTo(5, 6); path.close();
                c.drawPath(path, p);
                if (type == SHIELD) { path.reset(); path.moveTo(9, 12); path.lineTo(11, 14); path.lineTo(15, 10); c.drawPath(path, p); }
                else c.drawLine(4, 4, 20, 20, p);
                break;
            case CHEVRON_DOWN:
                path.reset(); path.moveTo(6, 9); path.lineTo(12, 15); path.lineTo(18, 9);
                c.drawPath(path, p);
                break;
            case GRID4:
                c.drawCircle(7, 7, 2.2f, fill); c.drawCircle(17, 7, 2.2f, fill);
                c.drawCircle(7, 17, 2.2f, fill); c.drawCircle(17, 17, 2.2f, fill);
                break;
            case ORBIT:
                c.drawCircle(12, 12, 4.2f, fill);
                r.set(2.5f, 8.5f, 21.5f, 15.5f);
                c.save(); c.rotate(-24, 12, 12); c.drawOval(r, p); c.restore();
                break;
            case NUVIO:
                c.drawCircle(12, 12, 9, p);
                path.reset();
                path.moveTo(10, 8.5f); path.lineTo(16, 12); path.lineTo(10, 15.5f); path.close();
                c.drawPath(path, fill);
                break;
        }
        c.restore();
    }
}
