package mazepvp;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.ItemStack;

public class MazeConfig
{
	public final static String[] blockTypeNames = new String[] {
		"wall", "outerWall", "pillar", "outerPillar", "floor",
		"lowerFloor", "spikes", "ceiling", "higherFloor"
	};
	public final static int[][] blockTypeDimensions= new int[][] {
		{4, 13}, {4, 13}, {1, 13}, {1, 13}, {4, 4}, {4, 4}, {4, 4}, {4, 4}, {4, 4}
	};
	
	public int playerMaxDeaths = 3;
	public int minPlayers = 5;
	public int maxPlayers = 20;
	public ArrayList<BossConfig> bosses = null;
	public double spawnMobProb = 1.0/3.0;
	public double chestAppearProb = 0.1;
	public double enderChestAppearProb = 0.2;
	public double groundReappearProb = 0.1;
	public ItemStack[] startItems;
	public double[] chestWeighs;
	public ItemStack[] chestItems;
	public int[][][][] blockTypes;
	public  List<String> fightStartedCommand;
	public  List<String> fightRespawnCommand;
	public  List<String> fightPlayerOutCommand;
	public  List<String> fightWinCommand;
	
	public MazeConfig() {
		this(true);
	}
	
	public MazeConfig(boolean loadDefaults) {
		if (loadDefaults) {
			MazePvP.copyConfigValues(MazePvP.theMazePvP.rootConfig, this);
		}
	}

	public static int getInt(Configuration conf, Configuration rootConf, boolean rootProps, String prop) {
		if (rootProps || conf.isInt(prop)) return conf.getInt(prop);
		return rootConf.getInt(prop);
	}

	public static double getDouble(Configuration conf, Configuration rootConf, boolean rootProps, String prop) {
		if (rootProps || conf.isDouble(prop)) return conf.getDouble(prop);
		return rootConf.getDouble(prop);
	}

	public static String getString(Configuration conf, Configuration rootConf, boolean rootProps, String prop) {
		if (rootProps || conf.isString(prop)) return conf.getString(prop);
		return rootConf.getString(prop);
	}

	public static List<String> getStringList(Configuration conf, Configuration rootConf, boolean rootProps, String prop) {
		if (rootProps || conf.isList(prop)) return conf.getStringList(prop);
		return rootConf.getStringList(prop);
	}

	public static int getBlockTypeIndex(String prop) {
		for (int i = 0; i < blockTypeNames.length; i++) {
			if (blockTypeNames[i].equals(prop)) return i;
		}
		return -1;
	}

	public static int[][][][] cloneBlockTypes(int[][][][] blockTypes) {
		int[][][][] clone = new int[blockTypes.length][][][];
		for (int i = 0; i < clone.length; i++) {
			clone[i] = new int[blockTypes[i].length][blockTypes[i][0].length][blockTypes[i][0][0].length];
			for (int j = 0; j < clone[i].length; j++) {
				for (int k = 0; k < clone[i][j].length; k++) {
					for (int l = 0; l < clone[i][j][k].length; l++) {
						clone[i][j][k][l] = blockTypes[i][j][k][l];
					}
				}
			}
		}
		return clone;
	}

	public List<String> getCommandProp(String propName) {
		if (propName.equals("commands.fightStarted")) return fightStartedCommand;
		else if (propName.equals("commands.fightRespawn")) return fightRespawnCommand;
		else if (propName.equals("commands.fightPlayerOut")) return fightPlayerOutCommand;
		else if (propName.equals("commands.fightWin")) return fightWinCommand;
		return null;
	}
}
