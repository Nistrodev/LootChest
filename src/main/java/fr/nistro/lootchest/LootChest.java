package fr.nistro.lootchest;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class LootChest extends JavaPlugin implements CommandExecutor {

    private FileConfiguration config;
    private File configFile;

    @Override
    public void onEnable() {

        File subDirectory = new File(getDataFolder(), "LootChest");
        if (!subDirectory.exists()) {
            subDirectory.mkdir();
        }
        configFile = new File(subDirectory, "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        getCommand("lootchest").setExecutor(this);
        config = YamlConfiguration.loadConfiguration(configFile);
        config.addDefault("chest-name", "&6Coffre par défaut");
        config.addDefault("chest-lore", "Un coffre générique");
        config.options().copyDefaults(true);
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être utilisée que par un joueur.");
            return true;
        }

        Player player = (Player) sender;

        String chestName = config.getString("chest-name");
        String chestLore = config.getString("chest-lore");

        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', chestName));
        meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', chestLore)));
        chest.setItemMeta(meta);

        player.getInventory().addItem(chest);

        player.sendMessage(ChatColor.GREEN + "Vous avez reçu un coffre personnalisé!");

        return true;
    }
}
