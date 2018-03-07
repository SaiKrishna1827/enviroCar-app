package org.envirocar.app.view.dashboard;


import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.events.AvrgSpeedUpdateEvent;
import org.envirocar.app.events.DistanceValueUpdateEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.text.DecimalFormat;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author Sai Krishna
 * Track Details fragment which shows details of recording Track when recording type is GPS Only
 */
public class DashboardGPSOnlyTrackDetailsFragment extends BaseInjectorFragment {

    private static final Logger LOGGER = Logger.getLogger(DashboardGPSOnlyTrackDetailsFragment.class);

    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("###.#");

    @InjectView(R.id.fragment_dashboard_header_gps_image)
    protected ImageView mGpsImage;
    @InjectView(R.id.fragment_dashboard_header_gps_text)
    protected TextView mGpsText;

    @InjectView(R.id.fragment_dashboard_header_time_timer)
    protected Chronometer mTimerText;
    @InjectView(R.id.fragment_dashboard_header_speed_text)
    protected TextView mSpeedText;
    @InjectView(R.id.fragment_dashboard_header_distance_text)
    protected TextView mDistanceText;


    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOGGER.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard_gpsonly_track_details, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // Update the image and text of the bluetooth related views.
        updateLocationViews(true);

        // return the inflated content view.
        return contentView;
    }


    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        mMainThreadWorker.schedule(() -> {
            if (event.mState == BluetoothServiceState.SERVICE_STOPPED) {
                mTimerText.setBase(SystemClock.elapsedRealtime());
                mTimerText.stop();
                mDistanceText.setText("0.0 km");
                mSpeedText.setText("0 km/h");
            }
        });
    }

    @Subscribe
    public void onReceiveAvrgSpeedUpdateEvent(AvrgSpeedUpdateEvent event) {
        mMainThreadWorker.schedule(() -> mSpeedText.setText(String.format("%s km/h",
                Integer.toString(event.mAvrgSpeed))));
    }

    @Subscribe
    public void onReceiveDistanceUpdateEvent(DistanceValueUpdateEvent event) {
        mMainThreadWorker.schedule(() ->
                mDistanceText.setText(String.format("%s km",
                        DECIMAL_FORMATTER.format(event.mDistanceValue))));
    }

    @Subscribe
    public void onReceiveGpsStatusChangedEvent(GpsStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        updateLocationViews(event.mIsGPSEnabled);
    }

    @Subscribe
    public void onReceiveStartingTimeEvent(StartingTimeEvent event) {
        mMainThreadWorker.schedule(() -> {
            mTimerText.setBase(event.mStartingTime);
            if(event.mIsStarted)
                mTimerText.start();
            else
                mTimerText.stop();
        });
    }

    private void updateLocationViews(boolean isFix) {
        if (isFix) {
            mGpsImage.setImageResource(R.drawable.ic_location_on_white_24dp);
            mTimerText.start();
        } else {
            mTimerText.setBase(SystemClock.elapsedRealtime());
            mTimerText.stop();
            mGpsImage.setImageResource(R.drawable.ic_location_off_white_24dp);
        }
    }

}
