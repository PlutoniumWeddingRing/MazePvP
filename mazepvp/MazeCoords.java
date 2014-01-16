package mazepvp;

public class MazeCoords
{
	public final int x, z;
	public int type; //1: wall, 10: falltrap, 20: chest
	
	public MazeCoords(int x, int z, int type) {
		this.x = x;
		this.z = z;
		this.type = type;
	}
	
	public MazeCoords(int x, int z) {
		this.x = x;
		this.z = z;
		type = 0;
	}
}
