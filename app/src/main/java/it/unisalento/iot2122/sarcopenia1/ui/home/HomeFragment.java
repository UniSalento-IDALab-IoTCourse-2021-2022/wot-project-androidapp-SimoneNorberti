package it.unisalento.iot2122.sarcopenia1.ui.home;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import it.unisalento.iot2122.sarcopenia1.databinding.FragmentHomeBinding;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    public String data;
    boolean start;
    TextView arrivedDataText, textViewTest;
    Button button;
    ListView listData;
    RadioButton u1, u2, u3;
    RadioGroup radioGroup;
    MqttClient client = null;
    public String ID = "ID001";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // receiveAPImodel(); //receive the pre-trained model

        start = true;
        arrivedDataText = binding.arrivedData;
        textViewTest = binding.textViewTest;
        button = binding.startButton;
        u1 = binding.radioButton; u1.setChecked(true);
        u2 = binding.radioButton2;
        u3 = binding.radioButton3;

        u1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ID = "ID001";
            }
        });
        u2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ID = "ID002";
            }
        });
        u3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ID = "ID003";
            }
        });


        String clientId = MqttClient.generateClientId();
        Log.d("MQTT", "clientId=" + clientId);

        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                if (start) {
                    try {
                        //     [ INIZIO ALLENAMENTO ]
                        receiveMqttData_sendAPI(clientId); // Ricevo i dati dal simulatore di sensori
                                                            // e li invia al back-end
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getContext(), "Allenamento iniziato!", Toast.LENGTH_SHORT).show();
                    button.setText("FERMA");
                    arrivedDataText.setText("Provo a ricevere i dati...");
                    start = false;
                } else {
                    //     [ ALLENAMENTO FINITO ]
                    try {
                        disconnectMqtt(clientId); // Termina connessione al simulatore di sensori
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    JSONObject endData = new JSONObject();
                    try {
                        endData.put("ID", ID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String url = "http://10.0.2.2:5000/api/endsession";   // e.g. "https://catfact.ninja/fact";
                    sendAPIData(endData, url);
                    Log.d("INVIO ENDDATA", "invio POST /api/endsession");
                    Toast.makeText(getContext(), "Allenamento terminato!", Toast.LENGTH_SHORT).show();
                    button.setText("INIZIA ALLENAMENTO");
                    arrivedDataText.setText("Clicca sul pulsante sopra per iniziare l' allenamento!");
                    start = true;
                }

            }
        });

        //listData = binding.listData;

        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    public void receiveMqttData_sendAPI(String clientId) throws MqttException {

        String broker = "tcp://mqtt.eclipseprojects.io";
        String topicBia = "unisalento/sarcopenia/sensorData";
        String username = "user1";
        String password = "pass1";
        int qos = 0;

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

            public void messageArrived(String topic, MqttMessage message){
                data = new String(message.getPayload());
                arrivedDataText.setText(data);

                // send JSON sensor data + ID user (REST API)
                try{
                    JSONObject JSONdata = new JSONObject(data);
                    JSONdata.put("ID", ID);
                    String url = "http://10.0.2.2:5000/api/senddata";   // e.g. "https://catfact.ninja/fact";
                    sendAPIData(JSONdata, url);
                } catch (JSONException e){
                    Log.d("JSON", "error: " + e);
                }


                // Log.d("MQTT", "topic: " + topic);
                // Log.d("MQTT", "Qos: " + message.getQos());
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

    public void disconnectMqtt(String clientId) throws MqttException {
        assert client != null;
        client.disconnect();
        client.close();
    }

    @SuppressLint("SetTextI18n")
    public void sendAPIData(JSONObject data , String url) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(requireContext());
        // Request a string response from the provided URL.
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, data,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
                        textViewTest.setText("Dati inviati correttamente tramite REST API");
                        Log.d("REST API", String.valueOf(response));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // textViewTest.setText("That didn't work! ");
                Log.d("REST API", String.valueOf(error));
            }
        });
        // Add the request to the RequestQueue.
        queue.add(postRequest);
    }


    /*
    public void receiveAPImodel(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(requireContext());
        String url = "http://10.0.2.2:5000/receivemodel";

        // Request a string response from the provided URL.
        StringRequest postRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @SuppressLint("WorldReadableFiles")
                    @Override
                    public void onResponse(String response) {
                        FileOutputStream outputStream;
                        try {
                            // data saved to /data/data/it.unisalento.iot2122.sarcopenia1/files/model.pmml
                            Log.d("REST API", "ML Model Received");
                            outputStream = requireContext().openFileOutput("model.pmml", Context.MODE_PRIVATE);
                            outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                            outputStream.close();
                            Toast.makeText(getContext(), "File Saved", Toast.LENGTH_LONG).show();

                        } catch (IOException e) {
                            Log.d("FILE", "Error when saving the file");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // textViewTest.setText("That didn't work! ");
                Log.d("REST API", String.valueOf(error));
            }
        });

        // Add the request to the RequestQueue.
        queue.add(postRequest);
    }
     */




}