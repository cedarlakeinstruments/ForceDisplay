package com.cedarlakeinstruments.io.forcedisplay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends ActionBarActivity
{

    private boolean _running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _running = true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Update text box from another thread
    private void updateUI(final View theView, final TextView text, final String theString)
    {
        // Update UI
        theView.post(new Runnable() {
            @Override
            public void run() {
                text.setText(theString);
            }
        });
    }

    // Connect button handler
    public void onConnect(View v)
    {
        final TextView bend   = (TextView)findViewById(R.id.textView);
        final View thisView = v;
        new Thread(new Runnable()
        {
            public void run()
            {
                final String mac ="00:06:66:00:A0:AF";
                updateUI(bend, bend, String.format("Connecting to %s...", mac));

                InputStream stream = connect(mac);
                if (stream != null)
                {
                    int val = 0;
                    int i = 0;
                    byte[] buffer = new byte[20];
                    while (_running)
                    {
                        try
                        {
                            val = stream.read(buffer);
                            if (val != 0)
                            {
                                // Convert to text
                                String value = new String(buffer, "UTF-8");
                                updateUI(thisView, bend, value);
                            }
                            Thread.sleep(2500);
                        }
                        catch (IOException ioEx) {
                            ioEx.printStackTrace();
                            updateUI(thisView, bend, String.format("Error: %s", ioEx.toString()));
                        }
                        catch (InterruptedException intEx) {
                            intEx.printStackTrace();
                        }
                    }
                }
                else
                {
                    // Update UI
                    updateUI(thisView, bend, "Failed to connect");
                }
            }
        }).start();
    }

    // Connects to device
    private InputStream connect(String mac)
    {
        InputStream stream = null;
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        bta.cancelDiscovery();
        BluetoothDevice device = null;
        if (BluetoothAdapter.checkBluetoothAddress(mac))
        {
            device = bta.getRemoteDevice(mac);
        }
        BluetoothSocket socket = null;
        if (device != null)
        {
            UUID serialID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            try
            {
                socket = device.createRfcommSocketToServiceRecord(serialID);
                socket.connect();
                stream = socket.getInputStream();
            }
            catch (java.io.IOException e)
            {
                e.printStackTrace();
            }
        }
        return stream;
    }

}
