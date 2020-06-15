package com.christian34.easyprefix.setup.responder.gui.pages;

import com.christian34.easyprefix.EasyPrefix;
import com.christian34.easyprefix.files.ConfigData;
import com.christian34.easyprefix.groups.EasyGroup;
import com.christian34.easyprefix.groups.Group;
import com.christian34.easyprefix.groups.GroupHandler;
import com.christian34.easyprefix.groups.Subgroup;
import com.christian34.easyprefix.messages.Message;
import com.christian34.easyprefix.messages.Messages;
import com.christian34.easyprefix.setup.responder.ChatRespond;
import com.christian34.easyprefix.setup.responder.GuiRespond;
import com.christian34.easyprefix.setup.responder.gui.Icon;
import com.christian34.easyprefix.setup.responder.gui.Page;
import com.christian34.easyprefix.user.User;
import com.christian34.easyprefix.utils.ChatFormatting;
import com.christian34.easyprefix.utils.VersionController;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * EasyPrefix 2020.
 *
 * @author Christian34
 */
@SuppressWarnings("UnusedReturnValue")
public class GuiSetup extends Page {
    private final User user;
    private final String DIVIDER = "§7-------------------------";

    public GuiSetup(User user) {
        super(user);
        this.user = user;
    }

    public GuiSetup mainPage() {
        GuiRespond guiRespond = new GuiRespond(user, Message.SETTINGS_TITLE_MAIN.toString(), 3);
        guiRespond.addIcon(Material.CHEST, Message.BTN_GROUPS.toString(), 2, 3).addClickAction(this::groupsList);

        guiRespond.addIcon(Material.NETHER_STAR, Message.SETTINGS_TITLE_MAIN.toString(), 2, 5).addClickAction(this::pluginSettingsGui);

        Material icon = (VersionController.getMinorVersion() < 12) ? Material.valueOf("CHEST") : Material.valueOf("WRITABLE_BOOK");
        guiRespond.addIcon(icon, Message.BTN_SUBGROUPS.toString(), 2, 7).addClickAction(this::openSubgroupsList);

        guiRespond.addCloseButton();
        guiRespond.openInventory();
        return this;
    }

    public GuiSetup pluginSettingsGui() {
        GuiRespond guiRespond = new GuiRespond(user, "§5EasyPrefix §8» " + Message.SETTINGS_TITLE_MAIN.toString(), 3);
        Material langMaterial = (VersionController.getMinorVersion() < 12) ? Material.valueOf("SIGN") : Material.valueOf("OAK_SIGN");
        String langName = Message.BTN_CHANGE_LANG.toString().replace("%lang%", Messages.langToName());
        guiRespond.addIcon(langMaterial, langName, 2, 2).setLore(Messages.getList(Message.LORE_CHANGE_LANG)).addClickAction(() -> {
            String crntLang = Messages.getLanguage();
            String nextLang = "en_EN";
            switch (crntLang) {
                case "en_EN":
                    nextLang = "de_DE";
                    break;
                case "de_DE":
                    nextLang = "it_IT";
                    break;
            }
            Messages.setLanguage(nextLang);
            pluginSettingsGui();
        });

        ConfigData configData = EasyPrefix.getInstance().getFileManager().getConfig();
        boolean useCp = configData.getBoolean(ConfigData.ConfigKeys.CUSTOM_PREFIX);
        String cpText = Message.BTN_SWITCH_CP.toString().replace("%active%", (useCp) ? Message.ENABLED.toString() : Message.DISABLED.toString());
        guiRespond.addIcon(Material.BEACON, cpText, 2, 4).setLore(Collections.singletonList(Message.LORE_SWITCH_CP.toString())).addClickAction(() -> {
            configData.set(ConfigData.ConfigKeys.CUSTOM_PREFIX.toString(), !useCp);
            EasyPrefix.getInstance().reload();
            pluginSettingsGui();
        });

        boolean useGender = configData.getBoolean(ConfigData.ConfigKeys.USE_GENDER);
        String genderText = Message.BTN_SWITCH_GENDER.toString().replace("%active%", (useGender) ? Message.ENABLED.toString() : Message.DISABLED.toString());
        guiRespond.addIcon(Material.CHAINMAIL_HELMET, genderText, 2, 6).setLore(Collections.singletonList(Message.LORE_SWITCH_GENDER.toString())).addClickAction(() -> {
            boolean use = !useGender;
            configData.set(ConfigData.ConfigKeys.USE_GENDER.toString(), use);
            EasyPrefix.getInstance().reload();
            pluginSettingsGui();
        });


        Material btnMaterial;
        if (VersionController.getMinorVersion() < 13) {
            try {
                btnMaterial = Material.valueOf("INK_SACK");
            } catch (Exception ignored) {
                btnMaterial = Material.BARRIER;
            }
        } else {
            btnMaterial = Material.LIME_DYE;
        }

        boolean useColors = configData.getBoolean(ConfigData.ConfigKeys.HANDLE_COLORS);
        String colorsText = Message.BTN_SWITCH_COLOR.toString().replace("%active%", (useColors) ? Message.ENABLED.toString() : Message.DISABLED.toString());
        guiRespond.addIcon(btnMaterial, colorsText, 2, 8).setLore(Collections.singletonList(Message.LORE_SWITCH_COLOR.toString())).addClickAction(() -> {
            boolean use = !useColors;
            configData.set(ConfigData.ConfigKeys.HANDLE_COLORS.toString(), use);
            EasyPrefix.getInstance().reload();
            pluginSettingsGui();
        });

        guiRespond.addCloseButton().addClickAction(this::mainPage);
        guiRespond.openInventory();
        return this;
    }

