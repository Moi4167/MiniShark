package fr.soup.wepbash;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import fr.soup.wepbash.attack.AttackerInterface;
import fr.soup.wepbash.attack.EAPAttacker;
import fr.soup.wepbash.attack.WEPAttacker;
import fr.soup.wepbash.attack.WPSAttacker;
import fr.soup.wepbash.attack.callbacks.AttackCallback;
import fr.soup.wepbash.attack.exceptions.FailedAttackException;
import fr.soup.wepbash.attack.exceptions.NoDevicesAvailableException;
import fr.soup.wepbash.attack.exceptions.PcapErrorException;
import fr.soup.wepbash.dialogs.ActionDialog;
import fr.soup.wepbash.dialogs.DeviceDialog;
import fr.soup.wepbash.discovery.DiscoveryCallback;
import fr.soup.wepbash.discovery.WiFiDiscovery;

/**
 * Created by cyprien on 08/07/16.
 */
public class WepBashMainActivity extends Activity{

    ListView lv;
    ArrayAdapter<String> adapter;
    final Context context = this;
    AttackerInterface attacker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Networks ListView
        lv = (ListView)findViewById(R.id.wifilist);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str = (String) lv.getItemAtPosition(position);

                if (str.contains("WEP"))
                    attacker = new WEPAttacker(str.split(" ")[0]);
                else if (str.contains("EAP"))
                    attacker = new EAPAttacker(str.split(" ")[0]);
                else if (str.contains("WPS"))
                    attacker = new WPSAttacker(str.split(" ")[0]);

                try {
                    ArrayList<String> buf = attacker.detectDevices();
                    deviceChooserDialog(buf);
                } catch (NoDevicesAvailableException e) {
                    Toast.makeText(context, "No suitable network interface found", Toast.LENGTH_SHORT).show();
                } catch (PcapErrorException e){
                    Toast.makeText(context, "Pcap Error "+e.getErrcode() + " : "+ e.getError(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    public void chooseNetInterface(int netInterface) throws SocketException {
        attacker.chooseDevice(netInterface);
        processAttack();
    }

    private void processAttack(){
        try {
            attacker.attack(this, new AttackCallback() {
                @Override
                public void succesCallback(String ssid, String key) {
                    chooserDialog(ssid, key);
                }
            });
        } catch (FailedAttackException e) {
            Toast.makeText(context, "Sorry, the attack failed", Toast.LENGTH_SHORT).show();
        } catch (UnknownHostException e) {
            //TODO Autogenerated catch
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void wifisearch(View view) {
        WiFiDiscovery discovery=new WiFiDiscovery(this, new DiscoveryCallback() {
            @Override
            public void callback(Context context, ArrayList<String>results) {
                ((WepBashMainActivity)context).updateListView(results);
            }
        });
        discovery.discover();
        Toast.makeText(this, "Scanning....", Toast.LENGTH_SHORT).show();
    }

    private void updateListView(ArrayList<String> results) {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, results);
        lv.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void chooserDialog(String ssid, String key){
        ActionDialog dialog=new ActionDialog();
        Bundle b = new Bundle();
        b.putString("ssid", ssid);
        b.putString("key", key);
        dialog.setArguments(b);
        dialog.show(getFragmentManager(), "chooserdialog");
    }

    public void deviceChooserDialog(ArrayList<String> ifs){
        DeviceDialog dialog=new DeviceDialog();
        Bundle b = new Bundle();
        b.putStringArrayList("ifs", ifs);
        dialog.setArguments(b);
        dialog.show(getFragmentManager(), "ifsdialog");
    }
}
