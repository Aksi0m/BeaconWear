package adnroid.wear.beaconwear;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import org.altbeacon.beacon.Region;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;

import java.util.Collection;
import java.util.Iterator;

public class MainActivity extends Activity implements BeaconConsumer {

    private BeaconManager manager;
    private Region region;
    private TextView mTextView;
    private  StringBuilder stringBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initBTScanning();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (manager != null && !manager.isBound(this)) {
            manager.bind(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (manager != null) {
            manager.unbind(this);
            stopScanning();
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        startScanning();
    }

    /**
     * initialise the beacon manager and region for scanning
     */
    private void initBTScanning() {
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();

        // Is Bluetooth supported on this device?
        if (mBluetoothAdapter != null) {

            // Is Bluetooth turned on?
            if (mBluetoothAdapter.isEnabled()) {

                manager = BeaconManager.getInstanceForApplication(getApplicationContext());
                // Add parser for iBeacons;
                manager.getBeaconParsers().add(new BeaconParser().
                        setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
                // Detect the Eddystone main identifier (UID) frame:
                manager.getBeaconParsers().add(new BeaconParser().
                        setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
                // Detect the Eddystone telemetry (TLM) frame:
                manager.getBeaconParsers().add(new BeaconParser().
                        setBeaconLayout("x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"));
                // Detect the Eddystone URL frame:
                manager.getBeaconParsers().add(new BeaconParser().
                        setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"));

                // Get the details for all the beacons we encounter.
                region = new Region("justGiveMeEverything", null, null, null);

                Toast.makeText(MainActivity.this, "BT init completed", Toast.LENGTH_SHORT).show();

            } else {

                Toast.makeText(MainActivity.this, "BT not enabled", Toast.LENGTH_SHORT).show();

            }
        } else {

            Toast.makeText(MainActivity.this, "BT not supported", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * start looking for beacons.
     */
    private void startScanning() {
        stringBuilder = new StringBuilder();

        manager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons,
                                                org.altbeacon.beacon.Region region) {
                if (beacons.size() > 0) {
                    Iterator<Beacon> beaconIterator = beacons.iterator();
                    while (beaconIterator.hasNext()) {
                        Beacon beacon = beaconIterator.next();

                        String newBeacon = "ID1: " + beacon.getId1() + "\nID2: " + beacon.getId2()
                                + "\nID3: " + beacon.getId3()
                                + "\nDistance: " + beacon.getDistance()
                                + "\n -------- \n";
                        stringBuilder.append(newBeacon);
                        runOnUiThread(updateTextView);
                    }
                }
            }
        });

        try {
            manager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.getStackTrace();
        }

    }

    /**
     * Stop looking for beacons
     */
    private void stopScanning() {
        try {
            manager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.getStackTrace();
        }
    }

    /**
     * Runnable that updates the text view on the UI thread
     */
    Runnable updateTextView = new Runnable() {
        @Override
        public void run() {
            mTextView.setText(stringBuilder);
        }
    };
}
