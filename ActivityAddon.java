package de.privat.activityaddon;

import de.privat.activityaddon.commands.ActivityCommand;
import de.privat.activityaddon.modules.ActivityInspector;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class ActivityAddon extends MeteorAddon {
    public static final Category CATEGORY = new Category("Debug");

    @Override
    public void onInitialize() {
        Modules.get().add(new ActivityInspector());
        Commands.add(new ActivityCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "de.privat.activityaddon";
    }
}
