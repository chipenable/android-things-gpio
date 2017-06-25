package ru.chipenable.androidthingsgpio;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final String LED_PIN_NAME = "BCM5";
    private static final String BUT_PIN_NAME = "BCM6";
    private static final long LED_PERIOD = 200;
    private static final String TAG = MainActivity.class.getName();

    private Gpio ledGpio;
    private Gpio butGpio;
    private LedTask ledTask;
    private boolean enableLed;

    private GpioCallback buttonCallback = new GpioCallback() {

        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                enableLed = gpio.getValue();
                return true;
            } catch (IOException e) {
                Log.d(TAG, e.toString());
                return false;
            }
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeripheralManagerService service = new PeripheralManagerService();
        Log.d(TAG, "Available GPIO: " + service.getGpioList());

        try{
            ledGpio = service.openGpio(LED_PIN_NAME);
            ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            ledTask = new LedTask(ledGpio, LED_PERIOD);
            new Thread(ledTask).start();

            butGpio = service.openGpio(BUT_PIN_NAME);
            butGpio.setDirection(Gpio.DIRECTION_IN);
            butGpio.setActiveType(Gpio.ACTIVE_LOW);
            butGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            butGpio.registerGpioCallback(buttonCallback);
        }
        catch(IOException e){
            Log.d(TAG, "Error on PeripheralIO API", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (ledTask != null){
            ledTask.stop();
        }

        releaseGpio(ledGpio, null);
        releaseGpio(butGpio, buttonCallback);
    }

    private class LedTask implements Runnable{

        private Gpio ledGpio;
        private long delay;
        private boolean isStopped;

        public LedTask(Gpio ledGpio, long delay){
            this.ledGpio = ledGpio;
            this.delay = delay;
            this.isStopped = false;
        }

        public void stop(){
            isStopped = true;
        }

        @Override
        public void run() {
            while(true) {
                try {
                    if (isStopped) {
                        ledGpio = null;
                        return;
                    }

                    if (enableLed) {
                        boolean value = ledGpio.getValue();
                        ledGpio.setValue(!value);
                        Log.d(TAG, "led " + (value ? "on" : "off"));
                    }
                    else{
                        ledGpio.setValue(false);
                    }

                    Thread.sleep(delay);

                } catch (IOException | InterruptedException e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }

    private void releaseGpio(Gpio gpio, GpioCallback callback) {
        if (gpio != null) {
            try {
                if (callback != null){
                    gpio.unregisterGpioCallback(callback);
                }
                gpio.close();
                gpio = null;
            } catch (IOException e) {
                Log.d(TAG, e.toString());
            }
        }
    }

}