    public GuiSetup createGroup() {
        GroupHandler groupHandler = EasyPrefix.getInstance().getGroupHandler();
        new ChatRespond(user, Message.CHAT_GROUP.toString(), (answer) -> {
            if (answer.split(" ").length == 1) {
                if (groupHandler.isGroup(answer)) {
                    user.sendMessage(Message.GROUP_EXISTS.toString());
                    return ChatRespond.Respond.ERROR;
                } else {
                    groupHandler.createGroup(answer.replace(" ", ""));
                    user.sendMessage(Message.GROUP_CREATED.toString());
                    return ChatRespond.Respond.ACCEPTED;
                }
            } else {
                return ChatRespond.Respond.WRONG_INPUT;
            }
        }).setErrorText("Please type in one word without spaces!");
        return this;
    }

    public GuiSetup groupsList() {
        GroupHandler groupHandler = EasyPrefix.getInstance().getGroupHandler();
        GuiRespond guiRespond = new GuiRespond(user, "§5EasyPrefix §8» " + Message.SETUP_GROUPS_TITLE.toString(), 5);
        final String divider = "§7-------------------------------";
        for (Group group : groupHandler.getGroups()) {
            String prefix = group.getPrefix(null, false);
            String suffix = group.getSuffix(null, false);
            ChatColor prefixColor = group.getGroupColor();
            List<String> lore = new ArrayList<>();
            lore.add(divider);
            if (prefix.length() > 25) {
                lore.add(Messages.getAndSet(Message.LORE_PREFIX, "§7«§f" + prefix.substring(0, 25)));
                lore.add("§f" + prefix.substring(26) + "§7»");
            } else {
                lore.add(Messages.getAndSet(Message.LORE_PREFIX, "§7«§f" + prefix + "§7»"));
            }
            lore.add(Messages.getAndSet(Message.LORE_SUFFIX, "§7«§f" + suffix + "§7»"));

            String groupChatColor = (group.getChatColor() != null) ? group.getChatColor().getCode() : "-";
            if (group.getChatColor() != null && group.getChatFormatting() != null) {
                if (!group.getChatFormatting().equals(ChatFormatting.RAINBOW)) {
                    groupChatColor += group.getChatFormatting().getCode();
                }
            } else {
                if (group.getChatFormatting() != null && group.getChatFormatting().equals(ChatFormatting.RAINBOW)) {
                    groupChatColor = Message.FORMATTING_RAINBOW.toString();
                }
            }

            lore.add(Messages.getAndSet(Message.LORE_COLOR, groupChatColor.replace("§", "&")));
            lore.add(Messages.getAndSet(Message.LORE_PERMISSION, "EasyPrefix.group." + group.getName()));

            guiRespond.addIcon(new ItemStack(Material.CHEST), prefixColor + group.getName()).setLore(lore).addClickAction(() -> openGroupProfile(group));
        }

        guiRespond.addIcon(Material.NETHER_STAR, Message.BTN_ADDGROUP, 5, 9).addClickAction(this::createGroup);

        guiRespond.addCloseButton().addClickAction(this::mainPage);
        guiRespond.openInventory();
        return this;
    }

