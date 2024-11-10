package it.moro.smartspawner;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TrialSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
        if (args.length == 1) {
            List<String> entities = Arrays.stream(EntityType.values()).map(EntityType::name).map(String::toLowerCase)
                    .toList();
            return entities.stream().filter(e -> e.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        } else if (args.length == 2) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return players.stream().filter(p -> p.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    //------------------------------------------------------------- COMANDI ---------------------------------------------------------
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = (Player) sender;

        if (player.hasPermission("smartspawner.spawnerpickaxe")) {
            if (command.getName().equalsIgnoreCase("spawnerpickaxe")) {
                ItemStack spawnerPickaxe = createSpawnerPickaxe();
                player.getInventory().addItem(spawnerPickaxe);
                player.sendMessage(
                        "Hai ricevuto: " + Objects.requireNonNull(config.getString("recipes.item-name")).replaceAll("&", "§") + "!");
                return true;
            }
        }

        if (player.hasPermission("smartspawner.givespawner")) {
            if (command.getName().equalsIgnoreCase("givespawner")) {
                if (args.length < 1 || args.length > 2) {
                    player.sendMessage("Uso corretto: /givespawner [entità] [nome giocatore (opzionale)]");
                    return true;
                }

                String entityTypeArg = args[0].toUpperCase();
                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeArg);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("Tipo entità non valido.");
                    return true;
                }

                ItemStack spawnerItem = createSpawnerItem(entityType);

                if (args.length == 2) {
                    Player targetPlayer = Bukkit.getPlayer(args[1]);
                    if (targetPlayer == null) {
                        player.sendMessage("Giocatore non trovato.");
                        return true;
                    }
                    targetPlayer.getInventory().addItem(spawnerItem);
                    targetPlayer.sendMessage("Hai ricevuto uno spawner di " + entityType.name().toLowerCase() + ".");
                } else {
                    player.getInventory().addItem(spawnerItem);
                    player.sendMessage("Hai ricevuto uno spawner di " + entityType.name().toLowerCase() + ".");
                }
                return true;
            }
        } else {
            player.sendMessage("Non hai il permesso per eseguire questo comando!");
            return true;
        }

        return false;
    }

    //------------------------------------------------------------- RACCOLTA SPAWNER ---------------------------------------------------------

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.GOLDEN_PICKAXE &&
                item.getItemMeta().getEnchants().containsKey(Enchantment.SILK_TOUCH) &&
                item.getItemMeta().getEnchantLevel(Enchantment.SILK_TOUCH) == 2) {
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
                                meta.setBlockState((BlockState) spawner);
                                spawnerItem.setItemMeta(meta);
                                block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
                                block.setType(Material.AIR);
                                player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
                                block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation(), 20, Material.TRIAL_SPAWNER.createBlockData());
                                if (event.getPlayer().getInventory().getItemInMainHand().getItemMeta() instanceof Damageable damageable) {
                                    ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
                                    if (hand.getType().getMaxDurability() - damageable.getDamage() < plugin.getConfig().getInt("damage-item-value")+1) {
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
        if (item.getType() == Material.GOLDEN_PICKAXE &&
                item.getItemMeta().getEnchants().containsKey(Enchantment.SILK_TOUCH) &&
                item.getItemMeta().getEnchantLevel(Enchantment.SILK_TOUCH) == 2) {
            /*if (block.getType() == Material.TRIAL_SPAWNER) {
                TrialSpawner spawner = (TrialSpawner) block.getState();
                TrialSpawnerConfiguration spawnerconfig = spawner.getNormalConfiguration();
                ItemStack spawnerItem = new ItemStack(Material.TRIAL_SPAWNER);
                BlockStateMeta meta = (BlockStateMeta) spawnerItem.getItemMeta();
                if (spawnerconfig.getSpawnedType() == null) {
                    return;
                }
                meta.setBlockState((BlockState)spawner);
                spawnerItem.setItemMeta(meta);
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
            } else */
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
                                if (hand.getType().getMaxDurability() - damageable.getDamage() < plugin.getConfig().getInt("damage-item-value")+1) {
                                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                                    player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
                                } else {
                                    damageable.setDamage(damageable.getDamage() + plugin.getConfig().getInt("damage-item-value")-1);
                                    event.getPlayer().getInventory().getItemInMainHand().setItemMeta((ItemMeta) damageable);
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
        // Crea il pickaxe come un oggetto d'oro
        ItemStack item = new ItemStack(Material.GOLDEN_PICKAXE);

        // Ottieni i metadati dell'oggetto
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(plugin.getConfig().getInt("item-initial-damage"));
            item.setItemMeta(damageable);
        }

        // Modifica i metadati per aggiungere nome e incantamenti
        String spawnerPickaxeName = Objects.requireNonNull(config.getString("recipes.item-name")).replaceAll("&", "§");
        meta.displayName(Component.text(spawnerPickaxeName));
        meta.addEnchant(Enchantment.SILK_TOUCH, 2, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);

        // Imposta i metadati aggiornati
        item.setItemMeta(meta);

        return item;
    }

}
