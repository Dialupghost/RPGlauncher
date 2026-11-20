package com.rpglauncher;

final class Skill {
    final String name, description;
    final int xp, parent;

    Skill(String name, String description, int xp, int parent) {
        this.name = name;
        this.description = description;
        this.xp = xp;
        this.parent = parent;
    }

    boolean available(int earned, int unlocked) {
        return earned >= xp && (parent < 0 || (unlocked & (1 << parent)) != 0);
    }
}
