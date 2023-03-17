package discreteEventSimProject.ui;

import java.util.Map;

import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
/**
 * Discrete event sim data updater/adapter for window data
 * @author John Turner
 *
 */
public class DES_UIDataUpdater extends UIDataUpdater {

	public DES_UIDataUpdater(Base_DispWindow _win) {super(_win);}

	public DES_UIDataUpdater(UIDataUpdater _otr) {super(_otr);}

	public DES_UIDataUpdater(Base_DispWindow _win, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
			Map<Integer, Boolean> _bVals) {
		super(_win, _iVals, _fVals, _bVals);
	}

}//class DES_UIDataUpdater