    public GuiSetup openSubgroupsList() {
        GuiRespond guiRespond = new GuiRespond(user, "§5EasyPrefix §8» " + Message.TITLE_SUBGROUPS.toString(), 5);
        GroupHandler groupHandler = EasyPrefix.getInstance().getGroupHandler();
        for (final Subgroup subgroup : groupHandler.getSubgroups()) {
            String prefix = subgroup.getPrefix(null, false);
            String suffix = subgroup.getSuffix(null, false);
            suffix = (suffix == null) ? "-" : suffix;
            ChatColor prefixColor = subgroup.getGroupColor();
            List<String> lore = new ArrayList<>();
            lore.add("§7-------------------------");
            if (prefix.length() > 25) {
                lore.add(Message.LORE_PREFIX.toString().replace("%value%", "§7«§f" + prefix.substring(0, 25)));
                lore.add("§f" + prefix.substring(26) + "§7»");
            } else {
                lore.add(Message.LORE_PREFIX.toString().replace("%value%", "§7«§f" + prefix + "§7»"));
            }
            lore.add(Message.LORE_SUFFIX.toString().replace("%value%", "§7«§f" + suffix + "§7»"));

            /* todo what's happening here? */
            if (groupHandler.getGender(subgroup.getName()) == null) {
                //  if (!Gender.getTypes().contains(subgroup.getName().toLowerCase())) {
                lore.add(Message.LORE_PERMISSION.toString().replace("%value%", "EasyPrefix.subgroup." + subgroup.getName()));
            }

            Material sgBtn = Material.BARRIER;
            try {
                if (VersionController.getMinorVersion() < 12) {
                    sgBtn = Material.CHEST;
                } else {
                    sgBtn = Material.WRITABLE_BOOK;
                }
            } catch (Exception ignored) {
            }
            guiRespond.addIcon(new ItemStack(sgBtn), prefixColor + subgroup.getName()).setLore(lore).addClickAction(() -> openSubgroupProfile(subgroup));
        }

        guiRespond.addCloseButton().addClickAction(this::mainPage);
        guiRespond.openInventory();
        return this;
    }

    public GuiSetup openProfile(EasyGroup easyGroup) {
        if (easyGroup instanceof Group) {
            openGroupProfile((Group) easyGroup);
        } else {
            openSubgroupProfile((Subgroup) easyGroup);
        }
        return this;
    }

