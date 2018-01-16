package juzix.com.juwallet;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.CallBack;
import com.zhouyou.http.exception.ApiException;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.greenrobot.event.EventBus;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import juzix.com.juwallet.permission.PermisionsConstant;
import juzix.com.juwallet.permission.PermissionsManager;
import juzix.com.juwallet.permission.PermissionsResultAction;
import juzix.com.juwallet.qrcode.QrCodeEvent;
import juzix.com.juwallet.qrcode.QrcodeActivity;
import juzix.com.juwallet.qrcode.QrcodeGen;
import juzix.com.juwallet.rx.RxLogicHandler;
import juzix.com.juwallet.rx.SimpleCallBack;


public class MainActivity extends AppCompatActivity {

    private Credentials credentials;
    private String fromAddress;
    private String toAddress;
    private ImageView qrcodeIv;
    private TextView infoTv,errorTv;
    private Dialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        qrcodeIv=findViewById(R.id.qrcode_iv);
        infoTv=findViewById(R.id.wallet_info_tv);
        errorTv=findViewById(R.id.error_info_tv);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void init(){
        loadWalletFile();
        if(!TextUtils.isEmpty(this.getFromAddress())){
            qrcodeIv.setImageBitmap(QrcodeGen.genQrcodeBitmap(150,this.getFromAddress()));
        }
    }

    /**
     * 扫码转账
     * @param view
     */
    public void send(View view) {
        Intent intent=new Intent(this, QrcodeActivity.class);
        startActivity(intent);
    }

    /**
     * 加载钱包
     * @param view
     */
    public void loadWallet(View view) {
        RxLogicHandler.<String>doWork(new GetBalanceExcutor(),new GetBalanceCallBack());
    }

    /**
     * 查询账户余额
     */
    class GetBalanceExcutor implements RxLogicHandler.Excutor<String>{

        @Override
        public String excute() throws Exception {
            Web3j web3j = Web3jFactory.build(new HttpService(Constant.WEB_URL));  // defaults to http://localhost:8545/
            EthGetBalance ethGetBalance=  web3j.ethGetBalance(getFromAddress(), new DefaultBlockParameter() {
                @Override
                public String getValue() {
                    return "latest";
                }
            }).sendAsync().get();
            BigInteger bigInteger=ethGetBalance.getBalance();
            return bigInteger+"";
        }
    }

    /**
     * 查询账户余额回调处理
     */
    class  GetBalanceCallBack extends SimpleCallBack<String>{

        @Override
        protected void onSuccess(String s) {
            showInfoSafe(s);
        }

        @Override
        protected void onFailed(Throwable e) {
            showErrorInfoSafe("查询余额失败:"+e.getMessage() +"   "+e.getCause());
        }
    }

    /**
     * 创建钱包
     * @param view
     */
    public void createWallet(View view) {
          Credentials credentials=  MyWalletUtil.createWallet();
          if(credentials==null){
              showSafeToast("创建钱包失败!");
              return;
          }
          this.setCredentials(credentials);
          this.setFromAddress(credentials.getAddress());
        if(!TextUtils.isEmpty(this.getFromAddress())){
            qrcodeIv.setImageBitmap(QrcodeGen.genQrcodeBitmap(150,this.getFromAddress()));
        }
    }



    /**
     * 转账
     */
    private void sendTransaction(){

        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                Web3j web3j = Web3jFactory.build(new HttpService(Constant.WEB_URL));  // defaults to http://localhost:8545/
                TransactionReceipt transactionReceipt = Transfer.sendFunds(web3j, credentials, getToAddress(), BigDecimal.valueOf(1.0), Convert.Unit.ETHER).send();

                String hash=transactionReceipt.getTransactionHash();
                EthGetTransactionReceipt ethGetTransactionReceipt =
                    web3j.ethGetTransactionReceipt(hash).sendAsync().get();
                String info="hash="+hash+" \nvalue="+BigDecimal.valueOf(1.0)+"\ntoAddress="+getToAddress()+"\nformAddress="+getFromAddress();
                e.onNext(info);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                showDialog("正在交易...");
            }

            @Override
            public void onNext(String s) {
                showErrorInfoSafe(s);
            }

            @Override
            public void onError(Throwable e) {
                showErrorInfoSafe("交易失败:"+e.getMessage()+"  cause="+e.getCause());
            }

            @Override
            public void onComplete() {
                dismissDialog();
            }
        });

    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    private void showSafeToast(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showErrorInfoSafe(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                errorTv.setText(msg);
            }
        });
    }
    /**
     * 收到对端钱包地址 开始转账
     * @param event
     */
    public void onEventMainThread(QrCodeEvent event) {
        String addres=event.getResult();
        this.setToAddress(addres);
        sendTransaction();

    }


    private void loadWalletFile(){
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, new String[]{PermisionsConstant.WRITE_EXTERNAL_STORAGE, PermisionsConstant.READ_EXTERNAL_STORAGE,PermisionsConstant.CAMERA}, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                try {
                    String source=AppFilePath.Wallet_DIR+Constant.WALLET_FILE_NAME;
                    Credentials credentials = WalletUtils.loadCredentials(Constant.PASSWORD, source);
                    MainActivity.this.setFromAddress(credentials.getAddress());
                    MainActivity.this.setCredentials(credentials);

                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,"加载钱包失败",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onDenied(String permission) {
                finish();
            }
        });
    }

    private void showInfoSafe(final String value){
        String info="钱包地址："+getFromAddress()+"\n"+"钱包余额："+value;

        infoTv.setText(info);
    }

    private void showDialog(String msg){
        if(dialog==null) {
            dialog = new ProgressDialog(this);
        }
        dialog.show();
    }

    private void dismissDialog(){
        if(dialog!=null&&dialog.isShowing()){
            dialog.dismiss();
        }
    }

    /**
     * 测试查询交易记录
     * @param view
     */
    public void testHis(View view) {
        EasyHttp.getInstance().setCertificates();
        EasyHttp.get("/address/0x2de128a62cadc32097b5719807c16db2024a08da")
            .baseUrl("https://etherscan.io")
            .execute(new CallBack<String>() {
                @Override
                public void onStart() {

                }

                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(ApiException e) {

                }

                @Override
                public void onSuccess(String o) {

                }
            });
    }
}
