package it.unisalento.iot2122.sarcopenia1.ui.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import it.unisalento.iot2122.sarcopenia1.MainActivity;
import it.unisalento.iot2122.sarcopenia1.MqttActivity;
import it.unisalento.iot2122.sarcopenia1.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    public String data;
    TextView arrivedDataText;
    Button button;
    boolean start;
    MqttClient client = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        start = true;
        arrivedDataText = binding.arrivedData;

        button = binding.startButton;
        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                if (start){
                    try {
                        receiveMqttData(true);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    button.setText("FERMA");
                    start = false;
                }
                else {
                    try {
                        receiveMqttData(false);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    button.setText("INIZIA ALLENAMENTO");
                    arrivedDataText.setText("Clicca sul pulsante sopra per iniziare l' allenamento!");
                    start = true;
                }

            }
        });
        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    public void receiveMqttData(boolean start_flag) throws MqttException {

        String broker = "tcp://mqtt.eclipseprojects.io";
        String topicBia = "unisalento/sarcopenia/data/bia";
        String username = "user1";
        String password = "pass1";
        int qos = 0;

        String clientId = MqttClient.generateClientId();
        Log.d("MQTT", "clientId=" + clientId);

        if (!start_flag){
            assert client != null;
            client.disconnect();
            client.close();
            return;
        }

        try {
            client = new MqttClient(broker, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }



        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setConnectionTimeout(180);
        options.setKeepAliveInterval(180);

        assert client != null;
        client.setCallback(new MqttCallback() {

            public void connectionLost(Throwable cause) {
                Log.d("MQTT", "connectionLost: " + cause.getMessage());
            }

            public void messageArrived(String topic, MqttMessage message) {
                data = new String(message.getPayload());
                arrivedDataText.setText(data);
                Log.d("MQTT", "topic: " + topic);
                Log.d("MQTT", "Qos: " + message.getQos());
                Log.d("MQTT", "message content: " + new String(message.getPayload()));
            }

            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("MQTT", "deliveryComplete---------" + token.isComplete());
            }

        });
        try {
            client.connect(options);
            Log.d("MQTT CONNECT", "ok");
            client.subscribe(topicBia, qos);
            Log.d("MQTT SUBSCRIBE", "ok");
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


}