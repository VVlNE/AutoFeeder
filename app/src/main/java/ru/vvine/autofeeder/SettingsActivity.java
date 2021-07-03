package ru.vvine.autofeeder;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    Button saveButton, cancelButton;
    ImageButton addTimeButton;
    EditText editCatName;
    RecyclerView feedingTimeList;
    SaveInPref saveInPref;
    ArrayList<String> feedingTimes;
    EditFeedingTimeAdapter adapter;

    MQTTSender mqttSender;

    int DIALOG_TIME = 1;
    int feedingHour = -1;
    int feedingMinute = -1;
    int POSITION = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editCatName = (EditText) findViewById(R.id.editCatName);
        saveInPref = new SaveInPref(getApplicationContext());
        editCatName.setText(saveInPref.readName("catName"));
        if (saveInPref.readFeedingTimes("feedingTimes")!= null) {
            feedingTimes = new ArrayList<String>(saveInPref.readFeedingTimes("feedingTimes"));
        }
        else {
            feedingTimes = new ArrayList<String>();
            feedingTimes.add("");
        }

        if (feedingTimes.get(0) == "") {
            feedingTimes.remove(0);
        }

        feedingTimeList = (RecyclerView) findViewById(R.id.editFeedingTimeList);
        EditFeedingTimeAdapter.OnFeedingTimeClickListener feedingTimeClickListener = new EditFeedingTimeAdapter.OnFeedingTimeClickListener() {
            @Override
            public void onFeedingTimeClick(String feedingTime, int position) {
                POSITION = position;
                showDialog(DIALOG_TIME);
            }

            @Override
            public void onDeleteFeedingTimeClick(String feedingTime, int position) {
                deleteFeedingTime(position);
            }
        };
        adapter = new EditFeedingTimeAdapter(this, feedingTimes, feedingTimeClickListener);
        feedingTimeList.setAdapter(adapter);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            Intent intent;
            @Override
            public void onClick(View view){
                switch(view.getId()) {
                    case R.id.saveButton:
                        if (saveSettings(editCatName.getText().toString())) {
                            String str="";
                            for(int i=0; i<feedingTimes.size(); i++){
                                str += feedingTimes.get(i)+"T";
                            }
                            System.out.println(str);
                            mqttSender.publishMessage(saveInPref.readName("catName")+"/Food/Time",str);
                            intent = new Intent(SettingsActivity.this, MainActivity.class);
                            intent.putExtra("feedingTimes",str);
                            startActivity(intent);
                        }
                        break;
                    case R.id.cancelButton:
                        intent = new Intent(SettingsActivity.this, MainActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.addTimeButton:
                        showDialog(DIALOG_TIME);
                        break;
                    default:
                        startActivity(intent);
                        break;
                }
            }
        };
        saveButton = (Button) findViewById(R.id.saveButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        addTimeButton  = (ImageButton) findViewById(R.id.addTimeButton);
        saveButton.setOnClickListener(onClickListener);
        cancelButton.setOnClickListener(onClickListener);
        addTimeButton.setOnClickListener(onClickListener);
        startMqtt("tcp://3.67.237.255",saveInPref.readName("catName"));
    }

    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_TIME) {
            return new TimePickerDialog(this, R.style.TimePickerDialogTheme, feedingTimeCallBack, feedingHour, feedingMinute, true);
        }
        return super.onCreateDialog(id);
    }

    TimePickerDialog.OnTimeSetListener feedingTimeCallBack = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            if ((hourOfDay != feedingHour) || (minute != feedingMinute)) {
                feedingHour = hourOfDay;
                feedingMinute = minute;
                String time = "";
                if (hourOfDay < 10) {
                    time = "0";
                }
                time += hourOfDay + ":";
                if (minute < 10) {
                    time += "0";
                }
                time += minute;
                if (!feedingTimes.contains(time)) {
                    if (POSITION >= 0) {
                        feedingTimes.set(POSITION, time);
                        POSITION = -1;
                    }
                    else {
                        feedingTimes.add(time);
                    }
                }
                sortFeedingTimes();
                feedingTimeList.setAdapter(adapter);
            }
        }
    };

    private void sortFeedingTimes() {
        for (int i = 0; i < feedingTimes.size(); i++) {
            for (int j = i + 1; j < feedingTimes.size(); j++) {
                feedingTimeSwap(i, j, 0);
            }
        }
    }

    private void feedingTimeSwap(int i, int j, int k) {
        if (feedingTimes.get(i).charAt(k) > feedingTimes.get(j).charAt(k)) {
            feedingTimes.add(i, feedingTimes.remove(j));
            feedingTimes.add(j, feedingTimes.remove(i + 1));
        }
        else if ((feedingTimes.get(i).charAt(k) == feedingTimes.get(j).charAt(k)) && (k < 5)) {
            feedingTimeSwap(i, j, (k + 1));
        }
    }

    private void deleteFeedingTime(int position) {
        feedingTimes.remove(position);
        feedingTimeList.setAdapter(adapter);
    }

    private boolean saveSettings(String catName) {
        try {
            for (int i = 0; i < catName.length(); i++) {
                int num = (int)catName.charAt(i);
                if (!(((num >= 48) && (num <= 57)) || ((num >= 65) && (num <= 90)) || ((num >= 97) && (num <= 122)))) {
                    throw new Exception(" Можно использовать только цифры и латинницу!");
                }
            }
            saveInPref.saveName("catName",catName);
            saveInPref.saveFeedingTimes("feedingTimes", feedingTimes);
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    ("Некорректное имя кота!" + e.getMessage()), Toast.LENGTH_SHORT);
            toast.show();
            return false;
        }
        return true;
    }

    private void startMqtt(String serverURI, String mainTopic){
        mqttSender = new MQTTSender(getApplicationContext(), serverURI, "vn348nv834gvudsjv843", mainTopic+"/#");
        mqttSender.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
            }

            @Override
            public void connectionLost(Throwable throwable) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("What $");
            }

        });
        mqttSender.connect();
    }
}
