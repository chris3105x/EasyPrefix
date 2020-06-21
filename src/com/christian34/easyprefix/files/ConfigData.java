package com.christian34.easyprefix.files;

import com.christian34.easyprefix.EasyPrefix;
import com.tchristofferson.configupdater.ConfigUpdater;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * EasyPrefix 2020.
 *
 * @author Christian34
 */
public class ConfigData {
    private final EasyPrefix instance;
    private File file;
    private FileConfiguration data;

    public ConfigData(EasyPrefix instance) {
        this.instance = instance;
    }

    public ConfigData load() {
        this.file = new File(FileManager.getPluginFolder(), "config.yml");
        if (!file.exists()) {
            this.instance.getPlugin().saveResource("config.yml", true);
        } else {
            try {
                ConfigUpdater.update(this.instance, "config.yml", file, new ArrayList<>());
            } catch (IOException ignored) {
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        return this;
    }

    public void save() {
        try {
            data.options().copyDefaults(true);
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        load();
    }

    public FileConfiguration getData() {
        return data;
    }

    public String getString(ConfigKeys key) {
        return getData().getString(key.toString());
    }

    public int getInt(ConfigKeys key) {
        return getData().getInt(key.toString());
    }

    public boolean getBoolean(ConfigKeys key) {
        return getData().getBoolean(key.toString());
    }

    public void set(String path, Object value) {
        getData().set(path, value);
        save();
    }

    public enum ConfigKeys {
        ENABLED("enabled"), LANG("lang"), CUSTOM_LAYOUT("user.custom-layout.enabled"), HANDLE_CHAT("chat.handle-chat"),
        COLOR_RAINBOW_COLORS("chat.color.rainbow.colors"), GUI_SHOW_ALL_CHATCOLORS("gui.show-all-chatcolors"),
        USE_GENDER("gender.enabled"), FORCE_GENDER("gender.force-gender"), USE_SUBGROUPS("subgroups.enabled"),
        USE_JOIN_QUIT("join-quit-messages.enabled"), HIDE_JOIN_QUIT("join-quit-messages.hide-messages"),
        JOIN_QUIT_SOUND_RECEIVER("join-quit-messages.sound.receiver"), USE_QUIT_SOUND("join-quit-messages.sound.quit.enabled"),
        QUIT_SOUND("join-quit-messages.sound.quit.sound"), USE_SQL("sql.enabled"), SQL_HOST("sql.host"), SQL_PORT("sql.port"),
        SQL_DATABASE("sql.database"), SQL_USERNAME("sql.username"), SQL_PASSWORD("sql.password"), SQL_TABLE_PREFIX("sql.table-prefix"),
        HANDLE_COLORS("chat.handle-colors"), PREFIX_ALIAS("user.custom-layout.alias.prefix"), SUFFIX_ALIAS("user.custom-layout.alias.suffix");

        private final String KEY;

        ConfigKeys(String key) {
            this.KEY = key;
        }

        @Override
        public String toString() {
            return "config." + KEY;
        }

    }

}