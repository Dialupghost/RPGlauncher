package com.rpglauncher;

enum Archetype {
    WARDEN("Warden", "THE KEEP", "A quiet home for your essentials.", 0xff161714, 0xffe8c580, "serif",
        new Skill("Watchtower", "Show a clock on your home screen", 10, -1),
        new Skill("Shield Rack", "Pin one app to the Keep", 25, 0),
        new Skill("Bastion", "Hide progress counters on Home", 50, 0)),
    RANGER("Ranger", "THE TRAIL", "A compact trail of launch tiles.", 0xff101c16, 0xffa6dfa6, "sans-serif",
        new Skill("Trail Markers", "Expand the trail from four apps to eight", 10, -1),
        new Skill("Tracks", "Show your last opened app as a shortcut", 25, 0),
        new Skill("Field Notes", "Track today's distinct apps on Home", 50, 0)),
    MAGE("Mage", "THE OBSERVATORY", "A text-first portal to your apps.", 0xff1b1427, 0xffd9b6ff, "monospace",
        new Skill("Quick Cast", "Search for apps right from Home", 10, -1),
        new Skill("Runes", "Show an alphabetical index in the app drawer", 25, 0),
        new Skill("Divination", "Show the next skill's XP target on Home", 50, 0));

    final String title, location, description, font;
    final int background, accent;
    final Skill[] skills;
    Archetype(String title, String location, String description, int background, int accent, String font, Skill... skills) {
        this.title = title;
        this.location = location;
        this.description = description;
        this.background = background;
        this.accent = accent;
        this.font = font;
        this.skills = skills;
    }
}
