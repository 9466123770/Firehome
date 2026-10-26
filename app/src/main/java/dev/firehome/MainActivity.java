package dev.firehome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    // ---- Edit these two ----
    static final String[] DOCK = {
        "com.netflix.ninja", "com.amazon.firebat", "org.smarttube.stable", "in.startv.hotstar"
    };
    static final Set<String> HIDDEN = new HashSet<>(Arrays.asList("dev.firehome"));
    // ------------------------
    static final int COLS = 5;

    static class Entry {
        String label, pkg, glyph;
        Intent intent;
        Drawable art;
        boolean banner;
        int accent = 0xFF3A3A3C;
    }

    int W, H, r = 0, c = 0, glowColor = 0xFF3A3A3C;
    float rad;
    FrameLayout root;
    GradientDrawable glow;
    TextView hello, nameTv, tagTv;
    LinearLayout shelf, content;
    ViewOutlineProvider rounded;
    List<Entry> entries = new ArrayList<>();
    List<List<Integer>> rows = new ArrayList<>();
    List<View> rowViews = new ArrayList<>();
    View[] tile, ring, label;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        W = dm.widthPixels;
        H = dm.heightPixels;
        rad = W * 0.012f;
        rounded = new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad);
            }
        };
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        Typeface thin = Typeface.create("sans-serif-thin", Typeface.NORMAL);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        // ambient glow
        View gv = new View(this);
        glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glow.setGradientRadius(W * 0.7f);
        glow.setGradientCenter(0.4f, 0.3f);
        setGlow(glowColor);
        gv.setBackground(glow);
        root.addView(gv, new FrameLayout.LayoutParams(-1, -1));

        // dock + grid
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setPadding((int) (W * 0.04f), 0, 0, 0);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2);
        cp.topMargin = (int) (H * 0.54f);
        root.addView(content, cp);

        // top shelf: focused app name
        shelf = new LinearLayout(this);
        shelf.setOrientation(LinearLayout.VERTICAL);
        shelf.setPivotX(0);
        shelf.setPivotY(0);
        nameTv = new TextView(this);
        nameTv.setTextColor(Color.WHITE);
        nameTv.setTypeface(thin);
        nameTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, W * 0.06f);
        nameTv.setSingleLine(true);
        tagTv = new TextView(this);
        tagTv.setTextColor(0xFF8E8E93);
        tagTv.setTypeface(light);
        tagTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, W * 0.016f);
        shelf.addView(nameTv);
        shelf.addView(tagTv);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(-2, -2);
        sp.leftMargin = (int) (W * 0.06f);
        sp.topMargin = (int) (H * 0.20f);
        root.addView(shelf, sp);

        // header: greeting left, clock right
        hello = new TextView(this);
        hello.setTextColor(0xFF8E8E93);
        hello.setTypeface(light);
        hello.setTextSize(TypedValue.COMPLEX_UNIT_PX, W * 0.016f);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        hp.leftMargin = (int) (W * 0.06f);
        hp.topMargin = (int) (H * 0.045f);
        root.addView(hello, hp);

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm a");
        time.setFormat24Hour("h:mm a");
        time.setTextColor(Color.WHITE);
        time.setTypeface(thin);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, W * 0.032f);
        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, d MMMM");
        date.setFormat24Hour("EEEE, d MMMM");
        date.setTextColor(0xFF8E8E93);
        date.setTypeface(light);
        date.setTextSize(TypedValue.COMPLEX_UNIT_PX, W * 0.0125f);
        right.addView(time);
        right.addView(date);
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        rp.topMargin = (int) (H * 0.03f);
        rp.rightMargin = (int) (W * 0.04f);
        root.addView(right, rp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        hello.setText(h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening");
        rebuild();
    }

    // ---------- data ----------
    List<Entry> loadEntries() {
        PackageManager pm = getPackageManager();
        Map<String, Entry> map = new LinkedHashMap<>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String p = ri.activityInfo.packageName;
                if (HIDDEN.contains(p) || map.containsKey(p)) continue;
                Entry e = new Entry();
                e.pkg = p;
                e.label = String.valueOf(ri.loadLabel(pm));
                e.intent = new Intent(Intent.ACTION_MAIN).addCategory(cat)
                        .setClassName(p, ri.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                Drawable bn = ri.activityInfo.loadBanner(pm);
                if (bn != null) { e.art = bn; e.banner = true; }
                else e.art = ri.loadIcon(pm);
                e.accent = accentOf(e.art);
                map.put(p, e);
            }
        }
        List<Entry> list = new ArrayList<>(map.values());
        Collections.sort(list, (a, b2) -> a.label.compareToIgnoreCase(b2.label));
        Entry s = new Entry();
        s.pkg = "settings";
        s.label = "Settings";
        s.glyph = "\u2699";
        s.accent = 0xFF6B7280;
        s.intent = new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        list.add(s);
        return list;
    }

    int accentOf(Drawable d) {
        int fallback = 0xFF3A3A3C;
        if (d == null) return fallback;
        try {
            Drawable.ConstantState cs = d.getConstantState();
            Drawable copy = cs != null ? cs.newDrawable().mutate() : d;
            Bitmap bm = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
            copy.setBounds(0, 0, 8, 8);
            copy.draw(new Canvas(bm));
            long sr = 0, sg = 0, sb = 0, n = 0;
            for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
                int p = bm.getPixel(x, y);
                if (Color.alpha(p) < 128) continue;
                if (Math.max(Color.red(p), Math.max(Color.green(p), Color.blue(p))) < 40) continue;
                sr += Color.red(p); sg += Color.green(p); sb += Color.blue(p); n++;
            }
            if (n == 0) return fallback;
            float[] hsv = new float[3];
            Color.colorToHSV(Color.rgb((int) (sr / n), (int) (sg / n), (int) (sb / n)), hsv);
            hsv[1] = Math.max(hsv[1], 0.55f);
            hsv[2] = Math.max(hsv[2], 0.85f);
            return Color.HSVToColor(hsv);
        } catch (Exception ex) {
            return fallback;
        }
    }

    static int shade(int color, float f) {
        return Color.rgb((int) (Color.red(color) * f), (int) (Color.green(color) * f), (int) (Color.blue(color) * f));
    }

    // ---------- views ----------
    void rebuild() {
        String prevPkg = null;
        if (r < rows.size() && c < rows.get(r).size()) prevPkg = entries.get(rows.get(r).get(c)).pkg;

        entries = loadEntries();
        rows.clear();
        rowViews.clear();
        content.removeAllViews();
        tile = new View[entries.size()];
        ring = new View[entries.size()];
        label = new View[entries.size()];

        List<Integer> dock = new ArrayList<>(), rest = new ArrayList<>();
        for (String p : DOCK)
            for (int k = 0; k < entries.size(); k++)
                if (entries.get(k).pkg.equals(p) && !dock.contains(k)) dock.add(k);
        for (int k = 0; k < entries.size(); k++) if (!dock.contains(k)) rest.add(k);
        boolean hasDock = !dock.isEmpty();
        if (hasDock) rows.add(dock);
        for (int k = 0; k < rest.size(); k += COLS)
            rows.add(new ArrayList<>(rest.subList(k, Math.min(rest.size(), k + COLS))));

        r = 0; c = 0;
        if (prevPkg != null)
            for (int rr = 0; rr < rows.size(); rr++)
                for (int cc = 0; cc < rows.get(rr).size(); cc++)
                    if (entries.get(rows.get(rr).get(cc)).pkg.equals(prevPkg)) { r = rr; c = cc; }

        for (int ri = 0; ri < rows.size(); ri++) {
            boolean isDock = hasDock && ri == 0;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setClipChildren(false);
            int w = (int) (W * (isDock ? 0.17f : 0.16f));
            if (isDock) {
                GradientDrawable rb = new GradientDrawable();
                rb.setColor(0x14FFFFFF);
                rb.setCornerRadius(W * 0.024f);
                row.setBackground(rb);
                row.setPadding((int) (W * 0.015f), (int) (H * 0.025f), (int) (W * 0.0f), (int) (H * 0.0f));
            }
            for (int idx : rows.get(ri)) row.addView(makeCell(idx, w));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.bottomMargin = (int) (H * 0.015f);
            content.addView(row, lp);
            rowViews.add(row);
        }

        content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                applyFocus(false);
            }
        });
    }

    View makeCell(int idx, int w) {
        Entry e = entries.get(idx);
        int h = w * 9 / 16;
        FrameLayout cell = new FrameLayout(this);
        cell.setClipChildren(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h + (int) (H * 0.05f));
        lp.rightMargin = (int) (W * 0.02f);
        cell.setLayoutParams(lp);

        FrameLayout t = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{shade(e.accent, 0.85f), shade(e.accent, 0.3f)});
        bg.setCornerRadius(rad);
        t.setBackground(bg);
        t.setOutlineProvider(rounded);
        t.setClipToOutline(true);

        if (e.art != null) {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(e.art);
            if (e.banner) {
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                t.addView(iv, new FrameLayout.LayoutParams(-1, -1));
            } else {
                int s = (int) (h * 0.55f);
                t.addView(iv, new FrameLayout.LayoutParams(s, s, Gravity.CENTER));
            }
        } else if (e.glyph != null) {
            TextView g = new TextView(this);
            g.setText(e.glyph);
            g.setTextColor(Color.WHITE);
            g.setGravity(Gravity.CENTER);
            g.setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.5f);
            t.addView(g, new FrameLayout.LayoutParams(-1, -1));
        }

        View rg = new View(this);
        GradientDrawable rd = new GradientDrawable();
        rd.setCornerRadius(rad);
        rd.setStroke(Math.max(4, (int) (W * 0.0035f)), Color.WHITE);
        rg.setBackground(rd);
        rg.setAlpha(0f);
        t.addView(rg, new FrameLayout.LayoutParams(-1, -1));
        cell.addView(t, new FrameLayout.LayoutParams(w, h));

        TextView lb = new TextView(this);
        lb.setText(e.label);
        lb.setTextColor(Color.WHITE);
        lb.setGravity(Gravity.CENTER);
        lb.setSingleLine(true);
        lb.setEllipsize(android.text.TextUtils.TruncateAt.END);
        lb.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        lb.setTextSize(TypedValue.COMPLEX_UNIT_PX, W * 0.0125f);
        lb.setAlpha(0f);
        FrameLayout.LayoutParams lbp = new FrameLayout.LayoutParams(w, -2);
        lbp.topMargin = h + (int) (H * 0.014f);
        cell.addView(lb, lbp);

        tile[idx] = t; ring[idx] = rg; label[idx] = lb;
        return cell;
    }

    // ---------- focus ----------
    void setGlow(int color) {
        glow.setColors(new int[]{(color & 0xFFFFFF) | 0x99000000, color & 0xFFFFFF});
    }

    void glowTo(final int target) {
        if (target == glowColor) return;
        ValueAnimator va = ValueAnimator.ofObject(new ArgbEvaluator(), glowColor, target);
        va.setDuration(500);
        va.addUpdateListener(a -> setGlow((Integer) a.getAnimatedValue()));
        va.start();
        glowColor = target;
    }

    void applyFocus(boolean anim) {
        if (rows.isEmpty()) return;
        int g = rows.get(r).get(c);
        long d = anim ? 220 : 0;
        for (int k = 0; k < entries.size(); k++) {
            boolean f = k == g;
            tile[k].animate().scaleX(f ? 1.1f : 1f).scaleY(f ? 1.1f : 1f)
                    .translationZ(f ? W * 0.02f : 0f).setDuration(d).setInterpolator(new DecelerateInterpolator()).start();
            ring[k].animate().alpha(f ? 1f : 0f).setDuration(d).start();
            label[k].animate().alpha(f ? 1f : 0f).setDuration(d).start();
        }
        Entry e = entries.get(g);
        nameTv.setText(e.label);
        tagTv.setText("Press OK to open");
        glowTo(e.accent);

        boolean down = r > 0;
        long sd = anim ? 450 : 0;
        shelf.animate().scaleX(down ? 0.4f : 1f).scaleY(down ? 0.4f : 1f)
                .translationY(down ? -H * 0.09f : 0f).setDuration(sd).setInterpolator(new DecelerateInterpolator()).start();
        float ty = r == 0 ? 0f : -H * 0.24f - rowViews.get(r).getTop();
        content.animate().translationY(ty).setDuration(sd).setInterpolator(new DecelerateInterpolator()).start();
        for (int i = 0; i < rowViews.size(); i++)
            rowViews.get(i).animate().alpha(i < r ? 0f : 1f).setDuration(sd).start();
    }

    void launch() {
        Entry e = entries.get(rows.get(r).get(c));
        try {
            startActivity(e.intent);
        } catch (Exception ex) {
            Toast.makeText(this, "Can't open " + e.label, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        if (ev.getAction() != KeyEvent.ACTION_DOWN || rows.isEmpty()) return super.dispatchKeyEvent(ev);
        switch (ev.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (c > 0) c--;
                applyFocus(true);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (c < rows.get(r).size() - 1) c++;
                applyFocus(true);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (r < rows.size() - 1) { r++; c = Math.min(c, rows.get(r).size() - 1); }
                applyFocus(true);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (r > 0) { r--; c = Math.min(c, rows.get(r).size() - 1); }
                applyFocus(true);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                launch();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (r > 0) { r = 0; c = 0; applyFocus(true); return true; }
                break;
            default:
                break;
        }
        return super.dispatchKeyEvent(ev);
    }
}
