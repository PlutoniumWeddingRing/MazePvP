package mazepvp;

import org.bukkit.Location;

public class PlayerProps
{
	public Location prevLocation;
	public int deathCount = 0;
	
	public PlayerProps(Location prevLocation) {
		this.prevLocation = prevLocation;
	}
}
