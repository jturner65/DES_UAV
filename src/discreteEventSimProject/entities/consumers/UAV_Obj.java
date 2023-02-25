package discreteEventSimProject.entities.consumers;

import java.util.concurrent.ThreadLocalRandom;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.GUI_AppManager;

/**
 * class referencing a single uav object TODO : harvest boids?
 * @author John Turner
 *
 */
public class UAV_Obj {
	public UAV_Team f;
	public int ID;
	private static int IDcount = 0;
	//# of animation frames to be used to cycle 1 motion by render objects
	//graphics and animation controlling variables
	public static final int numAnimFrames = 90;
	private myVectorf scaleBt;													//scale of rendered object
	/**
	 * animation controlling variables
	 */	
	private double animCntr;
	/**
	 * Fraction of animation cycle currently at
	 */
	public double animPhase;
	
	public static final float maxAnimCntr = 1000.0f, baseAnimSpd = 1.0f;
	
	//location and orientation variables	
	public float[] O_axisAngle;															//axis angle orientation of this UAV
	public static final int O_FWD = 0, O_RHT = 1,  O_UP = 2;
	public static final float fsqrt2 = (float)(Math.sqrt(2.0));
	private static final float rt2 = .5f * fsqrt2;

	public float oldRotAngle;	
	public final myVectorf scMult = new myVectorf(.5f,.5f,.5f);				//multiplier for scale based on mass
	private myVectorf rotVec;													//rotational vector, 
	
	public myPointf coords;														//com coords
	public myVectorf velocity;
	public myVectorf[] orientation;												//Rot matrix - 3x3 orthonormal basis matrix - cols are bases for body frame orientation in world frame
					
	public UAV_Obj(UAV_Team _f, myPointf _coords){
		ID = IDcount++;		
		//p = _p;		
		f = _f; 		
		//preCalcAnimSpd = (float) ThreadLocalRandom.current().nextDouble(.5f,2.0);		
		animPhase = ThreadLocalRandom.current().nextDouble(.25f, .75f ) ;//keep initial phase between .25 and .75 so that cyclic-force UAVs start moving right away
		animCntr = animPhase * f.getCurrTemplate().getMaxAnimCounter();

		rotVec = myVectorf.RIGHT.cloneMe(); 			//initial setup
		orientation = new myVectorf[3];
		orientation[O_FWD] = myVectorf.FORWARD.cloneMe();
		orientation[O_RHT] = myVectorf.RIGHT.cloneMe();
		orientation[O_UP] = myVectorf.UP.cloneMe();
		
		coords = new myPointf(_coords);	//new myPointf[2]; 
		velocity = new myVectorf();
		O_axisAngle=new float[]{0,1,0,0};
		oldRotAngle = 0;
		scaleBt = new myVectorf(scMult);					//for rendering different sized UAVs
		
	}//constructor
	
	//align the UAV along the current orientation matrix
	private void alignUAV(IRenderInterface pa, double delT){
		rotVec.set(O_axisAngle[1],O_axisAngle[2],O_axisAngle[3]);
		float rotAngle = (float) (oldRotAngle + ((O_axisAngle[0]-oldRotAngle) * delT));
		pa.rotate(rotAngle,rotVec.x, rotVec.y, rotVec.z);
		oldRotAngle = rotAngle;
	}//alignUAV	
	
	private static double epsValCalcSq = MyMathUtils.EPS * MyMathUtils.EPS;
	private float[] toAxisAngle() {
		float angle,x=rt2,y=rt2,z=rt2,s;
		float fyrx = -orientation[O_FWD].y+orientation[O_RHT].x,
			uxfz = -orientation[O_UP].x+orientation[O_FWD].z,
			rzuy = -orientation[O_RHT].z+orientation[O_UP].y;
			
		if (((fyrx*fyrx) < epsValCalcSq) && ((uxfz*uxfz) < epsValCalcSq) && ((rzuy*rzuy) < epsValCalcSq)) {			//checking for rotational singularity
			// angle == 0
			float fyrx2 = orientation[O_FWD].y+orientation[O_RHT].x,
				fzux2 = orientation[O_FWD].z+orientation[O_UP].x,
				rzuy2 = orientation[O_RHT].z+orientation[O_UP].y,
				fxryuz3 = orientation[O_FWD].x+orientation[O_RHT].y+orientation[O_UP].z-3;
			if (((fyrx2*fyrx2) < 1)	&& (fzux2*fzux2 < 1) && ((rzuy2*rzuy2) < 1) && ((fxryuz3*fxryuz3) < 1)) {	return new float[]{0,1,0,0}; }
			// angle == pi
			angle = (float) Math.PI;
			float fwd2x = (orientation[O_FWD].x+1)/2.0f,rht2y = (orientation[O_RHT].y+1)/2.0f,up2z = (orientation[O_UP].z+1)/2.0f,
				fwd2y = fyrx2/4.0f, fwd2z = fzux2/4.0f, rht2z = rzuy2/4.0f;
			if ((fwd2x > rht2y) && (fwd2x > up2z)) { // orientation[O_FWD].x is the largest diagonal term
				if (fwd2x< MyMathUtils.EPS) {	x = 0;} else {			x = (float) Math.sqrt(fwd2x);y = fwd2y/x;z = fwd2z/x;} 
			} else if (rht2y > up2z) { 		// orientation[O_RHT].y is the largest diagonal term
				if (rht2y< MyMathUtils.EPS) {	y = 0;} else {			y = (float) Math.sqrt(rht2y);x = fwd2y/y;z = rht2z/y;}
			} else { // orientation[O_UP].z is the largest diagonal term so base result on this
				if (up2z< MyMathUtils.EPS) {	z = 0;} else {			z = (float) Math.sqrt(up2z);	x = fwd2z/z;y = rht2z/z;}
			}
			return new float[]{angle,x,y,z}; // return 180 deg rotation
		}
		//no singularities - handle normally
		myVectorf tmp = new myVectorf(rzuy, uxfz, fyrx);
		s = tmp.magn;
		if (s < MyMathUtils.EPS){ s=1; }
		tmp._scale(s);//changes mag to s
			// prevent divide by zero, should not happen if matrix is orthogonal -- should be caught by singularity test above
		angle = (float) -Math.acos(( orientation[O_FWD].x + orientation[O_RHT].y + orientation[O_UP].z - 1)/2.0);
	   return new float[]{angle,tmp.x,tmp.y,tmp.z};
	}//toAxisAngle
	
