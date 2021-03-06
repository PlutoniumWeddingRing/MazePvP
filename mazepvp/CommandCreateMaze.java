package mazepvp;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandCreateMaze implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandCreateMaze(MazePvP main) {
		this.main = main;
	}
 
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	int posX, posY, posZ, height;
    	int entranceNum, mazeSize;
    	boolean hasHeight = (args.length == 4 || args.length == 7);
        if (!((args.length == 3 || args.length == 4) && (sender instanceof Player)) && args.length != 6 && args.length != 7) {
        	return false;
        }
        String mazeName = args[0];
        if (mazeName.contains(".")) {
        	sender.sendMessage("name cannot contain \".\"");
			return true;
        }
    	try {
    		mazeSize = Integer.parseInt(args[1]);
    		if (mazeSize <= 2)  {
    			sender.sendMessage("size is too small");
    			return true;
    		}
    	} catch(NumberFormatException e) {
    		sender.sendMessage("size is not a number");
			return true;
		}
    	if (hasHeight) {
	    	try {
	    		height = Integer.parseInt(args[2]);
	    		if (height <= 1)  {
	    			sender.sendMessage("height is too small");
	    			return true;
	    		}
	    	} catch(NumberFormatException e) {
	    		sender.sendMessage("height is not a number");
				return true;
			}
    	} else height = 1;
    	try {
    		entranceNum = Integer.parseInt(args[hasHeight?3:2]);
    	} catch(NumberFormatException e) {
    		sender.sendMessage("entrance count is not a number");
			return true;
		}
    	if (args.length == 6) {
	    	try {
	    		posX = Integer.parseInt(args[hasHeight?4:3]);
	    		posY = Integer.parseInt(args[hasHeight?5:4]);
	    		posZ = Integer.parseInt(args[hasHeight?6:5]);
			} catch(NumberFormatException e) {
				sender.sendMessage("coordinate is not a number");
				return true;
			}
    	} else {
    		Location playerLoc = ((Player)sender).getLocation();
    		posX = (int)Math.round(playerLoc.getX()-0.5);
    		posY = (int)Math.round(playerLoc.getY());
    		posZ = (int)Math.round(playerLoc.getZ()-0.5);
    	}
    	int i, j, k, xx, yy, zz;
		World world;
		if (sender instanceof Player) world = ((Player)sender).getWorld();
		else world = main.getServer().getWorlds().get(0);
    	Maze maze = main.findMaze(mazeName, world);
		if (maze != null) {
        	sender.sendMessage("A maze with that name already exists");
			return true;
		}
        maze = new Maze();
    	maze.name = mazeName;
		maze.mazeSize = mazeSize;
		maze.height = height;
		maze.maze = new int[maze.mazeSize*2+1][][];
		maze.isBeingChanged = new boolean[maze.mazeSize*2+1][][];
		for (i = 0; i < maze.mazeSize*2+1; i++) {
			maze.maze[i] = new int[maze.mazeSize*2+1][];
			maze.isBeingChanged[i] = new boolean[maze.mazeSize*2+1][];
			for (j = 0; j < maze.mazeSize*2+1; j++) {
				maze.maze[i][j] = new int[maze.height];
				maze.isBeingChanged[i][j] = new boolean[maze.height];
			}
		}
		for (i = 0; i < maze.mazeSize*2+1; i++) {
			for (j = 0; j < maze.mazeSize*2+1; j++) {
				for (k = 0; k < maze.height; k++) {
					if (i == 0 || i == maze.mazeSize*2
						|| j == 0 || j == maze.mazeSize*2) maze.maze[i][j][k] = 1;
					else if (i%2 == 0 && j%2 == 0) maze.maze[i][j][k] = 1;
					else if (i%2 == 0 || j%2 == 0) maze.maze[i][j][k] = 1;
					else maze.maze[i][j][k] = 0;
					maze.isBeingChanged[i][j][k] = false;
				}
			}
		}
		int pathNum = maze.mazeSize*maze.mazeSize;
		pathNum = pathNum-(int)Math.floor(Math.random()*pathNum*0.25);
		for (i = 0; i < entranceNum; i++) {
			int itNum = 0;
			while (true) {
				xx = (int)Math.floor(Math.random()*(maze.mazeSize));
				yy = (int)Math.floor(Math.random()*(maze.height));
				zz = (int)Math.floor(Math.random()*(maze.mazeSize));
				xx *= 2;
				zz *= 2;
				xx++; zz++;
				if (i%4 < 2) {
					if (i%2 == 0) xx = maze.mazeSize*2;
					else xx = 0;
				} else {
					if (i%2 == 0) zz = maze.mazeSize*2;
					else zz = 0;
				}
				if (maze.maze[xx][zz][yy] == 0) {
					itNum++;
					if (itNum >= 50) {
						sender.sendMessage("There are too many entrances");
						return true;
					}
					continue;
				}
				maze.maze[xx][zz][yy] = 0;
				break;
			}
		}
		for (yy = 0; yy < height; yy++) {
			int itNum = pathNum*10;
			for (i = 0; i < pathNum; i++) {
				itNum--;
				if (itNum <= 0) break;
				if (Math.random() < 0.5) {
					xx = 1+(int)Math.floor(Math.random()*(maze.mazeSize-1));
					xx *= 2;
					zz = (int)Math.floor(Math.random()*(maze.mazeSize));
					zz *= 2;
					zz++;
				} else {
					zz = 1+(int)Math.floor(Math.random()*(maze.mazeSize-1));
					zz *= 2;
					xx = (int)Math.floor(Math.random()*(maze.mazeSize));
					xx *= 2;
					xx++;
				}
				boolean skip = false;
				if (maze.maze[xx][zz][yy] == 0) skip = true;
				if (xx-2 >= 0 && zz-1 >= 0 && zz+1 < maze.mazeSize*2+1 && maze.maze[xx-2][zz][yy] == 0 && maze.maze[xx-1][zz-1][yy] == 0 && maze.maze[xx-1][zz+1][yy] == 0)
					skip = true;
				if (xx+2 < maze.mazeSize*2+1 && zz-1 >= 0 && zz+1 < maze.mazeSize*2+1 && maze.maze[xx+2][zz][yy] == 0 && maze.maze[xx+1][zz-1][yy] == 0 && maze.maze[xx+1][zz+1][yy] == 0)
					skip = true;
				if (zz-2 >= 0 && xx-1 >= 0 && xx+1 < maze.mazeSize*2+1 && maze.maze[xx][zz-2][yy] == 0 && maze.maze[xx-1][zz-1][yy] == 0 && maze.maze[xx+1][zz-1][yy] == 0)
					skip = true;
				if (zz+2 < maze.mazeSize*2+1 && xx-1 >= 0 && xx+1 < maze.mazeSize*2+1 && maze.maze[xx][zz+2][yy] == 0 && maze.maze[xx-1][zz+1][yy] == 0 && maze.maze[xx+1][zz+1][yy] == 0)
					skip = true;
				if (skip) {
					i--;
					continue;
				}
				maze.maze[xx][zz][yy] = 0;
			}
		}
		maze.mazeWorld = world;
		sender.sendMessage("Creating maze...");
		for (int yStart = 0; yStart < height; yStart++) {
	    	for (xx = 0; xx <= maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH); xx++) {
	    		for (zz = 0; zz <= maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH); zz++) {
					int xCoord = maze.blockToMazeCoord(xx), zCoord = maze.blockToMazeCoord(zz);
	    			for (yy = (yStart==0)?(-Maze.MAZE_PASSAGE_DEPTH):0; yy <= Maze.MAZE_PASSAGE_HEIGHT+2; yy++) {
	    				int bId = 0;
	    				byte bData = 0;
	    				if (xCoord%2 == 0 || zCoord%2 == 0) {
	    					boolean edges = (xx == 0 || xx == maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH) || zz == 0 || zz == maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH));
	    					int place;
	    					if (xCoord%2 == 0 && zCoord%2 == 0) {
	    						if (edges) place = 3;
	    						else place = 2;
	    						bId = maze.configProps.blockTypes[place][maze.configProps.blockTypes[place].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][0][0];
		    					bData = (byte)maze.configProps.blockTypes[place][maze.configProps.blockTypes[place].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][0][1];
	    					} else {
	    						if (edges) place = 1;
	    						else if (yStart == 0) place = 0;
	    						else place = 9;
	    						if (yy >= 1 && yy <= Maze.MAZE_PASSAGE_HEIGHT+1 && maze.maze[xCoord][zCoord][yStart] != 1) {
	    							bId = 0;
	    							bData = 0;
	    						} else if (xCoord%2 == 0) {
		    						bId = maze.configProps.blockTypes[place][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][zz-maze.mazeToBlockCoord(zCoord)][0];
			    					bData = (byte)maze.configProps.blockTypes[place][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][zz-maze.mazeToBlockCoord(zCoord)][1];
		    					} else {
		    						bId = maze.configProps.blockTypes[place][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][xx-maze.mazeToBlockCoord(xCoord)][0];
			    					bData = (byte)maze.configProps.blockTypes[place][maze.configProps.blockTypes[0].length-1-yy-Maze.MAZE_PASSAGE_DEPTH][xx-maze.mazeToBlockCoord(xCoord)][1];
		    					}
	    					}
	    				} else if (yy == 0) {
	    					int place = (yStart == 0)?4:8;
	    					bId = maze.configProps.blockTypes[place][zz-maze.mazeToBlockCoord(zCoord)][xx-maze.mazeToBlockCoord(xCoord)][0];
	    					bData = (byte)maze.configProps.blockTypes[place][zz-maze.mazeToBlockCoord(zCoord)][xx-maze.mazeToBlockCoord(xCoord)][1];
	    				} else if (yy == -Maze.MAZE_PASSAGE_DEPTH) {
	    					bId = maze.configProps.blockTypes[5][zz-maze.mazeToBlockCoord(zCoord)][xx-maze.mazeToBlockCoord(xCoord)][0];
	    					bData = (byte)maze.configProps.blockTypes[5][zz-maze.mazeToBlockCoord(zCoord)][xx-maze.mazeToBlockCoord(xCoord)][1];
	    				} else if (yy == -Maze.MAZE_PASSAGE_DEPTH+1) {
	    					bId = maze.configProps.blockTypes[6][zz-maze.mazeToBlockCoord(zCoord)][xx-maze.mazeToBlockCoord(xCoord)][0];
		    				bData = (byte)maze.configProps.blockTypes[6][zz-maze.mazeToBlockCoord(zCoord)][xx-maze.mazeToBlockCoord(xCoord)][1];
	    				} else if (yy == Maze.MAZE_PASSAGE_HEIGHT+2) {
							bId = maze.configProps.blockTypes[7][zz-maze.mazeToBlockCoord(zCoord)][xx-maze.mazeToBlockCoord(xCoord)][0];
	    					bData = (byte)maze.configProps.blockTypes[7][zz-maze.mazeToBlockCoord(zCoord)][xx-maze.mazeToBlockCoord(xCoord)][1];
	    				}
	    				world.getBlockAt(posX+xx, posY+Maze.MAZE_PASSAGE_DEPTH+yy+maze.mazeToBlockYCoord(yStart), posZ+zz).setTypeId(bId);
	    				world.getBlockAt(posX+xx, posY+Maze.MAZE_PASSAGE_DEPTH+yy+maze.mazeToBlockYCoord(yStart), posZ+zz).setData(bData);
	    			}
	    		}
	    	}
		}
    	maze.mazeX = posX;
    	maze.mazeY = posY+Maze.MAZE_PASSAGE_DEPTH;
    	maze.mazeZ = posZ;
    	maze.loadBossesFromConfig();
    	main.mazes.add(maze);
		sender.sendMessage("Maze "+mazeName+" created");
    	return true;
	}
}