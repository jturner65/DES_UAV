package pkgCS6730Project1;

import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.BaseBarMenu;

public class DES_SimSideBarMenu extends BaseBarMenu {

	public DES_SimSideBarMenu(my_procApplet _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd,float[] rdClosed, String _winTxt) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt);
	}

	/**
	 * initialize application-specific windows and titles in structs :
	 *  guiBtnRowNames, guiBtnNames, defaultUIBtnNames, guiBtnInst, guiBtnWaitForProc;
	 */
	@Override
	protected void initSideBarMenuBtns_Priv() {
		/**
		 * set row names for each row of ui action buttons getMouseOverSelBtnNames()
		 * @param _funcRowNames array of names for each row of functional buttons 
		 * @param _numBtnsPerFuncRow array of # of buttons per row of functional buttons - size must match # of entries in _funcRowNames array
		 * @param _numDbgBtns # of debug buttons
		 * @param _inclWinNames include the names of all the instanced windows
		 * @param _inclMseOvValues include a row for possible mouse over values
		 */
		//protected void setBtnData(String[] _funcRowNames, int[] _numBtnsPerFuncRow, int _numDbgBtns, boolean _inclWinNames, boolean _inclMseOvValues) {

		setBtnData(new String[]{"Functions 1","Functions 2","Functions 3","Functions 4"}, new int[] {3,4,4,4}, 5, false, false);

	}
}//class COTS_MorphSideBarMenu
