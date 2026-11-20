package com.rpglauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;

public final class MainActivity extends Activity {
    private SharedPreferences save;
    private Archetype hero;
    private LinearLayout root;
    private ScrollView scroll;
    private final List<App> apps = new ArrayList<>();
    private String page = "Home";
    private static final int TEXT = 0xfff3f0e9;
    private static final int MUTED = 0xffcbc6cb;
    private static final int DAILY_APP_CAP = 5;
    private static final int QUEST_TARGET = 3;

    private static final class App {
        final String name;
        final ComponentName component;
        App(String name, ComponentName component) { this.name = name; this.component = component; }
        String id() { return component.flattenToString(); }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        save = getSharedPreferences("adventure", MODE_PRIVATE);
        hero = Archetype.valueOf(save.getString("class", "WARDEN"));
        page = state != null ? state.getString("page", "Home") : save.contains("class") ? "Home" : "Classes";
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("page", page);
    }

    @Override protected void onResume() {
        super.onResume();
        refreshApps();
        show(page);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasCategory(Intent.CATEGORY_HOME)) show("Home");
    }

    @Override public void onBackPressed() {
        if (page.equals("Appearance")) show("Classes");
        else if (!page.equals("Home")) show("Home");
        else super.onBackPressed();
    }

    private void refreshApps() {
        apps.clear();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo item : getPackageManager().queryIntentActivities(query, 0)) {
            if (!item.activityInfo.packageName.equals(getPackageName())) {
                apps.add(new App(item.loadLabel(getPackageManager()).toString(),
                    new ComponentName(item.activityInfo.packageName, item.activityInfo.name)));
            }
        }
        apps.sort(Comparator.comparing(a -> a.name, String.CASE_INSENSITIVE_ORDER));
    }

    private String key(String suffix) { return hero.name() + "." + suffix; }
    private String today() { return LocalDate.now().toString(); }
    private int xp() { return save.getInt(key("xp"), 0); }
    private int mask() { return save.getInt(key("skills"), 0); }
    private boolean unlocked(int i) { return (mask() & (1 << i)) != 0; }
    private Set<String> visited(String date) {
        if (!date.equals(save.getString(key("date"), ""))) return new HashSet<>();
        return new HashSet<>(save.getStringSet(key("visited"), Collections.emptySet()));
    }

    private void launch(App app) {
        try {
            startActivity(Intent.makeMainActivity(app.component));
        } catch (android.content.ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "This app is no longer available", Toast.LENGTH_SHORT).show();
            refreshApps();
            show(page);
            return;
        }
        String date = today();
        Set<String> used = visited(date);
        SharedPreferences.Editor edit = save.edit().putString(key("last"), app.id());
        if (used.size() < DAILY_APP_CAP && used.add(app.component.getPackageName())) {
            edit.putString(key("date"), date).putStringSet(key("visited"), used).putInt(key("xp"), xp() + 5);
        }
        edit.apply();
    }

    private static final int FONT_REQUEST = 41;

    private int background() { return save.getInt(key("background"), hero.background); }
    private int foreground() { return android.graphics.Color.luminance(background()) > 0.25 ? 0xff19171a : TEXT; }
    private int accent() { return save.getInt(key("accent"), hero.accent); }
    private Typeface appearanceFont;

    private Typeface loadFont() {
        String font = save.getString(key("font"), hero.font);
        if (font.equals("custom")) {
            try { return Typeface.createFromFile(new java.io.File(getFilesDir(), key("typeface"))); }
            catch (RuntimeException e) { font = hero.font; }
        }
        return Typeface.create(font, Typeface.NORMAL);
    }

    private void appearance() {
        text(root, "Changes apply only to " + hero.title + ".", 16, MUTED);
        row(root, "Accent color", () -> editColor("Accent color", "accent", accent()));
        row(root, "Background color", () -> editColor("Background color", "background", background()));
        row(root, "Font family", () -> {
            String[] labels = {"Class default", "Sans serif", "Serif", "Monospace", "Condensed", "Import TTF or OTF"};
            String[] families = {hero.font, "sans-serif", "serif", "monospace", "sans-serif-condensed"};
            new AlertDialog.Builder(this).setTitle("Font family").setItems(labels, (dialog, i) -> {
                if (i < families.length) {
                    save.edit().putString(key("font"), families[i]).apply();
                    show("Appearance");
                } else {
                    try {
                        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), FONT_REQUEST);
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show();
                    }
                }
            }).show();
        });
        toggle("Compact spacing", "compact", false);
        toggle("Show class description on Home", "description", true);
        toggle("Show XP on Home", "showXp", true);
        row(root, "Reset this class's appearance", () -> {
            save.edit().remove(key("accent")).remove(key("background")).remove(key("font"))
                .remove(key("compact")).remove(key("description")).remove(key("showXp")).apply();
            new java.io.File(getFilesDir(), key("typeface")).delete();
            show("Appearance");
        });
    }

    private void toggle(String label, String setting, boolean defaultValue) {
        Switch control = new Switch(this);
        control.setText(label);
        control.setTextColor(foreground());
        control.setTypeface(appearanceFont);
        control.setMinHeight(dp(48));
        control.setChecked(save.getBoolean(key(setting), defaultValue));
        root.addView(control);
        control.setOnCheckedChangeListener((button, checked) -> save.edit().putBoolean(key(setting), checked).apply());
    }

    private void editColor(String title, String setting, int current) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(current, hsv);
        LinearLayout picker = column();
        picker.setPadding(dp(24), dp(8), dp(24), dp(16));
        TextView preview = new TextView(this);
        preview.setText("Color preview");
        preview.setTextSize(16);
        preview.setGravity(android.view.Gravity.CENTER);
        picker.addView(preview, new LinearLayout.LayoutParams(-1, dp(56)));

        String[] labels = {"Hue", "Saturation", "Brightness"};
        int[] limits = {359, 100, 100};
        View[] tracks = new View[3];
        Runnable update = () -> {
            int color = android.graphics.Color.HSVToColor(hsv);
            preview.setBackground(surface(color, 12));
            preview.setTextColor(android.graphics.Color.luminance(color) > 0.18f ? 0xff000000 : 0xffffffff);
            int[][] palettes = {
                {0xffff0000, 0xffffff00, 0xff00ff00, 0xff00ffff, 0xff0000ff, 0xffff00ff, 0xffff0000},
                {0xffffffff, android.graphics.Color.HSVToColor(new float[]{hsv[0], 1, 1})},
                {0xff000000, android.graphics.Color.HSVToColor(new float[]{hsv[0], hsv[1], 1})}
            };
            for (int i = 0; i < tracks.length; i++) {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, palettes[i]);
                gradient.setCornerRadius(dp(4));
                tracks[i].setBackground(gradient);
            }
        };
        for (int i = 0; i < labels.length; i++) {
            final int channel = i;
            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setPadding(0, dp(12), 0, dp(4));
            picker.addView(label);
            tracks[i] = new View(this);
            picker.addView(tracks[i], new LinearLayout.LayoutParams(-1, dp(8)));
            SeekBar slider = new SeekBar(this);
            slider.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            slider.setContentDescription(labels[i]);
            slider.setMax(limits[i]);
            slider.setProgress(Math.round(hsv[i] * (i == 0 ? 1 : 100)));
            picker.addView(slider, new LinearLayout.LayoutParams(-1, dp(48)));
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    hsv[channel] = channel == 0 ? progress : progress / 100f;
                    update.run();
                }
                @Override public void onStartTrackingTouch(SeekBar bar) {}
                @Override public void onStopTrackingTouch(SeekBar bar) {}
            });
        }
        update.run();
        ScrollView content = new ScrollView(this);
        content.addView(picker);
        new AlertDialog.Builder(this).setTitle(title).setView(content)
            .setNegativeButton("Cancel", null).setPositiveButton("Apply", (dialog, which) -> {
                save.edit().putInt(key(setting), android.graphics.Color.HSVToColor(hsv)).apply();
                show("Appearance");
            }).show();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != FONT_REQUEST || result != RESULT_OK || data == null || data.getData() == null) return;
        java.io.File pending = null;
        try {
            pending = java.io.File.createTempFile("font-", ".tmp", getFilesDir());
            try (java.io.InputStream in = getContentResolver().openInputStream(data.getData());
                 java.io.OutputStream out = new java.io.FileOutputStream(pending)) {
                if (in == null) throw new java.io.IOException("No font stream");
                byte[] buffer = new byte[8192];
                int total = 0;
                for (int read; (read = in.read(buffer)) != -1;) {
                    total += read;
                    if (total > 10 * 1024 * 1024) throw new java.io.IOException("Font too large");
                    out.write(buffer, 0, read);
                }
            }
            if (new Typeface.Builder(pending).build() == null) throw new java.io.IOException("Invalid font");
            java.nio.file.Files.move(pending.toPath(), new java.io.File(getFilesDir(), key("typeface")).toPath(),
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            save.edit().putString(key("font"), "custom").apply();
        } catch (java.io.IOException | RuntimeException e) {
            Toast.makeText(this, "Choose a valid TTF or OTF font under 10 MB", Toast.LENGTH_LONG).show();
        } finally {
            if (pending != null) pending.delete();
        }
        show("Appearance");
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }
    private GradientDrawable surface(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }
    private TextView text(LinearLayout parent, String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color == TEXT ? foreground() : color == MUTED && foreground() != TEXT ? 0xff39353b : color);
        view.setTypeface(appearanceFont);
        view.setPadding(0, dp(5), 0, dp(5));
        parent.addView(view);
        return view;
    }
    private Button action(String label, Runnable run) {
        Button view = new Button(this);
        view.setAllCaps(false);
        view.setText(label);
        view.setTextColor(TEXT);
        view.setTextSize(14);
        view.setTypeface(appearanceFont);
        view.setMinHeight(dp(48));
        view.setOnClickListener(v -> run.run());
        return view;
    }
    private Button row(LinearLayout parent, String label, Runnable run) {
        Button view = action(label, run);
        parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }
    private void show(String target) {
        page = target;
        appearanceFont = loadFont();
        int inset = save.getBoolean(key("compact"), false) ? 12 : 20;
        getWindow().setStatusBarColor(background());
        getWindow().setNavigationBarColor(background());
        LinearLayout frame = column();
        frame.setBackgroundColor(background());
        scroll = new ScrollView(this);
        root = column();
        root.setPadding(dp(inset), dp(inset + 4), dp(inset), dp(16));
        scroll.addView(root);
        frame.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = new LinearLayout(this);
        String[] pages = {"Home", "Apps", "Skills", "Quest", "Classes"};
        for (String name : pages) {
            Button tab = action(name, () -> show(name));
            tab.setPadding(0, 0, 0, 0);
            tab.setMinWidth(0);
            tab.setBackground(surface((name.equals(page) || (page.equals("Appearance") && name.equals("Classes"))) ? 0x26ffffff : background(), 0));
            tab.setTextColor(name.equals(page) ? accent() : foreground());
            tab.setSelected(name.equals(page));
            nav.addView(tab, new LinearLayout.LayoutParams(0, dp(56), 1));
        }
        frame.addView(nav);
        setContentView(frame);
        text(root, "RPG LAUNCHER  /  " + hero.title.toUpperCase(Locale.ROOT), 12, accent());
        text(root, page.equals("Home") ? hero.location : page, 30, foreground());
        if (!page.equals("Home") || (save.getBoolean(key("showXp"), true)
                && !(hero == Archetype.WARDEN && unlocked(2)))) {
            text(root, "Level " + (1 + xp() / 25) + "  ·  " + xp() + " XP", 15, MUTED);
        }
        switch (page) {
            case "Home": home(); break;
            case "Apps": appList(true); break;
            case "Skills": skills(); break;
            case "Quest": quest(); break;
            case "Classes": classes(); break;
            case "Appearance": appearance(); break;
        }
    }

    private App find(String id) {
        for (App app : apps) if (app.id().equals(id)) return app;
        return null;
    }
    private void home() {
        if (save.getBoolean(key("description"), true)) text(root, hero.description, 16, MUTED);
        switch (hero) {
            case WARDEN:
                if (unlocked(0)) {
                    TextClock clock = new TextClock(this);
                    clock.setTextSize(42);
                    clock.setTextColor(accent());
                    clock.setTypeface(appearanceFont);
                    root.addView(clock);
                }
                if (unlocked(1)) {
                    App pinned = find(save.getString(key("pin"), ""));
                    if (pinned != null) row(root, pinned.name, () -> launch(pinned));
                    row(root, pinned == null ? "Pin an app" : "Change pinned app", this::choosePin);
                }
                break;
            case RANGER:
                int count = Math.min(apps.size(), unlocked(0) ? 8 : 4);
                for (int i = 0; i < count; i += 2) {
                    LinearLayout pair = new LinearLayout(this);
                    root.addView(pair);
                    for (int j = i; j < Math.min(i + 2, count); j++) {
                        App app = apps.get(j);
                        Button tile = action(app.name, () -> launch(app));
                        tile.setSingleLine(true);
                        tile.setEllipsize(TextUtils.TruncateAt.END);
                        tile.setBackground(surface(0xff253a2e, 14));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(save.getBoolean(key("compact"), false) ? 48 : 64), 1);
                        params.setMargins(dp(3), dp(4), dp(3), dp(4));
                        pair.addView(tile, params);
                    }
                }
                if (unlocked(1)) {
                    App last = find(save.getString(key("last"), ""));
                    if (last != null) row(root, "Last opened: " + last.name, () -> launch(last));
                }
                if (unlocked(2)) text(root, "Recorded today: " + visited(today()).size() + "/" + DAILY_APP_CAP, 14, MUTED);
                break;
            case MAGE:
                if (unlocked(2)) {
                    String next = "All skills unlocked";
                    for (int i = 0; i < hero.skills.length; i++) if (!unlocked(i)) {
                        next = hero.skills[i].name + " · " + hero.skills[i].xp + " XP";
                        break;
                    }
                    text(root, next, 14, accent());
                }
                if (unlocked(0)) appList(false);
                break;
        }
        row(root, "All apps", () -> show("Apps"));
    }

    private void choosePin() {
        if (apps.isEmpty()) return;
        List<App> choices = new ArrayList<>(apps);
        String[] names = choices.stream().map(a -> a.name).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle("Pin an app").setItems(names, (dialog, i) -> {
            save.edit().putString(key("pin"), choices.get(i).id()).apply();
            show("Home");
        }).show();
    }

    private void appList(boolean drawer) {
        EditText search = new EditText(this);
        search.setHint("Find an app");
        search.setSingleLine(true);
        search.setTextColor(foreground());
        search.setTypeface(appearanceFont);
        search.setHintTextColor(MUTED);
        // Keep the keyboard closed until the user actually taps search.
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.addView(search);
        LinearLayout matches = column();
        root.addView(matches);
        Runnable filter = () -> {
            matches.removeAllViews();
            String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            String initial = "";
            for (App app : apps) {
                if ((!drawer && query.isEmpty()) || !app.name.toLowerCase(Locale.ROOT).contains(query)) continue;
                if (drawer && hero == Archetype.MAGE && unlocked(1)) {
                    String letter = app.name.isEmpty() ? "#" : app.name.substring(0, 1).toUpperCase(Locale.ROOT);
                    if (!letter.equals(initial)) text(matches, letter, 14, accent());
                    initial = letter;
                }
                row(matches, app.name, () -> launch(app));
            }
            if (matches.getChildCount() == 0 && (drawer || !query.isEmpty())) text(matches, "No matching apps", 15, MUTED);
        };
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { filter.run(); }
            public void afterTextChanged(android.text.Editable s) {}
        });
        filter.run();
    }

    private void skills() {
        text(root, "XP is cumulative. Unlock the root to open both branches.", 15, MUTED);
        for (int i = 0; i < hero.skills.length; i++) {
            final int index = i;
            Skill skill = hero.skills[i];
            LinearLayout node = column();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(skill.parent < 0 ? 0 : dp(20), dp(10), 0, 0);
            root.addView(node, params);
            text(node, (skill.parent < 0 ? "ROOT  /  " : "BRANCH  /  ") + skill.name, 19, accent());
            text(node, skill.description + " · " + skill.xp + " XP", 15, TEXT);
            if (skill.parent >= 0) text(node, "Requires " + hero.skills[skill.parent].name, 13, MUTED);
            boolean available = skill.available(xp(), mask());
            Button unlock = row(node, unlocked(i) ? "Unlocked" : available ? "Unlock" : "Locked", () -> {
                if (!unlocked(index) && skill.available(xp(), mask())) {
                    save.edit().putInt(key("skills"), mask() | (1 << index)).apply();
                    show("Skills");
                }
            });
            unlock.setEnabled(available && !unlocked(i));
        }
    }

    private void quest() {
        String date = today();
        int progress = Math.min(QUEST_TARGET, visited(date).size());
        boolean claimed = date.equals(save.getString(key("claimed"), ""));
        text(root, "Daily expedition", 21, accent());
        text(root, "Open three different apps from this launcher. " + progress + "/" + QUEST_TARGET, 16, TEXT);
        text(root, "Earn 5 XP for each of your first five distinct app launches today, plus 10 XP for this quest. Progress is per class.", 15, MUTED);
        Button claim = row(root, claimed ? "Reward claimed" : "Claim 10 XP", () -> {
            String now = today();
            if (visited(now).size() >= QUEST_TARGET && !now.equals(save.getString(key("claimed"), ""))) {
                save.edit().putString(key("claimed"), now).putInt(key("xp"), xp() + 10).apply();
            }
            show("Quest");
        });
        claim.setEnabled(!claimed && progress >= QUEST_TARGET);
    }

    private void classes() {
        text(root, "Choose a class. Each keeps its own XP and upgrades.", 16, MUTED);
        for (Archetype option : Archetype.values()) {
            text(root, option.title + "  /  " + option.location, 19, option.accent);
            text(root, option.description, 15, MUTED);
            row(root, save.contains("class") && option == hero ? "Continue as " + option.title : "Choose " + option.title, () -> {
                hero = option;
                save.edit().putString("class", hero.name()).apply();
                show("Home");
            });
        }
        row(root, "Customize " + hero.title, () -> show("Appearance"));
        row(root, "Android default Home settings", () -> {
            try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
            catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, "Set the Home app in Android Settings", Toast.LENGTH_LONG).show();
            }
        });
    }
}
