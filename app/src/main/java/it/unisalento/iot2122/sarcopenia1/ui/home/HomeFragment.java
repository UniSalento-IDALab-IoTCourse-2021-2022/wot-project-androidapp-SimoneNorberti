package it.unisalento.iot2122.sarcopenia1.ui.home;

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
    TextView arrivedData;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        arrivedData = binding.arrivedData;

        Button button = binding.startButton;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent intent = new Intent(v.getContext(), MqttActivity.class);
                // startActivity(intent);
                receiveMqttData();
            }
        });


        // arrivedData.setText(data);
        // homeViewModel.getText().observe(getViewLifecycleOwner(), arrivedData::setText);
        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void receiveMqttData() {

        String broker = "tcp://mqtt.eclipseprojects.io";
        String topicBia = "unisalento/sarcopenia/data/bia";
        String username = "user1";
        String password = "pass1";
        int qos = 0;

        String clientId = MqttClient.generateClientId();
        Log.d("MQTT", "clientId=" + clientId);
        MqttClient client = null;

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
                arrivedData.setText(data);
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
        } catch (MqttException e) {
            Log.d("MQTT CONNECT", "failed");
            e.printStackTrace();
        }
        try {
            client.subscribe(topicBia, qos);
            Log.d("MQTT SUBSCRIBE", "ok");
        } catch (MqttException e) {
            Log.d("MQTT SUBSCRIBE", "failed");
            e.printStackTrace();
        }

    }


}