    public GuiSetup openGroupProfile(Group group) {
        GuiRespond guiRespond = new GuiRespond(user, "§5EasyPrefix §8» §7" + group.getGroupColor() + group.getName(), 4);
        Icon prefixIcon = guiRespond.addIcon(Material.IRON_INGOT, Message.BTN_CHANGE_PREFIX, 2, 3);
        prefixIcon.setLore(Arrays.asList(this.DIVIDER, Message.LORE_GROUP_DETAIL.toString() + "§7«§f" + group.getPrefix(null, false) + "§7»", " ", Message.LORE_EDIT.toString()));
        prefixIcon.addClickAction(() -> new GuiModifyingGroups(user).editPrefix(group));

        Icon suffixIcon = guiRespond.addIcon(Material.GOLD_INGOT, Message.BTN_CHANGE_SUFFIX, 2, 5);
        suffixIcon.setLore(Arrays.asList(this.DIVIDER, Message.LORE_GROUP_DETAIL.toString() + "§7«§f" + group.getSuffix(null, false) + "§7»", " ", Message.LORE_EDIT.toString()));
        suffixIcon.addClickAction(() -> new GuiModifyingGroups(user).editSuffix(group));

        Icon joinMsgIcon = guiRespond.addIcon(Icon.getCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkZDIwYmU5MzUyMDk0OWU2Y2U3ODlkYzRmNDNlZmFlYjI4YzcxN2VlNmJmY2JiZTAyNzgwMTQyZjcxNiJ9fX0=", Material.BLAZE_ROD), "§aJoin Message", 3, 4);
        joinMsgIcon.setLore(Arrays.asList(this.DIVIDER, Message.LORE_GROUP_DETAIL.toString() + "§7«§f" + group.getJoinMessageText() + "§7»", " ", Message.LORE_EDIT.toString()));
        joinMsgIcon.addClickAction(() -> new GuiModifyingGroups(user).editJoinMessage(group));

        Icon quitMsgIcon = guiRespond.addIcon(Icon.getCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ4YTk5ZGIyYzM3ZWM3MWQ3MTk5Y2Q1MjYzOTk4MWE3NTEzY2U5Y2NhOTYyNmEzOTM2Zjk2NWIxMzExOTMifX19", Material.STICK), "§aQuit Message", 3, 6);
        quitMsgIcon.setLore(Arrays.asList(this.DIVIDER, Message.LORE_GROUP_DETAIL.toString() + "§7«§f" + group.getQuitMessageText() + "§7»", " ", Message.LORE_EDIT.toString()));
        quitMsgIcon.addClickAction(() -> new GuiModifyingGroups(user).editQuitMessage(group));

        String groupChatColor = "-";
        if (group.getChatColor() != null) {
            groupChatColor = group.getChatColor().getCode();
            if (group.getChatFormatting() != null && !group.getChatFormatting().equals(ChatFormatting.RAINBOW)) {
                groupChatColor += group.getChatFormatting().getCode();
            }
        } else {
            if (group.getChatFormatting() != null && group.getChatFormatting().equals(ChatFormatting.RAINBOW)) {
                groupChatColor = Message.FORMATTING_RAINBOW.toString();
            }
        }
        List<String> loreChatColor = Arrays.asList(this.DIVIDER, Message.LORE_GROUP_DETAIL.toString() + groupChatColor.replace("§", "&"), " ", Message.LORE_EDIT.toString());
        Material btnMaterial;
        if (VersionController.getMinorVersion() < 13) {
            try {
                btnMaterial = Material.valueOf("INK_SACK");
            } catch (Exception ignored) {
                btnMaterial = Material.BARRIER;
            }
        } else {
            btnMaterial = Material.LIME_DYE;
        }

        guiRespond.addIcon(btnMaterial, Message.BTN_CHANGE_CHATCOLOR, 2, 7).setLore(loreChatColor).addClickAction(() -> new GuiModifyingGroups(user).editChatColor(group));

        if (!group.getName().equals("default")) {
            guiRespond.addIcon(Material.BARRIER, Message.BTN_DELETE, 4, 9).addClickAction(() -> new GuiModifyingGroups(user).deleteConfirmation(group));
        }

        guiRespond.addCloseButton().addClickAction(this::groupsList);
        guiRespond.openInventory();
        return this;
    }

    public GuiSetup openSubgroupProfile(Subgroup subgroup) {
        GuiRespond guiRespond = new GuiRespond(user, "§5EasyPrefix §8» §7" + subgroup.getGroupColor() + subgroup.getName(), 3);

        Icon prefixIcon = guiRespond.addIcon(Material.IRON_INGOT, Message.BTN_CHANGE_PREFIX.toString(), 2, 4);
        prefixIcon.setLore(Arrays.asList(this.DIVIDER, Message.LORE_GROUP_DETAIL.toString() + "§7«§f" + subgroup.getPrefix(null, false) + "§7»", " ", Message.LORE_EDIT.toString()));
        prefixIcon.addClickAction(() -> new GuiModifyingGroups(user).editPrefix(subgroup));

        Icon suffixIcon = guiRespond.addIcon(Material.GOLD_INGOT, Message.BTN_CHANGE_SUFFIX.toString(), 2, 6);
        suffixIcon.setLore(Arrays.asList(this.DIVIDER, Message.LORE_GROUP_DETAIL.toString() + "§7«§f" + subgroup.getSuffix(null, false) + "§7»", " ", Message.LORE_EDIT.toString()));
        suffixIcon.addClickAction(() -> new GuiModifyingGroups(user).editSuffix(subgroup));

        if (!subgroup.getName().equals("default")) {
            guiRespond.addIcon(Material.BARRIER, Message.BTN_DELETE.toString(), 3, 9).addClickAction(() -> new GuiModifyingGroups(user).deleteConfirmation(subgroup));
        }

        guiRespond.addCloseButton().addClickAction(this::openSubgroupsList);
        guiRespond.openInventory();
        return this;
    }

}