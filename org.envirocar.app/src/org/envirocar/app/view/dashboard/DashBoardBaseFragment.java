package org.envirocar.app.view.dashboard;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.services.GPSOnlyRecordingService;
import org.envirocar.app.services.OBDConnectionService;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import butterknife.ButterKnife;

/**
 * @author Sai Krishna
 * Base fragment which provides the ability to switch between OBD + GPS Recording fragment
 * and GPS only Recording Fragment
 */
public class DashBoardBaseFragment extends BaseInjectorFragment {

    int previouslySelectedRecordingType;
    MenuItem menu_item_for_icon_changing;

    Fragment dashboardMainFragment = new DashboardMainFragment();
    Fragment dashBoardGPSOnlyMainFragment = new DashBoardGPSOnlyMainFragment();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getting previously selected or default recording type from SharedPreferences
        previouslySelectedRecordingType = PreferencesHandler.getPreviouslySelectedRecordingType(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // First inflate the dashboard base view.
        View contentView =  inflater.inflate(R.layout.fragment_dash_board_base, container, false);

        // Inject all fragment views.
        ButterKnife.inject(this, contentView);

        return contentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //showing previously selected or default recording type fragment on Fragment attached to Activity
        if(previouslySelectedRecordingType == 0){
            showOBDPlusGPSFragment();
        }else if(previouslySelectedRecordingType == 1){
            showGPSOnlyFragment();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
       /* this setting allows the menu
         to display or not display (which allows to switch between the OBD Recording fragment
         and GPS only recording fragment)*/
        updateMenuView(GPSOnlyRecordingService.CURRENT_SERVICE_STATE, OBDConnectionService.CURRENT_SERVICE_STATE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_dashboard_base_fragment, menu);
        menu_item_for_icon_changing = menu.findItem(R.id.menu_select_recording_type);
        if(previouslySelectedRecordingType == 0){
            menu_item_for_icon_changing.setTitle("OBD + GPS");
            menu.findItem(R.id.menu_item_obd_plus_gps).setChecked(true);
        }else if(previouslySelectedRecordingType == 1){
            menu_item_for_icon_changing.setTitle("GPS Only");
            menu.findItem(R.id.menu_item_gps_only).setChecked(true);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.menu_select_recording_type) {
            return true;
        }else if(previouslySelectedRecordingType != 0 && id == R.id.menu_item_obd_plus_gps){
            previouslySelectedRecordingType = 0;
            menu_item_for_icon_changing.setTitle("OBD + GPS");
            showOBDPlusGPSFragment();
            item.setChecked(true);
        }else if(previouslySelectedRecordingType != 1 && id == R.id.menu_item_gps_only){
            previouslySelectedRecordingType = 1;
            menu_item_for_icon_changing.setTitle("GPS Only");
            showGPSOnlyFragment();
            item.setChecked(true);
        }
        PreferencesHandler.setPreviouslySelectedRecordingType(getActivity(),previouslySelectedRecordingType);
        return super.onOptionsItemSelected(item);
    }

    //shows OBD + GPS recording type fragment to the user
    private void showOBDPlusGPSFragment(){
        replaceFragment(new DashboardMainFragment(),
                R.anim.translate_slide_in_left_fragment ,
                R.anim.translate_slide_out_right_fragment);
    }

    //shows GPS only recording type fragment to the user
    private void showGPSOnlyFragment(){
        replaceFragment(new DashBoardGPSOnlyMainFragment(),
                R.anim.translate_slide_in_right_fragment,
                R.anim.translate_slide_out_left_fragment);
    }

    /**
     * @param fragment
     * @param animIn
     * @param animOut
     */
    private void replaceFragment(Fragment fragment, int animIn, int animOut) {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        if (animIn != -1 && animOut != -1) {
            ft.setCustomAnimations(animIn, animOut);
        }
        ft.replace(R.id.fragment_dahsboard_base_container, fragment, fragment.getClass().getSimpleName());
        //        ft.addToBackStack(null);
        ft.commit();
    }


    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
        BluetoothServiceStateChangedEvent event) {
        //disables the menu item which enables user to switch between
        //OBD+GPS recording type and GPS Only recording type fragment
        //if any of the services(OBD+GPS or GPS Only) were started
        updateMenuView(event.mState,event.mState);
    }

    private void updateMenuView(BluetoothServiceState GPSOnlyServiceState,BluetoothServiceState OBDPlusGPSServiceState){
        if(GPSOnlyServiceState == BluetoothServiceState.SERVICE_STARTED
                || OBDPlusGPSServiceState == BluetoothServiceState.SERVICE_STARTED){
            setHasOptionsMenu(false);
        }else if(GPSOnlyServiceState == BluetoothServiceState.SERVICE_STOPPED
                && OBDPlusGPSServiceState == BluetoothServiceState.SERVICE_STOPPED){
            setHasOptionsMenu(true);
        }else if(GPSOnlyServiceState == BluetoothServiceState.SERVICE_STARTING
                || OBDPlusGPSServiceState == BluetoothServiceState.SERVICE_STARTING){
            setHasOptionsMenu(false);
        }else if(GPSOnlyServiceState == BluetoothServiceState.SERVICE_STOPPING
                || OBDPlusGPSServiceState == BluetoothServiceState.SERVICE_STOPPING){
            setHasOptionsMenu(false);
        }
    }

}
