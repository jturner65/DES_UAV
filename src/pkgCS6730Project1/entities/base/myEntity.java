package pkgCS6730Project1.entities.base;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
//
//import base_UI_Objects.my_procApplet;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_UI_Objects.GUI_AppManager;
import pkgCS6730Project1.DESSimWindow;
import pkgCS6730Project1.mySimulator;

//base class for all entities
public abstract class myEntity {	
	public int ID;//provide unique count-based ID to all entities
	public static int IDcount = 0;
	

	//public static UAV_DESSim pa;	
	public mySimulator sim;
	public static GUI_AppManager AppMgr;

	
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
	
	public myEntity(mySimulator _sim, String _name, myPointf _loc, EntityType[] _types) {
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
	
	//display this entity's label information
	protected void dispEntityLabel(IRenderInterface pa, DESSimWindow win) {
		win.unSetCamOrient();
		pa.pushMatState();
			pa.translate(labelVals[0],labelVals[1],labelVals[2]);
			pa.setFill(lblColors,255);
			pa.scale(.5f,.5f,.5f);
			String[] currStatus = this.showStatus();
			for(int i=0;i<currStatus.length;++i){
				pa.showText(currStatus[i], 0,(i+1)*10.0f, 0); 

				//pa.showOffsetText(new float[] {0,(i+1)*10.0f, 0},pa.gui_Black, currStatus[i]);
			}
		pa.popMatState();
		win.setCamOrient();

	}//dispEntityLabel
	
	public long getCurTimeInProc() {return timeVals[curTimeInProc];}
	public long getTTLRunTime() {return timeVals[ttlRunTime];}
	public long getTTLTaskTime() {return timeVals[ttlTaskTime];}
	public long getTTLTravelTime() {return timeVals[ttlTravelTime];}
	public long getTTLQueueTime() {return timeVals[ttlQueueTime];}
	public long getTTLNumTeamsProc() {return timeVals[ttlNumTeamsProc];}
	
	//draw this entity
	public abstract void drawEntity(IRenderInterface pa, DESSimWindow win, float delT, boolean drawMe, boolean drawLbls);
	
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


