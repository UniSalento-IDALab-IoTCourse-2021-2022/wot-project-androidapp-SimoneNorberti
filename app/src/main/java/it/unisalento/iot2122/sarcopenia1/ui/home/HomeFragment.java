package it.unisalento.iot2122.sarcopenia1.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.util.Objects;

import it.unisalento.iot2122.sarcopenia1.MainActivity;
import it.unisalento.iot2122.sarcopenia1.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    public String data;
    boolean start;
    TextView arrivedDataText, textViewTest;
    Button button;
    ListView listData;
    MqttClient client = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        start = true;
        arrivedDataText = binding.arrivedData;
        textViewTest = binding.textViewTest;
        button = binding.startButton;

        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                if (start) {
                    try {
                        receiveMqttData(true);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getContext(), "Allenamento iniziato!", Toast.LENGTH_SHORT).show();
                    button.setText("FERMA");
                    arrivedDataText.setText("Provo a ricevere i dati...");
                    start = false;
                } else {
                    try {
                        receiveMqttData(false);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getContext(), "Allenamento terminato!", Toast.LENGTH_SHORT).show();
                    button.setText("INIZIA ALLENAMENTO");
                    arrivedDataText.setText("Clicca sul pulsante sopra per iniziare l' allenamento!");
                    start = true;
                }

            }
        });


        listData = binding.listData;


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

        if (!start_flag) {
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
                // TODO send json data with REST API
                sendAPIData(data);

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

    @SuppressLint("SetTextI18n")
    public void sendAPIData(String data) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(requireContext());
        String url = "http://10.0.2.2:5000";
        // String url = "https://catfact.ninja/fact";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        textViewTest.setText("Response is: " + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textViewTest.setText("That didn't work! ");
                Log.d("REST API", String.valueOf(error));
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


}