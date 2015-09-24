package com.uxxu.konashi.lib;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import com.uxxu.konashi.lib.action.AioAnalogReadAction;
import com.uxxu.konashi.lib.action.BatteryLevelReadAction;
import com.uxxu.konashi.lib.action.HardwareResetAction;
import com.uxxu.konashi.lib.action.I2cModeAction;
import com.uxxu.konashi.lib.action.I2cReadAction;
import com.uxxu.konashi.lib.action.I2cSetReadParamAction;
import com.uxxu.konashi.lib.action.I2cSendConditionAction;
import com.uxxu.konashi.lib.action.I2cWriteAction;
import com.uxxu.konashi.lib.action.PioDigitalWriteAction;
import com.uxxu.konashi.lib.action.PioPinModeAction;
import com.uxxu.konashi.lib.action.PioPinPullupAction;
import com.uxxu.konashi.lib.action.PwmDutyAction;
import com.uxxu.konashi.lib.action.PwmLedDriveAction;
import com.uxxu.konashi.lib.action.PwmPeriodAction;
import com.uxxu.konashi.lib.action.PwmPinModeAction;
import com.uxxu.konashi.lib.action.UartBaudrateAction;
import com.uxxu.konashi.lib.action.UartModeAction;
import com.uxxu.konashi.lib.action.UartWriteAction;
import com.uxxu.konashi.lib.dispatcher.AioStoreUpdater;
import com.uxxu.konashi.lib.dispatcher.CharacteristicDispatcher;
import com.uxxu.konashi.lib.dispatcher.DispatcherContainer;
import com.uxxu.konashi.lib.dispatcher.I2cStoreUpdater;
import com.uxxu.konashi.lib.dispatcher.PioStoreUpdater;
import com.uxxu.konashi.lib.dispatcher.PwmStoreUpdater;
import com.uxxu.konashi.lib.dispatcher.UartStoreUpdater;
import com.uxxu.konashi.lib.filter.AioAnalogReadFilter;
import com.uxxu.konashi.lib.filter.BatteryLevelReadFilter;
import com.uxxu.konashi.lib.filter.I2cReadFilter;
import com.uxxu.konashi.lib.listeners.KonashiBaseListener;
import com.uxxu.konashi.lib.stores.AioStore;
import com.uxxu.konashi.lib.stores.I2cStore;
import com.uxxu.konashi.lib.stores.PioStore;
import com.uxxu.konashi.lib.stores.PwmStore;
import com.uxxu.konashi.lib.stores.UartStore;
import com.uxxu.konashi.lib.util.AioUtils;

import org.jdeferred.DoneCallback;
import org.jdeferred.DonePipe;
import org.jdeferred.Promise;

import java.util.List;
import java.util.UUID;

import info.izumin.android.bletia.Bletia;
import info.izumin.android.bletia.BletiaException;
import info.izumin.android.bletia.action.Action;


