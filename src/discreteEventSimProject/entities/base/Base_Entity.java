package discreteEventSimProject.entities.base;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.floats.myPointf;
import discreteEventSimProject.sim.base.DES_Simulator;
import discreteEventSimProject.ui.base.Base_DESWindow;

/**
 * base class for all entities
 * @author John Turner
 *
 */
public abstract class Base_Entity {	
	public int ID;//provide unique count-based ID to all entities
	public static int IDcount = 0;
	
	public DES_Simulator sim;
	
	public String name;
	public myPointf loc;			//initial location of this entity (may change if it moves)
	
	//flags relevant to entity processing - specific flags defined/processed in entity
	protected int[] entityFlags;	
	////////////////////
	//reporting metrics
	//array to hold various time metrix to be used for accounting
	protected long[] timeVals;				//array holding time values
	protected static final int
		curTimeInProc = 0,				//time since entering the current process (consumer only)
		ttlRunTime = 1,					//total time running processes - for resources this is total time the resource was actually in use
		ttlTaskTime = 2,				//total task time == total work done -> divided by total # of millis per process will give # of processes complete (consumer only)
		ttlTravelTime = 3,				//total travel time : consumer and transit lane
		ttlQueueTime = 4,				//total queue time : consumer and transit lane
		timeEnterQueue = 5,				//simulation time when queue will be entered - used to calculate total time in queue (consumer only)
		ttlNumTeamsProc	= 6;			//total # of teams processed (resource only) or total # of completed processes (consumer only)
		
	protected static final int numTimeVals = 7;
	//location to render labels on object
	protected float[] labelVals;
	//label colors
	protected int[] lblColors;

	
	//types of entity as per Birta ABCMod - is ara because may be compound type
	public EntityType[] types;		
	
	public Base_Entity(DES_Simulator _sim, String _name, myPointf _loc, EntityType[] _types) {
		sim=_sim;	name = _name; loc = new myPointf(_loc);types=_types;
		ID = IDcount++;	
		timeVals = new long[numTimeVals];
		initEntity();
	}
	
	protected abstract void initEntity();
	protected void initEntityFlags(int numFlags){entityFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setEntityFlags(i,false);}}
	public boolean getEntityFlags(int idx){int bitLoc = 1<<(idx%32);return (entityFlags[idx/32] & bitLoc) == bitLoc;}	
	public abstract void setEntityFlags(int idx, boolean val);
	
	//return an array holding all current time values
	public long[] getTimeRes() {return timeVals;}
	//return array of strings for status info for display
	protected abstract String[] showStatus();
	
	/**
	 * display this entity's label information
	 * @param ri
	 * @param win
	 */
	public final void dispEntityLabel(IRenderInterface ri, Base_DESWindow win) {
		ri.pushMatState();
			ri.translate(loc);
			win.unSetCamOrient();
			ri.translate(labelVals[0],labelVals[1],labelVals[2]);
			ri.setFill(lblColors,255);
			ri.scale(.5f,.5f,.5f);
			String[] currStatus = this.showStatus();
			for(int i=0;i<currStatus.length;++i){
				ri.showText(currStatus[i], 0,(i+1)*10.0f, 0); 
			}
			win.setCamOrient();
		ri.popMatState();

	}//dispEntityLabel
	
	public long getCurTimeInProc() {return timeVals[curTimeInProc];}
	public long getTTLRunTime() {return timeVals[ttlRunTime];}
	public long getTTLTaskTime() {return timeVals[ttlTaskTime];}
	public long getTTLTravelTime() {return timeVals[ttlTravelTime];}
	public long getTTLQueueTime() {return timeVals[ttlQueueTime];}
	public long getTTLNumTeamsProc() {return timeVals[ttlNumTeamsProc];}
	
	/**
	 * draw this entity
	 * @param pa
	 * @param win
	 * @param delT
	 * @param drawMe
	 */
	public abstract void drawEntity(IRenderInterface pa, Base_DESWindow win, float delT, boolean drawMe);
	
	
	
	
	public String toString() {
		String res = "Entity ID : " + ID + " | Name : " + name + "\n";
		if(types.length == 1) { res += "Entity Type : " + types[0];}
		else {
			res += "Compound Entity Type :";
			for (int i=0;i<types.length;++i) {res += " " + types[i];}
		}
		res += "\nEntity Location : " + loc.toStrBrf();
		return res;
	}
}//class myEntity 


