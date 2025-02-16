package it.moro.smartspawner;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TrialSpawner;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.spawner.TrialSpawnerConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SpawnerManager implements Listener, CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private EntityType SpawnerEntity;

    public SpawnerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    //------------------------------------------------------------- TAB COMPLETE ---------------------------------------------------------
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("spawnerpickaxe") && args.length == 1) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return players.stream().filter(p -> p.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (command.getName().equalsIgnoreCase("givespawner") || command.getName().equalsIgnoreCase("givetrialspawner")) {
            if (args.length == 1) {
                List<String> entities = Arrays.stream(EntityType.values()).map(EntityType::name).map(String::toLowerCase)
                        .toList();
                return entities.stream().filter(e -> e.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            } else if (args.length == 2) {
                List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                return players.stream().filter(p -> p.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    //------------------------------------------------------------- COMANDI ---------------------------------------------------------
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("spawnerpickaxe")) {
                if (player.hasPermission("smartspawner.spawnerpickaxe")) {
                    if (args.length == 0) {
                        givePickaxe(player);
                        return true;
                    } else if (args.length == 1) {
                        Player target = Bukkit.getPlayerExact(args[0]);
                        if (target != null) {
                            givePickaxe(target);
                            return true;
                        }
                    }
                } else {
                    player.sendMessage("§cNon hai il permesso per usare questo comando!");
                    return true;
                }
            } else if (command.getName().equalsIgnoreCase("givespawner")) {
                if (player.hasPermission("smartspawner.givespawner")) {
                    if (args.length == 1) {
                        EntityType entityType = null;
                        try {
                            entityType = EntityType.valueOf(args[0].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            player.sendMessage("L'entità non è stata trovata!");
                        }
                        if (entityType != null) {
                            giveSpawner(player, entityType.toString());
                            return true;
                        }
                    } else if (args.length == 2) {
                        Player target = Bukkit.getPlayerExact(args[1]);
                        EntityType entityType = null;
                        try {
                            entityType = EntityType.valueOf(args[0].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            player.sendMessage("L'entità non è stata trovata!");
                        }
                        if (target != null && entityType != null) {
                            giveSpawner(target, entityType.toString());
                        } else {
                            player.sendMessage("§cGiocatore non trovato!");
                        }
                        return true;
                    }
                } else {
                    player.sendMessage("§cNon hai il permesso per usare questo comando!");
                    return true;
                }
            } else if (command.getName().equalsIgnoreCase("givetrialspawner")) {
                if (player.hasPermission("smartspawner.givetrialspawner")) {
                    if (args.length == 1) {
                        EntityType entityType = null;
                        try {
                            entityType = EntityType.valueOf(args[0].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            player.sendMessage("L'entità non è stata trovata!");
                        }
                        if (entityType != null) {
                            giveTrialSpawner(player, entityType.toString());
                            return true;
                        }
                    } else if (args.length == 2) {
                        Player target = Bukkit.getPlayerExact(args[1]);
                        EntityType entityType = null;
                        try {
                            entityType = EntityType.valueOf(args[0].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            player.sendMessage("L'entità non è stata trovata!");
                        }
                        if (target != null && entityType != null) {
                            giveTrialSpawner(target, entityType.toString());
                        } else {
                            player.sendMessage("§cGiocatore non trovato!");
                        }
                        return true;
                    }
                } else {
                    player.sendMessage("§cNon hai il permesso per usare questo comando!");
                    return true;
                }
            } else {
                player.sendMessage("§cComando non riconosciuto!");
                return true;
            }

        } else if (sender instanceof ConsoleCommandSender) {
            if (command.getName().equalsIgnoreCase("spawnerpickaxe")) {
                if(args.length == 1) {
                    Player player = Bukkit.getPlayerExact(args[0]);
                    if (player != null) {
                        givePickaxe(player);
                    } else {
                        plugin.getLogger().info("Il giocatore non è stato trovato!");
                    }
                    return true;
                } else {
                    plugin.getLogger().info("Giocatore mancante!");
                }
            } else if (command.getName().equalsIgnoreCase("givespawner")) {
                if(args.length == 2) {
                    Player player = Bukkit.getPlayerExact(args[1]);
                    EntityType entityType = null;
                    try {
                        entityType = EntityType.valueOf(args[0].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().info("L'entità non è stata trovata!");
                    }
                    if (player != null && entityType != null) {
                        giveSpawner(player, entityType.toString());
                        return true;
                    } else {
                        plugin.getLogger().info("Il giocatore non è stato trovato!");
                    }
                } else {
                    plugin.getLogger().info("Giocatore o entità mancante!");
                }
            } else if (command.getName().equalsIgnoreCase("givetrialspawner")) {
                if(args.length == 2) {
                    Player player = Bukkit.getPlayerExact(args[1]);
                    EntityType entityType = null;
                    try {
                        entityType = EntityType.valueOf(args[0].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().info("L'entità non è stata trovata!");
                    }
                    if (player != null && entityType != null) {
                        giveTrialSpawner(player, entityType.toString());
                        return true;
                    } else {
                        plugin.getLogger().info("Il giocatore non è stato trovato!");
                    }
                } else {
                    plugin.getLogger().info("Giocatore o entità mancante!");
                }
            }
        }
        return false;
    }

    public void givePickaxe(Player player) {
        ItemStack spawnerPickaxe = createSpawnerPickaxe();
        player.getInventory().addItem(spawnerPickaxe);
        player.sendMessage(
                "Hai ricevuto: " + Objects.requireNonNull(config.getString("recipes.item-name"))
                        .replaceAll("&", "§") + "!");
    }

    public void giveSpawner(Player player, String entity) {
        EntityType entityType = null;
        try {
            entityType = EntityType.valueOf(entity.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("Tipo entità non valido.");
        }
        ItemStack spawnerItem = createSpawnerItem(entityType);
        player.getInventory().addItem(spawnerItem);
        player.sendMessage("Hai ricevuto uno Spawner di " + entity);
    }

    public void giveTrialSpawner(Player player, String entity) {
        if (Bukkit.getBukkitVersion().startsWith("1_21") || Bukkit.getBukkitVersion().startsWith("1.21")) {
            EntityType value;
            try {
                value = EntityType.valueOf(entity);
                ItemStack spawner = new ItemStack(Material.TRIAL_SPAWNER);
                BlockStateMeta metaSpawner = (BlockStateMeta) spawner.getItemMeta();
                TrialSpawner StateSpawner = (TrialSpawner) metaSpawner.getBlockState();
                TrialSpawnerConfiguration SpawnerConf = StateSpawner.getNormalConfiguration();
                SpawnerConf.setSpawnedType(value);
                metaSpawner.setBlockState(StateSpawner);
                spawner.setItemMeta(metaSpawner);
                player.getInventory().addItem(spawner);
                player.sendMessage("Hai ricevuto un TrialSpawner di " + entity);
            } catch (IllegalArgumentException e) {
                if (player != null) {
                    player.sendMessage("Entità non valida!");
                }
            }
        } else {
            player.sendMessage("§cTrialSpawner è disponibile solo per versioni 1.21!");
        }
    }

//------------------------------------------------------------- RACCOLTA SPAWNER ---------------------------------------------------------

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isEqualsItem(item, createSpawnerPickaxe()) ||
                !plugin.getConfig().getBoolean("custom-tool-required") &&
                        item.getType().name().contains("PICKAXE")) {
            if (block.getType() == Material.TRIAL_SPAWNER) {
                if (plugin.getConfig().getBoolean("trialspawner-collection")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (block.getState() instanceof TrialSpawner spawner) {
                                TrialSpawnerConfiguration spawnerconfig = spawner.getNormalConfiguration();
                                ItemStack spawnerItem = new ItemStack(Material.TRIAL_SPAWNER);
                                BlockStateMeta meta = (BlockStateMeta) spawnerItem.getItemMeta();
                                if (spawnerconfig.getSpawnedType() == null) {
                                    return;
                                }
                                meta.setBlockState(spawner);
                                spawnerItem.setItemMeta(meta);
                                block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
                                block.setType(Material.AIR);
                                player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
                                block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation(), 20, Material.TRIAL_SPAWNER.createBlockData());
                                if (event.getPlayer().getInventory().getItemInMainHand().getItemMeta() instanceof Damageable damageable) {
                                    ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
                                    if (hand.getType().getMaxDurability() - damageable.getDamage() < plugin.getConfig().getInt("damage-item-value") + 1) {
                                        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                                        player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
                                    } else {
                                        damageable.setDamage(damageable.getDamage() + plugin.getConfig().getInt("damage-item-value"));
                                        event.getPlayer().getInventory().getItemInMainHand().setItemMeta((ItemMeta) damageable);
                                    }
                                }
                            }
                        }
                    }.runTaskLater(plugin, 20L);
                }
            }
        }
    }


    @EventHandler//(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void breakSpawner(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isEqualsItem(item, createSpawnerPickaxe()) ||
                !plugin.getConfig().getBoolean("custom-tool-required") &&
                        item.getType().name().contains("PICKAXE")) {
            if (block.getType() == Material.SPAWNER) {
                if (plugin.getConfig().getBoolean("spawner-collection")) {
                    if (block.getState() instanceof CreatureSpawner spawner) {
                        event.setExpToDrop(0);
                        EntityType spawnerType = spawner.getSpawnedType();
                        ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
                        BlockStateMeta meta = (BlockStateMeta) spawnerItem.getItemMeta();
                        if (meta != null) {
                            spawner.setSpawnedType(spawnerType);
                            spawner.update();
                            meta.setBlockState(spawner);
                            spawnerItem.setItemMeta(meta);
                            block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
                            if (player.getInventory().getItemInMainHand().getItemMeta() instanceof Damageable damageable) {
                                ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
                                if (hand.getType().getMaxDurability() - damageable.getDamage() < plugin.getConfig().getInt("damage-item-value") + 1) {
                                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                                    player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
                                } else {
                                    damageable.setDamage(damageable.getDamage() + plugin.getConfig().getInt("damage-item-value") - 1);
                                    event.getPlayer().getInventory().getItemInMainHand().setItemMeta(damageable);
                                }
                            }
                        }
                    }
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    public boolean isEqualsItem(ItemStack item, ItemStack pickaxe) {
        if (Objects.equals(item.getType(), pickaxe.getType())) {
            ItemMeta itemMeta = item.getItemMeta();
            ItemMeta pickaxeMeta = pickaxe.getItemMeta();
            if (itemMeta != null || pickaxeMeta != null) {
                assert itemMeta != null;
                if (Objects.equals(itemMeta.displayName(), pickaxeMeta.displayName())) {
                    if (Objects.equals(itemMeta.displayName(), pickaxeMeta.displayName())) {
                        if (plugin.getConfig().getBoolean("recipes.enchants-required")) {
                            Map<Enchantment, Integer> itemEnchants = itemMeta.getEnchants();
                            Map<Enchantment, Integer> pickaxeEnchants = pickaxeMeta.getEnchants();
                            return Objects.equals(itemEnchants, pickaxeEnchants);
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
//------------------------------------------------------------- ANNULLO ATTACCO CON PICCONE ---------------------------------------------------------

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.GOLDEN_PICKAXE
                    && item.hasItemMeta()
                    && item.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)
                    && item.getItemMeta().getEnchantLevel(Enchantment.SILK_TOUCH) == 2) {
                event.setCancelled(true);
            }
        }
    }

    //------------------------------------------------------------- ANNULLO MODIFICA INCUDINE ---------------------------------------------------------
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (plugin.getConfig().getBoolean("anvil-protection")) {
            ItemStack firstItem = event.getInventory().getItem(0);
            ItemStack secondItem = event.getInventory().getItem(1);
            if (isItemProtected(firstItem) || isItemProtected(secondItem)) {
                event.setResult(null);
            }
        }
    }

    private boolean isItemProtected(ItemStack item) {
        return item != null && item.getType() == Material.GOLDEN_PICKAXE
                && item.getItemMeta().getEnchants().containsKey(Enchantment.SILK_TOUCH)
                && item.getItemMeta().getEnchantLevel(Enchantment.SILK_TOUCH) == 2;
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (plugin.getConfig().getBoolean("grindstone-protection")) {
            ItemStack firstItem = event.getInventory().getItem(0);
            ItemStack secondItem = event.getInventory().getItem(1);
            if (isItemProtected(firstItem) || isItemProtected(secondItem)) {
                event.setResult(null);
            }
        }
    }

    //--------------------------------------------------- VERIFICO L'ENTITA DELLO SPAWNER PRIMA DI PIAZZARLO --------------------------------------------
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem != null && newItem.getType() == Material.SPAWNER) {

            if (newItem.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                BlockState blockState = blockStateMeta.getBlockState();

                if (blockState instanceof CreatureSpawner spawner) {
                    EntityType entityType = spawner.getSpawnedType();
                    SpawnerEntity = Objects.requireNonNullElse(entityType, EntityType.PIG);
                }
            }
        } else if (newItem != null && newItem.getType() == Material.TRIAL_SPAWNER) {
            if (newItem.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                BlockState blockState = blockStateMeta.getBlockState();
                if (blockState instanceof TrialSpawner spawner) {
                    TrialSpawnerConfiguration spawnerconfig = spawner.getNormalConfiguration();
                    EntityType entityType = spawnerconfig.getSpawnedType();
                    SpawnerEntity = Objects.requireNonNullElse(entityType, EntityType.PIG);
                }
            }
        }
    }

    //------------------------------------------------------------- PIAZZO LO SPAWNER ---------------------------------------------------------
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlock();
        if (placedBlock.getType() == Material.SPAWNER) {
            if (plugin.getConfig().getBoolean("spawner-place")) {
                if (SpawnerEntity != null) {
                    World world = placedBlock.getWorld();
                    int x = placedBlock.getX();
                    int y = placedBlock.getY();
                    int z = placedBlock.getZ();
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(Material.SPAWNER);
                    CreatureSpawner spawner = (CreatureSpawner) block.getState();
                    spawner.setSpawnedType(SpawnerEntity);
                    spawner.update();
                }
            }
        } else if (placedBlock.getType() == Material.TRIAL_SPAWNER) {
            if (plugin.getConfig().getBoolean("trialspawner-place")) {
                if (SpawnerEntity != null) {
                    ItemStack placed = event.getItemInHand();
                    BlockStateMeta hand_meta = (BlockStateMeta) placed.getItemMeta();
                    TrialSpawner cs = (TrialSpawner) hand_meta.getBlockState();
                    TrialSpawnerConfiguration spawnerConfig = cs.getNormalConfiguration();
                    try {
                        EntityType entity = spawnerConfig.getSpawnedType();
                        TrialSpawner spawner = (TrialSpawner) placedBlock.getState();
                        TrialSpawnerConfiguration sc = spawner.getNormalConfiguration();
                        sc.setSpawnedType(entity);
                        spawner.update(true, false);

                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private ItemStack createSpawnerItem(EntityType entityType) {
        ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) spawnerItem.getItemMeta();
        CreatureSpawner spawner = (CreatureSpawner) meta.getBlockState();
        spawner.setSpawnedType(entityType);
        meta.setBlockState(spawner);
        spawnerItem.setItemMeta(meta);
        return spawnerItem;
    }

    //------------------------------------------------------------- RICETTA / CREAZIONE PICCONE -----------------------------------------------------
    public void addRecipeFromConfig() {
        if (plugin.getConfig().getBoolean("recipes.enable")) {
            List<String> shape = plugin.getConfig().getStringList("recipes.crafting.shape");
            ItemStack item = createSpawnerPickaxe();
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "piccone_spawner"), item);

            // Imposta la forma della ricetta
            recipe.shape(shape.toArray(new String[0]));

            // Controlla se la sezione ingredients esiste
            ConfigurationSection ingredients = plugin.getConfig().getConfigurationSection("recipes.crafting.ingredients");
            if (ingredients == null) {
                plugin.getLogger().warning("La sezione ingredients non è definita nel file di configurazione.");
                return;
            }

            // Imposta gli ingredienti
            for (String keyChar : ingredients.getKeys(false)) {
                char ingredientChar = keyChar.charAt(0);
                Material material = Material.getMaterial(Objects.requireNonNull(ingredients.getString(keyChar)));
                if (material != null) {
                    recipe.setIngredient(ingredientChar, material);
                } else {
                    plugin.getLogger().warning("Materiale non valido per l'ingrediente: " + ingredients.getString(keyChar));
                }
            }

            Bukkit.addRecipe(recipe);
        }
    }

    //------------------------------------------------------------- PICCONE SPAWNER ---------------------------------------------------------
    public ItemStack createSpawnerPickaxe() {
        String materialName = plugin.getConfig().getString("recipes.item");
        Material material = null;
        try {
            assert materialName != null;
            material = Material.matchMaterial(materialName.toUpperCase());
            if (material == null) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (plugin.getConfig().getInt("item-initial-damage") != 0) {
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(plugin.getConfig().getInt("item-initial-damage"));
                item.setItemMeta(damageable);
            }
        }
        if (!Objects.equals(plugin.getConfig().getString("recipes.item-name"), "")) {
            String spawnerPickaxeName = Objects.requireNonNull(config.getString("recipes.item-name")).replaceAll("&", "§");
            meta.displayName(Component.text(spawnerPickaxeName));
        }
        if (plugin.getConfig().getBoolean("recipes.enchants-required")) {
            List<String> enchantments = plugin.getConfig().getStringList("recipes.enchants");
            for (String enchant : enchantments) {
                try {
                    String[] parts = enchant.split(":");
                    if (parts.length != 2) {
                        continue;
                    }
                    String enchantName = parts[0];
                    int level = Integer.parseInt(parts[1]);
                    Enchantment enchantment = Enchantment.getByName(enchantName.toUpperCase());
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, level, true);
                    } else {
                        System.out.println("Incantesimo '" + enchantName + "' non trovato.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Livello incantesimo non valido in: " + enchant);
                }
            }
            if (plugin.getConfig().getBoolean("recipes.hide-enchants")) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

        }
        if (Bukkit.getBukkitVersion().startsWith("1_21") || Bukkit.getBukkitVersion().startsWith("1.21")) {
            NamespacedKey speedKey = new NamespacedKey(plugin, "attack_speed");
            AttributeModifier attackSpeedModifier = new AttributeModifier(
                    speedKey,
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeedModifier);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

}
