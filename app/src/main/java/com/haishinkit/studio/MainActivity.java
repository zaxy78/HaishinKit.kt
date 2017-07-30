package com.haishinkit.studio;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.haishinkit.events.Event;
import com.haishinkit.events.IEventListener;
import com.haishinkit.rtmp.RTMPConnection;
import android.widget.TextView;

import com.haishinkit.rtmp.RTMPConnection;
import com.haishinkit.rtmp.RTMPStream;
import com.haishinkit.util.EventUtils;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private RTMPConnection connection;
    private RTMPStream stream;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        connection = new RTMPConnection();
        stream = new RTMPStream(connection);
        connection.addEventListener("rtmpStatus", new IEventListener() {
            @Override
            public void handleEvent(Event event) {
                Map<String, Object> data = EventUtils.INSTANCE.toMap(event);
                String code = data.get("code").toString();
                if (code.equals(RTMPConnection.Codes.CONNECT_SUCCESS.getRawValue())) {
                    stream.publish("live");
                }
            }
        });
        connection.connect("rtmp://192.168.11.19/live");

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }
}
