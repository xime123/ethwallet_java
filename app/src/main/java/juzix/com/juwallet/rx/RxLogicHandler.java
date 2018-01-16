package juzix.com.juwallet.rx;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by 徐敏 on 2018/1/5.
 */

public class RxLogicHandler {
    public static <T> void doWork(final Excutor excutor,BaseCallBack<T> callBack){
        Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            public void subscribe(ObservableEmitter<T> e) throws Exception {
                T t=(T)excutor.excute();
                e.onNext(t);
                e.onComplete();
            }
        }).subscribe(new BaseOberver<T>(callBack));
    }

    public interface Excutor<Result>{
        Result excute()throws Exception;
    }
}
