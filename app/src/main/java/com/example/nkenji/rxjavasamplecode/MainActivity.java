package com.example.nkenji.rxjavasamplecode;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Flowable<String> flowable = Flowable.create(emitter -> {
            String[] datas = {"Hello", "World"};
            for (String data: datas) {
                //RxJava2.0から購読解除された場合、onNextなどの通知メソッドを呼んでも通知されない
                //createメソッドの場合、購読解除後も実装者責任
                if (emitter.isCancelled()) {
                    return;
                }

                //nullを渡すとNPEとなる
                emitter.onNext(data);
            }
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER);

        //observeOn: 実行するスレッドの指定
        flowable.observeOn(Schedulers.computation())
                .subscribe(new Subscriber<String>() {

                    private Subscription subscription;

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        //SubscriptionをSubscriber内でうけとる
                        this.subscription = subscription;
                        //受け取るデータ数をリクエストする
                        this.subscription.request(1L);
                        //データ数を制限することなく、データを通知するようにリクエストする
                        //this.subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(String s) {
                        String threadName = Thread.currentThread().getName();
                        System.out.println(threadName + ": " + s);

                        //次に受け取るデータ数をリクエストする
                        this.subscription.request(1L);
                    }

                    //onErrorメソッドの後、onNextなどの通知メソッドが呼ばれなくなる
                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        String threadName = Thread.currentThread().getName();
                        System.out.println(threadName + ": Complete");
                    }
                });
    }
}
