package com.afollestad.asyncsample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Spanned;
import android.widget.TextView;

import com.afollestad.async.Action;
import com.afollestad.async.Async;
import com.afollestad.async.Base;
import com.afollestad.async.Done;
import com.afollestad.async.Result;

public class MainActivity extends AppCompatActivity {

    private TextView mLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLog = (TextView) findViewById(R.id.log);

        Async.setLogRelay(new Base.LogRelay() {
            @Override
            public void onRelay(Spanned message) {
                mLog.append(message);
                mLog.append("\n");
            }
        });

        Action<String> one = new Action<String>() {
            @NonNull
            @Override
            public String id() {
                return "one";
            }

            @Nullable
            @Override
            protected String run() throws InterruptedException {
                Thread.sleep(6000);
                return "Hello, ";
            }

            @Override
            protected void done(String result) {
                mLog.append(String.format("Action %d (%s) is done! %s\n", index(), id(), result));
            }
        };
        Action<String> two = new Action<String>() {
            @NonNull
            @Override
            public String id() {
                return "two";
            }

            @Nullable
            @Override
            protected String run() throws InterruptedException {
                Thread.sleep(3000);
                return "how are you?";
            }

            @Override
            protected void done(String result) {
                mLog.append(String.format("Action %d (%s) is done! %s\n", index(), id(), result));
            }
        };

        Async.parallel(one, two)
                .done(new Done() {
                    @Override
                    public void result(@NonNull Result result) {
                        mLog.append(String.format("\n%s%s\n", result.get("one"), result.get("two")));
                    }
                });
    }
}