package juzix.com.juwallet.rx;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by 徐敏 on 2018/1/5.
 */

public  class BaseOberver<T> implements Observer<T> {
    protected BaseCallBack<T> callBack;
    private Disposable disposable;
    public BaseOberver(BaseCallBack<T> callBack) {
        this.callBack = callBack;
    }


    @Override
    public void onSubscribe(Disposable d) {
        this.disposable=d;
        callBack.onStart();
        //如果是网络请求的 可以在这里做网络判断，如果网络没有连接 直接走onComplete或者onError
    }

    @Override
    public void onNext(T response) {
        callBack.onSuccess(response);
    }

    @Override
    public void onError(Throwable e) {
        //这里可以用ApiException代替,callBack.onFailed(ApiException e),ApiException处理这个异常。
        callBack.onFailed(e);
        //取消订阅
        if(disposable!=null&&!disposable.isDisposed()){
            disposable.dispose();
        }
    }

    @Override
    public void onComplete() {
        callBack.onCompleted();
        if(disposable!=null&&!disposable.isDisposed()){
            disposable.dispose();
        }
    }
}
