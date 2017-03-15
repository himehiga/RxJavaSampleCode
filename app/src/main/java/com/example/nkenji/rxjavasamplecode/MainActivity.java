package com.example.nkenji.rxjavasamplecode;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    //生産者(Flowable / Observable)
    //消費者(Subscriber / Observer)
    //生産者と消費者で共有される(Subscription / Disposable)

    /*Flowable/ObservbleはReactive StreamsはObservable契約に従わないと正しくデータが通知されることが保証されない
        通知する際のルール
            ・nullを通知してはならない(RxJava2.0以降のみ)
            ・データの通知は行っても行わなくてもよい
            ・Flowable / Observableの処理を終了する際は完了かエラーの通知のどちらか一方を行わないといけない。両方を通知することはない。
            ・完了かエラーの通知をした後は他の通知を行ってはいけない
            ・通知をする際は一つずつ順番に行い同時に行ってはいけない
                →Flowable/Observableの処理を一つのスレッドで行っていれば問題ないが、複数のスレッドから通知処理を行えば、通知は保証されない
    */

    // onSubscribe中のrequestメソッド //
    /*
        onSubscribでrequestメソッドを呼ぶと通知処理が始まってしまう。
        onSubscribeメソッド内で初期化処理があるなら、requestメソッドを呼ぶ前に行う必要がある
     */

    //CompositeDisposable
    //複数のDisposableをまとめることでCompositeDisposableのdisposableのメソッドを呼ぶことで保持している全てのDisposableのdisposeメソッドを呼ぶことができる

    /*
    * 他の生産者/消費者
    * Single / SinbleObserver:      データを1件だけ通知 or エラーを通知
    * Maybe  / MaybeObserver:       データを1件だけ通知 or １件も通知せず完了を通知 or エラーを通知
    * Completable / CompletableObserver:  データを1件も通知せず完了を通知するか、もしくはエラーを通知する
    * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    private void callSingle(){
        String[] datas = {"Hello", "World"};

        Single<String> single = Single.create(emitter -> {
            emitter.onSuccess(datas[0]);
        });


        Single<String> singleTest = Single.create(emitter -> {
            emitter.onSuccess(datas[0]);
        });
        singleTest.subscribe(new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(String s) {

            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    private void callFlowable(){
        //Flowable
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
                //subscribeメソッド: 生産者がデータを通知する消費者を登録する
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

    private void callObservable(){
        //Observable
        Observable<String> observable = Observable.create(emitter -> {
            String[]  datas = {"Hello", "World"};

            for (String data: datas) {
                //FlowableならisCancelled
                if(emitter.isDisposed()) {
                    return;
                }

                emitter.onNext(data);
            }

            emitter.onComplete();
        });

        //ラムダ式で書くと、overrideするメソッドの順番が決まっている？？
        //どのoverrideで呼ばれているかぱっと見わからない？？
        observable.observeOn(Schedulers.computation())
                .subscribe(
                        //onNext
                        (item) -> {
                            String threadName = Thread.currentThread().getName();
                            System.out.println(threadName + ": " + item);
                        },
                        //onError
                        (error) -> {
                            error.printStackTrace();
                        },
                        //onComplete
                        () -> {
                            String threadName = Thread.currentThread().getName();
                            System.out.println(threadName + ": Complete");
                        },
                        //onSubscribe
                        (disposable) -> {
                        }
                );
    }

    private void callCompositDisposable(){
        CompositeDisposable compositeDisposable = new CompositeDisposable();
    }

}
