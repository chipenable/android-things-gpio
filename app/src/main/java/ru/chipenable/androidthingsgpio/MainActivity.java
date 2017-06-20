package ru.chipenable.androidthingsgpio;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final String LED_PIN_NAME = "BCM5";
    private static final long LED_PERIOD = 500;
    private static final String TAG = MainActivity.class.getName();

    private Gpio ledGpio;
    private LedTask ledTask;

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

        if (ledGpio != null){
            try {
                ledGpio.close();
            }
            catch(IOException e){
                Log.d(TAG, e.toString());
            }
        }
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

                    boolean value = ledGpio.getValue();
                    ledGpio.setValue(!value);
                    Log.d(TAG, "led " + (value ? "on" : "off"));
                    Thread.sleep(delay);

                } catch (IOException | InterruptedException e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }

}
