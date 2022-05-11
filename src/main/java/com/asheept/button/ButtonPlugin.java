package com.asheept.button;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNullableByDefault;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ButtonPlugin extends JavaPlugin implements CommandExecutor, Listener {

    public boolean isStarted = false;
    public boolean isFoodStarted = false;
    public static long now;
    public static long futrure;

    public HashMap<String, Long> timeHash = new HashMap<>();
    public HashSet<UUID> viewhash = new HashSet<>();
    public Map<Integer, ItemStack> inventory = new HashMap<>();
    public ArrayList<Integer> size = new ArrayList<>();
    public ArrayList<Material> foods = new ArrayList<>();


    @Override
    public void onEnable() {
        ChatConverter.getInstance().initItemConvert();

        for (Material material : Material.values())
        {
            if (material.isEdible())
            {
                foods.add(material);
            }
        }

        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        ChatConverter.getInstance().initItemConvert();

        new BukkitRunnable() {
            @Override
            public void run() {

                Bukkit.getOnlinePlayers().forEach(players ->
                {
                    if (players.getLocation().getY() <= 50)
                    {
                        players.teleport(Objects.requireNonNull(players.getBedSpawnLocation()));
                        removeItem(players);
                    }
                });

            }
        }.runTaskTimer(this, 0L, 1L);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("timer"))
        {
            if (args[0].equalsIgnoreCase("start"))
            {
                if (!isStarted)
                {
                    isStarted = true;
                    now = System.nanoTime();
                    File file = new File(getDataFolder() + File.separator + "config.yml");
                    YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
                    String ticksString = Objects.requireNonNull(fileConfig.get("timer-ticks")).toString();
                    int seconds = Integer.parseInt(ticksString) / 20;

                    Bukkit.getOnlinePlayers().forEach(players -> players.sendTitle("시간 맞추기 시작!", "§6" + seconds + "§r초", 5, 60, 5));

                    new BukkitRunnable() {
                        int ticks = Integer.parseInt(ticksString);
                        int count = 0;

                        @Override
                        public void run() {

                            --ticks;
                            ++count;
                            int seconds = ticks / 20;
                            int countSec = count / 20;

                            Bukkit.getOnlinePlayers().forEach(players ->
                            {
                                if (viewhash.contains(players.getUniqueId()))
                                {
                                    players.sendActionBar("§b" + countSec);
                                }
                            });

                            if (ticks == 0)
                            {
                                Bukkit.getOnlinePlayers().forEach(players ->
                                {
                                    File file = new File(getDataFolder() + File.separator + "config.yml");
                                    YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
                                    String ticksString = Objects.requireNonNull(fileConfig.get("timer-ticks")).toString();
                                    int sec = Integer.parseInt(ticksString) / 20;

                                    players.sendTitle(" ", sec + "초 종료!", 5, 40, 5);
                                    List<Map.Entry<String, Long>> entryList = new ArrayList<>(timeHash.entrySet());

                                    Collections.sort(entryList, new Comparator<Map.Entry<String, Long>>() {
                                        @Override
                                        public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {

                                            return o2.getValue().compareTo(o1.getValue());
                                        }
                                    });

                                    for (Map.Entry<String, Long> entry : entryList)
                                    {
                                        StringBuffer stringBuffer = new StringBuffer();
                                        String timeString = String.valueOf(entry.getValue());

                                        for (int i = 0; i < timeString.length(); i++)
                                        {
                                            if ((timeString.length() - i) % 3 == 0 && i != 0) stringBuffer.append(".");
                                            stringBuffer.append(timeString.charAt(i));
                                        }

                                        players.sendMessage(ChatColor.of(new Color(0x8BAD5E)) + entry.getKey() + " " + ChatColor.of(new Color(0xC7FA79)) + stringBuffer + "§r초");
                                        //Bukkit.broadcastMessage(entry.getKey() + " : " + stringBuffer);
                                    }
                                });

                                timeHash.clear();
                                isStarted = false;
                                cancel();
                            }
                        }
                    }.runTaskTimer(this, 0L, 1L);


                } else
                {
                    player.sendMessage("작동 중");
                }
            } else if (args[0].equalsIgnoreCase("stop"))
            {
                player.sendMessage("게임종료");
                isStarted = false;
            }
        } else if (label.equalsIgnoreCase("timerset"))
        {
            String times = args[0];

            File file = new File(getDataFolder() + File.separator + "config.yml");
            YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
            String ticks = Objects.requireNonNull(fileConfig.get("timer-ticks")).toString();

            try
            {
                fileConfig.set("timer-ticks", Integer.parseInt(times));
                fileConfig.save(file);
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            player.sendMessage("시간 변경§6 " + Integer.parseInt(ticks) / 20 + "§r초 §e→ §6" + Integer.parseInt(times) / 20 + "§r초");
        } else if (label.equalsIgnoreCase("food"))
        {
            if(args[0].equalsIgnoreCase("start"))
            {
                String value = args[1];
                isFoodStarted = true;
                Bukkit.getOnlinePlayers().forEach(players ->
                {
                    players.setSaturation(3.0F);
                });

                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        Bukkit.getOnlinePlayers().forEach(players ->
                        {
                            if(players.getSaturation() >= 3)
                            {
                                players.setSaturation(1.0F);
                            }

          /*                      Player sleep = Bukkit.getPlayer("Sleepground");
                                Player hsrd = Bukkit.getPlayer("HSRD");
                                Player suhyen = Bukkit.getPlayer("SUHYEN");
                                Player duck = Bukkit.getPlayer("DUCKGAE");

                                player.sendActionBar(replaceName(sleep) + " §r" + sleep.getFoodLevel() + " §7|| " + (replaceName(hsrd) + " §r" + hsrd.getFoodLevel() + " §7|| "
                                        + (replaceName(duck) + " §r" + duck.getFoodLevel() + " §7|| " + (replaceName(suhyen) + " §r" + suhyen.getFoodLevel()))));*/
                                players.setExhaustion(players.getExhaustion() + Float.parseFloat(value));;
                                players.sendActionBar("Exhaustion §d" + players.getExhaustion() + " §rSaturation §c" + players.getSaturation());
                        });
                        if(!isFoodStarted)
                        {
                            cancel();
                        }
                    }
                }.runTaskTimer(this, 0L, 1L);

            }
            else if(args[0].equalsIgnoreCase("stop"))
            {
                isFoodStarted = false;
            }

        }
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();


        if (event.getHand() == EquipmentSlot.HAND)
        {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
            {
                if (player.getInventory().getItemInMainHand().getType() == Material.NETHER_STAR)
                {
                    if (isStarted)
                    {
                        if (!timeHash.containsKey(player.getName()))
                        {
                            futrure = System.nanoTime();

                            timeHash.put(replaceName(player), TimeUnit.NANOSECONDS.toMillis(futrure - now));
                            Long times = timeHash.get(replaceName(player));
                            StringBuffer stringBuffer = new StringBuffer();
                            String timeString = String.valueOf(times);

                            for (int i = 0; i < timeString.length(); i++)
                            {
                                if ((timeString.length() - i) % 3 == 0 && i != 0) stringBuffer.append(".");
                                stringBuffer.append(timeString.charAt(i));
                            }

                            player.sendMessage(ChatColor.of(new Color(0xC7FA79)) + "" + stringBuffer + "초");
                        } else
                        {
                            player.sendTitle(" ", "§c이미 참여했습니다", 5, 20, 5);
                        }

                    } else
                    {
                        player.sendTitle(" ", "§c지금은 사용할 수 없습니다", 5, 20, 5);
                    }
                } else if (player.getInventory().getItemInMainHand().getType() == Material.TURTLE_EGG)
                {
                    if (isStarted)
                    {
                        if (!viewhash.contains(player.getUniqueId()))
                        {
                            if (player.getInventory().getItemInMainHand().getAmount() > 1)
                            {
                                player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                            } else if (player.getInventory().getItemInMainHand().getAmount() == 1)
                            {
                                player.getInventory().getItemInMainHand().setAmount(0);
                            }

                            player.sendMessage("§c아이템을 사용했습니다");
                            viewhash.add(player.getUniqueId());
                            new BukkitRunnable() {
                                int ticks = 41;

                                @Override
                                public void run() {
                                    --ticks;

                                    if (ticks == 0)
                                    {
                                        cancel();
                                        viewhash.remove(player.getUniqueId());
                                    }
                                }
                            }.runTaskTimer(this, 0L, 1L);
                        } else
                        {
                            player.sendTitle(" ", "§c지금은 사용할 수 없습니다", 5, 20, 5);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.getInventory().getItemInMainHand().getType() == Material.DIRT || player.getInventory().getItemInMainHand().getType() == Material.GRASS_BLOCK)
        {
            if (block.getRelative(BlockFace.DOWN).getType() != Material.BEDROCK)
            {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (isBed(block.getType()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Animals)
        {
            ((Animals) entity).setAge(10);
        }
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Animals)
        {
            int movX = event.getFrom().getBlockX() - event.getTo().getBlockX();
            int movZ = event.getFrom().getBlockZ() - event.getTo().getBlockZ();

            if (Math.abs(movX) > 0 || Math.abs(movZ) > 0)
            {
                //작물들이랑 farmland 수정 작업 -> getBlock 에서만
                if (entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.GRASS_BLOCK && entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.DIRT && entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.FARMLAND && entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR && entity.getLocation().getBlock().getType() != Material.FARMLAND && entity.getLocation().getBlock().getType() != Material.WHEAT && entity.getLocation().getBlock().getType() != Material.WHEAT_SEEDS && entity.getLocation().getBlock().getType() != Material.CARROT && entity.getLocation().getBlock().getType() != Material.POTATOES && entity.getLocation().getBlock().getType() != Material.MELON_SEEDS && entity.getLocation().getBlock().getType() != Material.MELON_STEM && entity.getLocation().getBlock().getType() != Material.BEETROOTS && entity.getLocation().getBlock().getType() != Material.FARMLAND)
                {
                    ((Animals) entity).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 3, true, true, true));
                    ((Animals) entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 3, true, true, true));
                }
            }
        }
    }


    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player)
        {
            Player player = (Player) event.getEntity();

            if (event.getCause() == EntityDamageEvent.DamageCause.STARVATION)
            {
                double health = player.getHealth();
                double damage = event.getFinalDamage();

                if (health <= damage)
                {
                    player.setGameMode(GameMode.SPECTATOR);
                    Bukkit.getOnlinePlayers().forEach(players ->
                    {
                        players.sendTitle(" ", replaceName(player) + " 탈락", 5, 40, 5);
                    });
                }
                return;
            } else if (event.getCause() == EntityDamageEvent.DamageCause.FALL)
            {
                event.setCancelled(true);

                return;
            }
            else if (event.getCause() == EntityDamageEvent.DamageCause.LAVA)
            {
                event.setCancelled(true);

                return;
            }
            else if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)
            {
                event.setCancelled(true);

                return;
            }

            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager instanceof Player)
            {
                Player damagerPlayer = (Player) damager;
                if (damagerPlayer.getInventory().getItemInMainHand().getType() == Material.STICK)
                {
                    event.setDamage(0);
                } else
                {
                    event.setCancelled(true);
                }
            }

        }
    }

    public static String replaceName(Player player) {
        String name = player.getName();
        if (name.equalsIgnoreCase("Lessso"))
        {
            name = name.replace(player.getName(), net.md_5.bungee.api.ChatColor.of(new java.awt.Color(0xE67E18)) + "레쏘§r");
        } else if (name.equalsIgnoreCase("Sleepground"))
        {
            name = name.replace(player.getName(), "§b잠뜰§r");
        } else if (name.equalsIgnoreCase("DUCKGAE"))
        {
            name = name.replace(player.getName(), "§6덕개§r");
        } else if (name.equalsIgnoreCase("lLeeShin"))
        {
            name = name.replace(player.getName(), ChatColor.of(new Color(0x7FF8D2)) + "이신§r");
        } else if (name.equalsIgnoreCase("SUHYEN"))
        {
            name = name.replace(player.getName(), "§d수현§r");
        } else if (name.equalsIgnoreCase("HSRD"))
        {
            name = name.replace(player.getName(), "§c라더§r");
        } else if (name.equalsIgnoreCase("Phillip_MS"))
        {
            name = name.replace(player.getName(), "§d필립§r");
        }
        return name;
    }

    public boolean isBed(Material material) {
        return (material.toString().contains("_BED"));
    }

    void removeItem(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++)
        {
            if (player.getInventory().getItem(slot) != null)
            {
                if (foods.contains(Objects.requireNonNull(player.getInventory().getItem(slot)).getType()))
                {
                    inventory.put(slot, player.getInventory().getItem(slot));
                }

                // player.sendMessage(slot + " " + inventory.get(slot) + " ");
            }
        }

        Random random = new Random();
        Iterator<Integer> keys = inventory.keySet().iterator();

        while (keys.hasNext())
        {
            int key = keys.next();
            size.add(key);
            //  player.sendMessage("§b" + key);
        }

        try
        {
            int slot = size.get(random.nextInt(size.size()));

            if (player.getInventory().getItem(slot) != null)
            {
                String name = ChatConverter.getInstance().convertItemName(player.getInventory().getItem(slot).getType());
                player.getInventory().getItem(slot).setAmount(0);
                //  player.sendMessage("§c" + slot + " §e " + name);
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 1.2F);
                player.sendMessage("§r음식을 잃었습니다 §7- " + ChatColor.of(new Color(0x6CB254)) + name);

            }

        } catch (IllegalArgumentException e)
        {
            player.sendTitle(" ", "§c더 이상 음식이 없습니다.", 5, 10, 5);
        }

        inventory.clear();
        size.clear();
    }
}
