package mazepvp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class Maze {
	public static final int MAZE_PASSAGE_WIDTH = 4;
	public static final int MAZE_PASSAGE_HEIGHT = 5;
	public static final int MAZE_PASSAGE_DEPTH = 5;
	public static final int WALL_CHANGE_SPEED = 60;
	public static final int BOSS_RESTORE_SPEED = 100;
	public static final int FIGHT_STOP_SPEED = 60;
	
	public World mazeWorld;
	public int mazeSize, height = 1;
	public int mazeX, mazeY, mazeZ;
	public ArrayList<Boss>bosses;
	public MazeConfig configProps;
	public int[][][] maze;
	public boolean[][][] isBeingChanged;
	public List<MazeCoords> blocksToRemove = new LinkedList<MazeCoords>();
	public List<MazeCoords> blocksToRestore = new LinkedList<MazeCoords>();
	public HashMap<String, Boolean> playerInsideMaze = new HashMap<String, Boolean>();
	public HashMap<String, PlayerProps> joinedPlayerProps = new HashMap<String, PlayerProps>();
	public static HashMap<String, Boolean> playerInsideAMaze = new HashMap<String, Boolean>();
	public String name = "";
	public boolean updatingHp = false;
	public boolean canBeEntered = true;
	public boolean hasWaitArea = false;
	public int waitX = 0, waitY = 0, waitZ = 0;
	public boolean fightStarted = false;
	public int fightStartTimer = 0;
	public LinkedList<int[]> joinSigns = new LinkedList<int[]>();
	public LinkedList<int[]> leaveSigns = new LinkedList<int[]>();
	public Player lastPlayer;
	
	public Maze() {
		configProps = new MazeConfig();
	}
	 
	public int blockToMazeCoord(int blockCoord) {
	 	if (blockCoord%(MAZE_PASSAGE_WIDTH+1) == 0) return blockCoord/(MAZE_PASSAGE_WIDTH+1)*2;
	 	if (blockCoord == mazeSize*(1+MAZE_PASSAGE_WIDTH)) return mazeSize;
	 	return blockCoord/(MAZE_PASSAGE_WIDTH+1)*2+1;
	 }
	 
	public int mazeToBlockCoord(int mazeCoord) {
	 	if (mazeCoord%2 == 0) return (mazeCoord/2)*(MAZE_PASSAGE_WIDTH+1);
	 	return (mazeCoord/2)*(MAZE_PASSAGE_WIDTH+1)+1;
	}
	 
	public int mazeToBlockYCoord(int yCoord) {
	 	return yCoord*(Maze.MAZE_PASSAGE_HEIGHT+2);
	}
	 
	public int blockToMazeYCoord(int yCoord) {
		int pos = yCoord/(Maze.MAZE_PASSAGE_HEIGHT+2);
	 	return Math.max(Math.min(pos, height-1), 0);
	}
	
	public Coord3D getMazeBossNewLocation(World worldObj) {
		return getMazeBossNewLocation(worldObj, 0, false);
	}
	
	public Coord3D getMazeBossNewLocation(World worldObj, int currentY) {
		return getMazeBossNewLocation(worldObj, currentY, true);
	}
	
	public Coord3D getMazeBossNewLocation(World worldObj, int currentY, boolean keepY) {
	 	double bossX, bossZ, bossY;
	 	int count = 0;
	 	while (true) {
	 		bossX = mazeToBlockCoord((int)Math.floor(Math.random()*mazeSize)*2+1);
	 		bossZ = mazeToBlockCoord((int)Math.floor(Math.random()*mazeSize)*2+1);
	 		if (keepY) bossY = mazeToBlockYCoord(currentY);
	 		else bossY = mazeToBlockYCoord((int)Math.floor(Math.random()*height));
	 		if (!worldObj.getBlockAt((int)Math.floor(mazeX+bossX), (int)Math.floor(mazeY+bossY), (int)Math.floor(mazeZ+bossZ)).isEmpty()) break;
	 		count++;
	 		if (count >= 100) break;
	 	}
	 	return new Coord3D(mazeX+bossX+MAZE_PASSAGE_WIDTH*0.5, mazeY+bossY+1, mazeZ+bossZ+MAZE_PASSAGE_WIDTH*0.5);
	 }
	 
	public void relocateMazeBoss(boolean coolDown, Boss boss) {
	 	World worldObj = boss.entity.getWorld();
		relocateMazeBoss(coolDown, boss, getMazeBossNewLocation(worldObj));
	}
	
	public void relocateMazeBoss(boolean coolDown, Boss boss, Coord3D bossLoc) {
		relocateMazeBoss(coolDown, boss, bossLoc, boss.entity.getLocation().getYaw(), boss.entity.getLocation().getPitch());
	}

	public void relocateMazeBoss(boolean coolDown, Boss boss, Coord3D bossLoc, float yaw, float pitch) {
		int j;
	 	World worldObj = boss.entity.getWorld();
		for (j = 0; j <= 8; j++) {
			worldObj.playEffect(boss.entity.getLocation(), Effect.SMOKE, j);
			if (j != 4) {
				worldObj.playEffect(new Location(worldObj, boss.entity.getLocation().getX(), boss.entity.getLocation().getY()+1, boss.entity.getLocation().getZ()), Effect.SMOKE, j);
			}
		}
		boss.entity.teleport(new Location(worldObj, bossLoc.x, bossLoc.y, bossLoc.z, yaw, pitch));
		for (j = 0; j <= 8; j++) {
			worldObj.playEffect(boss.entity.getLocation(), Effect.SMOKE, j);
			if (j != 4) {
				worldObj.playEffect(new Location(worldObj, boss.entity.getLocation().getX(), boss.entity.getLocation().getY()+1, boss.entity.getLocation().getZ()), Effect.SMOKE, j);
			}
		}
		if (coolDown) boss.tpCooldown = 20;
	}
	
	public void makeNewMazeBoss(int place) {
		makeNewMazeBoss(place, null);
	}
	 
	public void makeNewMazeBoss(int place, Coord3D loc) {
		Boss boss = bosses.get(place);
	 	if (boss.entity != null) return;
	 	Coord3D bossLoc;
	 	if (loc == null) {
	 		bossLoc = getMazeBossNewLocation(mazeWorld);
		 	if (MazePvP.theMazePvP.replaceBoss) {
		 		Monster switchEn = null;
		 		int switchNum = 1;
		 		Collection<Monster> entities = mazeWorld.getEntitiesByClass(Monster.class);
				Iterator<Monster> iter = entities.iterator();
				while (iter.hasNext()) {
					Monster en = iter.next();
					if (!en.isDead() && isInsideMaze(en.getLocation()) && !isBoss(en)) {
						if (Math.random() < 1.0/switchNum) switchEn = en;
						switchNum++;
					}
				}
				if (switchEn != null) {
					bossLoc = new Coord3D(switchEn.getLocation().getX(), switchEn.getLocation().getY(), switchEn.getLocation().getZ());
					switchEn.remove();
					final Coord3D constLoc = bossLoc;
					final Maze constMaze = this;
					final int constPlace = place;
					new BukkitRunnable() {
					    Maze maze = constMaze;
					    Coord3D loc = constLoc;
					    int place = constPlace;
					    public void run() {
					        maze.makeNewMazeBoss(place, loc);
					    }
					}.runTaskLater(MazePvP.theMazePvP, 5L);
					return;
				}
		 	}
	 	} else bossLoc = loc;
	 	boss.targetPlayer = "";
	 	boss.targetTimer = 0;
	 	boss.entity = (Zombie)mazeWorld.spawnEntity(new Location(mazeWorld, bossLoc.x, bossLoc.y, bossLoc.z), EntityType.ZOMBIE);
	 	boss.id = boss.entity.getUniqueId();
	 	boss.hp = configProps.bosses.get(place).maxHp;
	 	updateBossHpStr(place);
	 	mazeWorld.playEffect(new Location(mazeWorld, boss.entity.getLocation().getX(), boss.entity.getLocation().getY()+1, boss.entity.getLocation().getZ()), Effect.MOBSPAWNER_FLAMES, 0);
	 	ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
	 	LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
	 	meta.setColor(Color.fromRGB(0, 0, 0));
	 	boots.setItemMeta(meta);
	 	ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
	 	meta = (LeatherArmorMeta) legs.getItemMeta();
	 	meta.setColor(Color.fromRGB(0, 0, 0));
	 	legs.setItemMeta(meta);
	 	ItemStack plate = new ItemStack(Material.LEATHER_CHESTPLATE);
	 	meta = (LeatherArmorMeta) plate.getItemMeta();
	 	meta.setColor(Color.fromRGB(0, 0, 0));
	 	plate.setItemMeta(meta);
	 	EntityEquipment ee = boss.entity.getEquipment();
	 	ee.setHelmet(new ItemStack(Material.SKULL_ITEM, 1, (short)1));
	 	ee.setBoots(boots);
	 	ee.setLeggings(legs);
	 	ee.setChestplate(plate);
	 }
	 
	 public boolean isBoss(Monster en) {
		if (!(en instanceof Zombie)) return false;
		Iterator<Boss> it = bosses.iterator();
		while (it.hasNext()) {
			Boss boss = it.next();
			if (boss.entity == en) return true;
		}
		return false;
	}

	public void setToLookedAt(boolean[][][] isLookedAt, int x, int z, int y, boolean matters) {
	 	if (x%2 == 1 && z%2 == 1 && !matters) return;
	 	isLookedAt[x][z][y] = true;
	 }
	 
	 public boolean canBeReached(int startX, int startZ, int endX, int endZ, int startY, int maxSteps, boolean[][] wasVisited) {
	 	if (startX == endX && startZ == endZ) return true;
	 	LinkedList<MazeCoords> elements = new LinkedList<MazeCoords>();
	 	ArrayList<MazeCoords> prevElements = new ArrayList<MazeCoords>();
	 	elements.add(new MazeCoords(startX, startY, startZ));
	 	elements.add(new MazeCoords(-1, -1, -1));
	 	boolean reached = false;
	 	int steps = 0;
	 	MazeCoords coords;
	 	while (steps < maxSteps && !elements.isEmpty()) {
	 		coords = (MazeCoords)elements.remove();
	 		if (coords.x == -1) {
	 	    	if (!elements.isEmpty()) elements.push(new MazeCoords(-1, -1, -1));
	 			steps++;
	 		} else {
	 	    	wasVisited[coords.x][coords.z] = true;
		    		prevElements.add(coords);
		    		if ((coords.z == endZ && (coords.x-1 == endX || coords.x+1 == endX))
		    		 || (coords.x == endX && (coords.z-1 == endZ || coords.z+1 == endZ))) {
		    			reached = true;
		    			break;
		    		}
		    		if (coords.x > 0 && !wasVisited[coords.x-1][coords.z] && maze[coords.x-1][coords.z][startY] != 1) elements.push(new MazeCoords(coords.x-1, coords.y, coords.z));
		    		if (coords.x < mazeSize*2 && !wasVisited[coords.x+1][coords.z] && maze[coords.x+1][coords.z][startY] != 1) elements.push(new MazeCoords(coords.x+1, coords.y, coords.z));
		    		if (coords.z > 0 && !wasVisited[coords.x][coords.z-1] && maze[coords.x][coords.z-1][startY] != 1) elements.push(new MazeCoords(coords.x, coords.y, coords.z-1));
		    		if (coords.z < mazeSize*2 && !wasVisited[coords.x][coords.z+1] && maze[coords.x][coords.z+1][startY] != 1) elements.push(new MazeCoords(coords.x, coords.y, coords.z+1));
	 		}
	 	}
	 	Iterator<MazeCoords> it = prevElements.iterator();
	 	while (it.hasNext()) {
	 		coords = (MazeCoords)it.next();
	 		wasVisited[coords.x][coords.z] = false;
	 	}
	 	return reached;
	 }
	 
	 public boolean pillarIsAlone(int x, int z, int rx, int rz, int y) {
	 	if (rx < 0 || rx > mazeSize*2 || rz < 0 || rz > mazeSize*2) return false;
	 	if (!(x-1 == rx && z == rz) && x > 0 && maze[x-1][z][y] == 1) return false;
	 	if (!(x+1 == rx && z == rz) && x < mazeSize*2 && maze[x+1][z][y] == 1) return false;
	 	if (!(x == rx && z-1 == rz) && z > 0 && maze[x][z-1][y] == 1) return false;
	 	if (!(x == rx && z+1 == rz) && z < mazeSize*2 && maze[x][z+1][y] == 1) return false;
	 	return true;
	 }
	   
	   public void removeMazeBlocks(int mazeX, int mazeZ, int mazeY, World worldObj) {
		if (mazeX <= 0 || mazeX >= mazeSize*2 || mazeZ <= 0 || mazeZ >= mazeSize*2) return;
		if (isBeingChanged[mazeX][mazeZ][mazeY]) return;
	   	if ((maze[mazeX][mazeZ][mazeY] != 1 && (mazeX%2 == 0 || mazeZ%2 == 0)) || (mazeX%2 == 0 && mazeZ%2 == 0)) return;
		if (worldObj.getBlockAt(this.mazeX+mazeToBlockCoord(mazeX), this.mazeY+mazeToBlockYCoord(mazeY), this.mazeZ+mazeToBlockCoord(mazeZ)).getType() == Material.AIR) return;
		blocksToRemove.add(new MazeCoords(mazeX, mazeY, mazeZ, 20));
	   	isBeingChanged[mazeX][mazeZ][mazeY] = true;
	   }
	   
	   public void restoreMazeBlocks(int mazeX, int mazeZ, int mazeY) {
	   	if (mazeX <= 0 || mazeX >= mazeSize*2 || mazeZ <= 0 || mazeZ >= mazeSize*2) return;
	   	if (isBeingChanged[mazeX][mazeZ][mazeY]) return;
	   	if ((maze[mazeX][mazeZ][mazeY] != 0 && (mazeX%2 == 0 || mazeZ%2 == 0)) || (mazeX%2 == 0 && mazeZ%2 == 0)) return;
	   	int blockNum;
	   	if (mazeX%2 == 1 && mazeZ%2 == 1) blockNum = MAZE_PASSAGE_WIDTH*MAZE_PASSAGE_WIDTH;
	   	else blockNum = MAZE_PASSAGE_WIDTH*(MAZE_PASSAGE_HEIGHT+1);
	   	blocksToRestore.add(new MazeCoords(mazeX, mazeY, mazeZ, blockNum));
	   	isBeingChanged[mazeX][mazeZ][mazeY] = true;
	   }

	public boolean isInsideMaze(Location location) {
		if (location.getWorld() != mazeWorld) return false;
		return location.getX()-0.5 >= mazeX && location.getX()-0.5 <= mazeX+mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
		   &&  location.getZ()-0.5 >= mazeZ && location.getZ()-0.5 <= mazeZ+mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
		   &&  location.getY() >= mazeY-Maze.MAZE_PASSAGE_DEPTH && location.getY() <= mazeY+height*(Maze.MAZE_PASSAGE_HEIGHT+3)-2-0.5;
	}

	public void updateBossHpStr(int place) {
		Boss boss = bosses.get(place);
		int maxHp = configProps.bosses.get(place).maxHp;
		if (maxHp <= 0) return;
		if (updatingHp) return;
		updatingHp = true;
		double hpFraction = boss.hp/maxHp;
		double barNum = MazePvP.BOSS_HEALTH_BARS*hpFraction;
		if (hpFraction > 0.5) boss.hpStr = "§2";
		else if (hpFraction > 0.25) boss.hpStr = "§6";
		else boss.hpStr = "§4";
		for (int i = 0; i < MazePvP.BOSS_HEALTH_BARS; i++) {
			if (barNum-i > 0.75) boss.hpStr += "█";
			else if (barNum-i > 0.25 || i == 0) boss.hpStr += "▌";
			else boss.hpStr += " ";
		}
		updatingHp = false;
	}

	@SuppressWarnings("deprecation")
	public void removeEntrances() {
		for (int yStart = 0; yStart < height; yStart ++) {
			for (int xx = 0; xx < mazeSize*2+1; xx ++) {
				for (int zz = 0; zz < mazeSize*2+1; zz ++) {
					if (!(xx == 0 || xx == mazeSize*2) && !(zz == 0 || zz == mazeSize*2)) continue;
					if (maze[xx][zz][yStart] != 1) {
						int sx = mazeToBlockCoord(xx);
						int ex = sx+((zz == 0 || zz == mazeSize*2) ? MAZE_PASSAGE_WIDTH-1 : 0);
						int sz = mazeToBlockCoord(zz);
						int ez = sz+((xx == 0 || xx == mazeSize*2) ? MAZE_PASSAGE_WIDTH-1 : 0);
						for (int xxx = sx; xxx <= ex; xxx++) {
							for (int zzz = sz; zzz <= ez; zzz++) {
								for (int yyy = 1; yyy <= MAZE_PASSAGE_HEIGHT+1; yyy++) {
									int bId, bData;
									if (xx%2 == 0) {
			    						bId = configProps.blockTypes[1][configProps.blockTypes[1].length-1-yyy-Maze.MAZE_PASSAGE_DEPTH][zzz-sz][0];
				    					bData = (byte)configProps.blockTypes[1][configProps.blockTypes[1].length-1-yyy-Maze.MAZE_PASSAGE_DEPTH][zzz-sz][1];
			    					} else {
			    						bId = configProps.blockTypes[1][configProps.blockTypes[1].length-1-yyy-Maze.MAZE_PASSAGE_DEPTH][xxx-sx][0];
				    					bData = (byte)configProps.blockTypes[1][configProps.blockTypes[1].length-1-yyy-Maze.MAZE_PASSAGE_DEPTH][xxx-sx][1];
			    					}
				    				mazeWorld.getBlockAt(mazeX+xxx, mazeY+yyy+mazeToBlockYCoord(yStart), mazeZ+zzz).setTypeId(bId);
				    				mazeWorld.getBlockAt(mazeX+xxx, mazeY+yyy+mazeToBlockYCoord(yStart), mazeZ+zzz).setData((byte)bData);
								}
							}
						}
					}
				}
			}
		}
	}

	public void restoreEntrances() {
		for (int yStart = 0; yStart < height; yStart ++) {
			for (int xx = 0; xx < mazeSize*2+1; xx ++) {
				for (int zz = 0; zz < mazeSize*2+1; zz ++) {
					if (!(xx == 0 || xx == mazeSize*2) && !(zz == 0 || zz == mazeSize*2)) continue;
					if (maze[xx][zz][yStart] != 1) {
						int sx = mazeToBlockCoord(xx);
						int ex = sx+((zz == 0 || zz == mazeSize*2) ? MAZE_PASSAGE_WIDTH-1 : 0);
						int sz = mazeToBlockCoord(zz);
						int ez = sz+((xx == 0 || xx == mazeSize*2) ? MAZE_PASSAGE_WIDTH-1 : 0);
						for (int xxx = sx; xxx <= ex; xxx++) {
							for (int zzz = sz; zzz <= ez; zzz++) {
								for (int yyy = 1; yyy <= MAZE_PASSAGE_HEIGHT+1; yyy++) {
				    				mazeWorld.getBlockAt(mazeX+xxx, mazeY+yyy+mazeToBlockYCoord(yStart), mazeZ+zzz).setType(Material.AIR);
								}
							}
						}
					}
				}
			}
		}
	}

	public void giveStartItemsToPlayer(Player player) {
		for (int i = 0; i < configProps.startItems.length; i++)
			player.getInventory().addItem(configProps.startItems[i].clone());
	}

	public void addSign(int x, int y, int z, int x2, int y2, int z2, List<int[]> signList) {
		int[] newSign = new int[]{Math.min(x, x2), Math.min(y, y2), Math.min(z, z2),
								  Math.max(x, x2), Math.max(y, y2), Math.max(z, z2),
								  (x2 < x || y2 < y || z2 < z)?1:0};
		Iterator<int[]> it = signList.iterator();
    	while (it.hasNext()) {
    		int[] sign = it.next();
    		if (sign[0] <= newSign[3] && sign[1] <= newSign[4] && sign[2] <= newSign[5]
    		 && sign[3] >= newSign[0] && sign[4] >= newSign[1] && sign[5] >= newSign[2]) {
    			removeSign(sign, signList, false);
    			it.remove();
    		}
    	}
    	signList.add(newSign);
	}

	public int[] findSign(Location location, List<int[]> signList) {
		Iterator<int[]> it = signList.iterator();
    	while (it.hasNext()) {
    		int[] sign = it.next();
    		if (sign[0] <= location.getBlockX() && sign[1] <= location.getBlockY() && sign[2] <= location.getBlockZ()
    		 && sign[3] >= location.getBlockX() && sign[4] >= location.getBlockY() && sign[5] >= location.getBlockZ())
    			return sign;
    	}
		return null;
	}

	public void removeSign(int[] sign, List<int[]> signList) {
		removeSign(sign, signList, true);
	}

	public void removeSign(int[] sign, List<int[]> signList, boolean removeFromList) {
		if (removeFromList) signList.remove(sign);
		for (int xx = sign[0]; xx <= sign[3]; xx++) {
			for (int yy = sign[1]; yy <= sign[4]; yy++) {
				for (int zz = sign[2]; zz <= sign[5]; zz++) {
					Block block = mazeWorld.getBlockAt(xx, yy, zz);
					if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
						Sign signState = (Sign)block.getState(); 
						for (int i = 0; i < 4; i++) signState.setLine(i, "");
						signState.update();
					}
				}
			}
		}
		if (removeFromList && joinSigns.isEmpty() && !canBeEntered) {
    		canBeEntered = true;
    		restoreEntrances();
		}
	}
	
	public String parseText(String str, Player player) {
		return parseText(str, player, null);
	}

	public String parseText(String str, Player player, Player otherPlayer) {
		String newStr = "";
		String subtStr = "";
		boolean escaping = false;
		boolean insideSubt = false;
		for (int i = 0; i < str.length(); i++) {
			if (escaping) escaping = false;
			else {
				if (insideSubt) {
					if (str.charAt(i) == '>') {
						insideSubt = false;
						if (subtStr.equals("name")) subtStr = name;
						else if (subtStr.equals("minP")) subtStr = Integer.toString(configProps.minPlayers);
						else if (subtStr.equals("maxP")) subtStr = Integer.toString(configProps.maxPlayers);
						else if (subtStr.equals("currentP")) {
							if (fightStarted) subtStr = Integer.toString(getPlayersInGame().size());
							else subtStr = Integer.toString(playerInsideMaze.size());
						} else if (subtStr.equals("remainingP")) {
							if (fightStarted) subtStr = "0";
							else subtStr = Integer.toString(Math.max(0, configProps.minPlayers-playerInsideMaze.size()));
						}
						else if (subtStr.equals("timeLeft")) subtStr = Integer.toString((fightStartTimer == 1) ? MazePvP.theMazePvP.fightStartDelay/20 : (MazePvP.theMazePvP.fightStartDelay-fightStartTimer)/20);
						else if (subtStr.equals("state")) subtStr = fightStarted?MazePvP.theMazePvP.startedStateText:MazePvP.theMazePvP.waitingStateText;
						else if (subtStr.equals("livesLeft")) {
							if (player == null) subtStr = "X";
							else {
								PlayerProps props = joinedPlayerProps.get(player.getName());
								if (props == null) subtStr = "X";
								else subtStr = Integer.toString(configProps.playerMaxDeaths-props.deathCount);
							}
						} else if (subtStr.equals("player")) {
							if (player == null) subtStr = "[]";
							else subtStr = player.getName();
						} else if (subtStr.equals("allPlayers") || subtStr.equals("otherPlayers")) {
							if (otherPlayer == null) subtStr = "[]";
							else subtStr = otherPlayer.getName();
						} else subtStr = "<"+subtStr+">";
						newStr += subtStr;
					} else subtStr += str.charAt(i);
					continue;
				} else if (str.charAt(i) == '\\') {
					escaping = true;
					continue;
				} else if (str.charAt(i) == '<') {
					insideSubt = true;
					subtStr = "";
					continue;
				}
			}
			newStr += str.charAt(i);
		}
		if (insideSubt) newStr += "<"+subtStr;
		return newStr;
	}
	
	public boolean textContainsTag(String str, String tag) {
		String subtStr = "";
		boolean escaping = false;
		boolean insideSubt = false;
		for (int i = 0; i < str.length(); i++) {
			if (escaping) escaping = false;
			else {
				if (insideSubt) {
					if (str.charAt(i) == '>') {
						insideSubt = false;
						if (subtStr.equals(tag)) return true;
					} else subtStr += str.charAt(i);
					continue;
				} else if (str.charAt(i) == '\\') {
					escaping = true;
					continue;
				} else if (str.charAt(i) == '<') {
					insideSubt = true;
					subtStr = "";
					continue;
				}
			}
		}
		return false;
	}
	
	public void updateSigns() {
		updateSigns(joinSigns);
		updateSigns(leaveSigns);
	}
	
	public void updateSigns(List<int[]> signList) {
		Iterator<int[]> it = signList.iterator();
    	while (it.hasNext()) {
    		int[] sign = it.next();
    		Iterator<String> strIt = signList==joinSigns?MazePvP.theMazePvP.joinSignText.iterator():MazePvP.theMazePvP.leaveSignText.iterator();
    		outerLoop: for (int xx = sign[0]; xx <= sign[3]; xx++) {
    			for (int yy = sign[1]; yy <= sign[4]; yy++) {
    				for (int zz = sign[2]; zz <= sign[5]; zz++) {
    					Block block = mazeWorld.getBlockAt((sign[6] != 0)?(sign[3]-xx+sign[0]):xx, (sign[6] != 0)?(sign[4]-yy+sign[1]):yy,
    													   (sign[6] != 0)?(sign[5]-zz+sign[2]):zz);
    					if (block != null && (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST)) {
    						Sign signState = (Sign)block.getState();
    						for (int i = 0; i < 4; i++) {
    							if (!strIt.hasNext()) break;
    	    					String signLine = strIt.next();
    							signState.setLine(i, parseText(signLine, null));
    						}
    						signState.update();
							if (!strIt.hasNext()) break outerLoop;
    					} else {
    						removeSign(sign, signList, false);
    		    			it.remove();
    		    			break outerLoop;
    					}
    				}
    			}
    		}
    	}
	}

	public void playerJoin(Player player) {
		if (canBeEntered || (!canBeEntered && !fightStarted)) playerInsideMaze.put(player.getName(), true);
		if (canBeEntered || (!canBeEntered && hasWaitArea) || fightStarted) {
			joinedPlayerProps.put(player.getName(), new PlayerProps(player.getLocation(), MazePvP.cloneItems(player.getInventory().getContents()),
					MazePvP.getClonedArmor(player.getEquipment()), MazePvP.cloneItems(player.getEnderChest().getContents()), fightStarted, player.getGameMode()));
			savePlayerProps();
		}
		if (!canBeEntered) {
			Maze.playerInsideAMaze.put(player.getName(), true);
			if (!fightStarted) {
				sendWaitMessageToJoinedPlayers();
				if (hasWaitArea) {
					int telepY = MazePvP.getSafeY(waitX, waitY, waitZ, mazeWorld);
					player.teleport(new Location(mazeWorld, waitX+0.5, telepY, waitZ+0.5));
				}
			} else {
				Coord3D loc = getMazeBossNewLocation(mazeWorld);
				if (!player.isDead()) {
					player.teleport(new Location(mazeWorld, loc.x, loc.y, loc.z));
	        		MazePvP.cleanUpPlayer(player, canBeEntered);
	        		player.setFallDistance(0);
					//giveStartItemsToPlayer(player);
	        		player.setGameMode(GameMode.CREATIVE);
	        		if (MazePvP.theMazePvP.specSeeOthers) {
		        		Iterator <Map.Entry<String,Boolean>>it = playerInsideMaze.entrySet().iterator();
		        		while(it.hasNext()) {
		        			Map.Entry<String,Boolean> entry = it.next();
		        			if (entry.getValue() && Bukkit.getServer().getPlayer(entry.getKey()) != null) {
		        				Bukkit.getServer().getPlayer(entry.getKey()).hidePlayer(player);
		        			}
		        		}	
	        		} else {
	        			Iterator <Map.Entry<String,PlayerProps>>it = joinedPlayerProps.entrySet().iterator();
		        		while(it.hasNext()) {
		        			Map.Entry<String,PlayerProps> entry = it.next();
		        			Player currentPlayer = Bukkit.getServer().getPlayer(entry.getKey());
		        			if (currentPlayer != null && currentPlayer != player) {
		        				currentPlayer.hidePlayer(player);
		        				if (!playerInsideMaze.containsKey(currentPlayer.getName()) || !playerInsideMaze.get(currentPlayer.getName())) {
		        					player.hidePlayer(currentPlayer);
		        				}
		        			}
		        		}	
	        		}
				}
			}
		}
		if (canBeEntered) {
			MazePvP.cleanUpPlayer(player, canBeEntered);
			giveStartItemsToPlayer(player);
		}
	}

	@SuppressWarnings("deprecation")
	public void playerQuit(Player player) {
		boolean wasSpectating = true;
		if (playerInsideMaze.containsKey(player.getName()) && playerInsideMaze.get(player.getName())) {
			wasSpectating = false;
			playerInsideMaze.remove(player.getName());
		}
		if (!canBeEntered) {
			Maze.playerInsideAMaze.remove(player.getName());
			if (!fightStarted && !wasSpectating) {
				sendWaitMessageToJoinedPlayers();
				if (playerInsideMaze.size() < configProps.minPlayers) fightStartTimer = 0;
			}
		}
		PlayerProps savedProps = joinedPlayerProps.get(player.getName());
		if (savedProps != null) {
			if (!canBeEntered) player.teleport(savedProps.prevLocation);
			MazePvP.cleanUpPlayer(player, canBeEntered);
			player.getInventory().setContents(savedProps.savedInventory);
			player.getEquipment().setArmorContents(savedProps.savedArmor);
			player.updateInventory();
			player.setGameMode(savedProps.savedGM);
			if (!canBeEntered) {
				player.getEnderChest().setContents(savedProps.savedEnderChest);
        		Iterator <Map.Entry<String,PlayerProps>>it = joinedPlayerProps.entrySet().iterator();
        		while(it.hasNext()) {
        			Map.Entry<String,PlayerProps> entry = it.next();
        			Player currentPlayer = Bukkit.getServer().getPlayer(entry.getKey());
        			if (currentPlayer != null) {
        				currentPlayer.showPlayer(player);
        				player.showPlayer(currentPlayer);
        			}
        		}
			}
		}
		joinedPlayerProps.remove(player.getName());
		if (!canBeEntered && fightStarted && !wasSpectating) {
			List<Player> players = getPlayersInGame();
			if (players.size() == 1) {
				if (lastPlayer == null) {
					Player lPlayer = players.get(0);
					if (lPlayer != null) {
						sendStringListToPlayer(lPlayer, MazePvP.theMazePvP.winText);
						fightStartTimer = 0;
						lastPlayer = lPlayer;
					}
				}
			} else if (players.size() != 0) {
				sendPlayerOutMessageToPlayers();
				executeCommands(configProps.fightPlayerOutCommand, player);
			}
		}
		if (!canBeEntered && fightStarted && !wasSpectating && playerInsideMaze.isEmpty()) {
			lastPlayer = null;
			fightStarted = false;
			fightStartTimer = 0;
			stopFight(false);
		}
		if (joinedPlayerProps.isEmpty()) cleanUpMaze();
		if (!canBeEntered) updateSigns();
	}

	public void sendWaitMessageToJoinedPlayers() {
		Iterator<Map.Entry<String,Boolean>> it = playerInsideMaze.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String,Boolean> entry = it.next();
			if (entry.getValue()) {
				Player player = Bukkit.getPlayer(entry.getKey());
				if (playerInsideMaze.size() < configProps.minPlayers) {
					sendStringListToPlayer(player, MazePvP.theMazePvP.waitBroadcastText);
				} else sendStringListToPlayer(player, MazePvP.theMazePvP.waitBroadcastFullText);
			}
		}
	}

	public void sendTimeMessageToJoinedPlayers() {
		Iterator<Map.Entry<String,Boolean>> it = playerInsideMaze.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String,Boolean> entry = it.next();
			if (entry.getValue()) {
				Player player = Bukkit.getPlayer(entry.getKey());
				sendStringListToPlayer(player, MazePvP.theMazePvP.countdownText);
			}
		}
	}

	public void sendStartMessageToJoinedPlayers() {
		Iterator<Map.Entry<String,Boolean>> it = playerInsideMaze.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String,Boolean> entry = it.next();
			if (entry.getValue()) {
				Player player = Bukkit.getPlayer(entry.getKey());
				sendStringListToPlayer(player, MazePvP.theMazePvP.fightStartedText);
			}
		}
	}

	public void sendPlayerOutMessageToPlayers() {
		Iterator<Player> it = getPlayersInGame(true).iterator();
		while(it.hasNext()) {
			Player player = it.next();
			sendStringListToPlayer(player, MazePvP.theMazePvP.playerOutText);
		}
	}

	public void sendStringListToPlayer(Player player, List<String> strList) {
		Iterator<String> it = strList.iterator();
		while (it.hasNext()) {
			String message = it.next();
			player.sendMessage(parseText(message, player));
		}
	}

	public void startFight() {
		cleanUpMaze();
		Iterator<Map.Entry<String,Boolean>> it = playerInsideMaze.entrySet().iterator();
		boolean propsEmpty = joinedPlayerProps.isEmpty();
		while(it.hasNext()) {
			Map.Entry<String,Boolean> entry = it.next();
			Player player = Bukkit.getPlayer(entry.getKey());
			if (propsEmpty) {
				PlayerProps props = new PlayerProps(player.getLocation(), MazePvP.cloneItems(player.getInventory().getContents()),
													MazePvP.getClonedArmor(player.getEquipment()), MazePvP.cloneItems(player.getEnderChest().getContents()), false, player.getGameMode());
				joinedPlayerProps.put(player.getName(), props);
				savePlayerProps();
			}
			Coord3D loc = getMazeBossNewLocation(mazeWorld);
			if (!player.isDead()) {
				player.teleport(new Location(mazeWorld, loc.x, loc.y, loc.z));
        		MazePvP.cleanUpPlayer(player, canBeEntered);
        		player.setFallDistance(0);
				giveStartItemsToPlayer(player);
			} 
		}
	}

	public void savePlayerProps() {
		File mazeFile = new File(mazeWorld.getWorldFolder(), name+".mazeSave");
        PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(mazeFile, false));
	        Iterator<Entry<String, PlayerProps>> it = joinedPlayerProps.entrySet().iterator();
	        while (it.hasNext()) {
	        	Entry<String, PlayerProps> entry = it.next();
	        	entry.getValue().writeToFile(writer);
	        }
			writer.close();
		} catch (Exception e) {
			MazePvP.theMazePvP.getLogger().info("Failed to save player properties of maze \""+name+"\": "+e.toString());
		}
        if (writer !=null) writer.close();
	}

	public boolean loadPlayerProps() {
		File mazeFile = new File(mazeWorld.getWorldFolder(), name+".mazeSave");
		if (!mazeFile.exists()) return false;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(mazeFile));
	        String str = "";
	        while ((str = reader.readLine()) != null && str.length() > 0) {
	        	PlayerProps prop = PlayerProps.readFromFile(reader);
	        	Player player = Bukkit.getServer().getPlayer(str);
	        	prop.prevLocation.setWorld(player.getWorld());
	        	if (player != null) {
	        		player.teleport(prop.prevLocation);
	        		player.getInventory().setContents(prop.savedInventory);
	        		player.getEquipment().setArmorContents(prop.savedArmor);
	        		player.getEnderChest().setContents(prop.savedEnderChest);
	        		player.setGameMode(prop.savedGM);
	        	}
	        }
	        reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			MazePvP.theMazePvP.getLogger().info("Failed to load player properties of maze \""+name+"\": "+e.toString());
		}
		return true;
	}

	public void deletePlayerProps() {
		File mazeFile = new File(mazeWorld.getWorldFolder(), name+".mazeSave");
        mazeFile.delete();
	}

	public void stopFight(boolean sendMessage) {
		fightStarted = false;
		fightStartTimer = 0;
		while(!joinedPlayerProps.isEmpty()) {
			Player player = Bukkit.getPlayer(joinedPlayerProps.keySet().iterator().next());
			if (sendMessage) this.sendStringListToPlayer(player, MazePvP.theMazePvP.fightStoppedText);
			playerQuit(player);
		}
		deletePlayerProps();
	}
	
	public List<Player> getPlayersInGame() {
		return getPlayersInGame(false);
	}

	public List<Player> getPlayersInGame(boolean includeSpect) {
		Iterator<Map.Entry<String,PlayerProps>> it = joinedPlayerProps.entrySet().iterator();
		ArrayList<Player> players = new ArrayList<Player>();
		while(it.hasNext()) {
			Map.Entry<String,PlayerProps> entry = it.next();
			if (entry.getValue().deathCount < configProps.playerMaxDeaths
				&& (includeSpect || this.playerInsideMaze.get(entry.getKey()) != null)) {
				players.add(Bukkit.getPlayer(entry.getKey()));
			}
		}
		return players;
	}

	@SuppressWarnings("deprecation")
	public void cleanUpMaze() {
		int xx, zz;
		for (int yStart = 0; yStart < height; yStart ++) {
			for (xx = 1; xx <= mazeSize*2-1; xx += 2) {
	    		for (zz = 1; zz <= mazeSize*2-1; zz += 2) {
	    			if (!isBeingChanged[xx][zz][yStart] &&
	    				mazeWorld.getBlockAt(mazeX+mazeToBlockCoord(xx), mazeY+mazeToBlockYCoord(yStart), mazeZ+mazeToBlockCoord(zz)).isEmpty()) {
						int xCoord = mazeToBlockCoord(xx);
	    				for (int xxx = xCoord; xxx <= mazeToBlockCoord(xx)+Maze.MAZE_PASSAGE_WIDTH-1; xxx++) {
	    					int zCoord = mazeToBlockCoord(zz);
	    					for (int zzz = zCoord; zzz <= mazeToBlockCoord(zz)+Maze.MAZE_PASSAGE_WIDTH-1; zzz++) {
	    						int bId = configProps.blockTypes[(yStart==0)?4:8][zzz-zCoord][xxx-xCoord][0];
	    						int bData = (byte)configProps.blockTypes[(yStart==0)?4:8][zzz-zCoord][xxx-xCoord][1];
	    						mazeWorld.getBlockAt(mazeX+xxx, mazeY+mazeToBlockYCoord(yStart), mazeZ+zzz).setTypeId(bId);
	    						mazeWorld.getBlockAt(mazeX+xxx, mazeToBlockYCoord(yStart), mazeZ+zzz).setData((byte)bData);
		        				if (yStart == 0 && MazePvP.theMazePvP.showHeads) mazeWorld.getBlockAt(mazeX+xxx, mazeY-Maze.MAZE_PASSAGE_DEPTH+2, mazeZ+zzz).setType(Material.AIR);
	    					}
	    				}
	    			}
	    		}
			}
    	}

		for (int yStart = 0; yStart < height; yStart ++) {
			for (xx = 2; xx <= mazeSize*2; xx += 2) {
	    		for (zz = 2; zz <= mazeSize*2; zz += 2) {
	    			int posX = mazeToBlockCoord(xx);
	    			int posZ = mazeToBlockCoord(zz);
	    			int posY = mazeToBlockYCoord(yStart);
	    			if (mazeWorld.getBlockAt(mazeX+posX, mazeY+2+posY, mazeZ+posZ) == null || mazeWorld.getBlockAt(mazeX+posX, mazeY+2+posY, mazeZ+posZ).isEmpty()) {
						if (mazeWorld.getBlockAt(mazeX+posX, mazeY+1+posY, mazeZ+posZ).getType() == Material.CHEST) {
							Chest chest = ((Chest)mazeWorld.getBlockAt(mazeX+posX, mazeY+1+posY, mazeZ+posZ).getState());
							chest.getInventory().clear();
						}
						mazeWorld.getBlockAt(mazeX+posX, mazeY+1+posY, mazeZ+posZ).setTypeId(configProps.blockTypes[2][6][0][0]);
						mazeWorld.getBlockAt(mazeX+posX, mazeY+1+posY, mazeZ+posZ).setData((byte)configProps.blockTypes[2][6][0][1]);
						mazeWorld.getBlockAt(mazeX+posX, mazeY+2+posY, mazeZ+posZ).setTypeId(configProps.blockTypes[2][5][0][0]);
						mazeWorld.getBlockAt(mazeX+posX, mazeY+2+posY, mazeZ+posZ).setData((byte) configProps.blockTypes[2][5][0][1]);
					}
	    		}
			}
		}

        Collection<Entity> entities = mazeWorld.getEntitiesByClass(Entity.class);
        Iterator<Entity> it = entities.iterator();
        while (it.hasNext()) {
        	Entity en = it.next();
        	if (!(en instanceof Player) && isInsideMaze(en.getLocation())) en.remove();
        }
        Iterator<Boss> bit = bosses.iterator();
        while (bit.hasNext()) bit.next().entity = null;
	}

	public void loadBossesFromConfig() {
		bosses = new ArrayList<Boss>();
		Iterator<BossConfig> bcit = configProps.bosses.iterator();
        int place = 0;
        while (bcit.hasNext()) {
        	BossConfig bossProps = bcit.next();
        	Boss boss = new Boss();
			boss.hp = bossProps.maxHp;
			bosses.add(boss);
			updateBossHpStr(place);
			place++;
        }
	}

	public void executeCommands(List<String> commands, Player player) {
		Iterator<String> it = commands.iterator();
		while (it.hasNext()) {
			String cmd = it.next();
			boolean otherPlayers = textContainsTag(cmd, "otherPlayers");
			boolean allPlayers = textContainsTag(cmd, "allPlayers");
			if (allPlayers || otherPlayers) {
				Iterator<Player> pit = mazeWorld.getPlayers().iterator();
				while (pit.hasNext()) {
					Player currentPlayer = pit.next();
					if (isInsideMaze(currentPlayer.getLocation())) {
						if (allPlayers || (otherPlayers && currentPlayer != player)) {
							Bukkit.getServer().dispatchCommand(
									Bukkit.getServer().getConsoleSender(), parseText(cmd, player, currentPlayer));
						}
					}
				}
			} else Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), parseText(cmd, player));
		}
	}

}
