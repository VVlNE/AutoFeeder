package ru.vvine.autofeeder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import ru.vvine.autofeeder.MQTTSender;

import java.io.UnsupportedEncodingException;
import java.sql.Time;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button settingsButton, feedingCatButton;
    SaveInPref saveInPref;
    RecyclerView feedingTimeList;
    ArrayList<String> feedingTimes;
    FeedingTimeAdapter adapter;
    MQTTSender mqttSender;
    TextView lastFeeding, foodLeft, hasWater;
    String feedingTimesStr;

    //получаем из "catName"/Food/out
    String lastFeedingTime = "—";
    String foodLeftNumber = "—";
    boolean hasWaterFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        saveInPref = new SaveInPref(getApplicationContext());

        feedingTimeList = (RecyclerView)findViewById(R.id.feedingTimeList);
        lastFeeding = (TextView)findViewById(R.id.lastFeeding);
        foodLeft = (TextView)findViewById(R.id.foodLeft);
        hasWater = (TextView)findViewById(R.id.hasWater);

        if (saveInPref.readName("catName")!= null && saveInPref.readName("catName").length() > 0) {
            ((AppCompatActivity) MainActivity.this).getSupportActionBar().setTitle(saveInPref.readName("catName"));
        }
        else {
            ((AppCompatActivity) MainActivity.this).getSupportActionBar().setTitle("AutoFeeder");
        }

        if (saveInPref.readFeedingTimes("feedingTimes") != null) {
            feedingTimes = new ArrayList<String>(saveInPref.readFeedingTimes("feedingTimes"));
            adapter = new FeedingTimeAdapter(this, feedingTimes);
            feedingTimeList.setAdapter(adapter);
        }

        if (saveInPref.readName("lastFeedingTime")!= null && saveInPref.readName("lastFeedingTime").length() > 0) {
            lastFeedingTime = saveInPref.readName("lastFeedingTime");
        }

        if (saveInPref.readName("foodLeftNumber") != null) {
            if (saveInPref.readName("foodLeftNumber").length() > 0) {
                foodLeftNumber = saveInPref.readName("foodLeftNumber");
            }
        }

        setFoodComments();
        setWaterComment();

        View.OnClickListener onClickListener = new View.OnClickListener() {
            Intent intent;
            @Override
            public void onClick(View view){
                switch(view.getId()) {
                    case R.id.settingsButton:
                        intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.feedingCatButton:
                        if (saveInPref.readName("catName")!= null && saveInPref.readName("catName").length() > 0) {
                            mqttSender.publishMessage((saveInPref.readName("catName")) + "/Food/in", "Food");
                        }
                        else {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Введите имя кота в настройках!", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        settingsButton = (Button)findViewById(R.id.settingsButton);
        feedingCatButton = (Button)findViewById(R.id.feedingCatButton);
        settingsButton.setOnClickListener(onClickListener);
        feedingCatButton.setOnClickListener(onClickListener);

        startMqtt("tcp://3.67.237.255",saveInPref.readName("catName"));
//        mqttSender.publishMessage("Kekdddd", "BestTest");
    }

    private void setFoodComments() {
        hasWaterFlag = saveInPref.readFlag("hasWater");

        lastFeeding.setText("Последнее кормление в " + lastFeedingTime);
        foodLeft.setText("Осталось " + foodLeftNumber + " грамм еды");
    }

    private void setWaterComment() {
        if (hasWaterFlag) {
            hasWater.setText("В поилке есть вода");
        }
        else {
            hasWater.setText("В поилке нет воды!");
        }
    }

    private void startMqtt(String serverURI, String mainTopic){
        mqttSender = new MQTTSender(getApplicationContext(), serverURI, "higjuirgh4g8gu438gg438", mainTopic+"/#");
        mqttSender.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
//              mqttSender.publishMessage("RRRRRRRRRRRR", "BestTest");

                System.out.println("What +");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("What -");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                System.out.println("Hey "+ topic+" " +mqttMessage.toString());
                if(topic.contains("/Food/out")){
                    System.out.println("111 "+ topic+" " +mqttMessage.toString());
                    JSONObject jObject = new JSONObject(mqttMessage.toString());
                    String date = jObject.getString("date");
                    String time = jObject.getString("time");
                    String foodLeft = jObject.getString("foodLeft");

                    saveInPref.saveName("lastFeedingTime", time);
                    saveInPref.saveName("foodLeftNumber", foodLeft);

                    lastFeedingTime = time;
                    foodLeftNumber = foodLeft;

                    setFoodComments();

                    System.out.println(date+" "+time+" "+foodLeft);
                }
                else if(topic.contains("/Water")){
                    System.out.println("222 "+ topic+" " +mqttMessage.toString());
                    if (mqttMessage.toString().equals("true")) {
                        saveInPref.saveFlag("hasWaterFlag", true);
                        hasWaterFlag = true;
                    }
                    else {
                        saveInPref.saveFlag("hasWaterFlag", false);
                        hasWaterFlag = false;
                    }

                    setWaterComment();
                }
                else if(topic.contains("/Water")){
                    System.out.println("222 "+ topic+" " +mqttMessage.toString());
                    if (mqttMessage.toString().equals("true")) {
                        saveInPref.saveFlag("hasWaterFlag", true);
                        hasWaterFlag = true;
                    }
                    else {
                        saveInPref.saveFlag("hasWaterFlag", false);
                        hasWaterFlag = false;
                    }

                    setWaterComment();
                }
                else{
                    System.out.println("333 "+ topic+" " +mqttMessage.toString());
//                    Toast toast = Toast.makeText(getApplicationContext(),
//                            "Не происходит подача еды, проверьте кормушку!", Toast.LENGTH_SHORT);
//                    toast.show();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("What $");
            }

        });
        mqttSender.connect();
    }

}