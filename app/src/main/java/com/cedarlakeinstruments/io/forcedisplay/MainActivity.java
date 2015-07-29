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
    private final int BUF_SIZE = 20;
    private byte[] _buffer = new byte[BUF_SIZE];
    private int _bufferIndex = 0;

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
        final TextView statusText = (TextView)findViewById(R.id.textView2);
        final View thisView = v;
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    final String mac = "00:06:66:00:A0:AF";
                    updateUI(thisView, statusText, String.format("Connecting to %s...", mac));

                    InputStream stream = connect(mac);
                    if (stream != null) {
                        updateUI(thisView, statusText, String.format("Connected to %s", mac));
                        while (_running) {
                            // Display value
                            int sensor = readMessageAndDecode(stream);
                            if (sensor != -1) {
                                updateUI(thisView, bend, String.valueOf(sensor));
                            }
                        }
                    } else {
                        // Update UI
                        updateUI(thisView, statusText, "Failed to connect");
                    }
                    Thread.sleep(2500);
                }
                catch (InterruptedException intEx) {
                    intEx.printStackTrace();
                }
            }
        }).start();
    }

    // Read the input stream and return a decoded value
    private int readMessageAndDecode(InputStream stream)
    {
        int valRead = 0;
        int value = -1;
        final int CR = 13;
        final int LF = 10;
        //byte[] buffer = {'$','F','S','D','A','T',',','3','1','4','9',13,10};
        try
        {
            // We expect data in the format: '$FSDAT,1234<CR><LF>'
            // with a maximum of 4 bytes of data
            valRead = stream.read();
            _buffer[_bufferIndex++] = (byte)valRead;
            if (BUF_SIZE == _bufferIndex)
            {
                _bufferIndex = 0;
            }
            if (valRead == LF)
            {
                // Decode datastream
                _buffer[_bufferIndex] = '\0';
                _bufferIndex = 0;
                value = decode (new String(_buffer, "UTF-8"));
            }
        }
        catch (IOException ioEx) {
            ioEx.printStackTrace();
            value = -1;
        }
        return value;
    }

    // Decode the raw NMEA format data
    // We send data in the form of $FSDAT,xxxx<CR><LF>
    private int decode(String raw)
    {
        int value = -1;
        final String PREAMBLE = "$FSDAT,";
        int prePos = raw.indexOf(PREAMBLE);
        if (prePos != -1)
        {
            // Look for the CRLF
            final String POSTAMBLE = new String(new byte[]{13,10});
            // Point at end of string
            int postPos = raw.indexOf(POSTAMBLE);
            if (postPos != -1) {
                String strVal = raw.substring(prePos+PREAMBLE.length(), postPos);
                value = Integer.parseInt(strVal);
            }
        }
        return value;
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
            // Use predefined Bluetooth UUID for SPP protocol
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
