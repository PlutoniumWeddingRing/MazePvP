package mazepvp;

import java.awt.geom.Point2D;
import java.util.Iterator;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.util.BlockIterator;

public final class EventListeners implements Listener {

	@EventHandler
    public void wLoadListener(WorldLoadEvent event) {
		MazePvP.theMazePvP.loadMazeProps(event.getWorld());
	}

	@EventHandler
    public void wUnloadListener(WorldUnloadEvent event) {
		MazePvP.theMazePvP.removeMazes(event.getWorld());
	}
	
	@EventHandler
    public void wSaveListener(WorldSaveEvent event) {
		MazePvP.theMazePvP.saveMazeProps(event.getWorld());
	}

	@EventHandler
    public void explodeListener(EntityExplodeEvent event) {
		Iterator<Block> it = event.blockList().iterator();
		while (it.hasNext()) {
			Block block = it.next();
			Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
			while (mit.hasNext()) {
				Maze maze = mit.next();
				if (block.getX() >= maze.mazeX && block.getX() <= maze.mazeX+maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
	  	      	&&  block.getZ() >= maze.mazeZ && block.getZ() <= maze.mazeZ+maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
	  	      	&&  block.getY() >= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH && block.getY() <= maze.mazeY+Maze.MAZE_PASSAGE_HEIGHT+2) {
					it.remove();
					break;
				}
			}
		}
    }

