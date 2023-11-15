package me.fabichan.agcminetools;

import me.fabichan.agcminetools.Eventlistener.MinecraftPlayerJoinListener;
import me.fabichan.agcminetools.Utils.CommandManager;
import me.fabichan.agcminetools.Utils.DatabaseClient;
import me.fabichan.agcminetools.Utils.Interfaces.ICommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Main extends JavaPlugin {
    public JDA jda;
    
    public DatabaseClient dbclient;

    @Override
    public void onEnable() {
        getLogger().info("MineTools werden gestartet...");
        saveDefaultConfig();
        try {
            String botToken = getConfig().getString("bot.token");
            if (botToken == null || botToken.isEmpty() || botToken.equals("DISCORD_BOT_TOKEN")) {
                getLogger().severe("Bot-Token ist nicht gesetzt!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            jda = JDABuilder.createDefault(botToken).build();
            jda.awaitReady();
            getLogger().info(String.format("Bot %s ist online!", jda.getSelfUser().getName()));
        } catch (Exception e) {
            getLogger().severe(String.format("Bot konnte nicht gestartet werden: %s", e.getMessage()));
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        dbclient = new DatabaseClient(getConfig().getString("database.host"), getConfig().getString("database.port"), getConfig().getString("database.database"), getConfig().getString("database.username"), getConfig().getString("database.password"));
        if (dbclient.getConnection() == null) {
            getLogger().severe("Datenbankverbindung konnte nicht hergestellt werden!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        dbclient.initDatabase();
        CommandManager commandManager = new CommandManager();
        jda.addEventListener(commandManager);
        Guild guild = null;
        try{
            guild = jda.getGuildById(Objects.requireNonNull(getConfig().getString("bot.guildid")));
        } catch (Exception e) {
            //
        }
            if (guild == null) {
            getLogger().severe("Guild-ID ist nicht gesetzt oder der Bot ist nicht auf dem Server!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        for (ICommand command : commandManager.getCommands()) {
            getLogger().info(String.format("Slash-Command %s wird registriert!", command.getName()));
            guild.upsertCommand(command.getCommandData()).queue();
            getLogger().info(String.format("Slash-Command %s wurde registriert!", command.getName()));
        }
        registerMinecraftEvents();


    }

    private void registerMinecraftEvents() {
        getServer().getPluginManager().registerEvents(new MinecraftPlayerJoinListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("MineTools werden gestoppt...");
        // close database connection
        
        if (dbclient != null) {
            dbclient.closeConnection();
        }
        
        if (jda != null) {
            jda.shutdown();
        }
        
    }
}
