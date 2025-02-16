package it.moro.smartspawner;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.Objects;

public final class SmartSpawner extends JavaPlugin {

    @Override
    public void onEnable() {
        loadFiles();
        SpawnerManager spawner = new SpawnerManager(this);
        Bukkit.getPluginManager().registerEvents(spawner, this);
        Objects.requireNonNull(getCommand("spawnerpickaxe")).setExecutor(spawner);
        Objects.requireNonNull(getCommand("givespawner")).setExecutor(spawner);
        Objects.requireNonNull(getCommand("givetrialspawner")).setExecutor(spawner);
        spawner.addRecipeFromConfig();
        getLogger().info("\u001B[32mEnabled\u001B[0m");
    }

    @Override
    public void onDisable() {
        getLogger().info("\u001B[91mDisabled\u001B[0m");
    }

    private void loadFiles() {
        if (!getDataFolder().exists()) {
            if (getDataFolder().mkdir()) {
                File fileConfig = new File(getDataFolder(), "config.yml");
                ensureFileExists(fileConfig);
                getLogger().info("\u001B[32mPlugin data folder created successfully!\u001B[0m");
            } else {
                getLogger().info("\u001B[91mUnable to create plugin data folder! Missing permits?\u001B[0m");
            }
        } else {
            File fileConfig = new File(getDataFolder(), "config.yml");
            ensureFileExists(fileConfig);
        }
    }

    private void ensureFileExists(File file) {
        if (!file.exists()) {
            saveResource("config.yml", false);
            getLogger().info("\u001B[32mFile " + "config.yml" + " created!\u001B[0m");
        }
    }

}
