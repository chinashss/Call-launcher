package com.holoview.hololauncher.basic;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.reflect.ParameterizedType;

/**
 * Created by Administrator on 2017/6/26 0026.
 */

public abstract class BaseActivity<V extends BaseView, T extends BasePresenterImpl<V>> extends AppCompatActivity implements BaseView {
    public T mPresenter;
    protected final String activityName = this.getClass().getSimpleName();
    protected final String TAG = "activityLife";
    public MaterialDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
//            getWindow().setEnterTransition(new Slide().setDuration(400));
//            getWindow().setExitTransition(new Slide().setDuration(400));
//        }
        super.onCreate(savedInstanceState);
        DuckLog.life(activityName + ":onCreate");
        mPresenter = getInstance(this, 1);
        mPresenter.attachView((V) this);
        ActivityCollector.addActivity(this);


        //  lifecycleSubject.onNext(ActivityEvent.CREATE);
    }


    @Override
    protected void onStart() {
        super.onStart();
        //  lifecycleSubject.onNext(ActivityEvent.START);
        DuckLog.life(activityName + ":onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //lifecycleSubject.onNext(ActivityEvent.RESUME);
        DuckLog.life(activityName + ":onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        //  lifecycleSubject.onNext(ActivityEvent.PAUSE);
        DuckLog.life(activityName + ":onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // lifecycleSubject.onNext(ActivityEvent.STOP);
        DuckLog.life(activityName + ":onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        DuckLog.life(activityName + ":onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DuckLog.life(activityName + ":onDestroy");
        ActivityCollector.removeActivity(this);
        //  lifecycleSubject.onNext(ActivityEvent.DESTROY);
        if (mPresenter != null)
            mPresenter.detachView();
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public Context getContext() {
        return this;
    }

    public <T> T getInstance(Object o, int i) {
        try {
            return ((Class<T>) ((ParameterizedType) (o.getClass()
                    .getGenericSuperclass())).getActualTypeArguments()[i])
                    .newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showDialog(String content, String positiveText, String negativeText, MaterialDialog.SingleButtonCallback callback) {
        new MaterialDialog.Builder(this)
                .content(content)
                .positiveText(positiveText)
                .negativeText("取消")
                .onAny(callback)
                .show();
    }

    @Override
    public void showDialog(String content, String positiveText, MaterialDialog.SingleButtonCallback callback) {
        new MaterialDialog.Builder(this)
                .content(content)
                .positiveText(positiveText)
                .onAny(callback)
                .show();
    }


    @Override
    public MaterialDialog showProgressDialog(String title, String content) {
        return new MaterialDialog.Builder(this)
                .title(title)
                .content(content)
                .progress(true, 0)
                .show();
    }

//    @Override
//    public void startActivity(Intent intent) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
//            return;
//        }
//        super.startActivity(intent);
//    }
}
