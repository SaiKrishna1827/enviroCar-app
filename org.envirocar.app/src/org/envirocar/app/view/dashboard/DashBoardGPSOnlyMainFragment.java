package org.envirocar.app.view.dashboard;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.services.GPSOnlyRecordingService;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author Sai Krishna
 * Parent fragment which contains all the fragments required to record GPS Only track
 */
public class DashBoardGPSOnlyMainFragment extends BaseInjectorFragment {

    private static final Logger LOG = Logger.getLogger(DashBoardGPSOnlyMainFragment.class);


    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected TrackRecordingHandler mTrackRecordingHandler;
    @Inject
    protected LocationHandler mLocationHandler;


    @InjectView(R.id.fragment_startup_info_field)
    protected View mInfoField;
    @InjectView(R.id.fragment_startup_info_text)
    protected TextView mInfoText;
    @InjectView(R.id.fragment_startup_start_button)
    protected View mStartStopButton;
    @InjectView(R.id.fragment_startup_start_button_inner)
    protected TextView mStartStopButtonInner;

    private Scheduler.Worker mMainThreadScheduler = AndroidSchedulers.mainThread().createWorker();

    private BluetoothServiceState mServiceState = BluetoothServiceState.SERVICE_STOPPED;

    // All the sub-fragments to show in this fragment.
    private Fragment mCurrentlyVisible;
    private Fragment mDashboardHeaderFragment = new DashboardGPSOnlyTrackDetailsFragment();
    private Fragment mDashboardSettingsFragment = new DashboardGPSOnlyTrackSettingsFragment();
    private Fragment mDashboardMapFragment = new DashboardMapFragment();
    private Fragment mDashboardTempomatFragment = new DashboardTempomatFragment();
    private Fragment mDashboardTrackMapFragment = new DashboardTrackMapFragment();




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // This setting is an essential requirement to catch the events of a sub-fragment's
        // options shown in the toolbar.
        setHasOptionsMenu(true);

        // Inflate the layout for this fragment
        View contentView =  inflater.inflate(R.layout.fragment_dash_board_gpsonly_main, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_startup_settings_layout,mDashboardSettingsFragment);
        transaction.add(R.id.fragment_dashboard_header_fragment,mDashboardHeaderFragment);
        transaction.commit();

        onShowServiceStateUI(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);

