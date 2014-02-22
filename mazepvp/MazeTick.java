package mazepvp;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class MazeTick extends BukkitRunnable {
	private MazePvP main;

	public MazeTick(MazePvP main) {
		this.main = main;
	}

	@SuppressWarnings("deprecation")
	public void run() {
		main.wallChangeTimer++;
		main.mazeBossRestoreTimer++;
		Iterator<Maze> mit = main.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
      		int i, j, posX, posZ, xx, zz;
      		
      		if (main.mazeBossRestoreTimer >= Maze.BOSS_RESTORE_SPEED) {
        		Collection<Zombie> entities = maze.mazeWorld.getEntitiesByClass(Zombie.class);
    			Iterator<Zombie> iter = entities.iterator();
    			while (iter.hasNext()) {
    				Zombie en = iter.next();
    				if (en.getUniqueId().equals(maze.boss.id)) {
    					break;
    				}
    				if (!iter.hasNext()) {
    					maze.boss.entity= null;
    				}
    			}
      		}
      		if (!maze.canBeEntered) {
      			if (!maze.fightStarted) {
	      			if (maze.playerInsideMaze.size() >= maze.configProps.minPlayers) {
	      				maze.fightStartTimer++;
	      				if (maze.fightStartTimer == MazePvP.theMazePvP.fightStartDelay) {
	      					maze.fightStartTimer = 0;
	      					maze.fightStarted = true;
	      					maze.startFight();
	      					maze.updateSigns();
	      					maze.sendStartMessageToJoinedPlayers();
	      				} else if (maze.fightStartTimer == 1 || maze.fightStartTimer%20 == 0) maze.sendTimeMessageToJoinedPlayers();
	      			}
      			}
      			if (maze.lastPlayer != null) {
      				maze.fightStartTimer++;
      				if (maze.fightStartTimer >= Maze.FIGHT_STOP_SPEED) {
      					maze.fightStartTimer = 0;
						if (maze.lastPlayer != null && !maze.lastPlayer.isDead()) {
	      					maze.playerQuit(maze.lastPlayer);
						}
      				}
      			}
      		}
      		
      		if (maze.boss.entity!= null) {
      			if (!maze.isInsideMaze(maze.boss.entity.getLocation())) maze.relocateMazeBoss(false);
      			if (maze.configProps.bossMaxHp > 0 && maze.boss.hp > 0) {
	      			if (!maze.boss.hpStr.equals(maze.boss.entity.getCustomName())) {
	      				maze.boss.entity.setCustomName(maze.configProps.bossMaxHp > 0 ? maze.boss.hpStr : null);
	      				maze.boss.entity.setCustomNameVisible(maze.configProps.bossMaxHp > 0 ? true : false);
	      			}
      			}
      		}
			Collection<LivingEntity> entities = maze.mazeWorld.getEntitiesByClass(LivingEntity.class);
			Iterator<LivingEntity> iter = entities.iterator();
			while (iter.hasNext()) {
				LivingEntity en = iter.next();
				if (en.getHealth() > 0 && maze.isInsideMaze(en.getLocation())) {
					if (en.getLocation().getY() <= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2.5 && en.getLocation().getY() >= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+1) {
						if (maze.boss.entity== en) maze.relocateMazeBoss(false);
						else en.damage(en.getHealth()+10);
					}
				}
			}
			
	      	List<Player> players = maze.mazeWorld.getPlayers();
	      	Iterator<Player> pit = players.iterator();
			while (pit.hasNext()) {
				Player player = pit.next();
				if (player.getHealth() > 0 && maze.isInsideMaze(player.getLocation())) {
					if (player.getLocation().getY() <= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2 && player.getLocation().getY() >= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+1) {
						player.damage(player.getHealth()+10);
					}
				}
				if (maze.canBeEntered) {
					boolean inside = maze.isInsideMaze(player.getLocation());
					Boolean prevInside = maze.playerInsideMaze.containsKey(player.getName())?maze.playerInsideMaze.get(player.getName()):false;
					if (!prevInside && inside) {
						maze.playerJoin(player);
					} else if (prevInside && !inside) {
						maze.playerQuit(player);
					}
				}
				if (!maze.canBeEntered && maze.fightStarted && maze.playerInsideMaze.containsKey(player.getName()) && maze.playerInsideMaze.get(player.getName())) {
					if (!maze.isInsideMaze(player.getLocation())) {
						PlayerProps props = maze.joinedPlayerProps.get(player.getName());
						if (player != maze.lastPlayer && props.deathCount < maze.configProps.playerMaxDeaths) {
							Point2D.Double loc = maze.getMazeBossNewLocation(maze.mazeWorld);
							player.teleport(new Location(maze.mazeWorld, loc.x, maze.mazeY+1, loc.y));
							player.setFallDistance(0);
						} else {
							player.teleport(props.prevLocation);
							player.setFallDistance(0);
							maze.playerQuit(player);
						}
					}
				}
			}
			
			if (maze.boss.targetTimer > 0) {
				maze.boss.targetTimer--;
				if (maze.boss.targetTimer == 0) {
					maze.boss.targetPlayer = "";
				}
			}
			maze.boss.tpCooldown = Math.max(0, maze.boss.tpCooldown-1);
    		if (maze.boss.entity!= null && Math.random() < 0.02*maze.boss.targetTimer/(double)MazePvP.BOSS_TIMER_MAX) {
    			maze.boss.targetTimer = MazePvP.BOSS_TIMER_MAX/3;
    			Player player = Bukkit.getPlayer(maze.boss.targetPlayer);
    			if (player != null) {
    				Location ploc = player.getLocation();
    				Location mloc = maze.boss.entity.getLocation();
    				double angle = Math.atan2(mloc.getX()-ploc.getX(), mloc.getZ()-ploc.getZ());
    				angle +=  Math.PI;
    				maze.relocateMazeBoss(true, new Point2D.Double(ploc.getX() + 1.0*Math.sin(angle), ploc.getZ() + 1.0*Math.cos(angle)), ploc.getYaw(), mloc.getPitch());
    			} else {
    				maze.boss.targetPlayer = "";
    				maze.boss.targetTimer = 0;
    			}
    		}
			
	      	if (main.wallChangeTimer >= Maze.WALL_CHANGE_SPEED) {
	      		boolean isLookedAt[][] = new boolean[maze.mazeSize*2+1][];
	      		boolean shouldChange[][] = new boolean[maze.mazeSize*2+1][];
	      		boolean pathArray[][] = new boolean[maze.mazeSize*2+1][];
	      		boolean updateBoss = false;
	      		for (xx = 0; xx < maze.mazeSize*2+1; xx++) {
	      			isLookedAt[xx] = new boolean[maze.mazeSize*2+1];
	      			shouldChange[xx] = new boolean[maze.mazeSize*2+1];
	      			pathArray[xx] = new boolean[maze.mazeSize*2+1];
	      			for (zz = 0; zz < maze.mazeSize*2+1; zz++) {
	      				isLookedAt[xx][zz] = false;
	      				shouldChange[xx][zz] = false;
	      				pathArray[xx][zz] = false;
	      			}
	      		}
	      		int playerMazeX[][] = new int[players.size()][5];
	      		int playerMazeZ[][] = new int[players.size()][5];
	      		boolean playerMazeMatters[][] = new boolean[players.size()][5];
	      		Player currentPlayer;
	      		pit = players.iterator();
	        	for (int pp = 0; pp < players.size(); pp++) {
	        		currentPlayer = pit.next();
	        		if (currentPlayer.getWorld() != maze.mazeWorld) continue;
	      			float angle = currentPlayer.getLocation().getYaw();
	      			while (angle >= 180) angle -= 360;
	      			while (angle < -180) angle += 360;
	          		for (i = 0; i < 5; i++) {
	          			playerMazeX[pp][i] = -1;
	          			playerMazeZ[pp][i] = -1;
	          			playerMazeMatters[pp][i] = true;
	          		}
	            	Location playerLoc = currentPlayer.getLocation();
	            	playerMazeX[pp][0] = maze.blockToMazeCoord((int)Math.round(playerLoc.getX()-0.5-maze.mazeX));
	            	playerMazeZ[pp][0] = maze.blockToMazeCoord((int)Math.round(playerLoc.getZ()-0.5-maze.mazeZ));
	            	if (playerMazeX[pp][0]%2 == 0) playerMazeX[pp][0]++;
	            	if (playerMazeZ[pp][0]%2 == 0) playerMazeZ[pp][0]++;
	            	if (!(playerLoc.getX()-0.5 < maze.mazeX || playerMazeX[pp][0] > maze.mazeSize*2 || playerLoc.getZ()-0.5 < maze.mazeZ || playerMazeZ[pp][0] > maze.mazeSize*2)) {
	            		updateBoss = true;
	            		if (angle >= -135 && angle <= -45) playerMazeMatters[pp][1] = false;
	            		playerMazeX[pp][1] = maze.blockToMazeCoord((int)Math.round(playerLoc.getX()-0.5-maze.mazeX-Maze.MAZE_PASSAGE_WIDTH*0.5-1));
	            		playerMazeZ[pp][1] = maze.blockToMazeCoord((int)Math.round(playerLoc.getZ()-0.5-maze.mazeZ));
		            	if (angle >= 45 && angle <= 135) playerMazeMatters[pp][2] = false;
		            	playerMazeX[pp][2] = maze.blockToMazeCoord((int)Math.round(playerLoc.getX()-0.5-maze.mazeX+Maze.MAZE_PASSAGE_WIDTH*0.5+1));
		            	playerMazeZ[pp][2] = maze.blockToMazeCoord((int)Math.round(playerLoc.getZ()-0.5-maze.mazeZ));
			            if (angle >= -45 && angle <= 45) playerMazeMatters[pp][3] = false;
		            	playerMazeX[pp][3] = maze.blockToMazeCoord((int)Math.round(playerLoc.getX()-0.5-maze.mazeX));
		            	playerMazeZ[pp][3] = maze.blockToMazeCoord((int)Math.round(playerLoc.getZ()-0.5-maze.mazeZ-Maze.MAZE_PASSAGE_WIDTH*0.5-1));
	            		if (angle >= 135 || angle <= -135) playerMazeMatters[pp][4] = false;
		            	playerMazeX[pp][4] = maze.blockToMazeCoord((int)Math.round(playerLoc.getX()-0.5-maze.mazeX));
		            	playerMazeZ[pp][4] = maze.blockToMazeCoord((int)Math.round(playerLoc.getZ()-0.5-maze.mazeZ+Maze.MAZE_PASSAGE_WIDTH*0.5+1));
	            	} else playerMazeX[pp][0] = playerMazeZ[pp][0] = -1;
	            	for (i = 1; i < 5; i++) {
	            		if (playerMazeX[pp][i] == -1) continue;
	            		if (playerMazeX[pp][i] < 0 || playerMazeX[pp][i] > maze.mazeSize*2 || playerMazeZ[pp][i] < 0 || playerMazeZ[pp][i] > maze.mazeSize*2)
	            			playerMazeX[pp][i] = playerMazeZ[pp][i] = -1;
	            		else if (playerMazeX[pp][i]%2 == 0 || playerMazeZ[pp][i]%2 == 0) playerMazeX[pp][i] = playerMazeZ[pp][i] = -1;
	            		else if (maze.maze[(playerMazeX[pp][i]+playerMazeX[pp][0])/2][playerMazeZ[pp][i]] == 1
		            		|| maze.maze[playerMazeX[pp][i]][(playerMazeZ[pp][i]+playerMazeZ[pp][0])/2] == 1) playerMazeX[pp][i] = playerMazeZ[pp][i] = -1;
	            	}
	      			
	      			int xOffs = 0, zOffs = 0;
	      			if (angle >= -75 && angle <= 75) zOffs = 1;
	      			if (angle >= 15 && angle <= 165) xOffs = -1;
	      			if (angle >= 15 && angle <= 165) xOffs = -1;
	      			if (angle >= 105 || angle <= -105) zOffs = -1;
	      			if (angle >= -165 && angle <= -15) xOffs = 1;
		            	
	            	for (i = 0; i < 5; i++) {
	            		if (playerMazeX[pp][i] >= 0) {
	            			boolean lastPart = false;
	            			if (xOffs == 1) {
	            				for (xx = playerMazeX[pp][i]; xx < maze.mazeSize*2+1; xx++) {
	            					if (!lastPart) maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i], playerMazeMatters[pp][i]);
	            					if (playerMazeZ[pp][i] < maze.mazeSize*2 && (!lastPart || ((xx <= 0 || (playerMazeZ[pp][i]+1 < maze.mazeSize*2 && maze.maze[xx-1][playerMazeZ[pp][i]+2] != 1)) &&
	            							(xx <= 1 || maze.maze[xx-2][playerMazeZ[pp][i]+1] != 1)))) {
	            						maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i]+1, playerMazeMatters[pp][i]);
	            						if (playerMazeZ[pp][i]+1 < maze.mazeSize*2 && !(maze.maze[xx][playerMazeZ[pp][i]+1] == 1 &&
	            								((xx-1 < playerMazeX[pp][i] || maze.maze[xx-1][playerMazeZ[pp][i]+2] == 1) || (xx-1-xx%2 < playerMazeX[pp][i] || maze.maze[xx-1-xx%2][playerMazeZ[pp][i]+1] == 1))))
	            							maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i]+2, playerMazeMatters[pp][i]);
	            					}
	            					if (playerMazeZ[pp][i] > 0 && (!lastPart || ((xx <= 0 || (playerMazeZ[pp][i] > 1 && maze.maze[xx-1][playerMazeZ[pp][i]-2] != 1)) && 
	            							(xx <= 1 || maze.maze[xx-2][playerMazeZ[pp][i]-1] != 1)))) {
	            						maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i]-1, playerMazeMatters[pp][i]);
	            						if (playerMazeZ[pp][i] > 1 && !(maze.maze[xx][playerMazeZ[pp][i]-1] == 1 &&
	            								((xx-1 < playerMazeX[pp][i] || maze.maze[xx-1][playerMazeZ[pp][i]-2] == 1) || (xx-1-xx%2 < playerMazeX[pp][i] || maze.maze[xx-1-xx%2][playerMazeZ[pp][i]-1] == 1))))
	            							maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i]-2, playerMazeMatters[pp][i]);
	            					}
	            					if (lastPart) break;
	            					if (maze.maze[xx][playerMazeZ[pp][i]] == 1) lastPart = true;
	            				}
	            			} else if (xOffs == -1) {
	            				for (xx = playerMazeX[pp][i]; xx >= 0; xx--) {
	            					if (!lastPart) maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i], playerMazeMatters[pp][i]);
	            					if (playerMazeZ[pp][i] < maze.mazeSize*2 && (!lastPart || ((xx >= maze.mazeSize*2 || (playerMazeZ[pp][i]+1 < maze.mazeSize*2 && maze.maze[xx+1][playerMazeZ[pp][i]+2] != 1)) &&
	            							(xx+1 >= maze.mazeSize*2 || maze.maze[xx+2][playerMazeZ[pp][i]+1] != 1)))) {
	            						maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i]+1, playerMazeMatters[pp][i]);
	            						if (playerMazeZ[pp][i]+1 < maze.mazeSize*2 && !(maze.maze[xx][playerMazeZ[pp][i]+1] == 1 &&
	            								((xx >= playerMazeX[pp][i] || maze.maze[xx+1][playerMazeZ[pp][i]+2] == 1) || (xx+xx%2 >= playerMazeX[pp][i] || maze.maze[xx+1+xx%2][playerMazeZ[pp][i]+1] == 1))))
	            							maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i]+2, playerMazeMatters[pp][i]);
	            					}
	            					if (playerMazeZ[pp][i] > 0 && (!lastPart || ((xx >= maze.mazeSize*2 || (playerMazeZ[pp][i] > 1 && maze.maze[xx+1][playerMazeZ[pp][i]-2] != 1)) &&
	            							(xx+1 >= maze.mazeSize*2 || maze.maze[xx+2][playerMazeZ[pp][i]-1] != 1)))) {
	            						maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i]-1, playerMazeMatters[pp][i]);
	            						if (playerMazeZ[pp][i] > 1 && !(maze.maze[xx][playerMazeZ[pp][i]-1] == 1 &&
	            								((xx >= playerMazeX[pp][i] || maze.maze[xx+1][playerMazeZ[pp][i]-2] == 1) || (xx+xx%2 >= playerMazeX[pp][i] || maze.maze[xx+1+xx%2][playerMazeZ[pp][i]-1] == 1))))
	            							maze.setToLookedAt(isLookedAt, xx, playerMazeZ[pp][i]-1, playerMazeMatters[pp][i]);
	            					}
	            					if (lastPart) break;
	            					if (maze.maze[xx][playerMazeZ[pp][i]] == 1) lastPart = true;
	            				}
	            			}
	            			lastPart = false;
	            			if (zOffs == 1) {
	            				for (zz = playerMazeZ[pp][i]; zz < maze.mazeSize*2+1; zz++) {
	            					if (!lastPart) maze.setToLookedAt(isLookedAt, playerMazeX[pp][i], zz, playerMazeMatters[pp][i]);
	            					if (playerMazeX[pp][i] < maze.mazeSize*2 && (!lastPart || ((zz <= 0 || (playerMazeX[pp][i]+1 < maze.mazeSize*2 && maze.maze[playerMazeX[pp][i]+2][zz-1] != 1)) &&
	            							(zz <= 1 || maze.maze[playerMazeX[pp][i]+1][zz-2] != 1)))) {
	            						maze.setToLookedAt(isLookedAt, playerMazeX[pp][i]+1, zz, playerMazeMatters[pp][i]);
	            						if (playerMazeX[pp][i]+1 < maze.mazeSize*2 && !(maze.maze[playerMazeX[pp][i]+1][zz] == 1 &&
	            								((zz-1 >= 0 && maze.maze[playerMazeX[pp][i]+2][zz-1] == 1) || (zz-1-zz%2 >= 0 && maze.maze[playerMazeX[pp][i]+1][zz-1-zz%2] == 1))))
	            							maze.setToLookedAt(isLookedAt, playerMazeX[pp][i]+2, zz, playerMazeMatters[pp][i]);
	            					}
	            					if (playerMazeX[pp][i] > 0 && (!lastPart || ((zz <= 0 || (playerMazeX[pp][i] > 1 && maze.maze[playerMazeX[pp][i]-2][zz-1] != 1)) &&
	            							(zz <= 1 || maze.maze[playerMazeX[pp][i]-1][zz-2] != 1)))) {
	            						maze.setToLookedAt(isLookedAt, playerMazeX[pp][i]-1, zz, playerMazeMatters[pp][i]);
	            						if (playerMazeX[pp][i] > 1 && !(maze.maze[playerMazeX[pp][i]-1][zz] == 1 &&
	            								((zz-1 < playerMazeZ[pp][i] || maze.maze[playerMazeX[pp][i]-2][zz-1] == 1) || (zz-1-zz%2 < playerMazeZ[pp][i] || maze.maze[playerMazeX[pp][i]-1][zz-1-zz%2] == 1))))
	            							maze.setToLookedAt(isLookedAt, playerMazeX[pp][i]-2, zz, playerMazeMatters[pp][i]);
	            					}
	            					if (lastPart) break;
	            					if (maze.maze[playerMazeX[pp][i]][zz] == 1) lastPart = true;
	            				}
	            			} else if (zOffs == -1) {
	            				for (zz = playerMazeZ[pp][i]; zz >= 0; zz--) {
	            					if (!lastPart) maze.setToLookedAt(isLookedAt, playerMazeX[pp][i], zz, playerMazeMatters[pp][i]);
	            					if (playerMazeX[pp][i] < maze.mazeSize*2 && (!lastPart || ((zz >= maze.mazeSize*2 || (playerMazeX[pp][i]+1 < maze.mazeSize*2 && maze.maze[playerMazeX[pp][i]+2][zz+1] != 1)) &&
	            							(zz+1 >= maze.mazeSize*2 || maze.maze[playerMazeX[pp][i]+1][zz+2] != 1)))) {
	            						maze.setToLookedAt(isLookedAt, playerMazeX[pp][i]+1, zz, playerMazeMatters[pp][i]);
	            						if (playerMazeX[pp][i]+1 < maze.mazeSize*2 && !(maze.maze[playerMazeX[pp][i]+1][zz] == 1 &&
	            								((zz >= playerMazeZ[pp][i] || maze.maze[playerMazeX[pp][i]+2][zz+1] == 1) || (zz+zz%2 >= playerMazeZ[pp][i] || maze.maze[playerMazeX[pp][i]+1][zz+1+zz%2] == 1))))
	            							maze.setToLookedAt(isLookedAt, playerMazeX[pp][i]+2, zz, playerMazeMatters[pp][i]);
	            					}
	            					if (playerMazeX[pp][i] > 0 && (!lastPart || ((zz >= maze.mazeSize*2 || (playerMazeX[pp][i] > 1 && maze.maze[playerMazeX[pp][i]-2][zz+1] != 1)) &&
	            							(zz+1 >= maze.mazeSize*2 || maze.maze[playerMazeX[pp][i]-1][zz+2] != 1)))) {
	            						maze.setToLookedAt(isLookedAt, playerMazeX[pp][i]-1, zz, playerMazeMatters[pp][i]);
	            						if (playerMazeX[pp][i] > 1 && !(maze.maze[playerMazeX[pp][i]-1][zz] == 1 &&
	            								((zz >= playerMazeZ[pp][i] || maze.maze[playerMazeX[pp][i]-2][zz+1] == 1) || (zz+zz%2 >= playerMazeZ[pp][i] || maze.maze[playerMazeX[pp][i]-1][zz+1+zz%2] == 1))))
	            							maze.setToLookedAt(isLookedAt, playerMazeX[pp][i]-2, zz, playerMazeMatters[pp][i]);
	            					}
	            					if (lastPart) break;
	            					if (maze.maze[playerMazeX[pp][i]][zz] == 1) lastPart = true;
	            				}
	            			}
	            		}
	            	}
	        	}
	          	int changedNum;
	          	List<MazeCoords> pChangedBlocks, changedBlocks;
	          	MazeCoords coords;
	          	changedBlocks = new ArrayList<MazeCoords>();
	        	if (!players.isEmpty()) { 
	            	for (xx = 2; xx <= maze.mazeSize*2-2; xx += 2) {
	            		for (zz = 2; zz <= maze.mazeSize*2-2; zz += 2) {
	            			if (!maze.isBeingChanged[xx][zz] && !isLookedAt[xx][zz]) {
	    	        			posX = maze.mazeToBlockCoord(xx);
	    	        			posZ = maze.mazeToBlockCoord(zz);
	        					if (maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ) == null || maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ).isEmpty()) {
	        						if (maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+1, maze.mazeZ+posZ).getType() == Material.CHEST) {
		        						Chest chest = ((Chest)maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+1, maze.mazeZ+posZ).getState());
		        						boolean chestIsEmpty = true;
		        						ItemStack[] chestInv = chest.getInventory().getContents();
		        		    			for (j = 0; j < chestInv.length; j++) {
		        		    				if (chestInv[j] != null && chestInv[j].getType() == Material.AIR && chestInv[j].getAmount() == 0) {
		        		    					chestIsEmpty = false;
		        		    					break;
		        		    				}
		        		    			}
			            				if (chestIsEmpty || Math.random() < 0.1) {
			            					chest.getInventory().clear();
			            					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+1, maze.mazeZ+posZ).setTypeId(maze.configProps.blockTypes[2][6][0][0]);
			            					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+1, maze.mazeZ+posZ).setData((byte)maze.configProps.blockTypes[2][6][0][1]);
			            					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ).setTypeId(maze.configProps.blockTypes[2][5][0][0]);
			            					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ).setData((byte)maze.configProps.blockTypes[2][5][0][1]);
			            				}
	        						} else {
		            					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+1, maze.mazeZ+posZ).setTypeId(maze.configProps.blockTypes[2][6][0][0]);
		            					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+1, maze.mazeZ+posZ).setData((byte)maze.configProps.blockTypes[2][6][0][1]);
		            					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ).setTypeId(maze.configProps.blockTypes[2][5][0][0]);
		            					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ).setData((byte)maze.configProps.blockTypes[2][5][0][1]);
	        						}
	        					}
	            			}
	            		}
	            	}
	            	for (xx = 1; xx <= maze.mazeSize*2-1; xx += 2) {
	            		for (zz = 1; zz <= maze.mazeSize*2-1; zz += 2) {
	            			if (!maze.isBeingChanged[xx][zz] && !isLookedAt[xx][zz] && Math.random() < maze.configProps.groundReappearProb &&
	            				maze.mazeWorld.getBlockAt(maze.mazeX+maze.mazeToBlockCoord(xx), maze.mazeY, maze.mazeZ+maze.mazeToBlockCoord(zz)).isEmpty()) {
	            				int xCoord = maze.mazeToBlockCoord(xx);
	            				for (int xxx = xCoord; xxx <= maze.mazeToBlockCoord(xx)+Maze.MAZE_PASSAGE_WIDTH-1; xxx++) {
		            				int zCoord = maze.mazeToBlockCoord(zz);
	            					for (int zzz = zCoord; zzz <= maze.mazeToBlockCoord(zz)+Maze.MAZE_PASSAGE_WIDTH-1; zzz++) {
	            						int bId = maze.configProps.blockTypes[4][zzz-zCoord][xxx-xCoord][0];
	            						int bData = (byte)maze.configProps.blockTypes[4][zzz-zCoord][xxx-xCoord][1];
	            						maze.mazeWorld.getBlockAt(maze.mazeX+xxx, maze.mazeY, maze.mazeZ+zzz).setTypeId(bId);
	            						maze.mazeWorld.getBlockAt(maze.mazeX+xxx, maze.mazeY, maze.mazeZ+zzz).setData((byte)bData);
	  		        					if (MazePvP.theMazePvP.showHeads) maze.mazeWorld.getBlockAt(maze.mazeX+xxx, maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2, maze.mazeZ+zzz).setType(Material.AIR);
	            					}
	            				}
	            			}
	            		}
	            	}
	        	}
	        	pit = players.iterator();
	        	for (int pp = 0; pp < players.size(); pp++) {
	            	currentPlayer = pit.next();
	            	if (playerMazeX[pp][0] == -1) continue;
	            	int distance = 1;
	            	boolean first = true;
	            	changedNum = 0;
	            	pChangedBlocks = new ArrayList<MazeCoords>();
	            	while (true) {
	            		boolean chestShouldAppear = (Math.random() < maze.configProps.chestAppearProb+maze.configProps.enderChestAppearProb);
	            		if (chestShouldAppear) {
			            	for (xx = Math.max(2, playerMazeX[pp][0]-distance); xx <= Math.min(maze.mazeSize*2-2, playerMazeX[pp][0]+distance); xx += 2) {
			            		for (zz = Math.max(2, playerMazeZ[pp][0]-distance); zz <= Math.min(maze.mazeSize*2-2, playerMazeZ[pp][0]+distance); zz += 2) {
			            			if (!maze.isBeingChanged[xx][zz] && !isLookedAt[xx][zz] && maze.canBeReached(playerMazeX[pp][0], playerMazeZ[pp][0], xx, zz, distance, pathArray)) {
			            				pChangedBlocks.add(new MazeCoords(xx, zz, 20));
			            				changedNum++;
			            			}
			            		}
			            	}
	            		} else {
			            	for (xx = Math.max(2, playerMazeX[pp][0]-distance); xx <= Math.min(maze.mazeSize*2-2, playerMazeX[pp][0]+distance); xx += 2) {
			            		for (zz = Math.max(1, playerMazeZ[pp][0]-distance+1); zz <= Math.min(maze.mazeSize*2-1, playerMazeZ[pp][0]+distance-1); zz += 2) {
			            			if (!maze.isBeingChanged[xx][zz] && !isLookedAt[xx][zz] && !(maze.maze[xx][zz] == 1 && (maze.pillarIsAlone(xx, zz+1, xx, zz) || maze.pillarIsAlone(xx, zz-1, xx, zz)))
			            					&& maze.canBeReached(playerMazeX[pp][0], playerMazeZ[pp][0], xx, zz, distance, pathArray)) {
			            				pChangedBlocks.add(new MazeCoords(xx, zz, 1));
			            				changedNum++;
			            			}
			            		}
			            	}
			            	for (zz = Math.max(2, playerMazeZ[pp][0]-distance); zz <= Math.min(maze.mazeSize*2-2, playerMazeZ[pp][0]+distance); zz += 2) {
			            		for (xx = Math.max(1, playerMazeX[pp][0]-distance+1); xx <= Math.min(maze.mazeSize*2-1, playerMazeX[pp][0]+distance-1); xx += 2) {
			            			if (!maze.isBeingChanged[xx][zz] && !isLookedAt[xx][zz] && !(maze.maze[xx][zz] == 1 && (maze.pillarIsAlone(xx+1, zz, xx, zz) || maze.pillarIsAlone(xx-1, zz, xx, zz)))
			            					&& maze.canBeReached(playerMazeX[pp][0], playerMazeZ[pp][0], xx, zz, distance, pathArray)) {
			            				pChangedBlocks.add(new MazeCoords(xx, zz, 1));
			            				changedNum++;
			            			}
			            		}
			            	}
			            	for (xx = Math.max(1, playerMazeX[pp][0]-distance+1); xx <= Math.min(maze.mazeSize*2-1, playerMazeX[pp][0]+distance-1); xx += 2) {
			            		for (zz = Math.max(1, playerMazeZ[pp][0]-distance+1); zz <= Math.min(maze.mazeSize*2-1, playerMazeZ[pp][0]+distance-1); zz += 2) {
			            			if (!maze.isBeingChanged[xx][zz] && !isLookedAt[xx][zz] && maze.canBeReached(playerMazeX[pp][0], playerMazeZ[pp][0], xx, zz, distance, pathArray)) {
			            				pChangedBlocks.add(new MazeCoords(xx, zz, 10));
			            				changedNum++;
			            			}
			            		}
			            	}
	            		}
		            	if (first) {
		            		first = false;
		            		distance += 2;
		            		continue;
		            	}
		            	if (changedNum == 0 && distance < 5) {
		            		first = true;
		            		distance += 2;
		            	} else break;
	            	}
	            	if (changedNum > 0) {
	            		int repeatNum = 0;
	            		while(true) {
		            		changedNum = (int)Math.floor(Math.random()*changedNum);
		            		coords = (MazeCoords)pChangedBlocks.get(changedNum);
		            		if (repeatNum < 2 && coords.type == 1) {
		            			repeatNum++;
		            			continue;
		            		}
		            		if (!shouldChange[coords.x][coords.z]) {
		            			shouldChange[coords.x][coords.z] = true;
		            			changedBlocks.add(coords);
		            		}
		            		break;
	            		}
	            	}
	        	}
	        	int posY, yy, endX, endY, endZ;
	        	Iterator<MazeCoords> chIt = changedBlocks.iterator();
	        	if (chIt.hasNext()) {
		        	while (chIt.hasNext()) {
		        		coords = (MazeCoords)chIt.next();
	        			posX = maze.mazeToBlockCoord(coords.x);
	  					posY = 1;
		        		posZ = maze.mazeToBlockCoord(coords.z);
	      				endY = Maze.MAZE_PASSAGE_HEIGHT+1;
	      				if (coords.type == 20) {
	      					if (maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ) != null && !maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ).isEmpty()) {
	          					if (Math.random()*(maze.configProps.chestAppearProb+maze.configProps.enderChestAppearProb) < maze.configProps.enderChestAppearProb) {
	              					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+1, maze.mazeZ+posZ).setType(Material.ENDER_CHEST); 
	          						maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ).setType(Material.AIR);
	          					} else {
	              					maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+1, maze.mazeZ+posZ).setType(Material.CHEST); 
	          						maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+2, maze.mazeZ+posZ).setType(Material.AIR);
	        						Chest chest = (Chest)(maze.mazeWorld.getBlockAt(maze.mazeX+posX, maze.mazeY+1, maze.mazeZ+posZ).getState());
		    		    			int mazeItemNum = (int)Math.floor(Math.random()*20)+1;
		    		    			if (mazeItemNum < 8) mazeItemNum = 1;
		    		    			else if (mazeItemNum < 13) mazeItemNum = 2;
		    		    			else if (mazeItemNum < 17) mazeItemNum = 3;
		    		    			else if (mazeItemNum < 20) mazeItemNum = 4;
		    		    			else mazeItemNum = 5;
		    		    			for (j = 0; j < mazeItemNum; j++) {
		    		    				double currentWeigh = 0.0;
		    		    				double weighSum = 0.0;
		    		    				for (i = 0; i < maze.configProps.chestWeighs.length; i++) weighSum += maze.configProps.chestWeighs[i];
		    		    				double randNum = Math.random()*weighSum;
		    		    				for (i = 0; i < maze.configProps.chestWeighs.length; i++) {
		    		    					currentWeigh += maze.configProps.chestWeighs[i];
		    		    					if (currentWeigh >= randNum) break;
		    		    				}
		    		    				chest.getInventory().addItem(maze.configProps.chestItems[i].clone());
		    		    			}
	          					}
	      					}
	      					continue;
	      				}
		        		if (coords.x%2 == 0 || coords.z%2 == 0) {
	        				if (coords.x%2 != 0) {
	        					endX = posX+Maze.MAZE_PASSAGE_WIDTH-1;
	            				endZ = posZ;
	        				} else if (coords.z%2 != 0) {
	        					endZ = posZ+Maze.MAZE_PASSAGE_WIDTH-1;
	            				endX = posX;
	        				} else {
	            				endX = posX;
	            				endZ = posZ;
	        				}
	        			} else {
	        				endX = posX+Maze.MAZE_PASSAGE_WIDTH-1;
	        				endZ = posZ+Maze.MAZE_PASSAGE_WIDTH-1;
	        				posY = endY = 0;
	        			}
		        		if (coords.type == 1) {
		        			if (maze.maze[coords.x][coords.z] == 1) maze.maze[coords.x][coords.z] = 0;
		        			else if (maze.maze[coords.x][coords.z] == 0) maze.maze[coords.x][coords.z] = 1;
		        		}
		        		boolean spawnMob = false;
		        		if (coords.type == 10 && Math.random() < maze.configProps.spawnMobProb) spawnMob = true;
	        			for (xx = posX; xx <= endX; xx++) {
		        			for (yy = posY; yy <= endY; yy++) {
		        				for (zz = posZ; zz <= endZ; zz++) {
		            				int bId = 0;
		            				byte bData = 0;
		        					if (coords.type == 1) {
			        					if (maze.maze[coords.x][coords.z] == 1) {
			        						if (coords.x%2 == 0) {
			    	    						bId = maze.configProps.blockTypes[0][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][zz-posZ][0];
			    		    					bData = (byte)maze.configProps.blockTypes[0][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][zz-posZ][1];
			    	    					} else {
			    	    						bId = maze.configProps.blockTypes[0][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][xx-posX][0];
			    		    					bData = (byte)maze.configProps.blockTypes[0][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][xx-posX][1];
			    	    					}
			        					} else if (maze.maze[coords.x][coords.z] == 0) {
			        						bId = 0;
			        						bData = 0;
			        					}
		        					} else if (coords.type == 10) {
			        					if (spawnMob || maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz) == null || maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).isEmpty()) {
			        						bId = maze.configProps.blockTypes[4][zz-posZ][xx-posX][0];
			        						bData = (byte)maze.configProps.blockTypes[4][zz-posZ][xx-posX][1];
				            				if (MazePvP.theMazePvP.showHeads) maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2, maze.mazeZ+zz).setType(Material.AIR);
			        					} else {
			        						bId = 0;
			        						bData = 0;
			        					}
			        				}
		            				maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).setTypeId(bId);
		            				maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).setData(bData);
		            			}
		        			}
	        			}
	        			if (spawnMob) {
	        				EntityType mobType;
	        				int randNum = (int)Math.floor(Math.random()*10);
	        				if (randNum < 3) mobType = EntityType.ZOMBIE;
	        				else if (randNum < 6) mobType = EntityType.SKELETON;
	        				else if (randNum < 9) mobType = EntityType.SPIDER;
	        				else mobType = EntityType.CREEPER;
	        				LivingEntity mob = (LivingEntity)maze.mazeWorld.spawnEntity(new Location(maze.mazeWorld, maze.mazeX+posX+Maze.MAZE_PASSAGE_WIDTH*0.5, maze.mazeY+posY+1.0, maze.mazeZ+posZ+Maze.MAZE_PASSAGE_WIDTH*0.5), mobType);
	        				if (mobType == EntityType.SKELETON) {
	        					Skeleton skeleton = (Skeleton)mob;
	        					skeleton.getEquipment().setItemInHand(new ItemStack(Material.BOW));
	        					skeleton.setSkeletonType(SkeletonType.NORMAL);
	        				}
	        			}
		        	}
	        	}
	        	if (updateBoss) {
	        		if (maze.boss.entity== null) maze.makeNewMazeBoss();
	        		if (Math.random() < 0.05) {
	        			maze.relocateMazeBoss(false);
	        		}
	        	}
	        	//main.saveMazeProps(maze);
	      	}
	      	if (!maze.blocksToRemove.isEmpty()) {
	      		Iterator<MazeCoords> it = maze.blocksToRemove.iterator();
	      		int sx, sy, sz, ex, ey, ez;
	      		//boolean save = false;
	      		while (it.hasNext()) {
	      			MazeCoords coords = (MazeCoords)it.next();
	      			coords.type--;
	      			if (coords.type == 0 || coords.x%2 == 0 || coords.z%2 == 0) {
	      				//save = true;
	      				sx = maze.mazeToBlockCoord(coords.x);
						sy = 1;
	        			sz = maze.mazeToBlockCoord(coords.z);
	    				ey = Maze.MAZE_PASSAGE_HEIGHT+1;
		        		if (coords.x%2 == 0 || coords.z%2 == 0) {
	        				if (coords.x%2 != 0) {
	        					ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
	            				ez = sz;
	        				} else if (coords.z%2 != 0) {
	        					ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
	            				ex = sx;
	        				} else {
	            				ex = sx;
	            				ez = sz;
	        				}
	        			} else {
	        				ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
	        				ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
	        				sy = ey = 0;
	        			}
		        		if (maze.maze[coords.x][coords.z] == 1) maze.maze[coords.x][coords.z] = 0;
		        		for (xx = sx; xx <= ex; xx++) {
		        			for (int yy = sy; yy <= ey; yy++) {
		        				for (zz = sz; zz <= ez; zz++) {
		        					maze.mazeWorld.playEffect(new Location(maze.mazeWorld, maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz), Effect.STEP_SOUND, maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).getTypeId() + (maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).getData() << 12));
		        					maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).setType(Material.AIR);
		            			}
		        			}
		        		}
		        		maze.isBeingChanged[coords.x][coords.z] = false;
	      				it.remove();
	      			} else if (coords.type%5 == 0) {
		        		for (xx = maze.mazeToBlockCoord(coords.x); xx < maze.mazeToBlockCoord(coords.x)+Maze.MAZE_PASSAGE_WIDTH-1; xx++) {
			        		for (zz = maze.mazeToBlockCoord(coords.z); zz <  maze.mazeToBlockCoord(coords.z)+Maze.MAZE_PASSAGE_WIDTH-1; zz++) {
			        			ParticleEffect.SMOKE.display(new Location(maze.mazeWorld, maze.mazeX+xx+1, maze.mazeY+1, maze.mazeZ+zz+1), (float)0.0, (float)0.0, (float)0.0, (float)0.0, 10);
			        		}
		        		}
	      			}
	      		}
	      		//if (save) main.saveMazeProps(maze);
	      	}
	      	if (!maze.blocksToRestore.isEmpty()) {
	      		Iterator<MazeCoords> it = maze.blocksToRestore.iterator();
	      		int sx, sy, sz, ex, ey, ez;
	      		//boolean save = false;
	      		while (it.hasNext()) {
	      			MazeCoords coords = (MazeCoords)it.next();
	  				sx = maze.mazeToBlockCoord(coords.x);
					sy = 1;
	      			sz = maze.mazeToBlockCoord(coords.z);
	  				ey = Maze.MAZE_PASSAGE_HEIGHT+1;
	  				boolean restoreFloor = false;
		        	if (coords.x%2 == 0 || coords.z%2 == 0) {
	      				if (coords.x%2 != 0) {
	      					ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
	          				ez = sz;
	      				} else if (coords.z%2 != 0) {
	      					ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
	          				ex = sx;
	      				} else {
	          				ex = sx;
	          				ez = sz;
	      				}
	      			} else {
	      				ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
	      				ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
	      				sy = ey = 0;
	      				restoreFloor = true;
	      			}
	        		xx = 0;
	        		int yy = 0;
	        		zz = 0;
	        		int itCount;
	        		boolean end = true;
	      			
	  				for (i = 0; i < 3; i++) {
	  	        		itCount = 0;
	  	        		while (true) {
	  	        			end = true;
	  		        		outerloop: for (xx = sx; xx <= ex; xx++) {
	  		        			for (yy = sy; yy <= ey; yy++) {
	  		        				for (zz = sz; zz <= ez; zz++) {
	  			        				if (maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz) == null || maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).isEmpty()) {
	  			        					end = false;
	  			        					break outerloop;
	  			        				}
	  			        			}
	  		        			}
	  		        		}
	  	        			if (end) break;
	  	        			xx = sx+(int)Math.floor(Math.random()*(ex-sx+1));
	  	        			yy = sy+(int)Math.floor(Math.random()*(ey-sy+1));
	  	        			zz = sz+(int)Math.floor(Math.random()*(ez-sz+1));
	  	        			if (maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz) == null || maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).isEmpty()) {
	  	        				break;
	  	        			}
	  	        			itCount++;
	  	        			if (itCount > 1000) {
	  	        				break;
	  	        			}
	  	        		}
	  	        		if (end) {
	    	        		if (maze.maze[coords.x][coords.z] == 0 && !(coords.x%2 == 1 && coords.z%2 == 1)) maze.maze[coords.x][coords.z] = 1;
	        				//save = true;
	    	        		maze.isBeingChanged[coords.x][coords.z] = false;
	    	        		if (restoreFloor && MazePvP.theMazePvP.showHeads) {
	    	        			for (xx = sx; xx <= ex; xx++) {
	  		        				for (zz = sz; zz <= ez; zz++) {
	  		        					maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2, maze.mazeZ+zz).setType(Material.AIR);
	  			        			}
		  		        		}
	    	        		}
	    	        		it.remove();
	    	        		break;
	  	        		} else {
	  	        			int bId = 0, bData = 0;
	  	        			if (restoreFloor) {
	  	        				bId = maze.configProps.blockTypes[4][zz-sz][xx-sx][0];
        						bData = (byte)maze.configProps.blockTypes[4][zz-sz][xx-sx][1];
	  	        			} else {
	  	        				if (coords.x%2 == 0) {
    	    						bId = maze.configProps.blockTypes[0][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][zz-sz][0];
    		    					bData = (byte)maze.configProps.blockTypes[0][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][zz-sz][1];
    	    					} else {
    	    						bId = maze.configProps.blockTypes[0][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][xx-sx][0];
    		    					bData = (byte)maze.configProps.blockTypes[0][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][xx-sx][1];
    	    					}
	  	        			}
	  	        			maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).setTypeId(bId);
	  	        			maze.mazeWorld.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).setData((byte) bData);
	  	        		}
	  				}
	      		}
	      		//if (save) main.saveMazeProps(maze);
	      	}
		}
		if (main.wallChangeTimer >= Maze.WALL_CHANGE_SPEED) {
			main.wallChangeTimer = 0;
		}
		if (main.mazeBossRestoreTimer >= Maze.BOSS_RESTORE_SPEED) {
			main.mazeBossRestoreTimer = 0;
		}
	}

}
