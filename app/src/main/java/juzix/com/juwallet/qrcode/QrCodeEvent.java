package juzix.com.juwallet.qrcode;

/**
 * Created by 徐敏 on 2018/1/3.
 */

public class QrCodeEvent {
    private String result;

    public QrCodeEvent(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
