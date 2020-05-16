package com.christian34.easyprefix.database;

import com.christian34.easyprefix.EasyPrefix;
import com.christian34.easyprefix.files.ConfigData;
import com.christian34.easyprefix.files.ConfigData.ConfigKeys;
import com.christian34.easyprefix.files.FileManager;
import com.christian34.easyprefix.files.GroupsData;
import com.christian34.easyprefix.messages.Messages;
import com.christian34.easyprefix.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.util.Set;
import java.util.UUID;

/**
 * EasyPrefix 2020.
 *
 * @author Christian34
 */
public class Database {
    private final String host, database, username, tablePrefix, password;
    private final int port;
    private Connection connection;
    private final EasyPrefix instance;

    public Database(EasyPrefix instance) {
        String tablePrefix1;
        this.instance = instance;
        ConfigData config = instance.getFileManager().getConfig();
        this.host = config.getString(ConfigKeys.SQL_HOST);
        this.database = config.getString(ConfigKeys.SQL_DATABASE);
        this.username = config.getString(ConfigKeys.SQL_USERNAME);
        this.password = config.getString(ConfigKeys.SQL_PASSWORD);
        tablePrefix1 = config.getString(ConfigKeys.SQL_TABLE_PREFIX);
        this.port = config.getInt(ConfigKeys.SQL_PORT);
        if (tablePrefix1 == null || tablePrefix1.isEmpty()) {
            tablePrefix1 = "";
        } else if (!tablePrefix1.endsWith("_")) tablePrefix1 += "_";
        this.tablePrefix = tablePrefix1;
        connect();
    }

    public void close() {
        synchronized(this) {
            try {
                if (getConnection() != null && !getConnection().isClosed()) {
                    getConnection().close();
                }
            } catch(SQLException ignored) {
            }
        }
    }

    private void connect() {
        synchronized(this) {
            try {
                if (connection != null && !connection.isClosed()) return;
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false", username, password);
                createTables();
            } catch(SQLSyntaxErrorException e) {
                Messages.log("§cDatabase '" + database + "' does not exist!");
            } catch(SQLException e) {
                Messages.log("§cAccess denied for user '" + this.username + "'@'" + this.host + "'");
                Messages.log("§cPlease check if the sql server is running and you entered the right username and password.");
            } catch(ClassNotFoundException e) {
                Messages.log("§cYour installation does not support sql!");
            }
        }
    }