	private myVectorf getFwdVec( float delT){
		if(velocity.magn < MyMathUtils.EPS){			return orientation[O_FWD]._normalize();		}
		else {		
			myVectorf tmp = velocity.cloneMe()._normalize();			
			return new myVectorf(orientation[O_FWD], delT, tmp);		
		}
	}//getFwdVec
	
	private myVectorf getUpVec(){	
		float fwdUpDotm1 = orientation[O_FWD]._dot(myVectorf.UP);
		if (1.0 - (fwdUpDotm1 * fwdUpDotm1) < epsValCalcSq){
			return myVectorf._cross(orientation[O_RHT], orientation[O_FWD]);
		}
		return myVectorf.UP.cloneMe();
	}	
	
	private void setOrientation(float delT){
		//find new orientation at new coords - 
		orientation[O_FWD].set(getFwdVec(delT));
		orientation[O_UP].set(getUpVec());	
		orientation[O_RHT] = orientation[O_UP]._cross(orientation[O_FWD]); //sideways is cross of up and forward - backwards(righthanded)
		orientation[O_RHT]._normalize();
		if(Math.abs(orientation[O_FWD]._dot(orientation[O_UP])) > MyMathUtils.EPS){
			orientation[O_UP] = orientation[O_FWD]._cross(orientation[O_RHT]); //sideways is cross of up and forward
			//orientation[O_UP].set(orientation[O_UP]._normalize());
			orientation[O_RHT]._normalize();
		}
		O_axisAngle = toAxisAngle();
	}
	
	//move uav in direction and magnitude of vector _vel, using delT, and aligning along vector _vel
	public void moveUAV(myVectorf _vel, float delT) {
		velocity.set(_vel);			//velocity vector only used to determine orientation
		setOrientation(delT);
	}//moveUAV
	
	private void drawTmpl(IRenderInterface ri) {
		ri.pushMatState();
		f.getCurrTemplate().drawMe(animPhase, ID);
		ri.popMatState();
	}

	
	//draw this body on mesh
	public void drawMe(IRenderInterface ri, float delT){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			alignUAV(ri, delT);
			ri.scale(scaleBt.x,scaleBt.y,scaleBt.z);																	//make appropriate size
			drawTmpl(ri);
		ri.popMatState();
		animIncr(velocity.magn*.1f);
	}//drawme	
	
	public void drawMeDbgFrame(GUI_AppManager AppMgr, IRenderInterface ri, float delT){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			drawMyVec(ri, rotVec, IRenderInterface.gui_Black,4.0f);AppMgr.drawAxes(100, 2.0f, new myPoint(0,0,0), orientation, 255);
			alignUAV(ri, delT);
			ri.scale(scaleBt.x,scaleBt.y,scaleBt.z);																	//make appropriate size				
			drawTmpl(ri);
		ri.popMatState();
		animIncr(velocity.magn*.1f);		
	}
	
	//draw this UAV as a ball - replace with sphere render obj 
	public void drawMeBall(GUI_AppManager AppMgr, IRenderInterface ri, boolean debugAnim){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			if(debugAnim){drawMyVec(ri,rotVec, IRenderInterface.gui_Black,4.0f);AppMgr.drawAxes(100, 2.0f, new myPoint(0,0,0), orientation, 255);}
			ri.scale(scaleBt.x,scaleBt.y,scaleBt.z);																	//make appropriate size				
			f.sphTmpl.drawMe(animPhase, ID);
		ri.popMatState();
		//animIncr();
	}//drawme 
	
	public void drawMyVec(IRenderInterface p, myVectorf v, int clr, float sw){
		p.pushMatState();
			p.setColorValStroke(clr, 255);
			p.setStrokeWt(sw);
			p.drawLine(0,0,0,v.x, v.y, v.z);
		p.popMatState();
	}
	
	private void animIncr(float vel){
		animCntr += (baseAnimSpd + vel);//*preCalcAnimSpd;						//set animMod based on velocity -> 1 + mag of velocity	
		double maxAnimCntr = f.getCurrTemplate().getMaxAnimCounter();
		animCntr %= maxAnimCntr;
		animPhase = (animCntr/maxAnimCntr);									//phase of animation cycle
	}//animIncr		
	
	public String toString(){
		String result = "ID : " + ID;
		return result;
	}	
}//myUAVObj class
