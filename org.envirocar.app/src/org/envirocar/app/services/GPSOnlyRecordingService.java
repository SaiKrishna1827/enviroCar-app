package org.envirocar.app.services;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;

import com.squareup.otto.Subscribe;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.events.TrackDetailsProvider;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.NewMeasurementEvent;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.events.gps.GpsSatelliteFix;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.injection.BaseInjectorService;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.AbstractCalculatedMAFAlgorithm;
import org.envirocar.core.trackprocessing.CalculatedMAFWithStaticVolumetricEfficiency;
import org.envirocar.core.trackprocessing.ConsumptionAlgorithm;
import org.envirocar.core.utils.CarUtils;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;
import org.envirocar.storage.EnviroCarDB;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;


/*
* @author Sai Krishna
* this service is used to record GPS Only track
*/

public class GPSOnlyRecordingService extends BaseInjectorService {
    private static final Logger LOG = Logger.getLogger(GPSOnlyRecordingService.class);

    public static void startService(Context context){
        ServiceUtils.startService(context, GPSOnlyRecordingService.class);
    }

    public static void stopService(Context context){
        ServiceUtils.stopService(context, GPSOnlyRecordingService.class);
    }

    public static BluetoothServiceState CURRENT_SERVICE_STATE = BluetoothServiceState
            .SERVICE_STOPPED;

    @Inject
    protected LocationHandler mLocationHandler;
    @Inject
    protected TrackDetailsProvider mTrackDetailsProvider;
    @Inject
    protected PowerManager.WakeLock mWakeLock;
    @Inject
    protected MeasurementProvider measurementProvider;
    @Inject
    protected CarPreferenceHandler carHandler;
    @Inject
    protected EnviroCarDB enviroCarDB;
    @Inject
    protected TrackRecordingHandler trackRecordingHandler;

    private AbstractCalculatedMAFAlgorithm mafAlgorithm;

    // Text to speech variables.
    private TextToSpeech mTTS;
    private boolean mIsTTSAvailable;
    private boolean mIsTTSPrefChecked;


    // Different subscriptions
    private Subscription mTTSPrefSubscription;
    private Subscription mMeasurementSubscription;

    // This satellite fix indicates that there is no satellite connection yet.
    private GpsSatelliteFix mCurrentGpsSatelliteFix = new GpsSatelliteFix(0, false);

    private GPSOnlyRecordingConnectionRecognizer connectionRecognizer = new GPSOnlyRecordingConnectionRecognizer();
    private ConsumptionAlgorithm consumptionAlgorithm;

    private final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();

    @Override
    public void onCreate() {
        LOG.info("GPSOnlyRecordingService.onCreate()");
        super.onCreate();

        // register on the event bus
        this.bus.register(this);
        this.bus.register(mTrackDetailsProvider);
        this.bus.register(connectionRecognizer);
        this.bus.register(measurementProvider);

        mTTS = new TextToSpeech(getApplicationContext(), status -> {
            try {
                if (status == TextToSpeech.SUCCESS) {
                    mTTS.setLanguage(Locale.ENGLISH);
                    mIsTTSAvailable = true;
                } else {
                    LOG.warn("TextToSpeech is not available.");
                }
            } catch(IllegalArgumentException e){
                LOG.warn("TextToSpeech is not available");
            }
        });

        mTTSPrefSubscription =
                PreferencesHandler.getTextToSpeechObservable(getApplicationContext())
                        .subscribe(aBoolean -> {
                            mIsTTSPrefChecked = aBoolean;
                        });

        /**
         * create the consumption and MAF algorithm, final for this connection
         */
        Car car = carHandler.getCar();
        this.consumptionAlgorithm = CarUtils.resolveConsumptionAlgorithm(car.getFuelType());

        this.mafAlgorithm = new CalculatedMAFWithStaticVolumetricEfficiency(car);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("GPSOnlyRecordingService.onStartCommand()");
        doTextToSpeech("Establishing connection");

        // Acquire the wake lock for keeping the CPU active.
        mWakeLock.acquire();
        // Start the location
        mLocationHandler.startLocating();


        if(mLocationHandler.isGPSEnabled()){
            setBluetoothServiceState(BluetoothServiceState.SERVICE_STARTED);
            subscribeForMeasurements();
        }else{
            LOG.severe("GPS is disabled");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LOG.info("GPSOnlyRecordingService.onDestroy()");
        super.onDestroy();

        // Stop this remoteService and emove this remoteService from foreground state.
        stopGPSOnlyRecordingServiceConnection();

        // Unregister from the event bus.
        bus.unregister(this);
        bus.unregister(mTrackDetailsProvider);
        bus.unregister(connectionRecognizer);
        bus.unregister(measurementProvider);

        LOG.info("GPSOnlyRecordingService successfully destroyed");
    }

    /**
     * Sets the current remoteService state and fire an event on the bus.
     *
     * @param state the state of the remoteService.
     */
    private void setBluetoothServiceState(BluetoothServiceState state) {
        // Set the new remoteService state
        CURRENT_SERVICE_STATE = state; // TODO FIX
        // and fire an event on the event bus.
        this.bus.post(new BluetoothServiceStateChangedEvent(CURRENT_SERVICE_STATE));
    }


    @Override
    public List<Object> getInjectionModules() {
        return Arrays.<Object>asList(new OBDServiceModule());
    }

    @Subscribe
    public void onReceiveGpsSatelliteFixEvent(GpsSatelliteFixEvent event) {
        boolean isFix = event.mGpsSatelliteFix.isFix();
        if (isFix != mCurrentGpsSatelliteFix.isFix()) {
            if (isFix) {
                doTextToSpeech("GPS positioning established");
            } else {
                doTextToSpeech("GPS positioning lost. Try to move the phone");
            }
            this.mCurrentGpsSatelliteFix = event.mGpsSatelliteFix;
        }
    }



    /**
     * Method that stops the remoteService, removes everything from the waiting list
     */
    private void stopGPSOnlyRecordingServiceConnection() {
        LOG.info("stopGPSOnlyRecordingServiceConnection called");
        backgroundWorker.schedule(() -> {
            stopForeground(true);

            // If there is an active UUID subscription.
            if (mTTSPrefSubscription != null && !mTTSPrefSubscription.isUnsubscribed())
                mTTSPrefSubscription.unsubscribe();
            if (mMeasurementSubscription != null && !mMeasurementSubscription.isUnsubscribed())
                mMeasurementSubscription.unsubscribe();

            if (mTrackDetailsProvider != null)
                mTrackDetailsProvider.clear();
            if (connectionRecognizer != null)
                connectionRecognizer.shutDown();
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }

            mLocationHandler.stopLocating();
            doTextToSpeech("Stopped locating");

            setBluetoothServiceState(BluetoothServiceState.SERVICE_STOPPED);
        });
    }


