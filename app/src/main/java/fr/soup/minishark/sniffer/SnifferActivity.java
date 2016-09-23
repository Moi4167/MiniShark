package fr.soup.minishark.sniffer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import fr.soup.minishark.R;
import fr.soup.minishark.dialogs.ConnectionDialog;

/**
 * Created by cyprien on 08/07/16.
 */
public class SnifferActivity extends Activity{

    public static final String SNIFFER_FLAGS_INTENT = "snifferactivityflagsintent";

    final Context context = this;
    private ListView listView;
    private boolean tcpdumpBound = false;
    private String flags;
    private ArrayList<String> packets;
    private ArrayAdapter<String> adapter;
    TcpDumpWrapper mService;

    private BroadcastReceiver sharkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == TcpDumpWrapper.REFRESH_DATA_INTENT) {
                packets.add(intent.getStringExtra(TcpDumpWrapper.REFRESH_DATA));
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sniffer);

        Intent intent = getIntent();
        flags= intent.getStringExtra(SNIFFER_FLAGS_INTENT);

        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifi = wifi.getConnectionInfo();

        if (wifi.isWifiEnabled() == false)
        {
            Toast.makeText(context.getApplicationContext(), R.string.disconnected_toast, Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }

        if (mWifi.getSupplicantState() != SupplicantState.COMPLETED) {
            new ConnectionDialog().show(getFragmentManager(),"connection_dialog");
        }

        listView=(ListView)findViewById(R.id.sharkListView);
        listView.setTranscriptMode(2);
        packets=new ArrayList<>();
        adapter= new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, packets);

        listView.setAdapter(adapter);
        registerReceiver(sharkReceiver, new IntentFilter(TcpDumpWrapper.REFRESH_DATA_INTENT));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tcpdumpBound) {
            Intent intent = new Intent();
            intent.setAction(TcpDumpWrapper.STOP_TCPDUMP);
            sendBroadcast(intent);
            unbindService(mConnection);
            tcpdumpBound = false;
            Toast.makeText(context.getApplicationContext(), R.string.tcpdump_stopped, Toast.LENGTH_LONG).show();
        }
    }

    public void snifferStart(View view) {

        EditText editText = (EditText) findViewById(R.id.manualflags);
        flags += editText.getText().toString() + " ";


        if(((CheckBox)findViewById(R.id.saveinfile)).isChecked()) {
            editText = (EditText) findViewById(R.id.pcapfile);
            flags += "-w /storage/emulated/legacy/" + editText.getText().toString() + " ";
        }

        if(((CheckBox)findViewById(R.id.rununtil)).isChecked()) {
            editText = (EditText) findViewById(R.id.rununtiltime);
            flags += "-G " + editText.getText().toString() + " -W 1 ";
        }

        Intent intent = new Intent(this, TcpDumpWrapper.class);
        intent.putExtra(SnifferActivity.SNIFFER_FLAGS_INTENT, flags);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }


     private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TcpDumpWrapper.TcpDumpWrapperBinder binder = (TcpDumpWrapper.TcpDumpWrapperBinder) service;
            mService = binder.getService();
            tcpdumpBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            tcpdumpBound = false;
        }
    };
}