    public ResultSet getValue(String statement) {
        try {
            if (connection.isClosed()) connect();
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(statement.replace("%p%", getTablePrefix()));
        } catch(SQLException e) {
            Messages.log("§cCouldn't get value from statement '" + statement + "'!");
            Messages.log("§c" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean exists(String statement) {
        try {
            if (connection.isClosed()) connect();
            Statement stmt = connection.createStatement();
            ResultSet result = stmt.executeQuery(statement.replace("%p%", getTablePrefix()));
            return result.next();
        } catch(SQLException e) {
            Messages.log("§cCouldn't get value from statement '" + statement + "'!");
            Messages.log("§c" + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public void update(String statement) {
        Bukkit.getScheduler().runTaskAsynchronously(this.instance, () -> {
            try {
                if (connection.isClosed()) connect();
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(statement.replace("%p%", getTablePrefix()));
                stmt.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void createTables() {
        update("CREATE TABLE IF NOT EXISTS `%p%users` (`uuid` CHAR(36) NOT NULL, `group` VARCHAR(64) NULL DEFAULT NULL, `force_group` BOOLEAN NULL DEFAULT NULL, `subgroup` VARCHAR(64) NULL DEFAULT NULL, `custom_prefix` VARCHAR(128) NULL DEFAULT NULL, `custom_suffix` VARCHAR(128) NULL DEFAULT NULL, `gender` VARCHAR(32) NULL DEFAULT NULL, `chat_color` CHAR(2) NULL DEFAULT NULL, `chat_formatting` CHAR(2) NULL DEFAULT NULL, PRIMARY KEY(`uuid`))ENGINE = InnoDB CHARSET = utf8 COLLATE utf8_bin;");
        update("CREATE TABLE IF NOT EXISTS `%p%groups` (`group` VARCHAR(64) not null, UNIQUE(`group`), prefix VARCHAR(128) default NULL null, suffix VARCHAR(128) default NULL null, chat_color CHAR(2) default NULL null, chat_formatting CHAR(2) default NULL null, join_msg VARCHAR(255) default NULL null, quit_msg VARCHAR(255) default NULL null)ENGINE = InnoDB CHARSET = utf8 COLLATE utf8_bin;");
        update("CREATE TABLE IF NOT EXISTS `%p%genders` ( `id` INT NOT NULL AUTO_INCREMENT , `type` INT(1) NOT NULL , `group_name` VARCHAR(64) NOT NULL , `gender` VARCHAR(32) NOT NULL , `prefix` VARCHAR(128) default NULL null , `suffix` VARCHAR(128) default NULL null , PRIMARY KEY (`id`)) ENGINE = InnoDB CHARSET = utf8 COLLATE utf8_bin;");
        update("CREATE TABLE IF NOT EXISTS `%p%subgroups` ( `group` VARCHAR(64) NOT NULL , UNIQUE(`group`), `prefix` VARCHAR(128) default NULL null , `suffix` VARCHAR(128) default NULL null ) ENGINE = InnoDB CHARSET = utf8 COLLATE utf8_bin;");
    }

    public void uploadGroups() throws SQLException {
        GroupsData groupsData = this.instance.getFileManager().getGroupsData();
        groupsData.load();
        FileConfiguration data = groupsData.getData();

        Set<String> groups = data.getConfigurationSection("groups").getKeys(false);
        for (String groupName : groups) {
            try {
                String sql = "INSERT INTO `%p%groups`(`group`) VALUES (?)";
                PreparedStatement stmt = prepareStatement(sql);
                stmt.setString(1, groupName);
                stmt.executeUpdate();
                Messages.log("§7Uploaded group '" + groupName + "' to database!");
            } catch(Exception ignored) {
            }

            String sql = "UPDATE `%p%groups` SET `prefix`= ?,`suffix`= ?,`chat_color`= ?,`chat_formatting`= ?," + "`join_msg`= ?,`quit_msg`= ? WHERE `group` = ?";
            PreparedStatement stmt = prepareStatement(sql);
            DataStatement statement = new DataStatement(sql);

            String prefix = data.getString("groups." + groupName + ".prefix");
            statement.setObject(1, prefix);

            String suffix = data.getString("groups." + groupName + ".suffix");
            statement.setObject(2, suffix);

            String chatcolor = data.getString("groups." + groupName + ".chat-color");
            statement.setObject(3, chatcolor);

            String chatformatting = data.getString("groups." + groupName + ".chat-formatting");
            if (chatformatting != null && chatformatting.equalsIgnoreCase("%rainbow%")) {
                chatformatting = "%r";
            }
            statement.setObject(4, chatformatting);

            String joinMessage = data.getString("groups." + groupName + ".join-msg");
            statement.setObject(5, joinMessage);

            String quitMessage = data.getString("groups." + groupName + ".quit-msg");
            statement.setObject(6, quitMessage);

            ConfigurationSection section = data.getConfigurationSection("groups." + groupName + ".genders");
            if (section != null) {
                Set<String> genderTypes = section.getKeys(false);
                for (String gender : genderTypes) {
                    String path = "groups." + groupName + ".genders." + gender + ".";
                    String pref = data.getString(path + "prefix");
                    String suf = data.getString(path + "suffix");

                    String sql2 = "SELECT `id` FROM `%p%genders` WHERE `type` = ? AND `gender` = ? AND `group_name` = ?";
                    PreparedStatement stmt2 = prepareStatement(sql2);
                    stmt2.setInt(1, 0);
                    stmt2.setString(2, gender);
                    stmt2.setString(3, groupName);
                    ResultSet result = stmt2.executeQuery();
                    String sql3;
                    PreparedStatement stmt3;
                    if (!result.next()) {
                        sql3 = "INSERT INTO `%p%genders`(`gender`, `type`, `group_name`, `prefix`, `suffix`) VALUES (?," + " ?, ?, ?, ?)";
                        stmt3 = prepareStatement(sql3);
                        stmt3.setString(1, gender);
                        stmt3.setInt(2, 0);
                        stmt3.setString(3, groupName);
                        stmt3.setString(4, pref);
                        stmt3.setString(5, suf);
                        stmt3.executeUpdate();
                    } else {
                        sql3 = "UPDATE `%p%genders` SET `prefix`=?,`suffix`=? WHERE `type` = ? AND `gender` = ? AND " + "`group_name` = ?";
                        stmt3 = prepareStatement(sql3);
                        stmt3.setString(1, pref);
                        stmt3.setString(2, suf);
                        stmt3.setInt(3, 0);
                        stmt3.setString(4, gender);
                        stmt3.setString(5, groupName);
                        stmt3.executeUpdate();
                    }
                }
            } else {
                String sql2 = "DELETE FROM `%p%genders` WHERE `type` = 0 AND `group_name` = ?";
                PreparedStatement stmt2 = prepareStatement(sql2);
                stmt2.setString(1, groupName);
                stmt2.executeUpdate();
            }
            statement.setObject(7, groupName);
            if (!statement.execute()) {
                Messages.log("Error:\n" + statement.getException().getMessage());
            }
        }

    }

    private void uploadSubgroups() throws SQLException {
        FileConfiguration data = this.instance.getFileManager().getGroupsData().getData();
        ConfigurationSection mainSection = data.getConfigurationSection("subgroups");
        if (mainSection == null) return;
        Set<String> groups = mainSection.getKeys(false);
        for (String groupName : groups) {
            DataStatement statement = new DataStatement("INSERT INTO `%p%subgroups`(`group`) VALUES (?)");
            statement.setObject(1, groupName);
            if (statement.execute()) {
                Messages.log("§7Uploaded subgroup '" + groupName + "' to database!");
            }

            DataStatement stmt = new DataStatement("UPDATE `%p%subgroups` SET `prefix`= ?,`suffix`= ? WHERE `group` = ?");
            stmt.setObject(1, data.getString("subgroups." + groupName + ".prefix"));
            stmt.setObject(2, data.getString("subgroups." + groupName + ".suffix"));
            stmt.setObject(3, groupName);
            stmt.execute();

            ConfigurationSection section = data.getConfigurationSection("subgroups." + groupName + ".genders");
            if (section != null) {
                Set<String> genderTypes = section.getKeys(false);
                for (String gender : genderTypes) {
                    String path = "subgroups." + groupName + ".genders." + gender + ".";
                    String pref = data.getString(path + "prefix");
                    String suf = data.getString(path + "suffix");

                    String sql2 = "SELECT `id` FROM `%p%genders` WHERE `type` = ? AND `gender` = ? AND `group_name` = ?";
                    PreparedStatement stmt2 = prepareStatement(sql2);
                    stmt2.setInt(1, 1);
                    stmt2.setString(2, gender);
                    stmt2.setString(3, groupName);
                    ResultSet result = stmt2.executeQuery();
                    String sql3;
                    PreparedStatement stmt3;
                    if (!result.next()) {
                        sql3 = "INSERT INTO `%p%genders`(`gender`, `type`, `group_name`, `prefix`, `suffix`) VALUES (?, ?, ?, ?, ?)";
                        stmt3 = prepareStatement(sql3);
                        stmt3.setString(1, gender);
                        stmt3.setInt(2, 1);
                        stmt3.setString(3, groupName);
                        stmt3.setString(4, pref);
                        stmt3.setString(5, suf);
                        stmt3.executeUpdate();
                    } else {
                        sql3 = "UPDATE `%p%genders` SET `prefix`=?,`suffix`=? WHERE `type` = ? AND `gender` = ? AND `group_name` = ?";
                        stmt3 = prepareStatement(sql3);
                        stmt3.setString(1, pref);
                        stmt3.setString(2, suf);
                        stmt3.setInt(3, 1);
                        stmt3.setString(4, gender);
                        stmt3.setString(5, groupName);
                        stmt3.executeUpdate();
                    }
                }
            } else {
                String sql2 = "DELETE FROM `%p%genders` WHERE `type` = 1 AND `group_name` = ?";
                PreparedStatement stmt2 = prepareStatement(sql2);
                stmt2.setString(1, groupName);
                stmt2.executeUpdate();
            }
        }

    }

    private void uploadUsers() throws SQLException {
        File dirUsers = new File(FileManager.getPluginFolder() + "/user");
        File[] listOfFiles = dirUsers.listFiles();
        if (listOfFiles != null) {
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    UUID uuid = UUID.fromString(listOfFile.getName().replace(".yml", ""));
                    UserData userData = new UserData(uuid);
                    String groupName = userData.getFileData().getString("group");
                    String subgroupName = userData.getFileData().getString("subgroup");
                    String chatColor = userData.getFileData().getString("chat-color");
                    String chatFormatting = userData.getFileData().getString("chat-formatting");
                    String cstmPrefix = userData.getFileData().getString("custom-prefix");
                    String cstmSuffix = userData.getFileData().getString("custom-suffix");
                    String gender = userData.getFileData().getString("gender");
                    boolean forceGroup = userData.getFileData().getBoolean("force-group");
                    PreparedStatement stmt = prepareStatement("INSERT INTO `%p%users`(`uuid`) VALUES (?)");
                    stmt.setString(1, uuid.toString());
                    try {
                        stmt.executeUpdate();
                    } catch(SQLIntegrityConstraintViolationException ignored) {
                    }
                    String sql = "UPDATE `%p%users` SET `group`=?,`force_group`=?,`subgroup`=?,`custom_prefix`=?," + "`custom_suffix`=?,`gender`=?,`chat_color`=?,`chat_formatting`=? WHERE `uuid` ='" + uuid.toString() + "'";
                    stmt = prepareStatement(sql);
                    stmt.setString(1, groupName);
                    stmt.setBoolean(2, forceGroup);
                    stmt.setString(3, subgroupName);
                    stmt.setString(4, cstmPrefix);
                    stmt.setString(5, cstmSuffix);
                    stmt.setString(6, gender);
                    stmt.setString(7, chatColor);
                    stmt.setString(8, chatFormatting);
                    try {
                        stmt.executeUpdate();
                    } catch(SQLIntegrityConstraintViolationException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public void uploadData() throws SQLException {
        long startTime = System.currentTimeMillis();
        Messages.log("§cUploading data to SQL...");
        Messages.log("§7loading files...");
        this.instance.getFileManager().load();
        Messages.log("§7creating tables...");
        createTables();
        Messages.log("§7uploading groups...");
        uploadGroups();
        Messages.log("§7uploading subgroups...");
        uploadSubgroups();
        Messages.log("§7uploading users...");
        uploadUsers();
        long ms = System.currentTimeMillis() - startTime;
        Messages.log("§aUpload took " + ms + " ms!");
    }

    public void downloadData() throws SQLException {
        long startTime = System.currentTimeMillis();
        Messages.log("§cDownloading data to local storage...");
        this.instance.getFileManager().load();
        GroupsData groupsData = this.instance.getFileManager().getGroupsData();
        groupsData.load();
        groupsData.set("groups", null);
        groupsData.set("subgroups", null);

        String sql = "SELECT `group`,`prefix`,`suffix`,`chat_color`,`chat_formatting`,`join_msg`,`quit_msg` FROM " + "`%p%groups`";
        ResultSet resultSet = getValue(sql);
        while (resultSet.next()) {
            String group = resultSet.getString("group");
            groupsData.set("groups." + group + ".prefix", resultSet.getString("prefix"));
            groupsData.set("groups." + group + ".suffix", resultSet.getString("suffix"));
            groupsData.set("groups." + group + ".chat-color", resultSet.getString("chat_color"));
            groupsData.set("groups." + group + ".chat-formatting", resultSet.getString("chat_formatting"));
            groupsData.set("groups." + group + ".join-msg", resultSet.getString("join_msg"));
            groupsData.set("groups." + group + ".quit-msg", resultSet.getString("quit_msg"));
        }

        resultSet = getValue("SELECT `group_name`, `gender`, `prefix`, `suffix`  FROM `%p%genders` WHERE `type` = '0'");
        while (resultSet.next()) {
            String group = resultSet.getString("group_name");
            groupsData.set("groups." + group + ".genders." + resultSet.getString("gender") + ".prefix", resultSet.getString("prefix"));
            groupsData.set("groups." + group + ".genders." + resultSet.getString("gender") + ".suffix", resultSet.getString("suffix"));
        }

        resultSet = getValue("SELECT `group`,`prefix`,`suffix` FROM `%p%subgroups`");
        while (resultSet.next()) {
            String group = resultSet.getString("group");
            groupsData.set("subgroups." + group + ".prefix", resultSet.getString("prefix"));
            groupsData.set("subgroups." + group + ".suffix", resultSet.getString("suffix"));
        }

        resultSet = getValue("SELECT `group_name`, `gender`, `prefix`, `suffix`  FROM `%p%genders` WHERE `type` = '1'");
        while (resultSet.next()) {
            String group = resultSet.getString("group_name");
            groupsData.set("subgroups." + group + ".genders." + resultSet.getString("gender") + ".prefix", resultSet.getString("prefix"));
            groupsData.set("subgroups." + group + ".genders." + resultSet.getString("gender") + ".suffix", resultSet.getString("suffix"));
        }

        resultSet = getValue("SELECT `uuid`, `group`, `force_group`, `subgroup`, `custom_prefix`, `custom_suffix`, " + "`gender`, `chat_color`, `chat_formatting` FROM `%p%users`");
        while (resultSet.next()) {
            String uuid = resultSet.getString("uuid");
            UserData userData = new UserData(UUID.fromString(uuid));
            userData.set("group", resultSet.getString("group"));
            userData.set("force-group", resultSet.getBoolean("force_group"));
            userData.set("subgroup", resultSet.getString("subgroup"));
            userData.set("custom-prefix", resultSet.getString("custom_prefix"));
            userData.set("custom-suffix", resultSet.getString("custom_suffix"));
            userData.set("gender", resultSet.getString("gender"));
            userData.set("chat-color", resultSet.getString("chat_color"));
            userData.set("chat-formatting", resultSet.getString("chat_formatting"));
            userData.save();
        }
        groupsData.save();

        long ms = System.currentTimeMillis() - startTime;
        Messages.log("§aDownload took " + ms + " ms!");
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public Connection getConnection() {
        return connection;
    }

    public PreparedStatement prepareStatement(String sql) {
        try {
            sql = sql.replace("%p%", getTablePrefix());
            return getConnection().prepareStatement(sql);
        } catch(SQLException e) {
            return null;
        }
    }

}