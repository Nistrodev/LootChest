package fr.nistro.lootchest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class LootChest extends JavaPlugin implements CommandExecutor {

    private FileConfiguration config;
    private File configFile;

    @Override
    public void onEnable() {
        File subDirectory = new File("plugins/LootChest");
        if (!subDirectory.exists()) {
            subDirectory.mkdir();
        }
        try {
            File targetFile = new File("plugins/LootChest/config.yml");
            InputStream modelFile = getClass().getResourceAsStream("/config-model.yml");
            if (!targetFile.exists()) {
                Files.copy(modelFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.out.println("Une erreur est survenue lors de la copie du fichier: " + e.getMessage());
        }
        config = YamlConfiguration.loadConfiguration    (new File("plugins/LootChest/config.yml"));

        // Vérifiez que les données de configuration sont valides
        if (!config.isString("chest-name")) {
            getLogger().severe("chest-name n'est pas défini dans le fichier de configuration!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!config.isString("chest-lore")) {
            getLogger().severe("chest-lore n'est pas défini dans le fichier de configuration!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!config.isConfigurationSection("items")) {
            getLogger().severe("Aucun objet n'a été défini dans le fichier de configuration!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Enregistrez les commandes
        getCommand("lootchest").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être utilisée que par un joueur.");
            return true;
        }

        Player player = (Player) sender;

        String chestName = ChatColor.translateAlternateColorCodes('&', config.getString("chest-name"));
        String chestLore = ChatColor.translateAlternateColorCodes('&', config.getString("chest-lore"));

        List<ItemStack> items = config.getConfigurationSection("items").getKeys(false).stream()
                .map(itemId -> {
                    ConfigurationSection itemSection = config.getConfigurationSection("items." + itemId);
                    if (!itemSection.isString("name") || !itemSection.isList("lore") || !itemSection.isString("material") || !itemSection.isInt("spawn_chance")) {
                        getLogger().warning("Un des objets n'a pas été configuré correctement");
                        return null;
                    }
                    String name = itemSection.getString("name");
                    List<String> lore = itemSection.getStringList("lore");
                    Material material;
                    try {
                        material = Material.valueOf(itemSection.getString("material"));
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Material non valide pour l'objet " + itemId);
                        return null;
                    }
                    int spawnChance = itemSection.getInt("spawn_chance");
                    if (spawnChance < 0 || spawnChance > 100) {
                        getLogger().warning("Chance de spawn non valide pour l'objet " + itemId);
                        return null;
                    }
                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                    meta.setLore(lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList()));
                    item.setItemMeta(meta);
                    return new ItemWithChance(item, spawnChance);
                })
                .filter(itemWithChance -> itemWithChance != null && ThreadLocalRandom.current().nextInt(100) < itemWithChance.getChance())
                .map(ItemWithChance::getItem)
                .collect(Collectors.toList());

        Inventory chest = Bukkit.createInventory(null, 27, chestName);
        items.forEach(chest::addItem);

        ItemStack chestItem = new ItemStack(Material.CHEST);
        ItemMeta meta = chestItem.getItemMeta();
        meta.setDisplayName(chestName);
        meta.setLore(Collections.singletonList(chestLore));
        chestItem.setItemMeta(meta);
        BlockStateMeta chestMeta = (BlockStateMeta) chestItem.getItemMeta();
        Chest chestBlock = (Chest) chestMeta.getBlockState();
        chestBlock.getInventory().setContents(chest.getContents());
        chestMeta.setBlockState(chestBlock);
        chestItem.setItemMeta(chestMeta);


        player.getInventory().addItem(chestItem);

        player.sendMessage(ChatColor.GREEN + "Vous avez reçu un coffre personnalisé!");

        return true;
    }

    // Classe interne pour stocker un objet et sa chance de spawn
    private static class ItemWithChance {
        private final ItemStack item;
        private final int chance;

        public ItemWithChance(ItemStack item, int chance) {
            this.item = item;
            this.chance = chance;
        }

        public ItemStack getItem() {
            return item;
        }

        public int getChance() {
            return chance;
        }
    }
}