/**
 * konashiを管理するメインクラス
 * 
 * @author monakaz, YUKAI Engineering
 * http://konashi.ux-xu.com
 * ========================================================================
 * Copyright 2014 Yukai Engineering Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
public class KonashiManager extends KonashiBaseManager {
    // konashi members
    // PIO
    private PioStore mPioStore;
    private CharacteristicDispatcher<PioStore, PioStoreUpdater> mPioDispatcher;

    // PWM
    private PwmStore mPwmStore;
    private CharacteristicDispatcher<PwmStore, PwmStoreUpdater> mPwmDispatcher;

    // AIO
    private AioStore mAioStore;
    private CharacteristicDispatcher<AioStore, AioStoreUpdater> mAioDispatcher;

    // I2C
    private I2cStore mI2cStore;
    private CharacteristicDispatcher<I2cStore, I2cStoreUpdater> mI2cDispatcher;

    // UART
    private UartStore mUartStore;
    private CharacteristicDispatcher<UartStore, UartStoreUpdater> mUartDispatcher;

    private Bletia mBletia;
    private EventEmitter mEmitter;
    private CallbackHandler mCallbackHandler;
    private DispatcherContainer mDispacherContainer;

    ///////////////////////////
    // Initialization
    ///////////////////////////
    
    private void initializeMembers(){
        // PIO
        mPioDispatcher = mDispacherContainer.getPioDispatcher();
        mPioStore = new PioStore(mPioDispatcher);

        // PWM
        mPwmDispatcher = mDispacherContainer.getPwmDispatcher();
        mPwmStore = new PwmStore(mPwmDispatcher);

        // AIO
        mAioDispatcher = mDispacherContainer.getAioDispatcher();
        mAioStore = new AioStore(mAioDispatcher);

        // I2C
        mI2cDispatcher = mDispacherContainer.getI2cDispatcher();
        mI2cStore = new I2cStore(mI2cDispatcher);

        // UART
        mUartDispatcher = new CharacteristicDispatcher<>(UartStoreUpdater.class);
        mUartStore = new UartStore(mUartDispatcher);
    }

    @Override
    public void initialize(Context context) {
        super.initialize(context);

        mBletia = new Bletia(context);
        mEmitter = new EventEmitter();
        mDispacherContainer = new DispatcherContainer();
        mCallbackHandler = new CallbackHandler(this, mEmitter, mDispacherContainer);
        mBletia.addListener(mCallbackHandler);

        initializeMembers();
    }


    /**
     * konashiとの接続を解除する
     */
    public void disconnect(){
        mBletia.disconenct();
    }

    ///////////////////////////
    // Observer
    ///////////////////////////


    public void addListener(KonashiListener listener) {
        mEmitter.add(listener);
    }

    public void removeListener(KonashiListener listener) {
        mEmitter.remove(listener);
    }

    /**
     * konashiのイベントのリスナーを追加する
     * @param listener 追加するリスナー
     */
    public void addListener(KonashiBaseListener listener){
        mNotifier.addListener(listener);
    }


    /**
     * 指定したリスナーを削除する
     * @param listener 削除するリスナー
     */
    public void removeListener(KonashiBaseListener listener){
        mNotifier.removeListener(listener);
    }

    /**
     * すべてのリスナーを削除する
     */
    public void removeAllListeners() {
        mNotifier.removeAllListeners();
    }


    /**
     * konashiのイベントのオブザーバを追加する
     * @param observer 追加するオブザーバ
     * @deprecated This method deprecated in 0.5.0.
     * Use {@link #addListener(KonashiBaseListener)} instead.
     */
    @Deprecated
    public void addObserver(KonashiObserver observer){
        mNotifier.addObserver(observer);
    }

    /**
     * 指定したオブザーバを削除する
     * @param observer 削除するオブザーバ
     * @deprecated This method deprecated in 0.5.0.
     * Use {@link #removeListener(KonashiBaseListener)} instead.
     */
    @Deprecated
    public void removeObserver(KonashiObserver observer){
        mNotifier.removeObserver(observer);
    }
    
    /**
     * すべてのオブザーバを削除する
     * @deprecated This method deprecated in 0.5.0.
     * Use {@link #removeAllListeners()} instead.
     */
    @Deprecated
    public void removeAllObservers(){
        mNotifier.removeAllObservers();
    }
    
    ///////////////////////////
    // PIO
    ///////////////////////////
    
    /**
     * PIOのピンを入力として使うか、出力として使うかの設定を行う
     * @param pin 設定するPIOのピン名。
     * @param mode ピンに設定するモード。INPUT か OUTPUT が設定できます。
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pinMode(int pin, int mode){
        return execute(new PioPinModeAction(getKonashiService(), pin, mode, mPioStore.getPioModes()), mPioDispatcher);
    }
    
    /**
     * PIOのピンを入力として使うか、出力として使うかの設定を行う
     * @param modes PIO0 〜 PIO7 の計8ピンの設定
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pinModeAll(int modes){
        return execute(new PioPinModeAction(getKonashiService(), modes), mPioDispatcher);
    }
    
    /**
     * PIOのピンをプルアップするかの設定を行う
     * @param pin 設定するPIOのピン名
     * @param pullup ピンをプルアップするかの設定。PULLUP か NO_PULLS が設定できます。
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pinPullup(int pin, int pullup){
        return execute(new PioPinPullupAction(getKonashiService(), pin, pullup, mPioStore.getPioPullups()), mPioDispatcher);
    }
    
    /**
     * PIOのピンをプルアップするかの設定を行う
     * @param pullups PIO0 〜 PIO7 の計8ピンのプルアップの設定
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pinPullupAll(int pullups){
        return execute(new PioPinPullupAction(getKonashiService(), pullups), mPioDispatcher);
    }
    
    /**
     * PIOの特定のピンの入力状態を取得する
     * @param pin PIOのピン名
     * @return HIGH(1) もしくは LOW(0)
     */
    public int digitalRead(int pin){
        return mPioStore.getPioInput(pin);
    }
    
    /**
     * PIOのすべてのピンの状態を取得する
     * @return PIOの状態(PIO0〜PIO7の入力状態が8bit(1byte)で表現)
     */
    public int digitalReadAll(){
        return mPioStore.getPioInputs();
    }
    
    /**
     * PIOの特定のピンの出力状態を設定する
     * @param pin 設定するPIOのピン名
     * @param output 設定するPIOの出力状態。HIGH もしくは LOW が指定可能
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> digitalWrite(int pin, int output){
        return execute(new PioDigitalWriteAction(getKonashiService(), pin, output, mPioStore.getPioOutputs()), mPioDispatcher);
    }
    
    /**
     * PIOの特定のピンの出力状態を設定する
     * @param outputs PIOの出力状態。PIO0〜PIO7の出力状態が8bit(1byte)で表現
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> digitalWriteAll(int outputs){
        return execute(new PioDigitalWriteAction(getKonashiService(), outputs), mPioDispatcher);
    }
    
    
    ///////////////////////////
    // PWM
    ///////////////////////////
    
    /**
     * PIO の指定のピンを PWM として使用する/しないかを設定する
     * @param pin PWMモードの設定をするPIOのピン番号。Konashi.PIO0 〜 Konashi.PIO7。
     * @param mode 設定するPWMのモード。Konashi.PWM_DISABLE, Konashi.PWM_ENABLE, Konashi.PWM_ENABLE_LED_MODE のいずれかをセットする。
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pwmMode(final int pin, int mode){
        Promise<BluetoothGattCharacteristic, BletiaException, Object> promise =
                execute(new PwmPinModeAction(getKonashiService(), pin, mode, mPwmStore.getPwmModes())).then(mPwmDispatcher);

        if (mode == Konashi.PWM_ENABLE_LED_MODE) {
            promise.then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Object>() {
                @Override
                public Promise<BluetoothGattCharacteristic, BletiaException, Object> pipeDone(BluetoothGattCharacteristic result) {
                    return pwmPeriod(pin, Konashi.PWM_LED_PERIOD).then(mPwmDispatcher);
                }
            }).then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Object>() {
                @Override
                public Promise<BluetoothGattCharacteristic, BletiaException, Object> pipeDone(BluetoothGattCharacteristic result) {
                    return pwmLedDrive(pin, 0.0f).then(mPwmDispatcher);
                }
            });
        }

        return promise;
    }
    
    /**
     * 指定のピンのPWM周期を設定する
     * @param pin PWMモードの設定をするPIOのピン番号。Konashi.PIO0 〜 Konashi.PIO7。
     * @param period 周期。単位はマイクロ秒(us)で32bitで指定してください。最大2^(32)us = 71.5分。
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pwmPeriod(int pin, int period){
        return execute(new PwmPeriodAction(getKonashiService(), pin, period, mPwmStore.getPwmDuty(pin))).then(mPwmDispatcher);
    }
    
    /**
     * 指定のピンのPWMのデューティ(ONになっている時間)を設定する。
     * @param pin PWMモードの設定をするPIOのピン番号。Konashi.PIO0 〜 Konashi.PIO7。
     * @param duty デューティ。単位はマイクロ秒(us)で32bitで指定してください。最大2^(32)us = 71.5分。
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pwmDuty(int pin, int duty){
        return execute(new PwmDutyAction(getKonashiService(), pin, duty, mPwmStore.getPwmPeriod(pin))).then(mPwmDispatcher);
    }
    
    /**
     * 指定のピンのLEDの明るさを0%〜100%で指定する
     * @param pin PWMモードの設定をするPIOのピン番号。Konashi.PIO0 〜 Konashi.PIO7。
     * @param dutyRatio LEDの明るさ。0.0F〜100.0F をしてしてください。
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pwmLedDrive(int pin, float dutyRatio){
        return execute(new PwmLedDriveAction(getKonashiService(), pin, dutyRatio, mPwmStore.getPwmPeriod(pin))).then(mPwmDispatcher);
    }
    
    /**
     * pwmLedDrive(int pin, float dutyRatio) の doubleでdutyRatioを指定する版。
     * @param pin PWMモードの設定をするPIOのピン番号。Konashi.PIO0 〜 Konashi.PIO7。
     * @param dutyRatio LEDの明るさ。0.0〜100.0 をしてしてください。
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pwmLedDrive(int pin, double dutyRatio){
        return pwmLedDrive(pin, (float) dutyRatio);
    }
    
    
    ///////////////////////////
    // AIO
    ///////////////////////////

    /**
     * AIO の指定のピンの入力電圧を取得する
     * @param pin AIOのピン名。指定可能なピン名は AIO0, AIO1, AIO2
     */
    public Promise<Integer, BletiaException, Object> analogRead(final int pin) {
        return execute(new AioAnalogReadAction(getKonashiService(), pin))
                .then(mAioDispatcher)
                .then(new AioAnalogReadFilter(pin));
    }
    
    /**
     * AIO の指定のピンに任意の電圧を出力する
     * @param pin AIOのピン名。指定可能なピン名は AIO0, AIO1, AIO
     * @param milliVolt 設定する電圧をmVで指定。0〜1300を指定可能
     */
