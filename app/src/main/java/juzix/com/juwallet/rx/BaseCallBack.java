package juzix.com.juwallet.rx;

/**
 * Created by 徐敏 on 2018/1/5.
 */

public abstract class BaseCallBack<T> {
    protected abstract void onStart();
    protected abstract void onSuccess(T t);
    protected abstract void onFailed(Throwable e);
    protected abstract void onCompleted();
}