        return contentView;
    }

    @Override
    public void onResume() {
        updateStartStopButton(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
        updateInfoField();
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        if (!getActivity().isFinishing() && mDashboardSettingsFragment != null) {
            try {
                getFragmentManager().beginTransaction()
                        .remove(mDashboardSettingsFragment)
                        .remove(mDashboardHeaderFragment)
                        .commit();
            } catch (IllegalStateException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        LOG.info("onDestroy()");

        if(!getActivity().isFinishing() && mDashboardSettingsFragment != null){
            try{
                getFragmentManager().beginTransaction()
                        .remove(mDashboardMapFragment)
                        .commit();
            } catch (IllegalStateException e){
                LOG.warn(e.getMessage(), e);
            }
        }

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_dashboard_tempomat_map:
                if (mDashboardTrackMapFragment.isVisible())
                    return false;

                replaceFragment(mDashboardTrackMapFragment,
                        R.id.fragment_startup_container,
                        R.anim.translate_slide_in_left_fragment,
                        R.anim.translate_slide_out_right_fragment);
                return true;
            case R.id.menu_dashboard_tempomat_show_cruise:
                if (mDashboardTempomatFragment.isVisible())
                    return false;

                replaceFragment(mDashboardTempomatFragment,
                        R.id.fragment_startup_container,
                        R.anim.translate_slide_in_right_fragment,
                        R.anim.translate_slide_out_left_fragment);
                return true;
        }
        return false;
    }


    @OnClick(R.id.fragment_startup_start_button)
    public void onStartStopButtonClicked() {
        switch (GPSOnlyRecordingService.CURRENT_SERVICE_STATE) {
            case SERVICE_STOPPED:
                onButtonStartClicked();
                break;
            case SERVICE_STARTED:
                onButtonStopClicked();
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOG.info(String.format("onReceiveBluetoothServiceStateChangedEvent(): %s",
                event.toString()));
        mServiceState = GPSOnlyRecordingService.CURRENT_SERVICE_STATE;
        mMainThreadScheduler.schedule(() -> {
            onShowServiceStateUI(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
            // Update the start stop button.
            updateStartStopButton(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
            // Update the info field.
            updateInfoField();
        });
    }

    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(NewCarTypeSelectedEvent event) {
        mMainThreadScheduler.schedule(() -> {
            updateStartStopButton(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
            // Update the info field.
            updateInfoField();
        });
    }

    @Subscribe
    public void onReceiveGpsStatusChangedEvent(GpsStateChangedEvent event) {
        mMainThreadScheduler.schedule(() -> {
            updateStartStopButton(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
            // Update the info field.
            updateInfoField();
        });
    }

    private void onButtonStartClicked() {
        if (!mLocationHandler.isGPSEnabled()) {
            Snackbar.make(getView(), R.string.dashboard_gps_disabled_snackbar,
                    Snackbar.LENGTH_LONG);
            return;
        }

        // Start the background remoteService.
        getActivity().startService(
                new Intent(getActivity(), GPSOnlyRecordingService.class));

    }

    private void onButtonStopClicked() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.dashboard_dialog_stop_track)
                .content(R.string.dashboard_dialog_stop_track_content)
                .negativeText(R.string.cancel)
                .positiveText(R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        mTrackRecordingHandler.finishCurrentTrack();
                    }
                })
                .show();
    }

    /**
     * @param fragment
     * @param enterAnimation
     * @param exitAnimation
     */
    private void showFragment(Fragment fragment, int enterAnimation, int exitAnimation) {
        if (fragment == null || getFragmentManager() == null)
            return;

        FragmentTransaction transaction = getActivity().getSupportFragmentManager()
                .beginTransaction();
        if (enterAnimation != -1) {
            transaction.setCustomAnimations(enterAnimation, exitAnimation);
        }
        transaction.show(fragment);
        transaction.commitAllowingStateLoss();
    }

    /**
     * @param fragment
     * @param enterAnimation
     * @param exitAnimation
     */
    private void hideFragment(Fragment fragment, int enterAnimation, int exitAnimation) {
        if (fragment == null || getFragmentManager() == null)
            return;

        FragmentTransaction transaction = getActivity().getSupportFragmentManager()
                .beginTransaction();
        if (exitAnimation != -1) {
            transaction.setCustomAnimations(enterAnimation, exitAnimation);
        }
        transaction.hide(fragment);
        transaction.commitAllowingStateLoss();
    }

    /**
     * @param fragment
     * @param container
     * @param enterAnimation
     * @param exitAnimation
     */
    private void replaceFragment(Fragment fragment, int container, int enterAnimation, int
            exitAnimation) {
        if (fragment == null || getFragmentManager() == null)
            return;

        FragmentTransaction transaction = getActivity()
                .getSupportFragmentManager()
                .beginTransaction();
        if (enterAnimation != -1 && exitAnimation != -1) {
            transaction.setCustomAnimations(enterAnimation, exitAnimation);
        }

        transaction.replace(container, fragment);
        transaction.commitAllowingStateLoss();

        mCurrentlyVisible = fragment;
    }

    private void onShowServiceStateUI(BluetoothServiceState state) {
        switch (state) {
            case SERVICE_STOPPED:
                if (getFragmentManager() == null)
                    return;

                if (!mDashboardSettingsFragment.isVisible())
                    showFragment(mDashboardSettingsFragment,
                            R.anim.translate_slide_in_left_fragment,
                            R.anim.translate_slide_out_left_fragment);

                // Hide the header fragment
                hideFragment(mDashboardHeaderFragment,
                        mCurrentlyVisible != null ? R.anim.translate_slide_in_top_fragment : -1,
                        mCurrentlyVisible != null ? R.anim.translate_slide_out_top_fragment : -1);

                // Replace the container with the mapview.
                if (mCurrentlyVisible != mDashboardMapFragment) {
                    // TODO HERE CHANGE TO TRACK MAP FRAGMENT
                    replaceFragment(mDashboardMapFragment, R.id.fragment_startup_container,
                            mCurrentlyVisible != null ?
                                    R.anim.translate_slide_in_left_fragment : -1,
                            mCurrentlyVisible != null ?
                                    R.anim.translate_slide_out_right_fragment : -1);
                }

                mCurrentlyVisible = mDashboardMapFragment;


                break;
            case SERVICE_STARTED:
                // Hide the settings if visible
                //                if (mDashboardSettingsFragment.isVisible()) {
                hideFragment(mDashboardSettingsFragment,
                        R.anim.translate_slide_in_left_fragment,
                        R.anim.translate_slide_out_left_fragment);
                //                }

                //                if (!mDashboardHeaderFragment.isVisible())
                showFragment(mDashboardHeaderFragment,
                        R.anim.translate_slide_in_top_fragment,
                        R.anim.translate_slide_out_top_fragment);

                // Show the tempomat fragment
                if (!mDashboardTempomatFragment.isVisible())
                    replaceFragment(mDashboardTempomatFragment,
                            R.id.fragment_startup_container,
                            R.anim.translate_slide_in_right_fragment,
                            R.anim.translate_slide_out_left_fragment);

                mCurrentlyVisible = mDashboardTempomatFragment;

                break;
            case SERVICE_STOPPING:
                break;
        }
    }

    private void updateStartStopButton(BluetoothServiceState state) {
        switch (state) {
            case SERVICE_STOPPED:
                if (hasSettingsSelected()) {
                    updateStartStopButton(getResources().getColor(R.color.green_dark_cario),
                            getString(R.string.dashboard_start_track), true);
                } else {
                    updateStartStopButton(Color.GRAY,
                            getString(R.string.dashboard_start_track), false);
                }
                break;
            case SERVICE_STARTED:
                // Update the StartStopButton
                updateStartStopButton(Color.RED, getString(R.string.dashboard_stop_track), true);
                // hide the info field when the track is started.
                mInfoField.setVisibility(View.INVISIBLE);
                break;
            case SERVICE_STARTING:
                updateStartStopButton(Color.GRAY,
                        getString(R.string.dashboard_track_is_starting), false);
                break;
            case SERVICE_STOPPING:
                updateStartStopButton(Color.GRAY,
                        getString(R.string.dashboard_track_is_stopping), false);
                break;
            default:
                break;
        }
    }

    private boolean hasSettingsSelected() {
        return  mLocationHandler.isGPSEnabled() &&
                mCarManager.getCar() != null;
    }

    private void updateStartStopButton(int color, String text, boolean enabled) {
        mMainThreadScheduler.schedule(() -> {
            mStartStopButtonInner.setBackgroundColor(color);
            mStartStopButtonInner.setText(text);
            mStartStopButton.setEnabled(enabled);
        });
    }

    /**
     * Updates the info field of the default startup fragment.
     */
    @UiThread
    private void updateInfoField() {
        boolean showInfo = false;
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.dashboard_info_base));
        if (!mLocationHandler.isGPSEnabled()) {
            sb.append(getString(R.string.dashboard_info_activate_gps));
            showInfo = true;
        }
        if (mCarManager.getCar() == null) {
            sb.append(getString(R.string.dashboard_info_select_car_type));
            showInfo = true;
        }
        if (showInfo) {
            mInfoText.setText(sb.toString());
            mInfoField.setVisibility(View.VISIBLE);
        } else {
            mInfoField.setVisibility(View.INVISIBLE);
        }
    }

    @UiThread
    private void onServiceStarted() {
        // Hide the dashboard settings fragment if visible.
        if (mDashboardSettingsFragment.isVisible()) {
            getFragmentManager()
                    .beginTransaction()
                    .hide(mDashboardSettingsFragment)
                    .commitAllowingStateLoss();
        }

        getFragmentManager()
                .beginTransaction()
                .show(mDashboardHeaderFragment)
                .commitAllowingStateLoss();

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_startup_container, new DashboardTempomatFragment())
                .commitAllowingStateLoss();

        updateStartToStopButton();
    }

    @UiThread
    private void onServiceStopped() {
        getFragmentManager().beginTransaction()
                .hide(mDashboardTempomatFragment)
                .hide(mDashboardHeaderFragment)
                .replace(R.id.fragment_startup_container, mDashboardMapFragment)
                .commitAllowingStateLoss();

        if (mDashboardSettingsFragment != null) {
            getFragmentManager()
                    .beginTransaction()
                    .show(mDashboardSettingsFragment)
                    .commitAllowingStateLoss();
        }

        updateStartToStopButton();
    }

    @UiThread
    private void updateStartToStopButton() {
        if (mServiceState == BluetoothServiceState.SERVICE_STARTED) {
            mStartStopButtonInner.setBackgroundColor(Color.RED);
            mStartStopButtonInner.setText("STOP TRACK");
        } else if (mServiceState == BluetoothServiceState.SERVICE_STOPPED) {
            mStartStopButtonInner.setBackgroundColor(
                    getResources().getColor(R.color.green_dark_cario));
            mStartStopButtonInner.setText("START TRACK");
        }
    }



}
