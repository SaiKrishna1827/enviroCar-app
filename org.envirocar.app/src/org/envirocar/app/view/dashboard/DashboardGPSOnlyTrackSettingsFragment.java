package org.envirocar.app.view.dashboard;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.view.carselection.CarSelectionActivity;
import org.envirocar.core.entity.Car;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author Sai Krishna
 * Settings fragment which allows user to select Car when recording type is GPS Only
 */
public class DashboardGPSOnlyTrackSettingsFragment extends BaseInjectorFragment {

    private static final Logger LOG = Logger.getLogger(DashboardGPSOnlyTrackSettingsFragment.class);

    @Inject
    protected CarPreferenceHandler mCarPrefHandler;

    @InjectView(R.id.fragment_startup_car_selection)
    protected View mCarTypeView;
    @InjectView(R.id.fragment_startup_car_selection_text1)
    protected TextView mCarTypeTextView;
    @InjectView(R.id.fragment_startup_car_selection_text2)
    protected TextView mCarTypeSubTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOG.info("onCreateView()");
        // Inflate the layout for this fragment
        View contentView = inflater.inflate(R.layout.fragment_dashboard_gpsonly_track_settings, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);


        mCarTypeView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CarSelectionActivity.class);
            getActivity().startActivity(intent);
        });

        setCarTypeText(mCarPrefHandler.getCar());

        return contentView;
    }

    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(NewCarTypeSelectedEvent event) {
        LOG.debug(String.format("Received event: %s", event.toString()));
        setCarTypeText(event.mCar);
    }

    /**
     * @param car
     */
    private void setCarTypeText(Car car) {
        if (car != null) {
            mCarTypeTextView.setText(String.format("%s - %s",
                    car.getManufacturer(),
                    car.getModel()));

            mCarTypeSubTextView.setText(String.format("%s    %s    %s ccm",
                    car.getConstructionYear(),
                    car.getFuelType(),
                    car.getEngineDisplacement()));

            mCarTypeSubTextView.setVisibility(View.VISIBLE);
        } else {
            mCarTypeTextView.setText(R.string.dashboard_carselection_no_car_selected);
            mCarTypeSubTextView.setText(R.string.dashboard_carselection_no_car_selected_advise);
            mCarTypeSubTextView.setVisibility(View.VISIBLE);
        }
    }

}
