package mazepvp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class MazePvP extends JavaPlugin {
	
	public static final String MAIN_FILE_NAME = "maze-names.txt";
	public static final int BOSS_TIMER_MAX = 100;
	public static final int BOSS_HEALTH_BARS = 6;
	
	public static MazePvP theMazePvP;

	public MazeTick tickTask = null;
	public ArrayList<Maze> mazes = new ArrayList<Maze>();
	public String mazeBossName = "MazeBoss";
	public int mazeBossMaxHp = 0;
	public int mazeBossStrength = 0;
	public double mazeSpawnMobProb = 1.0/3.0;
	public double mazeChestAppearProb = 0.3;
	public double mazeGroundReappearProb = 0.1;
	public double[] mazeChestWeighs;
	public ItemStack[] mazeChestItems;
	public ItemStack[] startItems;
	public ItemStack[] mazeBossDropItems;
	public double[] mazeBossDropWeighs;
	public int wallChangeTimer = 0;
	public boolean showHeads = true;
	
	public MazePvP() {
	}
	
	public void onEnable(){
		theMazePvP = this;
		tickTask = new MazeTick(this);
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, tickTask, 1, 1);
		getServer().getPluginManager().registerEvents(new EventListeners(), this);
		getCommand("createmaze").setExecutor(new CommandCreateMaze(this));
		getCommand("listmazes").setExecutor(new CommandListMazes(this));
		getCommand("deletemaze").setExecutor(new CommandDeleteMaze(this));
		saveDefaultConfig();
		loadConfiguration();
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
	 }

   public void saveMazeProps(World world)
    {
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
            	var1.printf("%d %d %d %d %f %d\n", new Object[] {maze.mazeX, maze.mazeY, maze.mazeZ, maze.mazeSize, maze.mazeBossHp, maze.canBeEntered?1:0});
                var1.printf("%s\n", new Object[]{(maze.mazeBossId==null)?"":maze.mazeBossId.toString()});
            	for (int i = 0; i < maze.mazeSize*2+1; i++) {
            		for (int j = 0; j < maze.mazeSize*2+1; j++) {
            			if (j == maze.mazeSize*2) var1.printf("%d\n", new Object[] {maze.maze[i][j]});
            			else var1.printf("%d ", new Object[] {maze.maze[i][j]});
            		}
            	}
                var1.close();
                } catch (Exception var4)
                {
                	getLogger().info("Failed to load properties of maze \""+maze.name+"\": "+var4.getMessage());
                }
            }

            if (nameWriter != null) nameWriter.close();
        } catch (Exception var4)
        {
        	getLogger().info("Failed to save maze properties: " + var4);
        }
    }
   
   @SuppressWarnings("deprecation")
   public void loadConfiguration() {
		Configuration config = getConfig();
		showHeads = config.getBoolean("showHeadsOnSpikes");
		mazeBossName = config.getString("boss.name");
		mazeBossMaxHp = config.getInt("boss.hp");
		mazeBossStrength = config.getInt("boss.attack");
		mazeGroundReappearProb = config.getDouble("probabilities.groundReappear");
		mazeChestAppearProb = config.getDouble("probabilities.chestAppear");
		mazeSpawnMobProb = config.getDouble("probabilities.mobAppear");
		
		int itemCount = config.getInt("chestItems.itemCount");
		
		ItemStack tempChestItems[] = new ItemStack[itemCount];
		double tempChestWeighs[] = new double[itemCount];
		int chestItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = config.getInt("chestItems.item"+(i+1)+".id");
			int amount = config.getInt("chestItems.item"+(i+1)+".amount");
			double weigh = config.getDouble("chestItems.item"+(i+1)+".weigh");
			if (id == 0) tempChestItems[i] = null;
			else {
				tempChestItems[i] = new ItemStack(id, amount);
				tempChestWeighs[i] = weigh;
				chestItemNum++;
			}
		}
		mazeChestWeighs = new double[chestItemNum];
		mazeChestItems = new ItemStack[chestItemNum];
		int place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempChestItems[i] != null) {
				mazeChestItems[place] = tempChestItems[i];
				mazeChestWeighs[place] = tempChestWeighs[i];
				place++;
			}
		}
		
		itemCount = config.getInt("boss.drops.itemCount");
		
		ItemStack tempBossItems[] = new ItemStack[itemCount];
		double tempBossWeighs[] = new double[itemCount];
		int bossItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = config.getInt("boss.drops.item"+(i+1)+".id");
			int amount = config.getInt("boss.drops.item"+(i+1)+".amount");
			double weigh = config.getDouble("boss.drops.item"+(i+1)+".weigh");
			if (id == 0) tempBossItems[i] = null;
			else {
				tempBossItems[i] = new ItemStack(id, amount);
				tempBossWeighs[i] = weigh;
				bossItemNum++;
			}
		}
		mazeBossDropWeighs = new double[bossItemNum];
		mazeBossDropItems = new ItemStack[bossItemNum];
		place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempBossItems[i] != null) {
				mazeBossDropItems[place] = tempBossItems[i];
				mazeBossDropWeighs[place] = tempBossWeighs[i];
				place++;
			}
		}
		
		itemCount = config.getInt("startItems.itemCount");
		ItemStack tempStartItems[] = new ItemStack[itemCount];
		int startItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = config.getInt("startItems.item"+(i+1)+".id");
			int amount = config.getInt("startItems.item"+(i+1)+".amount");
			if (id == 0) tempStartItems[i] = null;
			else {
				tempStartItems[i] = new ItemStack(id, amount);
				startItemNum++;
			}
		}
		startItems = new ItemStack[startItemNum];
		place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempStartItems[i] != null) {
				startItems[place] = tempStartItems[i];
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
	            if ((var2 = var1.readLine()) != null) {
	            	var3 = var2.split("\\s");
	                if (var3.length < 4 || var3.length > 6) {
	                	var1.close();
	                	throw new Exception("Malformed input");
	                }
	                maze.mazeX = Integer.parseInt(var3[0]);
	                maze.mazeY = Integer.parseInt(var3[1]);
	                maze.mazeZ = Integer.parseInt(var3[2]);
	                maze.mazeSize = Integer.parseInt(var3[3]);
	                maze.mazeBossHp = (var3.length >= 5) ? Double.parseDouble(var3[4]) : 0;
	                maze.canBeEntered = (var3.length >= 6) ? (Integer.parseInt(var3[5]) != 0) : true;
	                maze.updateBossHpStr();
	                if ((var2 = var1.readLine()) != null) {
	                	if (var2.equals("")) maze.mazeBossId = null;
	                	else maze.mazeBossId = UUID.fromString(var2);
	                } else {
	                	var1.close();
	                	throw new Exception("Malformed input");
	                }
	                maze.mazeWorld = world;
	                maze.maze = new int[maze.mazeSize*2+1][];
	                maze.isBeingChanged = new boolean[maze.mazeSize*2+1][];
	            	for (int i = 0; ; i++) {
	            		if ((var2 = var1.readLine()) == null) {
	            			if (i < maze.mazeSize*2) {
	                        	var1.close();
	                        	throw new Exception("Malformed input");
	            			}
	            			break;
	            		}
	                	var3 = var2.split("\\s");
	                    if (var3.length != maze.mazeSize*2+1) {
	                    	var1.close();
	                    	throw new Exception("Malformed input");
	                    }
	                    maze.maze[i] = new int[maze.mazeSize*2+1];
	                    maze.isBeingChanged[i] = new boolean[maze.mazeSize*2+1];
	            		for (int j = 0; j < var3.length; j++) {
	            			maze.maze[i][j] = Integer.parseInt(var3[j]);
	            			maze.isBeingChanged[i][j] = false;
	            		}
	            	}
	        		Collection<Zombie> entities = maze.mazeWorld.getEntitiesByClass(Zombie.class);
	    			Iterator<Zombie> iter = entities.iterator();
	    			while (iter.hasNext()) {
	    				Zombie en = iter.next();
	    				if (en.getUniqueId().equals(maze.mazeBossId)) {
	    					maze.mazeBoss = en;
	    					//System.out.println("FOUND BOSS");
	    					break;
	    				}
	    			}
	            } else {
	            	var1.close();
	            	throw new Exception("Malformed input");
	            }
	            mazes.add(maze);
	            var1.close();
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

	public void giveStartItemsToPlayer(Player player) {
		for (int i = 0; i < startItems.length; i++)
			player.getInventory().addItem(startItems[i].clone());
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

}