//    @Override
//    public void analogWrite(int pin, int milliVolt){
//        if(!isEnableAccessKonashi()){
//            notifyKonashiError(KonashiErrorReason.NOT_READY);
//            return;
//        }
//
//        if(pin >= Konashi.AIO0 && pin <= Konashi.AIO2 && milliVolt >= 0 && milliVolt <= Konashi.ANALOG_REFERENCE){
//            byte[] val = new byte[3];
//            val[0] = (byte)pin;
//            val[1] = (byte)((milliVolt >> 8) & 0xFF);
//            val[2] = (byte)((milliVolt >> 0) & 0xFF);
//
//            KonashiUtils.log("analogWrite pin: " + pin + ", value: " + milliVolt);
//
//            addWriteMessage(KonashiUUID.ANALOG_DRIVE_UUID, val);
//        } else {
//            notifyKonashiError(KonashiErrorReason.INVALID_PARAMETER);
//        }
//    }

    
    ///////////////////////////
    // UART
    ///////////////////////////
    /**
     * UART の有効/無効を設定する
     * @param mode 設定するUARTのモード。Konashi.UART_DISABLE, Konashi.UART_ENABLE を指定
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> uartMode(int mode){
        return execute(new UartModeAction(getKonashiService(), mode, mUartStore), mUartDispatcher);
    }
    
    /**
     * UART の通信速度を設定する
     * @param baudrate UARTの通信速度。Konashi.UART_RATE_2K4 か Konashi.UART_RATE_9K6 を指定
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> uartBaudrate(int baudrate){
        return execute(new UartBaudrateAction(getKonashiService(), baudrate, mUartStore), mUartDispatcher);
    }

    /**
     * UART でデータを送信する
     * @param bytes 送信するデータ(byte[])
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> uartWrite(byte[] bytes) {
        return execute(new UartWriteAction(getKonashiService(), bytes, mUartStore), mUartDispatcher);
    }

    /**
     * UART でデータを送信する
     * @param string 送信するデータ(string)
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> uartWrite(String string) {
        return execute(new UartWriteAction(getKonashiService(), string, mUartStore), mUartDispatcher);
    }

    /**
     * UART でデータを1バイト送信する
     * for konashi v1(old code)
     * @param data 送信するデータ
     */
