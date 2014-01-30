package mazepvp;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class Maze {
	public static final int MAZE_PASSAGE_WIDTH = 4;
	public static final int MAZE_PASSAGE_HEIGHT = 5;
	public static final int MAZE_PASSAGE_DEPTH = 5;
	public static final int WALL_CHANGE_SPEED = 60;
	
	public World mazeWorld;
	public int mazeSize;
	public int mazeX, mazeY, mazeZ;
	public Zombie mazeBoss = null;
	public String mazeBossName = "MazeBoss";
	public String mazeBossHpStr = "";
	public UUID mazeBossId = null;
	public String mazeBossTargetPlayer = "";
	public int mazeBossTargetTimer = 0;
	public int mazeBossTpCooldown = 0;
	public int mazeBossMaxHp = 0;
	public double mazeBossHp = 0.0;
	public int mazeBossStrength = 0;
	public double mazeSpawnMobProb = 1.0/3.0;
	public double mazeChestAppearProb = 0.3;
	public double mazeGroundReappearProb = 0.1;
	public int[][] maze;
	public boolean[][] isBeingChanged;
	public List<MazeCoords> blocksToRemove = new LinkedList<MazeCoords>();
	public List<MazeCoords> blocksToRestore = new LinkedList<MazeCoords>();
	public HashMap<String, Boolean> playerInsideMaze = new HashMap<String, Boolean>();
	public static HashMap<String, Boolean> playerInsideAMaze = new HashMap<String, Boolean>();
	public double[] mazeChestWeighs;
	public ItemStack[] mazeChestItems;
	public double[] mazeBossDropWeighs;
	public ItemStack[] mazeBossDropItems;
	public String name = "";
	public boolean updatingHp = false;
	public boolean canBeEntered = true;
	public boolean hasWaitArea = false;
	public int waitX = 0, waitY = 0, waitZ = 0;
	public int minPlayers = 5;
	public int maxPlayers = 20;
	public boolean fightStarted = false;
	public int fightStartTimer = 0;
	public LinkedList<int[]> joinSigns = new LinkedList<int[]>();
	public LinkedList<int[]> leaveSigns = new LinkedList<int[]>();
	
	public Maze() {
		mazeBossName = MazePvP.theMazePvP.mazeBossName;
		mazeBossHp = mazeBossMaxHp = MazePvP.theMazePvP.mazeBossMaxHp;
		updateBossHpStr();
		mazeBossStrength = MazePvP.theMazePvP.mazeBossStrength;
		mazeGroundReappearProb = MazePvP.theMazePvP.mazeGroundReappearProb;
		mazeChestAppearProb = MazePvP.theMazePvP.mazeChestAppearProb;
		mazeSpawnMobProb = MazePvP.theMazePvP.mazeSpawnMobProb;
		mazeChestWeighs = MazePvP.theMazePvP.mazeChestWeighs;
		mazeChestItems = MazePvP.theMazePvP.mazeChestItems;
		mazeBossDropWeighs = MazePvP.theMazePvP.mazeBossDropWeighs;
		mazeBossDropItems = MazePvP.theMazePvP.mazeBossDropItems;
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
	 
	public Point2D.Double getMazeBossNewLocation(World worldObj) {
	 	double bossX, bossZ;
	 	int count = 0;
	 	while (true) {
	 		bossX = mazeToBlockCoord((int)Math.floor(Math.random()*mazeSize)*2+1);
	 		bossZ = mazeToBlockCoord((int)Math.floor(Math.random()*mazeSize)*2+1);
	 		if (!worldObj.getBlockAt((int)Math.floor(mazeX+bossX), mazeY, (int)Math.floor(mazeZ+bossZ)).isEmpty()) break;
	 		count++;
	 		if (count >= 100) break;
	 	}
	 	return new Point2D.Double(mazeX+bossX+MAZE_PASSAGE_WIDTH*0.5, mazeZ+bossZ+MAZE_PASSAGE_WIDTH*0.5);
	 }
	 
	public void relocateMazeBoss(boolean coolDown) {
	 	World worldObj = mazeBoss.getWorld();
		relocateMazeBoss(coolDown, getMazeBossNewLocation(worldObj));
	}
	
	public void relocateMazeBoss(boolean coolDown, Point2D.Double bossLoc) {
		relocateMazeBoss(coolDown, bossLoc, mazeBoss.getLocation().getYaw(), mazeBoss.getLocation().getPitch());
	}

	public void relocateMazeBoss(boolean coolDown, Point2D.Double bossLoc, float yaw, float pitch) {
		int j;
	 	World worldObj = mazeBoss.getWorld();
		for (j = 0; j <= 8; j++) {
			worldObj.playEffect(mazeBoss.getLocation(), Effect.SMOKE, j);
			if (j != 4) {
				worldObj.playEffect(new Location(worldObj, mazeBoss.getLocation().getX(), mazeBoss.getLocation().getY()+1, mazeBoss.getLocation().getZ()), Effect.SMOKE, j);
			}
		}
		mazeBoss.teleport(new Location(worldObj, bossLoc.x, mazeY+1, bossLoc.y, yaw, pitch));
		for (j = 0; j <= 8; j++) {
			worldObj.playEffect(mazeBoss.getLocation(), Effect.SMOKE, j);
			if (j != 4) {
				worldObj.playEffect(new Location(worldObj, mazeBoss.getLocation().getX(), mazeBoss.getLocation().getY()+1, mazeBoss.getLocation().getZ()), Effect.SMOKE, j);
			}
		}
		if (coolDown) mazeBossTpCooldown = 20;
	}
	 
	public void makeNewMazeBoss(World worldObj) {
	 	Point2D.Double bossLoc = getMazeBossNewLocation(worldObj);
	 	mazeBoss = (Zombie)worldObj.spawnEntity(new Location(worldObj, bossLoc.x, mazeY+1, bossLoc.y), EntityType.ZOMBIE);
	 	mazeBossId = mazeBoss.getUniqueId();
	 	mazeBossHp = mazeBossMaxHp;
	 	updateBossHpStr();
	 	worldObj.playEffect(new Location(worldObj, mazeBoss.getLocation().getX(), mazeBoss.getLocation().getY()+1, mazeBoss.getLocation().getZ()), Effect.MOBSPAWNER_FLAMES, 0);
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
	 	EntityEquipment ee = mazeBoss.getEquipment();
	 	ee.setHelmet(new ItemStack(Material.SKULL_ITEM, 1, (short)1));
	 	ee.setBoots(boots);
	 	ee.setLeggings(legs);
	 	ee.setChestplate(plate);
	 }
	 
	 public void setToLookedAt(boolean[][] isLookedAt, int x, int z, boolean matters) {
	 	if (x%2 == 1 && z%2 == 1 && !matters) return;
	 	isLookedAt[x][z] = true;
	 }
	 
	 public boolean canBeReached(int startX, int startZ, int endX, int endZ, int maxSteps, boolean[][] wasVisited) {
	 	if (startX == endX && startZ == endZ) return true;
	 	LinkedList<MazeCoords> elements = new LinkedList<MazeCoords>();
	 	ArrayList<MazeCoords> prevElements = new ArrayList<MazeCoords>();
	 	elements.add(new MazeCoords(startX, startZ));
	 	elements.add(new MazeCoords(-1, -1));
	 	boolean reached = false;
	 	int steps = 0;
	 	MazeCoords coords;
	 	while (steps < maxSteps && !elements.isEmpty()) {
	 		coords = (MazeCoords)elements.remove();
	 		if (coords.x == -1) {
	 	    	if (!elements.isEmpty()) elements.push(new MazeCoords(-1, -1));
	 			steps++;
	 		} else {
	 	    	wasVisited[coords.x][coords.z] = true;
		    		prevElements.add(coords);
		    		if ((coords.z == endZ && (coords.x-1 == endX || coords.x+1 == endX))
		    		 || (coords.x == endX && (coords.z-1 == endZ || coords.z+1 == endZ))) {
		    			reached = true;
		    			break;
		    		}
		    		if (coords.x > 0 && !wasVisited[coords.x-1][coords.z] && maze[coords.x-1][coords.z] != 1) elements.push(new MazeCoords(coords.x-1, coords.z));
		    		if (coords.x < mazeSize*2 && !wasVisited[coords.x+1][coords.z] && maze[coords.x+1][coords.z] != 1) elements.push(new MazeCoords(coords.x+1, coords.z));
		    		if (coords.z > 0 && !wasVisited[coords.x][coords.z-1] && maze[coords.x][coords.z-1] != 1) elements.push(new MazeCoords(coords.x, coords.z-1));
		    		if (coords.z < mazeSize*2 && !wasVisited[coords.x][coords.z+1] && maze[coords.x][coords.z+1] != 1) elements.push(new MazeCoords(coords.x, coords.z+1));
	 		}
	 	}
	 	Iterator<MazeCoords> it = prevElements.iterator();
	 	while (it.hasNext()) {
	 		coords = (MazeCoords)it.next();
	 		wasVisited[coords.x][coords.z] = false;
	 	}
	 	return reached;
	 }
	 
	 public boolean pillarIsAlone(int x, int z, int rx, int rz) {
	 	if (rx < 0 || rx > mazeSize*2 || rz < 0 || rz > mazeSize*2) return false;
	 	if (!(x-1 == rx && z == rz) && x > 0 && maze[x-1][z] == 1) return false;
	 	if (!(x+1 == rx && z == rz) && x < mazeSize*2 && maze[x+1][z] == 1) return false;
	 	if (!(x == rx && z-1 == rz) && z > 0 && maze[x][z-1] == 1) return false;
	 	if (!(x == rx && z+1 == rz) && z < mazeSize*2 && maze[x][z+1] == 1) return false;
	 	return true;
	 }
	   
	   public void removeMazeBlocks(int mazeX, int mazeZ, World worldObj) {
		if (mazeX <= 0 || mazeX >= mazeSize*2 || mazeZ <= 0 || mazeZ >= mazeSize*2) return;
	   	if (isBeingChanged[mazeX][mazeZ]) return;
	   	if ((maze[mazeX][mazeZ] != 1 && (mazeX%2 == 0 || mazeZ%2 == 0)) || (mazeX%2 == 0 && mazeZ%2 == 0)) return;
	   	if (worldObj.getBlockAt(this.mazeX+mazeToBlockCoord(mazeX), mazeY, this.mazeZ+mazeToBlockCoord(mazeZ)).getType() == Material.AIR) return;
	   	blocksToRemove.add(new MazeCoords(mazeX, mazeZ, 20));
	   	isBeingChanged[mazeX][mazeZ] = true;
	   }
	   
	   public void restoreMazeBlocks(int mazeX, int mazeZ) {
	   	if (mazeX <= 0 || mazeX >= mazeSize*2 || mazeZ <= 0 || mazeZ >= mazeSize*2) return;
	   	if (isBeingChanged[mazeX][mazeZ]) return;
	   	if ((maze[mazeX][mazeZ] != 0 && (mazeX%2 == 0 || mazeZ%2 == 0)) || (mazeX%2 == 0 && mazeZ%2 == 0)) return;
	   	int blockNum;
	   	if (mazeX%2 == 1 && mazeZ%2 == 1) blockNum = MAZE_PASSAGE_WIDTH*MAZE_PASSAGE_WIDTH;
	   	else blockNum = MAZE_PASSAGE_WIDTH*(MAZE_PASSAGE_HEIGHT+1);
	   	blocksToRestore.add(new MazeCoords(mazeX, mazeZ, blockNum));
	   	isBeingChanged[mazeX][mazeZ] = true;
	   }

	public boolean isInsideMaze(Location location) {
		if (location.getWorld() != mazeWorld) return false;
		return location.getX()-0.5 >= mazeX && location.getX()-0.5 <= mazeX+mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
		   &&  location.getZ()-0.5 >= mazeZ && location.getZ()-0.5 <= mazeZ+mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
		   &&  location.getY() >= mazeY-Maze.MAZE_PASSAGE_DEPTH && location.getY() <= mazeY+Maze.MAZE_PASSAGE_HEIGHT+1;
	}

	public void updateBossHpStr() {
		if (mazeBossMaxHp <= 0) return;
		if (updatingHp) return;
		updatingHp = true;
		double hpFraction = mazeBossHp/mazeBossMaxHp;
		double barNum = MazePvP.BOSS_HEALTH_BARS*hpFraction;
		if (hpFraction > 0.5) mazeBossHpStr = "§2";
		else if (hpFraction > 0.25) mazeBossHpStr = "§6";
		else mazeBossHpStr = "§4";
		for (int i = 0; i < MazePvP.BOSS_HEALTH_BARS; i++) {
			if (barNum-i > 0.75) mazeBossHpStr += "█";
			else if (barNum-i > 0.25 || i == 0) mazeBossHpStr += "▌";
			else mazeBossHpStr += " ";
		}
		updatingHp = false;
	}

	@SuppressWarnings("deprecation")
	public void removeEntrances() {
		for (int xx = 0; xx < mazeSize*2+1; xx ++) {
			for (int zz = 0; zz < mazeSize*2+1; zz ++) {
				if (!(xx == 0 || xx == mazeSize*2) && !(zz == 0 || zz == mazeSize*2)) continue;
				if (maze[xx][zz] != 1) {
					int sx = mazeToBlockCoord(xx);
					int ex = sx+((zz == 0 || zz == mazeSize*2) ? MAZE_PASSAGE_WIDTH-1 : 0);
					int sz = mazeToBlockCoord(zz);
					int ez = sz+((xx == 0 || xx == mazeSize*2) ? MAZE_PASSAGE_WIDTH-1 : 0);
					for (int xxx = sx; xxx <= ex; xxx++) {
						for (int zzz = sz; zzz <= ez; zzz++) {
							for (int yyy = 1; yyy <= MAZE_PASSAGE_HEIGHT+1; yyy++) {
			    				mazeWorld.getBlockAt(mazeX+xxx, mazeY+yyy, mazeZ+zzz).setTypeId(98);
			    				mazeWorld.getBlockAt(mazeX+xxx, mazeY+yyy, mazeZ+zzz).setData((byte)3);
							}
						}
					}
				}
			}
		}
	}

	public void restoreEntrances() {
		for (int xx = 0; xx < mazeSize*2+1; xx ++) {
			for (int zz = 0; zz < mazeSize*2+1; zz ++) {
				if (!(xx == 0 || xx == mazeSize*2) && !(zz == 0 || zz == mazeSize*2)) continue;
				if (maze[xx][zz] != 1) {
					int sx = mazeToBlockCoord(xx);
					int ex = sx+((zz == 0 || zz == mazeSize*2) ? MAZE_PASSAGE_WIDTH-1 : 0);
					int sz = mazeToBlockCoord(zz);
					int ez = sz+((xx == 0 || xx == mazeSize*2) ? MAZE_PASSAGE_WIDTH-1 : 0);
					for (int xxx = sx; xxx <= ex; xxx++) {
						for (int zzz = sz; zzz <= ez; zzz++) {
							for (int yyy = 1; yyy <= MAZE_PASSAGE_HEIGHT+1; yyy++) {
			    				mazeWorld.getBlockAt(mazeX+xxx, mazeY+yyy, mazeZ+zzz).setType(Material.AIR);
							}
						}
					}
				}
			}
		}
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

	public String parseSignText(String str) {
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
						else if (subtStr.equals("minP")) subtStr = Integer.toString(minPlayers);
						else if (subtStr.equals("maxP")) subtStr = Integer.toString(maxPlayers);
						else if (subtStr.equals("currentP")) subtStr = Integer.toString(playerInsideMaze.size());
						else if (subtStr.equals("remainingP")) subtStr = Integer.toString(Math.max(0, minPlayers-playerInsideMaze.size()));
						else if (subtStr.equals("timeLeft")) subtStr = Integer.toString((MazePvP.theMazePvP.fightStartDelay-fightStartTimer)/20);
						else if (subtStr.equals("state")) subtStr = fightStarted?"Started":"Waiting";
						else subtStr = "<"+subtStr+">";
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
    					if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
    						Sign signState = (Sign)block.getState();
    						for (int i = 0; i < 4; i++) {
    							if (!strIt.hasNext()) break;
    	    					String signLine = strIt.next();
    							signState.setLine(i, parseSignText(signLine));
    						}
    						signState.update();
							if (!strIt.hasNext()) break outerLoop;
    					} else {for (int i = 0; i < 4; i++) strIt.next();}
    				}
    			}
    		}
    	}
	}

	public void playerJoin(Player player) {
		if (canBeEntered) MazePvP.theMazePvP.giveStartItemsToPlayer(player);
		playerInsideMaze.put(player.getName(), true);
		if (!canBeEntered) {
			Maze.playerInsideAMaze.put(player.getName(), true);
			if (!fightStarted) {
				sendWaitMessageToJoinedPlayers();
			}
		}
	}

	public void playerQuit(Player player) {
		if (canBeEntered) player.getInventory().clear();
		playerInsideMaze.remove(player.getName());
		if (!canBeEntered) {
			Maze.playerInsideAMaze.remove(player.getName());
			if (!fightStarted) {
				sendWaitMessageToJoinedPlayers();
				if (playerInsideMaze.size() < minPlayers) fightStartTimer = 0; 
			}
		}
	}

	public void sendWaitMessageToJoinedPlayers() {
		Iterator<Map.Entry<String,Boolean>> it = playerInsideMaze.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String,Boolean> entry = it.next();
			if (entry.getValue()) {
				Player player = Bukkit.getPlayer(entry.getKey());
				if (playerInsideMaze.size() < minPlayers) {
					player.sendMessage(playerInsideMaze.size()+" player(s) joined, waiting for "+(minPlayers-playerInsideMaze.size())+" more");
				} else player.sendMessage(playerInsideMaze.size()+" player(s) joined");
			}
		}
	}

	public void sendTimeMessageToJoinedPlayers() {
		Iterator<Map.Entry<String,Boolean>> it = playerInsideMaze.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String,Boolean> entry = it.next();
			if (entry.getValue()) {
				Player player = Bukkit.getPlayer(entry.getKey());
				player.sendMessage("Match will start in "+(MazePvP.theMazePvP.fightStartDelay-(fightStartTimer==1?0:fightStartTimer))/20);
			}
		}
	}

	public void sendStartMessageToJoinedPlayers() {
		Iterator<Map.Entry<String,Boolean>> it = playerInsideMaze.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String,Boolean> entry = it.next();
			if (entry.getValue()) {
				Player player = Bukkit.getPlayer(entry.getKey());
				player.sendMessage("The match has started! Good luck!");
			}
		}
	}

}