    private void doTextToSpeech(String string) {
        if (mIsTTSAvailable && mIsTTSPrefChecked) {
            mTTS.speak(string, TextToSpeech.QUEUE_ADD, null);
        }
    }


    private void subscribeForMeasurements() {
        // this is the first access to the measurement objects push it further
        Long samplingRate = PreferencesHandler.getSamplingRate(getApplicationContext()) * 1000;
        mMeasurementSubscription = measurementProvider.measurements(samplingRate)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(getMeasurementSubscriber());
    }

    private Subscriber<Measurement> getMeasurementSubscriber() {
        return new Subscriber<Measurement>() {
            PublishSubject<Measurement> measurementPublisher =
                    PublishSubject.create();

            @Override
            public void onStart() {
                LOG.info("onStart(): MeasuremnetProvider Subscription");
                add(trackRecordingHandler.startNewTrack(measurementPublisher));
            }

            @Override
            public void onCompleted() {
                LOG.info("onCompleted(): MeasurementProvider");
                measurementPublisher.onCompleted();
                measurementPublisher = null;
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(e.getMessage(), e);
                measurementPublisher.onError(e);
                measurementPublisher = null;
            }

            @Override
            public void onNext(Measurement measurement) {
                LOG.info("onNext()");
                try {
                    if (!measurement.hasProperty(Measurement.PropertyKey.MAF)) {
                        try {
                            measurement.setProperty(Measurement.PropertyKey
                                    .CALCULATED_MAF, mafAlgorithm.calculateMAF(measurement));
                        } catch (NoMeasurementsException e) {
                            LOG.warn(e.getMessage());
                        }
                    }

                    if (consumptionAlgorithm != null) {
                        double consumption = consumptionAlgorithm.calculateConsumption(measurement);
                        double co2 = consumptionAlgorithm.calculateCO2FromConsumption(consumption);
                        measurement.setProperty(Measurement.PropertyKey.CONSUMPTION, consumption);
                        measurement.setProperty(Measurement.PropertyKey.CO2, co2);
                    }
                } catch (FuelConsumptionException e) {
                    LOG.warn(e.getMessage());
                } catch (UnsupportedFuelTypeException e) {
                    LOG.warn(e.getMessage());
                }

                measurementPublisher.onNext(measurement);
                bus.post(new NewMeasurementEvent(measurement));
            }
        };
    }


    private final class GPSOnlyRecordingConnectionRecognizer {
        private static final long GPS_INTERVAL = 1000 * 60 * 2; // 2 minutes;


        private final Scheduler.Worker mBackgroundWorker = Schedulers.io().createWorker();
        private Subscription mGPSCheckerSubscription;

        private final Action0 gpsConnectionCloser = () -> {
            LOG.warn("Connection closed due to no GPS values");
            stopSelf();
        };


        @Subscribe
        public void onReceiveGpsLocationChangedEvent(GpsLocationChangedEvent event) {
            if (mGPSCheckerSubscription != null) {
                mGPSCheckerSubscription.unsubscribe();
                mGPSCheckerSubscription = null;
            }


            mGPSCheckerSubscription = mBackgroundWorker.schedule(
                    gpsConnectionCloser, GPS_INTERVAL, TimeUnit.MILLISECONDS);
        }


        public void shutDown() {
            LOG.info("shutDown() GPSOnlyRecordingConnectionRecognizer");
            if (mGPSCheckerSubscription != null)
                mGPSCheckerSubscription.unsubscribe();
        }
    }


}
