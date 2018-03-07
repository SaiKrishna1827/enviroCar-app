package org.envirocar.app.events;

import com.google.common.base.MoreObjects;

/**
 * Created by Sai Krishna on 3/7/2018.
 * When recording type is GPS Only then change of speed is posted in the bus by this event
 */

public class GPSSpeedChangeEvent {

    public final float mGPSSpeed;

    /**
     * Constructor.
     *
     * @param mGPSSpeed the new speed value;
     */
    public GPSSpeedChangeEvent(float mGPSSpeed) {
        this.mGPSSpeed = mGPSSpeed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("GPS Speed", mGPSSpeed)
                .toString();
    }

}
