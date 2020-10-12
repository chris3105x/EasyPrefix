package com.christian34.easyprefix.groups;

import com.christian34.easyprefix.EasyPrefix;
import com.christian34.easyprefix.files.GroupsData;
import com.christian34.easyprefix.groups.gender.GenderedLayout;
import com.christian34.easyprefix.messages.Messages;
import com.christian34.easyprefix.sql.Data;
import com.christian34.easyprefix.sql.DeleteStatement;
import com.christian34.easyprefix.sql.SelectQuery;
import com.christian34.easyprefix.sql.UpdateStatement;
import com.christian34.easyprefix.sql.database.StorageType;
import com.christian34.easyprefix.user.User;
import com.christian34.easyprefix.utils.ChatFormatting;
import com.christian34.easyprefix.utils.Color;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * EasyPrefix 2020.
 *
 * @author Christian34
 */
public class Group extends EasyGroup {
    private final String NAME;
    private final GroupsData groupsData;
    private final GroupHandler groupHandler;
    private final ChatColor groupColor;
    private final EasyPrefix instance;
    private String prefix, suffix, joinMessage, quitMessage;
    private Color chatColor;
    private ChatFormatting chatFormatting;
    private GenderedLayout genderedLayout = null;

    public Group(GroupHandler groupHandler, String name) {
        this.NAME = name;
        this.instance = groupHandler.getInstance();
        this.groupsData = instance.getFileManager().getGroupsData();
        this.groupHandler = groupHandler;

        List<String> keys = Arrays.asList("prefix", "suffix", "chat_color", "chat_formatting", "join_msg", "quit_msg");
        Data data;
        if (instance.getStorageType() == StorageType.SQL) {
            SelectQuery selectQuery = new SelectQuery("groups").setColumns(keys).addCondition("group", name);
            data = selectQuery.getData();
        } else {
            HashMap<String, Object> storage = new HashMap<>();
            for (String key : keys) {
                storage.put(key, getGroupsData().getData().getString(getFilePath() + key.replace("_", "-")));
            }
            data = new Data(storage);
        }

        if (groupHandler.handleGenders()) {
            this.genderedLayout = new GenderedLayout(this);
        }

        this.prefix = data.getStringOr("prefix", "");
        this.prefix = prefix.replace("§", "&");

        this.suffix = data.getStringOr("suffix", "");
        this.suffix = suffix.replace("§", "&");

        String chatFormatting = data.getString("chat_formatting");
        if (chatFormatting != null && chatFormatting.length() == 2) {
            this.chatFormatting = ChatFormatting.getByCode(chatFormatting.substring(1, 2));
            if (this.chatFormatting == null) setChatFormatting(null);
        }

        String chatColor = data.getString("chat_color");
        if (chatColor == null || chatColor.length() < 2) {
            if (this.chatFormatting != null && this.chatFormatting.equals(ChatFormatting.RAINBOW)) {
                this.chatColor = Color.GRAY;
            } else {
                setChatColor(Color.GRAY);
            }
        } else {
            this.chatColor = Color.getByCode(chatColor.substring(1, 2));
        }

        this.groupColor = getGroupColor(prefix);
        this.joinMessage = data.getString("join_msg");
        this.quitMessage = data.getString("quit_msg");
    }

    public String getJoinMessage(User user) {
        if (this.joinMessage == null || this.joinMessage.isEmpty()) {
            this.joinMessage = this.groupHandler.getGroup("default").getJoinMessageText();
        }
        return translate(joinMessage, user);
    }

    public String getJoinMessageText() {
        if (this.joinMessage == null || this.joinMessage.isEmpty()) {
            this.joinMessage = this.groupHandler.getGroup("default").getJoinMessageText();
        }
        return joinMessage;
    }

    public void setJoinMessage(String joinMessage) {
        this.joinMessage = joinMessage.replace("§", "&");
        saveData("join-msg", this.joinMessage);
    }

    public String getQuitMessage(User user) {
        if (this.quitMessage == null || this.quitMessage.isEmpty()) {
            this.quitMessage = this.groupHandler.getGroup("default").getQuitMessageText();
        }
        return translate(quitMessage, user);
    }

