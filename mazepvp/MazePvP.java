package mazepvp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public final class MazePvP extends JavaPlugin {
	
	public static final String MAIN_FILE_NAME = "maze-names.txt";
	public static final int BOSS_TIMER_MAX = 100;
	public static final int BOSS_HEALTH_BARS = 6;
	
	public static MazePvP theMazePvP;

	public MazeTick tickTask = null;
	public ArrayList<Maze> mazes = new ArrayList<Maze>();
	public int wallChangeTimer = 0;
	public int mazeBossRestoreTimer = 0;
	public boolean showHeads = true;
	public boolean canSpectate = true;
	public boolean specSeeOthers = true;
	public boolean replaceBoss = true;
	public int fightStartDelay = 5*20;
	public List<String> joinSignText;
	public List<String> leaveSignText;
	public List<String> joinMazeText;
	public List<String> joinSpectateMazeText;
	public List<String> leaveMazeText;
	public List<String> fightAlreadyStartedText;
	public List<String> mazeFullText;
	public List<String> joinedOtherText;
	public List<String> countdownText;
	public List<String> fightStartedText;
	public List<String> waitBroadcastText;
	public List<String> waitBroadcastFullText;
	public String startedStateText;
	public String waitingStateText;
	public List<String> fightRespawnText;
	public List<String> lastRespawnText;
	public List<String> playerOutText;
	public List<String> winText;
	public List<String> fightStoppedText;
	public MazeConfig rootConfig;
	
	public MazePvP() {
	}
	
	public void onEnable(){
		theMazePvP = this;
		tickTask = new MazeTick(this);
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, tickTask, 1, 1);
		getServer().getPluginManager().registerEvents(new EventListeners(), this);
		getCommand("mpcreate").setExecutor(new CommandCreateMaze(this));
		getCommand("mplist").setExecutor(new CommandListMazes(this));
		getCommand("mpdelete").setExecutor(new CommandDeleteMaze(this));
		getCommand("mpclear").setExecutor(new CommandClearMaze(this));
		getCommand("mpsetwp").setExecutor(new CommandSetWaitingPlace(this));
		getCommand("mpremovewp").setExecutor(new CommandRemoveWaitingPlace(this));
		getCommand("mpsetplayernum").setExecutor(new CommandSetPlayerNum(this));
		getCommand("mpjoinsign").setExecutor(new CommandCreateJoinSign(this));
		getCommand("mpleavesign").setExecutor(new CommandCreateLeaveSign(this));
		getCommand("mpstopfight").setExecutor(new CommandStopFight(this));
		getCommand("mpset").setExecutor(new CommandSetMazeProp(this));
		getCommand("mpget").setExecutor(new CommandGetMazeProp(this));
		getCommand("mpadditem").setExecutor(new CommandAddItem(this));
		getCommand("mpremoveitem").setExecutor(new CommandRemoveItem(this));
		getCommand("mpaddboss").setExecutor(new CommandAddBoss(this));
		getCommand("mpcopyboss").setExecutor(new CommandCopyBoss(this));
		getCommand("mpremoveboss").setExecutor(new CommandRemoveBoss(this));
		getCommand("mprecover").setExecutor(new CommandRecoverPlayers(this));
		saveDefaultConfig();
		loadConfiguration();
    	MazePvP.writeConfigToYml(rootConfig, getConfig());
    	saveConfig();
		Iterator<World> wit = Bukkit.getServer().getWorlds().iterator();
		while (wit.hasNext()) {
			loadMazeProps(wit.next());
		}
	 }
	 
	public void onDisable() {
		Bukkit.getServer().getScheduler().cancelTasks(this);
		Iterator<World> wit = Bukkit.getServer().getWorlds().iterator();
		while (wit.hasNext()) {
			saveMazeProps(wit.next());
		}
		 Iterator<Maze> it = mazes.iterator();
         while (it.hasNext()) {
         	Maze maze = it.next();
	        if (!maze.canBeEntered && !maze.joinedPlayerProps.isEmpty()) {
		        maze.stopFight(false);
	        }
         }
	 }

	public void saveMazeProps(World world) {
        try
        {
        	File mazeNamesFile = new File(world.getWorldFolder(), MAIN_FILE_NAME);
            PrintWriter nameWriter = null;

            Iterator<Maze> it = mazes.iterator();
            while (it.hasNext()) {
            	Maze maze = it.next();
            	if (maze.mazeWorld != world) continue;
                try
                {
            	if (nameWriter == null) nameWriter = new PrintWriter(new FileWriter(mazeNamesFile, false));
            	nameWriter.printf("%s\n", new Object[]{maze.name});
            	File mazeFile = new File(world.getWorldFolder(), maze.name+".maze");
            	PrintWriter var1 = new PrintWriter(new FileWriter(mazeFile, false));
            	var1.printf("%d %d %d %d %s %d %d %d %d %d %d\n", new Object[] {maze.mazeX, maze.mazeY, maze.mazeZ, maze.mazeSize, "X", maze.canBeEntered?1:0, maze.hasWaitArea?1:0, maze.waitX, maze.waitY, maze.waitZ, maze.height});
                Iterator<Boss> bit = maze.bosses.iterator();
                while (bit.hasNext()) {
                	Boss boss = bit.next();
                	var1.printf("%f\n%s\n", new Object[]{boss.hp, (boss.id==null)?"":boss.id.toString()});
                }
                for (int yy = 0; yy < maze.height; yy++) {
	            	for (int i = 0; i < maze.mazeSize*2+1; i++) {
	            		for (int j = 0; j < maze.mazeSize*2+1; j++) {
	            			if (j == maze.mazeSize*2) var1.printf("%d\n", new Object[] {maze.maze[i][j][yy]});
	            			else var1.printf("%d ", new Object[] {maze.maze[i][j][yy]});
	            		}
	            	}
                }
            	List<int[]> signList;
            	for (int s = 0; s < 2; s++) {
            		if (s == 0) signList = maze.joinSigns;
            		else signList = maze.leaveSigns;
	            	var1.printf("%d\n", new Object[]{signList.size()});
	            	Iterator<int[]> jit = signList.iterator();
	            	while (jit.hasNext()) {
	            		int[] sign = jit.next();
	            		for (int i = 0; i < sign.length; i++) var1.printf((i+1 == sign.length) ? "%d\n":"%d ", new Object[]{sign[i]});
	            	}
            	}
                var1.close();
                YamlConfiguration mazeConfig = new YamlConfiguration();
                MazePvP.writeConfigToYml(maze.configProps, mazeConfig);
                mazeFile = new File(world.getWorldFolder(), maze.name+".yml");
                mazeConfig.save(mazeFile);
                } catch (Exception var4)
                {
                	getLogger().info("Failed to save properties of maze \""+maze.name+"\": "+var4.getMessage());
                }
            }

            if (nameWriter != null) nameWriter.close();
        } catch (Exception var4)
        {
        	getLogger().info("Failed to save maze properties: " + var4);
        }
    }
   
   public void loadConfiguration() {
		Configuration config = getConfig();
		rootConfig = new MazeConfig(false);
		showHeads = config.getBoolean("showHeadsOnSpikes");
		canSpectate = config.getBoolean("canPlayersSpectate");
		specSeeOthers = config.getBoolean("canSpectatorsSeeEachOther");
		replaceBoss = config.getBoolean("replaceMobsWithBoss");
		fightStartDelay = config.getInt("fightStartDelay")*20;
		MazePvP.loadConfigFromYml(rootConfig, getConfig(), getConfig(), true);
		
		joinSignText = config.getStringList("texts.joinSign");
		leaveSignText = config.getStringList("texts.leaveSign");
		joinMazeText = config.getStringList("texts.onJoin");
		joinSpectateMazeText = config.getStringList("texts.onJoinSpectate");
		leaveMazeText = config.getStringList("texts.onLeave");
		fightAlreadyStartedText = config.getStringList("texts.onJoinAfterFightStarted");
		mazeFullText = config.getStringList("texts.onJoinWhenMazeFull");
		joinedOtherText = config.getStringList("texts.onJoinWhenAlreadyJoinedOtherMaze");
		mazeFullText = config.getStringList("texts.onJoinWhenMazeFull");
		countdownText = config.getStringList("texts.countdown");
		fightStartedText = config.getStringList("texts.fightStarted");
		waitBroadcastText = config.getStringList("texts.joinBroadcast");
		waitBroadcastFullText = config.getStringList("texts.joinBroadcastWhenFull");
		fightRespawnText = config.getStringList("texts.fightRespawn");
		lastRespawnText = config.getStringList("texts.fightRespawnLastLife");
		playerOutText = config.getStringList("texts.fightPlayerOut");
		winText = config.getStringList("texts.fightWin");
		fightStoppedText = config.getStringList("texts.fightStopped");
		startedStateText = config.getString("texts.startedState");
		waitingStateText = config.getString("texts.waitingState");
   }

	@SuppressWarnings("deprecation")
	public static void loadBossProps(BossConfig bossProps, Configuration config, int bossNum) {
		String propStr = "boss.";
		if (bossNum >= 0) propStr = "bosses.boss"+(bossNum+1)+".";
		bossProps.name = config.getString(propStr+"name");
		bossProps.maxHp = config.getInt(propStr+"hp");
		bossProps.strength = config.getInt(propStr+"attack");
		bossProps.mazeFloor = config.getInt(propStr+"mazeLevel");
		
		int itemCount = config.getInt(propStr+"drops.itemCount");
		ItemStack tempBossItems[] = new ItemStack[itemCount];
		double tempBossWeighs[] = new double[itemCount];
		int bossItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = config.getInt(propStr+"drops.item"+(i+1)+".id");
			int amount = config.getInt(propStr+"drops.item"+(i+1)+".amount");
			double weigh = config.getDouble(propStr+"drops.item"+(i+1)+".weigh");
			String data = config.getString(propStr+"drops.item"+(i+1)+".data");
			if (id == 0) tempBossItems[i] = null;
			else {
				tempBossItems[i] = new ItemStack(id, amount);
				tempBossWeighs[i] = weigh;
				tempBossItems[i] = MazePvP.setItemData(tempBossItems[i], data);
				bossItemNum++;
			}
		}
		bossProps.dropWeighs = new double[bossItemNum];
		bossProps.dropItems = new ItemStack[bossItemNum];
		int place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempBossItems[i] != null) {
				bossProps.dropItems[place] = tempBossItems[i];
				bossProps.dropWeighs[place] = tempBossWeighs[i];
				place++;
			}
		}
	}

	public void loadMazeProps(World world) {
		try
        {
        	File mazeNamesFile = new File(world.getWorldFolder(), MAIN_FILE_NAME);
            if (!mazeNamesFile.exists())
            {
                return;
            }
            BufferedReader nameReader = new BufferedReader(new FileReader(mazeNamesFile));
            String var2 = "";
            ArrayList<String> mazeNames = new ArrayList<String>();
            while ((var2 = nameReader.readLine()) != null) {
            	if (var2.length() > 0) mazeNames.add(var2);
            }
            nameReader.close();
            
            Iterator<String> it = mazeNames.iterator();
            while (it.hasNext()) {
            	String str = it.next();
            	try
                {
            	File mazeFile = new File(world.getWorldFolder(), str+".maze");
            	if (!mazeFile.exists()) {
                	throw new Exception("Couldn't find maze file \""+str+".maze\" in world \""+world.getName()+"\"");
            	}
	            BufferedReader var1 = new BufferedReader(new FileReader(mazeFile));
	            String[] var3;
            	Maze maze = new Maze();
            	maze.name = str;
	            
	            YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), maze.name+".yml"));
	            MazePvP.loadConfigFromYml(maze.configProps, config, getConfig(), false);
	            maze.loadBossesFromConfig();
	            boolean oldBossFormat = (config.isString("boss.name") && config.isInt("boss.hp") && config.isInt("boss.attack"));
            	if ((var2 = var1.readLine()) != null) {
	            	var3 = var2.split("\\s");
	                if (var3.length < 4 || var3.length > 12) {
	                	var1.close();
	                	throw new Exception("Malformed input");
	                }
	                maze.mazeX = Integer.parseInt(var3[0]);
	                maze.mazeY = Integer.parseInt(var3[1]);
	                maze.mazeZ = Integer.parseInt(var3[2]);
	                maze.mazeSize = Integer.parseInt(var3[3]);
	                if (oldBossFormat) maze.bosses.get(0).hp = (var3.length >= 5) ? Double.parseDouble(var3[4]) : 0;
	                maze.canBeEntered = (var3.length >= 6) ? (Integer.parseInt(var3[5]) != 0) : true;
	        		if (var3.length >= 7) maze.hasWaitArea = Integer.parseInt(var3[6]) != 0;
	        		if (var3.length >= 8) maze.waitX = Integer.parseInt(var3[7]);
	        		if (var3.length >= 9) maze.waitY = Integer.parseInt(var3[8]);
	        		if (var3.length >= 10) maze.waitZ = Integer.parseInt(var3[9]);
                	if (var3.length >= 12) maze.configProps.minPlayers = Integer.parseInt(var3[10]);
                	if (var3.length >= 12) maze.configProps.maxPlayers = Integer.parseInt(var3[11]);
                	if (var3.length >= 11) maze.height = Integer.parseInt(var3[10]);
	                Iterator<Boss> bit = maze.bosses.iterator();
	                int place = 0;
	                while (bit.hasNext()) {
	                	Boss boss = bit.next();
	                	if (!oldBossFormat) {
	                		if ((var2 = var1.readLine()) != null) {
			                	boss.hp = Double.parseDouble(var2);
			        			maze.updateBossHpStr(place);
			                } else {
			                	var1.close();
			                	throw new Exception("Malformed input");
			                }
	                	}
	                	if ((var2 = var1.readLine()) != null) {
		                	if (var2.equals("")) boss.id = null;
		                	else boss.id = UUID.fromString(var2);
		                } else {
		                	var1.close();
		                	throw new Exception("Malformed input");
		                }
	                	place++;
	                }
	                maze.mazeWorld = world;
	                maze.maze = new int[maze.mazeSize*2+1][][];
	                maze.isBeingChanged = new boolean[maze.mazeSize*2+1][][];
	            	for (int yy = 0; yy < maze.height; yy++) {
		            	for (int i = 0; i < maze.mazeSize*2+1; i++) {
		            		if ((var2 = var1.readLine()) == null) {
	                        	var1.close();
	                        	throw new Exception("Malformed input");
		            		}
		                	var3 = var2.split("\\s");
		                    if (var3.length != maze.mazeSize*2+1) {
		                    	var1.close();
		                    	throw new Exception("Malformed input");
		                    }
		                    if (yy == 0) {
		                    	maze.maze[i] = new int[maze.mazeSize*2+1][];
			                    maze.isBeingChanged[i] = new boolean[maze.mazeSize*2+1][];
		                    }
		            		for (int j = 0; j < var3.length; j++) {
			                    if (yy == 0) {
			                    	maze.maze[i][j] = new int[maze.height];
				                    maze.isBeingChanged[i][j] = new boolean[maze.height];
			                    }
		            			maze.maze[i][j][yy] = Integer.parseInt(var3[j]);
		            			maze.isBeingChanged[i][j][yy] = false;
		            		}
		            	}
	            	}
	            	List<int[]> signList;
	            	for (int s = 0; s < 2; s++) {
	            		if (s == 0) signList = maze.joinSigns;
	            		else signList = maze.leaveSigns;
		            	if ((var2 = var1.readLine()) != null && var2.length() > 0) {
		                	int joinSignNum = Integer.parseInt(var2);
		                	for (int i = 0; i < joinSignNum; i++) {
		                		if ((var2 = var1.readLine()) != null) {
		    	                	var3 = var2.split("\\s");
		                			if (var3.length != 7) {
		                				var1.close();
				                    	throw new Exception("Malformed input");
		                			}
		                			int x1 = Integer.parseInt(var3[0]);
		                			int y1 = Integer.parseInt(var3[1]);
		                			int z1 = Integer.parseInt(var3[2]);
		                			int x2 = Integer.parseInt(var3[3]);
		                			int y2 = Integer.parseInt(var3[4]);
		                			int z2 = Integer.parseInt(var3[5]);
		                			int reversed = Integer.parseInt(var3[6]);
		                			signList.add(new int[]{x1, y1, z1, x2, y2, z2, reversed});
		                		} else {
		                			var1.close();
			                    	throw new Exception("Malformed input");
		                		}
		                	}
		                }
	            	}
	        		Collection<Zombie> entities = maze.mazeWorld.getEntitiesByClass(Zombie.class);
	        		bit = maze.bosses.iterator();
	        		while (bit.hasNext()) {
	        			Boss boss = bit.next();
		    			Iterator<Zombie> iter = entities.iterator();
		    			while (iter.hasNext()) {
		    				Zombie en = iter.next();
		    				if (en.getUniqueId().equals(boss.id)) {
		    					boss.entity = en;
		    					break;
		    				}
		    			}
	        		}
	            } else {
	            	var1.close();
	            	throw new Exception("Malformed input");
	            }
	            var1.close();
	    		
	            maze.updateSigns();
	            mazes.add(maze);
	            } catch (Exception var4) {
	            	getLogger().info("Failed to load properties of maze \""+str+"\": "+var4.getMessage());
	            }
            }
        } catch (Exception var4)
        {
        	getLogger().info("Failed to load maze properties: "+var4.getMessage());
        	mazes = new ArrayList<Maze>();
        }
	}

	public Maze findMaze(String mazeName, World world) {
		Iterator<Maze> it = mazes.iterator();
		while (it.hasNext()) {
			Maze maze = it.next();
			if (maze.mazeWorld == world && maze.name.equals(mazeName)) return maze;
		}
		return null;
	}

	public void removeMazes(World world) {
		Iterator<Maze> it = mazes.iterator();
		while (it.hasNext()) {
			Maze maze = it.next();
			if (maze.mazeWorld == world) {
				it.remove();
			}
		}
	}

	public static BlockFace getRandomRotation() {
		int rand = (int)Math.floor(Math.random()*16);
		if (rand == 0) return BlockFace.EAST;
		if (rand == 1) return BlockFace.EAST_NORTH_EAST;
		if (rand == 2) return BlockFace.EAST_SOUTH_EAST;
		if (rand == 3) return BlockFace.NORTH;
		if (rand == 4) return BlockFace.NORTH_EAST;
		if (rand == 5) return BlockFace.NORTH_NORTH_EAST;
		if (rand == 6) return BlockFace.NORTH_NORTH_WEST;
		if (rand == 7) return BlockFace.NORTH_WEST;
		if (rand == 8) return BlockFace.SOUTH;
		if (rand == 9) return BlockFace.SOUTH_EAST;
		if (rand == 10) return BlockFace.SOUTH_SOUTH_EAST;
		if (rand == 11) return BlockFace.SOUTH_SOUTH_WEST;
		if (rand == 12) return BlockFace.SOUTH_WEST;
		if (rand == 13) return BlockFace.WEST;
		if (rand == 14) return BlockFace.WEST_NORTH_WEST;
		return BlockFace.WEST_SOUTH_WEST;
	}

	public static int getSafeY(int posX, int posY, int posZ, World world) {
		posY--;
		int yy, prevPosY = posY;
		for (yy = posY; yy <= 254; yy++) {
			if (world.getBlockAt(posX, yy, posZ).getType().isSolid() && !world.getBlockAt(posX, yy+1, posZ).getType().isSolid()) {
				posY = yy;
				break;
			}
		}
		for (yy = posY; yy >= 0; yy--) {
			if (world.getBlockAt(posX, yy, posZ).getType().isSolid() && !world.getBlockAt(posX, yy+1, posZ).getType().isSolid()) {
				if (posY == prevPosY || posY-prevPosY > prevPosY-yy) posY = yy;
				break;
			}
		}
		posY++;
		return posY;
	}

	@SuppressWarnings("deprecation")
	public static void cleanUpPlayer(Player player, boolean keepEnderChest) {
		player.getInventory().clear();
		if (!player.isDead()) {
			player.setHealth(player.getMaxHealth());
			player.setFoodLevel(20);
		}
		while (!player.getActivePotionEffects().isEmpty())
			player.removePotionEffect(player.getActivePotionEffects().iterator().next().getType());
		player.getInventory().setHelmet(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setBoots(null);
		if (!keepEnderChest) player.getEnderChest().clear();
		player.updateInventory();
	}

	public static ItemStack[] cloneItems(ItemStack[] items) {
		if (items == null) return null;
		ItemStack[] cloneItems = new ItemStack[items.length];
		for (int i = 0; i < items.length; i++) {
			cloneItems[i] = (items[i]==null)?null:items[i].clone();
		}
		return cloneItems;
	}

	public static ItemStack[] getClonedArmor(EntityEquipment armor) {
		ItemStack[] items = armor.getArmorContents();
		ItemStack[] cloneArmor = new ItemStack[items.length];
		for (int i = 0; i < items.length; i++) {
			cloneArmor[i] = (items[i]==null)?null:items[i].clone();
		}
		return cloneArmor;
	}

	public static ItemStack[] cloneISArray(ItemStack[] array) {
		ItemStack[] clone = new ItemStack[array.length];
		for (int i = 0; i < array.length; i++) clone[i] = array[i].clone();
		return clone;
	}

	public static boolean propHasIntValue(String prop) {
		return (prop.equals("playerLives") || prop.equals("playerNum.min") || prop.equals("playerNum.max") || prop.matches("boss[0-9]*\\.hp")
				 || prop.matches("boss[0-9]*\\.attack") || prop.matches("boss[0-9]*\\.mazeLevel"));
	}

	public static boolean propHasDoubleValue(String prop) {
		if (prop.equals("probabilities.groundReappear") || prop.equals("probabilities.chestAppear") || prop.equals("probabilities.enderChestAppear") || prop.equals("probabilities.mobAppear"))
			return true;
		return false;
	}

	public static boolean propHasStringValue(String prop) {
		return (prop.matches("boss[0-9]*\\.name"));
	}

	public static boolean propHasItemValue(String prop) {
		return (prop.startsWith("chestItems") || prop.startsWith("startItems") || prop.matches("boss[0-9]+\\.drops.*"));
	}

	public static String getItemValue(String prop, int bNum) {
		if (prop.startsWith("chestItems")) return "chestItems";
		if (prop.startsWith("startItems")) return "startItems";
		if (prop.matches("boss[0-9]+\\.drops.*")) return "boss"+bNum+".drops";
		return null;
	}

	public static void copyConfigValues(MazeConfig src, MazeConfig dest) {
		dest.bosses = new ArrayList<BossConfig>();
		Iterator<BossConfig> it = src.bosses.iterator();
		while (it.hasNext()) {
			BossConfig boss = it.next();
			dest.bosses.add(boss.clone());
		}
		dest.fightStartedCommand = new ArrayList<String>(src.fightStartedCommand);
		dest.fightRespawnCommand = new ArrayList<String>(src.fightRespawnCommand);
		dest.fightPlayerOutCommand = new ArrayList<String>(src.fightPlayerOutCommand);
		dest.fightWinCommand = new ArrayList<String>(src.fightWinCommand);
		dest.groundReappearProb = src.groundReappearProb;
		dest.chestAppearProb = src.chestAppearProb;
		dest.enderChestAppearProb = src.enderChestAppearProb;
		dest.spawnMobProb = src.spawnMobProb;
		dest.chestWeighs = src.chestWeighs.clone();
		dest.chestItems = MazePvP.cloneISArray(src.chestItems.clone());
		dest.playerMaxDeaths = src.playerMaxDeaths;
		dest.startItems = MazePvP.cloneISArray(src.startItems.clone());
		dest.minPlayers = src.minPlayers;
		dest.maxPlayers = src.maxPlayers;
		dest.blockTypes = MazeConfig.cloneBlockTypes(src.blockTypes);
	}
	
	@SuppressWarnings("deprecation")
	public static void writeConfigToYml(MazeConfig config, Configuration ymlConf) {
		if (config == MazePvP.theMazePvP.rootConfig) {
			ymlConf.set("showHeadsOnSpikes", MazePvP.theMazePvP.showHeads);
			ymlConf.set("canPlayersSpectate", MazePvP.theMazePvP.canSpectate);
			ymlConf.set("canSpectatorsSeeEachOther", MazePvP.theMazePvP.specSeeOthers);
			ymlConf.set("replaceMobsWithBoss", MazePvP.theMazePvP.replaceBoss);
			ymlConf.set("fightStartDelay", MazePvP.theMazePvP.fightStartDelay/20);
		}
		ymlConf.set("playerLives", config.playerMaxDeaths);
        ymlConf.set("playerNum.min", config.minPlayers);
        ymlConf.set("playerNum.max", config.maxPlayers);
        Iterator<BossConfig> bit = config.bosses.iterator();
        int bossNum = 0;
        ymlConf.set("bosses.bossCount", config.bosses.size());
        while (bit.hasNext()) {
        	String propStr = "bosses.boss"+(bossNum+1)+".";
        	BossConfig bossProps = bit.next();
            ymlConf.set(propStr+"name", bossProps.name);
            ymlConf.set(propStr+"hp", bossProps.maxHp);
            ymlConf.set(propStr+"attack", bossProps.strength);
            ymlConf.set(propStr+"mazeLevel", bossProps.mazeFloor);
            int itemNum = bossProps.dropItems.length;
            ymlConf.set(propStr+"drops.itemCount", itemNum);
            for (int i = 0; i < itemNum; i++) {
                ymlConf.set(propStr+"drops.item"+(i+1)+".id", bossProps.dropItems[i].getTypeId());
                ymlConf.set(propStr+"drops.item"+(i+1)+".amount", bossProps.dropItems[i].getAmount());
                ymlConf.set(propStr+"drops.item"+(i+1)+".weigh", bossProps.dropWeighs[i]);
                ymlConf.set(propStr+"drops.item"+(i+1)+".data", MazePvP.getItemData(bossProps.dropItems[i]));
            }
        	bossNum++;
        }
		ymlConf.set("commands.fightStarted", config.fightStartedCommand);
		ymlConf.set("commands.fightRespawn", config.fightRespawnCommand);
		ymlConf.set("commands.fightPlayerOut", config.fightPlayerOutCommand);
        ymlConf.set("commands.fightWin", config.fightWinCommand);
		if (config == MazePvP.theMazePvP.rootConfig) {
			ymlConf.set("texts.startedState", MazePvP.theMazePvP.startedStateText);
			ymlConf.set("texts.waitingState", MazePvP.theMazePvP.waitingStateText);
			ymlConf.set("texts.joinSign", MazePvP.theMazePvP.joinSignText);
			ymlConf.set("texts.leaveSign", MazePvP.theMazePvP.leaveSignText);
			ymlConf.set("texts.joinBroadcast", MazePvP.theMazePvP.waitBroadcastText);
			ymlConf.set("texts.joinBroadcastWhenFull", MazePvP.theMazePvP.waitBroadcastFullText);
			ymlConf.set("texts.countdown", MazePvP.theMazePvP.countdownText);
			ymlConf.set("texts.fightStarted", MazePvP.theMazePvP.fightStartedText);
			ymlConf.set("texts.fightRespawn", MazePvP.theMazePvP.fightRespawnText);
			ymlConf.set("texts.fightRespawnLastLife", MazePvP.theMazePvP.lastRespawnText);
			ymlConf.set("texts.fightPlayerOut", MazePvP.theMazePvP.playerOutText);
			ymlConf.set("texts.fightWin", MazePvP.theMazePvP.winText);
			ymlConf.set("texts.fightStopped", MazePvP.theMazePvP.fightStoppedText);
			ymlConf.set("texts.onJoin", MazePvP.theMazePvP.joinMazeText);
			ymlConf.set("texts.onJoinSpectate", MazePvP.theMazePvP.joinSpectateMazeText);
			ymlConf.set("texts.onLeave", MazePvP.theMazePvP.leaveMazeText);
			ymlConf.set("texts.onJoinAfterFightStarted", MazePvP.theMazePvP.fightAlreadyStartedText);
			ymlConf.set("texts.onJoinWhenMazeFull", MazePvP.theMazePvP.mazeFullText);
			ymlConf.set("texts.onJoinWhenAlreadyJoinedOtherMaze", MazePvP.theMazePvP.joinedOtherText);
		}
        ymlConf.set("probabilities.groundReappear", config.groundReappearProb);
        ymlConf.set("probabilities.chestAppear", config.chestAppearProb);
        ymlConf.set("probabilities.enderChestAppear", config.enderChestAppearProb);
        ymlConf.set("probabilities.mobAppear", config.spawnMobProb);
        int itemNum = config.chestItems.length;
        ymlConf.set("chestItems.itemCount", itemNum);
        for (int i = 0; i < itemNum; i++) {
            ymlConf.set("chestItems.item"+(i+1)+".id", config.chestItems[i].getTypeId());
            ymlConf.set("chestItems.item"+(i+1)+".amount", config.chestItems[i].getAmount());
            ymlConf.set("chestItems.item"+(i+1)+".weigh", config.chestWeighs[i]);
            ymlConf.set("chestItems.item"+(i+1)+".data", MazePvP.getItemData(config.chestItems[i]));
        }
        itemNum = config.startItems.length;
        ymlConf.set("startItems.itemCount", itemNum);
        for (int i = 0; i < itemNum; i++) {
            ymlConf.set("startItems.item"+(i+1)+".id", config.startItems[i].getTypeId());
            ymlConf.set("startItems.item"+(i+1)+".amount", config.startItems[i].getAmount());
            ymlConf.set("startItems.item"+(i+1)+".data", MazePvP.getItemData(config.startItems[i]));
        }
        for (int place = 0; place < config.blockTypes.length; place++) {
            List<String> blockList = new LinkedList<String>();
        	for (int i = 0; i < config.blockTypes[place].length; i++) {
	        	String blocks = "";
	        	for (int j = 0; j < config.blockTypes[place][i].length; j++) {
	        		blocks += config.blockTypes[place][i][j][0];
	        		if (config.blockTypes[place][i][j][1] != 0)
	        			blocks += ":"+config.blockTypes[place][i][j][1];
	        		if (j+1 < config.blockTypes[place][i].length) blocks += " ";
	        	}
	        	blockList.add(blocks);
	        }
        	ymlConf.set("blocks."+MazeConfig.blockTypeNames[place], blockList);
        }
	}

	@SuppressWarnings("deprecation")
	public static void loadConfigFromYml(MazeConfig config, Configuration ymlConf, Configuration rootConf, boolean rootProps) {
		config.playerMaxDeaths = MazeConfig.getInt(ymlConf, rootConf, rootProps, "playerLives"); 
        config.minPlayers = MazeConfig.getInt(ymlConf, rootConf, rootProps, "playerNum.min");
        config.maxPlayers = MazeConfig.getInt(ymlConf, rootConf, rootProps, "playerNum.max");
		config.bosses = new ArrayList<BossConfig>();
		if (ymlConf.isString("boss.name") && ymlConf.isInt("boss.hp") && ymlConf.isInt("boss.attack")) {
			BossConfig bossProps = new BossConfig();
			loadBossProps(bossProps, ymlConf, -1);
			config.bosses.add(bossProps);
		} else {
			int bossNum = ymlConf.getInt("bosses.bossCount");
			for (int i = 0; i < bossNum; i++) {
				BossConfig bossProps = new BossConfig(); 
				loadBossProps(bossProps, ymlConf, i);
				config.bosses.add(bossProps);
			}
		}
		config.fightStartedCommand = MazeConfig.getStringList(ymlConf, rootConf, rootProps, "commands.fightStarted");
		config.fightRespawnCommand = MazeConfig.getStringList(ymlConf, rootConf, rootProps, "commands.fightRespawn");
		config.fightPlayerOutCommand = MazeConfig.getStringList(ymlConf, rootConf, rootProps, "commands.fightPlayerOut");
        config.fightWinCommand = MazeConfig.getStringList(ymlConf, rootConf, rootProps, "commands.fightWin");
        config.groundReappearProb = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "probabilities.groundReappear");
        config.chestAppearProb = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "probabilities.chestAppear");
        config.enderChestAppearProb = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "probabilities.enderChestAppear");
        config.spawnMobProb = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "probabilities.mobAppear");
		
		int itemCount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "chestItems.itemCount");
		
		ItemStack tempChestItems[] = new ItemStack[itemCount];
		double tempChestWeighs[] = new double[itemCount];
		int chestItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = MazeConfig.getInt(ymlConf, rootConf, rootProps, "chestItems.item"+(i+1)+".id");
			int amount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "chestItems.item"+(i+1)+".amount");
			double weigh = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "chestItems.item"+(i+1)+".weigh");
			String data = MazeConfig.getString(ymlConf, rootConf, rootProps, "chestItems.item"+(i+1)+".data");
			if (id == 0) tempChestItems[i] = null;
			else {
				tempChestItems[i] = new ItemStack(id, amount);
				tempChestItems[i] = MazePvP.setItemData(tempChestItems[i], data);
				tempChestWeighs[i] = weigh;
				chestItemNum++;
			}
		}
		config.chestWeighs = new double[chestItemNum];
		config.chestItems = new ItemStack[chestItemNum];
		int place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempChestItems[i] != null) {
				config.chestItems[place] = tempChestItems[i];
				config.chestWeighs[place] = tempChestWeighs[i];
				place++;
			}
		}
		
		itemCount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "startItems.itemCount");
		ItemStack tempStartItems[] = new ItemStack[itemCount];
		int startItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = MazeConfig.getInt(ymlConf, rootConf, rootProps, "startItems.item"+(i+1)+".id");
			int amount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "startItems.item"+(i+1)+".amount");
			String data = MazeConfig.getString(ymlConf, rootConf, rootProps, "startItems.item"+(i+1)+".data");
			if (id == 0) tempStartItems[i] = null;
			else {
				tempStartItems[i] = new ItemStack(id, amount);
				tempStartItems[i] = MazePvP.setItemData(tempStartItems[i], data);
				startItemNum++;
			}
		}
		config.startItems = new ItemStack[startItemNum];
		place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempStartItems[i] != null) {
				config.startItems[place] = tempStartItems[i];
				place++;
			}
		}
		
		config.blockTypes = new int[MazeConfig.blockTypeDimensions.length][][][];
		for (place = 0; place < MazeConfig.blockTypeNames.length; place++) {
			List<String> blocks = MazeConfig.getStringList(ymlConf, rootConf, rootProps, "blocks."+MazeConfig.blockTypeNames[place]);
			Iterator<String> it = blocks.iterator();
			String blockStr = "";
			while (it.hasNext()) {
				blockStr += it.next()+" ";
			}
			String idStr = "", dataStr = "";
			int strPlace = 0;
			boolean readingData = false;
			config.blockTypes[place] = new int
			   [MazeConfig.blockTypeDimensions[place][1]]
			   [MazeConfig.blockTypeDimensions[place][0]]
			   [2];
	        for (int i = 0; i < MazeConfig.blockTypeDimensions[place][1]; i++) {
	        	for (int j = 0; j < MazeConfig.blockTypeDimensions[place][0]; ) {
	        		if (strPlace >= blockStr.length()) {
        				config.blockTypes[place][i][j][0] = 1;
        				config.blockTypes[place][i][j][1] = 0;
        				j++;
	        		} else if (blockStr.charAt(strPlace) >= '0' && blockStr.charAt(strPlace) <= '9') {
	        			if (readingData) dataStr += blockStr.charAt(strPlace);
	        			else idStr += blockStr.charAt(strPlace);
	        		} else if (blockStr.charAt(strPlace) == ':' && !readingData) readingData = true;
	        		else {
	        			if (idStr.length() > 0) {
	        				int id, data;
	        				if (dataStr.length() == 0) data = 0;
	        				else data = Integer.parseInt(dataStr);
	        				id = Integer.parseInt(idStr);
	        				idStr = "";
	        				dataStr = "";
	        				config.blockTypes[place][i][j][0] = id;
	        				config.blockTypes[place][i][j][1] = data;
	        				readingData = false;
	        				j++;
	        			}
	        		}
	        		strPlace++;
	        	}
	        }
		}
	}

	public static boolean propIsCommand(String propName) {
		return (propName.equals("commands.fightStarted") || propName.equals("commands.fightRespawn")
			 || propName.equals("commands.fightPlayerOut") || propName.equals("commands.fightWin"));
	}
	
	public static String toBase64(ItemStack is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos);
        oos.writeObject(is);
        oos.close();
        return new String(Base64Coder.encode(baos.toByteArray()));
    }
	
	public static ItemStack fromBase64(String s) throws IOException, ClassNotFoundException {
		byte [] data = Base64Coder.decode(s);
		BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(data));
		ItemStack o  = (ItemStack)ois.readObject();
		ois.close();
		return o;
	}

	public static String getItemData(ItemStack itemStack) {
		try {
			String str =  toBase64(itemStack);
			return str;
		} catch (IOException e) {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public static ItemStack setItemData(ItemStack itemStack, String data) {
		try {
			if (data.length() == 0) throw new Exception();
			ItemStack newItem = fromBase64(data);
			newItem.setTypeId(itemStack.getTypeId());
			newItem.setAmount(itemStack.getAmount());
			return newItem;
		} catch (Exception e) {
			return itemStack;
		}
	}

}
