/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.google.common.base.Preconditions;

import org.envirocar.core.entity.Car;
import org.envirocar.core.utils.CarUtils;

import rx.Observable;

import static org.envirocar.app.services.obd.OBDServiceHandler.context;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class PreferencesHandler implements PreferenceConstants {

    public static final boolean DEFAULT_BLUETOOTH_AUTOCONNECT = false;
    public static final boolean DEFAULT_DISPLAY_STAYS_ACTIVE = false;
    public static final boolean DEFAULT_TEXT_TO_SPEECH = false;
    public static final boolean DEFAULT_BLUETOOTH_SERVICE_AUTOSTART = true;

    //this method returns the previously selected/default recording type(OBD+GPS or GPS Only)
    public static int getPreviouslySelectedRecordingType(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_SELECT_RECORDING_TYPE, DEFAULT_SELECT_RECORDING_TYPE);
    }

    //this method sets the previously selected/default recording type(OBD+GPS or GPS Only) into the sharedpreferences
    public static void setPreviouslySelectedRecordingType(Context context,int type){
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putInt(PREF_SELECT_RECORDING_TYPE, type);
        editor.apply();
    }


    public static boolean isAutoconnectEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_BLUETOOTH_AUTOCONNECT, DEFAULT_BLUETOOTH_AUTOCONNECT);
    }

    public static Observable<Boolean> getAutoconnectObservable(final Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context))
                .getBoolean(PREF_BLUETOOTH_AUTOCONNECT, DEFAULT_BLUETOOTH_AUTOCONNECT)
                .asObservable();
    }

    public static boolean isBackgroundHandlerEnabled(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_BLUETOOTH_SERVICE_AUTOSTART, DEFAULT_BLUETOOTH_SERVICE_AUTOSTART);
    }

    public static Observable<Boolean> getBackgroundHandlerEnabledObservable(final Context context){
        return RxSharedPreferences.create(getSharedPreferences(context))
                .getBoolean(PREF_BLUETOOTH_SERVICE_AUTOSTART, DEFAULT_BLUETOOTH_SERVICE_AUTOSTART)
                .asObservable();
    }

    public static int getDiscoveryInterval(Context context) {
        return getSharedPreferences(context)
                .getInt(PREF_BLUETOOTH_DISCOVERY_INTERVAL, DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL);
    }

    public static Observable<Integer> getDiscoveryIntervalObservable(Context context) {
        return getRxSharedPreferences(context)
                .getInteger(PREF_BLUETOOTH_DISCOVERY_INTERVAL, DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL)
                .asObservable();
    }

    public static boolean isDisplayStaysActive(Context context) {
        return getSharedPreferences(context)
                .getBoolean(DISPLAY_STAYS_ACTIV, DEFAULT_BLUETOOTH_AUTOCONNECT);
    }

    public static Observable<Boolean> getDisplayStaysActiveObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(DISPLAY_STAYS_ACTIV, DEFAULT_DISPLAY_STAYS_ACTIVE)
                .asObservable();
    }

    public static boolean isTextToSpeechEnabled(Context context) {
        return getSharedPreferences(context)
                .getBoolean(PREF_TEXT_TO_SPEECH, DEFAULT_TEXT_TO_SPEECH);
    }

    public static Observable<Boolean> getTextToSpeechObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(PREF_TEXT_TO_SPEECH, DEFAULT_TEXT_TO_SPEECH)
                .asObservable();
    }

    private static RxSharedPreferences getRxSharedPreferences(Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context));
    }

    public static Long getSamplingRate(Context context) {
        return Long.parseLong(getSharedPreferences(context)
                .getString(SAMPLING_RATE, "5"));
    }

    public static Observable<Long> getRxSharedSamplingRate(Context context) {
        return getRxSharedPreferences(context)
                .getString(SAMPLING_RATE, "5")
                .asObservable().map(s -> Long.parseLong(s));
    }

    public static boolean isObfuscationEnabled(Context context) {
        return getSharedPreferences(context)
                .getBoolean(PreferenceConstants.OBFUSCATE_POSITION, false);
    }

    public static Observable<Boolean> getObfuscationObservable(Context context){
        return getRxSharedPreferences(context)
                .getBoolean(OBFUSCATE_POSITION, false)
                .asObservable();
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        Preconditions.checkNotNull(context, "Input context cannot be null.");
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isDieselConsumptionEnabled(Context context){
        return getSharedPreferences(context)
                .getBoolean(PREF_ENABLE_DIESE_CONSUMPTION, false);
    }

    public static Observable<Boolean> getDieselConsumptionObservable(Context context){
        return getRxSharedPreferences(context)
                .getBoolean(PREF_ENABLE_DIESE_CONSUMPTION, false)
                .asObservable();
    }

    public static Observable<Car> getSelectedCarObsevable(){
        return getRxSharedPreferences(context)
                .getString(PREFERENCE_TAG_CAR, null)
                .asObservable()
                .map(s -> s != null ? CarUtils.instantiateCar(s) : null);
    }
}