	@EventHandler
    public void projHitListener(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof Egg || event.getEntity() instanceof Snowball || event.getEntity() instanceof EnderPearl)) return;
		Iterator<Maze> it = MazePvP.theMazePvP.mazes.iterator();
		while (it.hasNext()) {
			Maze maze = it.next();
			World world = event.getEntity().getWorld();
			if (event.getEntity() instanceof EnderPearl && event.getEntity().getShooter() != null && maze.isInsideMaze(event.getEntity().getShooter().getLocation())) {
	      		Point2D.Double loc = maze.getMazeBossNewLocation(world);
	      		LivingEntity thrower = event.getEntity().getShooter();
        		for (int j = 0; j <= 8; j++) {
        			world.playEffect(thrower.getLocation(), Effect.SMOKE, j);
        			if (j != 4) {
        				world.playEffect(new Location(world, thrower.getLocation().getX(), thrower.getLocation().getY()+1, thrower.getLocation().getZ()), Effect.SMOKE, j);
        			}
        		}
            	thrower.teleport(new Location(world, loc.x, maze.mazeY+1, loc.y));
        		for (int j = 0; j <= 8; j++) {
        			world.playEffect(thrower.getLocation(), Effect.SMOKE, j);
        			if (j != 4) {
        				world.playEffect(new Location(world, thrower.getLocation().getX(), thrower.getLocation().getY()+1, thrower.getLocation().getZ()), Effect.SMOKE, j);
        			}
        		}
                thrower.setFallDistance(0.0F);
	      	}
			BlockIterator bit = new BlockIterator(world, event.getEntity().getLocation().toVector(), event.getEntity().getVelocity().normalize(), 0.0D, 4);
			Block block;
			if (bit.hasNext()) {
				block = null;
				while (bit.hasNext()) {
					block = bit.next();
		            if (block.getType() != Material.AIR) {
		                break;
		            }
		            if (!bit.hasNext()) return;
		        }
				int yOffs = 0;
				int yOffs2 = 0;
				if (event.getEntity() instanceof Egg) yOffs = 1;
				if (event.getEntity() instanceof EnderPearl || event.getEntity() instanceof Snowball) yOffs2 = 2;
		  		if (block.getType() != Material.AIR) {
			  		if (block.getX() >= maze.mazeX && block.getX() <= maze.mazeX+maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
			      	&&  block.getZ() >= maze.mazeZ && block.getZ() <= maze.mazeZ+maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
			      	&&  block.getY() >= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+yOffs && block.getY() <= maze.mazeY+Maze.MAZE_PASSAGE_HEIGHT+yOffs2) {
			  			if (event.getEntity() instanceof Egg) {
			  	      		int mazeX = maze.blockToMazeCoord(block.getX()-maze.mazeX);
			  	      		int mazeZ = maze.blockToMazeCoord(block.getZ()-maze.mazeZ);
			  	      		int sideHit = 0;
			  	      		//System.out.println("B: "+block.getLocation());
			  	      		//System.out.println("S: "+event.getEntity().getLocation());
			  	      		if (Math.abs(event.getEntity().getLocation().getY()-0.5-block.getY()) >
			  	      		    Math.max(Math.abs(event.getEntity().getLocation().getZ()-0.5-block.getZ()), Math.abs(event.getEntity().getLocation().getX()-0.5-block.getX()))) {
				  	      		if (event.getEntity().getLocation().getY()-0.5 < block.getY()) sideHit = 0;
			  	      			else sideHit = 1;
			  	      		} else if (mazeX%2 == 0 && mazeZ%2 == 0) {
			  	      			if (Math.abs(event.getEntity().getLocation().getZ()-0.5-block.getZ()) > Math.abs(event.getEntity().getLocation().getX()-0.5-block.getX())) {
				  	      			if (event.getEntity().getLocation().getZ()-0.5 < block.getZ()) sideHit = 2;
				  	      			else sideHit = 3;
			  	      			} else {
				  	      			if (event.getEntity().getLocation().getX()-0.5 < block.getX()) sideHit = 4;
				  	      			else sideHit = 5;
			  	      			}
			  	      		} else if (mazeZ%2 == 0) {
			  	      			if (event.getEntity().getLocation().getZ()-0.5 < block.getZ()) sideHit = 2;
			  	      			else sideHit = 3;
			  	      		} else if (mazeX%2 == 0) {
			  	      			if (event.getEntity().getLocation().getX()-0.5 < block.getX()) sideHit = 4;
			  	      			else sideHit = 5;
			  	      		}
			  	      		//System.out.println("side: "+sideHit);
				      		if (block.getY() > maze.mazeY) {
				      			if (mazeX%2 == 0 && mazeZ%2 == 0) {
				      				if (sideHit == 5) mazeZ++;
				      				else if (sideHit == 4) mazeZ--;
				      				else if (sideHit == 3) mazeX--;
				      				else if (sideHit == 2) mazeX++;
				      			}
				      		} else if (sideHit > 1) {
				      			if (mazeX%2 == 0) {
				      				if (world.getBlockAt(new Location(world, block.getX()+1, block.getY(), block.getZ())).getType() != Material.AIR) mazeX++;
				      				else if (world.getBlockAt(new Location(world, block.getX()-1, block.getY(), block.getZ())).getType() != Material.AIR) mazeX--;
				      				else return;
				      			} else if (mazeZ%2 == 0) {
				      				if (world.getBlockAt(new Location(world, block.getX(), block.getY(), block.getZ()+1)).getType() != Material.AIR) mazeZ++;
				      				else if (world.getBlockAt(new Location(world, block.getX(), block.getY(), block.getZ()-1)).getType() != Material.AIR) mazeZ--;
				      				else return;
				      			}
				      		}
				      		maze.removeMazeBlocks(mazeX, mazeZ, world);
			  			} else if (event.getEntity() instanceof Snowball) {
			  	      		int mazeX = maze.blockToMazeCoord(block.getX()-maze.mazeX);
			  	      		int mazeZ = maze.blockToMazeCoord(block.getZ()-maze.mazeZ);
			  	      		int sideHit = 0;
			  	      		//System.out.println("B: "+block.getLocation());
			  	      		//System.out.println("S: "+event.getEntity().getLocation());
			  	      		if (Math.abs(event.getEntity().getLocation().getY()-0.5-block.getY()) >
			  	      		    Math.max(Math.abs(event.getEntity().getLocation().getZ()-0.5-block.getZ()), Math.abs(event.getEntity().getLocation().getX()-0.5-block.getX()))) {
				  	      		if (event.getEntity().getLocation().getY()-0.5 < block.getY()) sideHit = 0;
			  	      			else sideHit = 1;
			  	      		} else if (mazeX%2 == 0 && mazeZ%2 == 0) {
			  	      			if (Math.abs(event.getEntity().getLocation().getZ()-0.5-block.getZ()) > Math.abs(event.getEntity().getLocation().getX()-0.5-block.getX())) {
				  	      			if (event.getEntity().getLocation().getZ()-0.5 < block.getZ()) sideHit = 2;
				  	      			else sideHit = 3;
			  	      			} else {
				  	      			if (event.getEntity().getLocation().getX()-0.5 < block.getX()) sideHit = 4;
				  	      			else sideHit = 5;
			  	      			}
			  	      		} else if (mazeZ%2 == 0) {
			  	      			if (event.getEntity().getLocation().getZ()-0.5 < block.getZ()) sideHit = 2;
			  	      			else sideHit = 3;
			  	      		} else if (mazeX%2 == 0) {
			  	      			if (event.getEntity().getLocation().getX()-0.5 < block.getX()) sideHit = 4;
			  	      			else sideHit = 5;
			  	      		}
			  	      		//System.out.println("side: "+sideHit);
			  				if (block.getY() > maze.mazeY) {
			  	              	float midPos;
			  	      			if (mazeX%2 == 0 && mazeZ%2 == 0) {
			  	      				if (sideHit == 2) mazeZ--;
			  	      				else if (sideHit == 3) mazeZ++;
			  	      				else if (sideHit == 4) mazeX--;
			  	      				else if (sideHit == 5) mazeX++;
			  	      			} else if (mazeX%2 == 0 && mazeZ%2 != 0) {
			  	      	        	midPos = maze.mazeToBlockCoord(mazeZ)+Maze.MAZE_PASSAGE_WIDTH*0.5f;
			  	      	        	if (block.getZ() >= maze.mazeZ+midPos) {
			  	      	        		//if (sideHit == 4 && mazeX > 0 && mazeZ < maze.mazeSize*2 && maze.maze[mazeX-1][mazeZ+1] == 1) return;
			  	      	        		//if (sideHit == 5 && mazeX < maze.mazeSize*2 && mazeZ < maze.mazeSize*2 && maze.maze[mazeX+1][mazeZ+1] == 1) return;
			  	      	        		mazeZ++;
			  	    	        		if (sideHit == 4) mazeX--;
			  	    	        		else if (sideHit == 5) mazeX++;
			  	      	        	} else {
			  	      	        		//if (sideHit == 4 && mazeX > 0 && mazeZ > 0 && maze.maze[mazeX-1][mazeZ-1] == 1) return;
			  	      	        		//if (sideHit == 5 && mazeX < maze.mazeSize*2 && mazeZ > 0 && maze.maze[mazeX+1][mazeZ-1] == 1) return;
			  	      	        		mazeZ--;
			  	    	        		if (sideHit == 4) mazeX--;
			  	    	        		else if (sideHit == 5) mazeX++;
			  	      	        	}
			  	      			} else if (mazeX%2 != 0 && mazeZ%2 == 0) {
			  	      				midPos = maze.mazeToBlockCoord(mazeX)+Maze.MAZE_PASSAGE_WIDTH*0.5f;
			  		    	        	if (block.getX() >= maze.mazeX+midPos) {
			  		    	        		//if (sideHit == 2 && mazeZ > 0 && mazeX < maze.mazeSize*2 && maze.maze[mazeX+1][mazeZ-1] == 1) return;
			  		    	        		//if (sideHit == 3 && mazeZ < maze.mazeSize*2 && mazeX < maze.mazeSize*2 && maze.maze[mazeX+1][mazeZ+1] == 1) return;
			  		    	        		mazeX++;
			  		    	        		if (sideHit == 2) mazeZ--;
			  		    	        		else if (sideHit == 3) mazeZ++;
			  		    	        	} else {
			  		    	        		//if (sideHit == 2 && mazeZ > 0 && mazeX > 0 && maze.maze[mazeX-1][mazeZ-1] == 1) return;
			  		    	        		//if (sideHit == 3 && mazeZ < maze.mazeSize*2 && mazeX > 0 && maze.maze[mazeX-1][mazeZ+1] == 1) return;
			  		    	        		mazeX--;
			  		    	        		if (sideHit == 2) mazeZ--;
			  		    	        		else if (sideHit == 3) mazeZ++;
			  		    	        	}
			  	      			}
			  	      		} else {
			  	      			if (mazeX%2 == 0) {
			  	      				if (sideHit == 5) mazeX++;
			  	      				else if (sideHit == 4) mazeX--;
			  	      			} else if (mazeZ%2 == 0) {
			  	      				if (sideHit == 3) mazeZ++;
			  	      				else if (sideHit == 2) mazeZ--;
			  	      			}
			  	      		}
			  	      		maze.restoreMazeBlocks(mazeX, mazeZ);
				      	}
					}
				}
			}
		}
    }
	@EventHandler
    public void teleportListener(PlayerTeleportEvent event) {
    	if(event.getCause().equals(TeleportCause.ENDER_PEARL)) {
			Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
			while (mit.hasNext()) {
				Maze maze = mit.next();
	    		if (maze.isInsideMaze(event.getPlayer().getLocation())) {
	    			event.setCancelled(true);
	    			break;
	    		}
			}
    	}
    }
	
	@SuppressWarnings("deprecation")
	@EventHandler
    public void entityDeathListener(EntityDeathEvent event) {
		Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
			if (maze.mazeBoss == event.getEntity()) {
				maze.mazeBoss.getEquipment().setBootsDropChance(0);
				maze.mazeBoss.getEquipment().setLeggingsDropChance(0);
				maze.mazeBoss.getEquipment().setChestplateDropChance(0);
				maze.mazeBoss.getEquipment().setHelmetDropChance(0);
				maze.mazeBoss.getEquipment().setItemInHandDropChance(0);
				event.getDrops().clear();
				double currentWeigh = 0.0;
				double weighSum = 0.0;
				int i;
				for (i = 0; i < maze.mazeBossDropWeighs.length; i++) weighSum += maze.mazeBossDropWeighs[i];
				double randNum = Math.random()*weighSum;
				for (i = 0; i < maze.mazeBossDropWeighs.length; i++) {
					currentWeigh += maze.mazeBossDropWeighs[i];
					if (currentWeigh >= randNum) break;
				}
				event.getDrops().add(maze.mazeBossDropItems[i].clone());
				maze.mazeBoss.setCustomName(null);
				maze.mazeBoss.setCustomNameVisible(false);
				maze.mazeBoss = null;
			}
			if (MazePvP.theMazePvP.showHeads && event.getEntity() instanceof Player || event.getEntity() instanceof Spider || event.getEntity() instanceof Zombie || event.getEntity() instanceof Skeleton || event.getEntity() instanceof Creeper) {
				if (maze.isInsideMaze(event.getEntity().getLocation())) {
					Location loc = event.getEntity().getLocation();
					if (loc.getY() <= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2.5) {
						int x = (int)Math.round(loc.getX()-0.5);
						int z = (int)Math.round(loc.getZ()-0.5);
						if ((x-maze.mazeX)%(Maze.MAZE_PASSAGE_WIDTH+1) == 0) x++;
						if ((z-maze.mazeZ)%(Maze.MAZE_PASSAGE_WIDTH+1) == 0) z++;
						event.getEntity().getWorld().getBlockAt(x, maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2, z).setTypeIdAndData(144, (byte)1, true);
						Skull skull = (Skull)(event.getEntity().getWorld().getBlockAt(x, maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2, z).getState());
						if (event.getEntity() instanceof Skeleton) skull.setSkullType(SkullType.SKELETON);
						else if (event.getEntity() == maze.mazeBoss) skull.setSkullType(SkullType.WITHER);
						else if (event.getEntity() instanceof Zombie) skull.setSkullType(SkullType.ZOMBIE);
						else if (event.getEntity() instanceof Player) {
							skull.setSkullType(SkullType.PLAYER);
							skull.setOwner(((Player)event.getEntity()).getName());
						} else if (event.getEntity() instanceof Creeper) skull.setSkullType(SkullType.CREEPER);
						else if (event.getEntity() instanceof Spider) {
							skull.setSkullType(SkullType.PLAYER);
							skull.setOwner("MHF_Spider");
						}
						skull.setRotation(MazePvP.getRandomRotation());
						skull.update();
					}
				}
			}
		}
    }
	
	@EventHandler
    public void entityDamageListener(EntityDamageByEntityEvent event) {
		Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
			if (maze.mazeBoss != null && event.getEntity() instanceof Player) {
    			maze.mazeBoss.setCustomName(maze.mazeBossName);
    			maze.mazeBoss.setCustomNameVisible(false);
			}
	    	if (maze.mazeBoss == event.getDamager() && event.getEntity() instanceof LivingEntity) {
	    		if (maze.mazeBossTpCooldown > 0) {
	    			event.setCancelled(true);
	    		} else {
	    			event.setDamage(maze.mazeBossStrength == 0 ? ((LivingEntity)event.getEntity()).getHealth()*10 : maze.mazeBossStrength);
	    		}
	    	}
	    	if (maze.mazeBoss == event.getEntity()) {
	    		if (event.getDamager() instanceof Player) {
		    		maze.mazeBossTargetPlayer = ((Player)event.getDamager()).getName();
		    		maze.mazeBossTargetTimer = Math.min(MazePvP.BOSS_TIMER_MAX, maze.mazeBossTargetTimer+20);
	    		}
	    		maze.mazeBossHp = Math.max(0.0,  maze.mazeBossHp-event.getDamage());
	    		maze.updateBossHpStr();
	    		if (maze.mazeBossHp <= 0.0 && maze.mazeBossMaxHp > 0) {
	    			maze.mazeBoss.setHealth(0);
	    		}
	    		if (event.getCause() == DamageCause.SUFFOCATION) {
	    			maze.relocateMazeBoss(false);
	    		}
	    	}
		}
    }
	
	@EventHandler
    public void playerQuitListener(PlayerQuitEvent event) {
		Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
			if (maze.playerInsideMaze.containsKey(event.getPlayer().getName()) && maze.playerInsideMaze.get(event.getPlayer().getName())) {
				if (!maze.canBeEntered) {
					maze.playerQuit(event.getPlayer());
					maze.updateJoinSigns();
				} else {
					maze.playerInsideMaze.remove(event.getPlayer().getName());
				}
			}
		}
    }
	
	@EventHandler
    public void playerJoinListener(PlayerJoinEvent event) {
		Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
			if (maze.canBeEntered && maze.isInsideMaze(event.getPlayer().getLocation())) {
				maze.playerInsideMaze.put(event.getPlayer().getName(), true);
			}
		}
    }
	
	@EventHandler
	public void blockBreakListener(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN) {
			Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
			while (mit.hasNext()) {
				Maze maze = mit.next();
				if (maze.canBeEntered) continue;
				if (maze.mazeWorld != event.getBlock().getWorld()) continue;
				int[] sign = maze.findJoinSign(event.getBlock().getLocation());
				if (sign != null) {
					maze.removeJoinSign(sign);
					break;
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType() == Material.WALL_SIGN || event.getClickedBlock().getType() == Material.SIGN_POST) {
				Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
				while (mit.hasNext()) {
					Maze maze = mit.next();
					if (maze.canBeEntered) continue;
					if (maze.mazeWorld != event.getClickedBlock().getWorld()) continue;
					int[] sign = maze.findJoinSign(event.getClickedBlock().getLocation());
					if (sign != null) {
						String pName = event.getPlayer().getName();
						if (maze.playerInsideMaze.containsKey(pName) && maze.playerInsideMaze.get(pName)) {
							event.getPlayer().sendMessage("You left maze \""+maze.name+"\"");
							maze.playerQuit(event.getPlayer());
							maze.updateJoinSigns();
						} else {
							if (maze.fightStarted) {
								event.getPlayer().sendMessage("The fight has already started");
							} else if (maze.maxPlayers <= maze.playerInsideMaze.size()) {
								event.getPlayer().sendMessage("No more players can join that maze");
							} else if (Maze.playerInsideAMaze.containsKey(pName) && Maze.playerInsideAMaze.get(pName)) {
								event.getPlayer().sendMessage("You already joined a maze.");
								event.getPlayer().sendMessage("Click on its sign again to leave that maze");
							} else {
								event.getPlayer().sendMessage("You joined maze \""+maze.name+"\"");
								maze.playerJoin(event.getPlayer());
								maze.updateJoinSigns();
							}
						}
						break;
					}
				}
			}
		}
	}
	
}
