package mazepvp;

import java.util.UUID;

import org.bukkit.entity.Zombie;

public class Boss
{
	public Zombie entity = null;
	public String hpStr = "";
	public UUID id = null;
	public String targetPlayer = "";
	public int targetTimer = 0;
	public int tpCooldown = 0;
	public double hp = 0.0;
	
	public Boss() {
		
	}
}