//    @Override
//    public void uartWrite(byte data){
//        if(!isEnableAccessKonashi()){
//            notifyKonashiError(KonashiErrorReason.NOT_READY);
//            return;
//        }
//
//        if(mUartSetting==Konashi.UART_ENABLE){
//            byte[] val = new byte[1];
//            val[0] = data;
//
//            addWriteMessage(KonashiUUID.UART_TX_UUID, val);
//        } else {
//            notifyKonashiError(KonashiErrorReason.NOT_ENABLED_UART);
//        }
//    }
    
    
    ///////////////////////////
    // I2C
    ///////////////////////////
    
    /**
     * I2Cのコンディションを発行する
     * @param condition コンディション。Konashi.I2C_START_CONDITION, Konashi.I2C_RESTART_CONDITION, Konashi.I2C_STOP_CONDITION を指定できる。
     */
    private Promise<BluetoothGattCharacteristic, BletiaException, Object> i2cSendCondition(int condition) {
        return execute(new I2cSendConditionAction(getKonashiService(), condition, mI2cStore));
    }

    /**
     * I2Cを有効/無効を設定する
     * @param mode 設定するI2Cのモード。Konashi.I2C_DISABLE , Konashi.I2C_ENABLE, Konashi.I2C_ENABLE_100K, Konashi.I2C_ENABLE_400Kを指定。
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> i2cMode(int mode) {
        return execute(new I2cModeAction(getKonashiService(), mode, mI2cStore), mI2cDispatcher);
    }

    /**
     * I2Cのスタートコンディションを発行する
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> i2cStartCondition() {
        return i2cSendCondition(Konashi.I2C_START_CONDITION);
    }

    /**
     * I2Cのリスタートコンディションを発行する
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> i2cRestartCondition() {
        return i2cSendCondition(Konashi.I2C_RESTART_CONDITION);
    }

    /**
     * I2Cのストップコンディションを発行する
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> i2cStopCondition() {
        return i2cSendCondition(Konashi.I2C_STOP_CONDITION);
    }

    /**
     * I2Cで指定したアドレスにデータを書き込む
     * @param length 書き込むデータ(byte)の長さ。最大 Konashi.I2C_DATA_MAX_LENGTH (19)byteまで
     * @param data 書き込むデータの配列
     * @param address 書き込み先アドレス
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> i2cWrite(int length, byte[] data, byte address) {
        return execute(new I2cWriteAction(getKonashiService(), address, data, mI2cStore));
    }

    /**
     * I2Cで指定したアドレスからデータを読み込むリクエストを行う
     * @param length 読み込むデータの長さ。最大 Konashi.I2C_DATA_MAX_LENGTHs (19)
     * @param address 読み込み先のアドレス
     */
    public Promise<byte[], BletiaException, Object> i2cRead(int length, byte address) {
        return execute(new I2cSetReadParamAction(getKonashiService(), length, address, mI2cStore))
                .then(mI2cDispatcher)
                .then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Object>() {
                    @Override
                    public Promise<BluetoothGattCharacteristic, BletiaException, Object> pipeDone(BluetoothGattCharacteristic result) {
                        return execute(new I2cReadAction(getKonashiService()));
                    }
                })
                .then(new I2cReadFilter());
    }

    ///////////////////////////
    // Hardware
    ///////////////////////////

    /**
     * konashiをリセットする
     */
    public Promise<BluetoothGattCharacteristic, BletiaException, Object> reset(){
        return execute(new HardwareResetAction(getKonashiService()));
    }

    /**
     * konashi のバッテリ残量を取得
     * @return 0 〜 100 のパーセント単位でバッテリ残量が返る
     */
    public Promise<Integer, BletiaException, Object> getBatteryLevel(){
        return execute(new BatteryLevelReadAction(getService(KonashiUUID.BATTERY_SERVICE_UUID)))
                .then(new BatteryLevelReadFilter());
    }

    /**
     * konashi の電波強度を取得
     * @return 電波強度(単位はdb)
     */
    public Promise<Integer, BletiaException, Object> getSignalStrength() {
        return mBletia.readRemoteRssi();
    }

    @Override
    protected void connect(BluetoothDevice device){
        mBletia.connect(device);
    }

    private <T> Promise<T, BletiaException, Object> execute(Action<T> action, DoneCallback<T> callback) {
        return execute(action).then(callback);
    }

    private <T> Promise<T, BletiaException, Object> execute(Action<T> action) {
        return mBletia.execute(action);
    }

    private BluetoothGattService getKonashiService() {
        return mBletia.getService(KonashiUUID.KONASHI_SERVICE_UUID);
    }

    private BluetoothGattService getService(UUID uuid) {
        return mBletia.getService(uuid);
    }
}
