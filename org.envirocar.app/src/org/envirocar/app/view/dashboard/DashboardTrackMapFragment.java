/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.view.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.events.TrackPathOverlayEvent;
import org.envirocar.app.view.utils.MapUtils;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTouch;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class DashboardTrackMapFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(DashboardTrackMapFragment.class);

    @InjectView(R.id.fragment_dashboard_frag_map_mapview)
    protected MapView mMapView;
    @InjectView(R.id.fragment_dashboard_frag_map_follow_fab)
    protected FloatingActionButton mFollowFab;

    private PathOverlay mPathOverlay;

    private final Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread()
            .createWorker();

    private boolean mIsFollowingLocation;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOG.info("onCreateView()");

        setHasOptionsMenu(true);

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard_frag_map, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        //        mMapView.setOnTouchListener(new View.OnTouchListener() {
        //            @Override
        //            public boolean onTouch(View v, MotionEvent event) {
        //                mMapView.getUserLocationOverlay().disableFollowLocation();
        //                mFollowFab.setVisibility(View.VISIBLE);
        //                return false;
        //            }
        //        });

        // If the mPathOverlay has already been set, then add the overlay to the mapview.
        if (mPathOverlay != null)
            mMapView.getOverlays().add(mPathOverlay);

        return contentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Init the map view
        //we should initialise the mapview in onActivityCreated, not in onCreateView
        //as activity is not attached to the fragment in onCreateView
        initMapView();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Init the map view
        initMapView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // inflate the map menu for the dashboard when this fragment is visible.
        inflater.inflate(R.menu.menu_dashboard_map, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @OnTouch(R.id.fragment_dashboard_frag_map_mapview)
    protected boolean onTouchMapView() {
        if (mIsFollowingLocation) {
            // Disable the follow location mode.
            UserLocationOverlay userLocationOverlay = mMapView.getUserLocationOverlay();
            userLocationOverlay.disableFollowLocation();
            mIsFollowingLocation = false;

            // show the floating action button that can enable the follow location mode.
            showFollowFAB();
        }
        return false;
    }

    @OnClick(R.id.fragment_dashboard_frag_map_follow_fab)
    protected void onClickFollowFab() {
        if (!mIsFollowingLocation) {
            UserLocationOverlay userLocationOverlay = mMapView.getUserLocationOverlay();
            userLocationOverlay.enableFollowLocation();
            userLocationOverlay.goToMyPosition(true); // animated is not working... don't know why
            mIsFollowingLocation = true;

            hideFollowFAB();
        }
    }

    //Initialises the map view
    private void initMapView(){
        mMapView.setTileSource(MapUtils.getOSMTileLayer());
        mMapView.setUserLocationEnabled(true);
        mMapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);
        mMapView.setUserLocationRequiredZoom(18);
        mIsFollowingLocation = true;
        mFollowFab.setVisibility(View.INVISIBLE);
    }

    /**
     * Shows the floating action button for toggling the follow location ability.
     */
    private void showFollowFAB() {
        // load the translate animation.
        Animation slideLeft = AnimationUtils.loadAnimation(getActivity(),
                R.anim.translate_slide_in_right);

        // and start it on the fab.
        mFollowFab.startAnimation(slideLeft);
        mFollowFab.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideFollowFAB() {
        // load the translate animation.
        Animation slideRight = AnimationUtils.loadAnimation(getActivity(),
                R.anim.translate_slide_out_right);

        // set a listener that makes the button invisible when the animation has finished.
        slideRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // nothing to do..
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mFollowFab.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // nothing to do..
            }
        });

        // and start it on the fab.
        mFollowFab.startAnimation(slideRight);
    }

    @Subscribe
    public void onReceivePathOverlayEvent(TrackPathOverlayEvent event) {
        mMainThreadWorker.schedule(() -> {
            mPathOverlay = event.mTrackOverlay;
            if (mMapView != null)
                mMapView.addOverlay(mPathOverlay);
        });
    }

}