    public String getQuitMessageText() {
        if (this.quitMessage == null || this.quitMessage.isEmpty()) {
            this.quitMessage = this.groupHandler.getGroup("default").getQuitMessageText();
        }
        return quitMessage;
    }

    public void setQuitMessage(@NotNull String quitMessage) {
        this.quitMessage = quitMessage.replace("§", "&");
        saveData("quit-msg", this.quitMessage);
    }

    private GroupsData getGroupsData() {
        return groupsData;
    }

    private void saveData(@NotNull String key, @Nullable Object value) {
        EasyPrefix instance = groupHandler.getInstance();
        if (instance.getStorageType() == StorageType.SQL) {
            UpdateStatement updateStatement = new UpdateStatement("groups")
                    .addCondition("group", this.NAME)
                    .setValue(key.replace("-", "_"), value);
            if (!updateStatement.execute()) {
                Messages.log("Couldn't save data to database! Error GDB1");
            }
        } else {
            groupsData.setAndSave(getFilePath() + key.replace("_", "-"), value);
        }
        this.groupHandler.getInstance().getGroupHandler().load();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    @Nonnull
    public String getPrefix(User user, boolean translate) {
        String prefix;
        if (this.groupHandler.handleGenders() && user != null) {
            prefix = this.genderedLayout.getPrefix(user.getGenderType());
            if (prefix == null) prefix = this.prefix;
        } else {
            prefix = this.prefix;
        }
        if (translate) prefix = translate(prefix, user);
        return prefix;
    }

    @Override
    public void setPrefix(@NotNull String prefix) {
        this.prefix = prefix.replace("§", "&");
        saveData("prefix", this.prefix);
    }

    @Override
    @Nonnull
    public String getSuffix(@Nullable User user, boolean translate) {
        String suffix;
        if (this.groupHandler.handleGenders() && user != null) {
            suffix = this.genderedLayout.getSuffix(user.getGenderType());
            if (suffix == null) suffix = this.suffix;
        } else {
            suffix = this.suffix;
        }
        if (translate) suffix = translate(suffix, user);
        return suffix;
    }

    @Override
    public void setSuffix(@NotNull String suffix) {
        this.suffix = suffix.replace("§", "&");
        saveData("suffix", this.suffix);
    }

    @Override
    @NotNull
    public ChatColor getGroupColor() {
        return groupColor;
    }

    @Override
    public String getFilePath() {
        return "groups." + NAME + ".";
    }

    @Override
    public void delete() {
        EasyPrefix instance = this.groupHandler.getInstance();
        if (instance.getStorageType() == StorageType.LOCAL) {
            groupsData.setAndSave("groups." + getName(), null);
        } else {
            DeleteStatement deleteStatement = new DeleteStatement("groups").addCondition("group", getName());
            if (!deleteStatement.execute()) {
                Messages.log("§cCouldn't delete group '" + getName() + "'!");
            }
        }
        instance.getGroupHandler().getGroups().remove(this);
        instance.getUsers().clear();
    }

    public Color getChatColor() {
        return chatColor;
    }

    public void setChatColor(Color color) {
        this.chatColor = color;
        String value = null;
        if (color != null) {
            value = color.getCode().replace("§", "&");
            if (ChatFormatting.RAINBOW.equals(chatFormatting)) {
                setChatFormatting(null);
            }
        } else {
            if (!(chatFormatting != null && chatFormatting.equals(ChatFormatting.RAINBOW))) {
                setChatFormatting(null);
            }
        }
        saveData("chat-color", value);
    }

    @Nullable
    public ChatFormatting getChatFormatting() {
        return chatFormatting;
    }

    public void setChatFormatting(ChatFormatting chatFormatting) {
        this.chatFormatting = chatFormatting;
        String value = null;
        if (chatFormatting != null) {
            if (chatFormatting.equals(ChatFormatting.RAINBOW)) {
                setChatColor(null);
                value = "%r";
            } else value = chatFormatting.getCode().replace("§", "&");
        }
        saveData("chat-formatting", value);
    }

}