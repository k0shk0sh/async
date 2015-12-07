package com.afollestad.asyncsample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Spanned;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.async.Action;
import com.afollestad.async.Async;
import com.afollestad.async.Base;
import com.afollestad.async.Done;
import com.afollestad.async.Pool;
import com.afollestad.async.Result;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements Done {

    @Bind(R.id.log)
    TextView mLog;
    @Bind(R.id.pushAction)
    Button mPushAction;

    private Pool mPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Async.setLogRelay(new Base.LogRelay() {
            @Override
            public void onRelay(final Spanned message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLog.append(message);
                        mLog.append("\n");
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    private Action[] getActions() {
        return new Action[]{
                new Action<String>() {
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
                        mLog.append(String.format("Action %d (%s) is done! %s\n", getPoolIndex(), id(), result));
                    }
                },
                new Action<String>() {
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
                        mLog.append(String.format("Action %d (%s) is done! %s\n", getPoolIndex(), id(), result));
                    }
                }
        };
    }

    @OnClick(R.id.executeParallel)
    public void onClickExecuteParallel() {
        mLog.setText("");
        mPushAction.setEnabled(true);
        mPool = Async.parallel(getActions()).done(this);
    }

    @OnClick(R.id.executeSeries)
    public void onClickExecuteSeries() {
        mLog.setText("");
        mPushAction.setEnabled(true);
        mPool = Async.series(getActions()).done(this);
    }

    @Override
    public void result(@NonNull Result result) {
        mLog.append(String.format("\n%s%s\n",
                // Prints out Action.toString(), which includes ID and Result
                result.get("one"), result.get("two")));
        mPushAction.setEnabled(false);
    }

    @OnClick(R.id.pushAction)
    public void onClickPushAction() {
        mPool.push(new Action() {
            @NonNull
            @Override
            public String id() {
                return "pushed_action";
            }

            @Nullable
            @Override
            protected Object run() throws InterruptedException {
                Thread.sleep(2000);
                return null;
            }
        });
    }

    @OnClick(R.id.cancelAll)
    public void onClickCancelAll() {
        mPool = null;
        Async.cancelAll();
    }
}