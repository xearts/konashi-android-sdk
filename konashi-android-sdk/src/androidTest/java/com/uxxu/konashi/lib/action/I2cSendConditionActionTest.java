package com.uxxu.konashi.lib.action;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by izumin on 9/21/15.
 */
public class I2cSendConditionActionTest {

    @Mock private BluetoothGattService mService;
    @Mock private BluetoothGattCharacteristic mCharacteristic;

    private I2cSendConditionAction mAction;

    private ArgumentCaptor<byte[]> mValueCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mValueCaptor = ArgumentCaptor.forClass(byte[].class);
        when(mService.getCharacteristic(any(UUID.class))).thenReturn(mCharacteristic);
    }

    @Test
    public void hasValidParams_WithInvalidCondition() throws Exception {
        mAction = new I2cSendConditionAction(mService, 0x03);
        assertThat(mAction.hasValidParams()).isFalse();
    }

    @Test
    public void hasValidParams_WithValidCondition() throws Exception {
        mAction = new I2cSendConditionAction(mService, 0x02);
        assertThat(mAction.hasValidParams()).isTrue();
    }

    @Test
    public void setValue_StopCondition() throws Exception {
        mAction = new I2cSendConditionAction(mService, 0x00);
        mAction.setValue();
        verify(mCharacteristic, times(1)).setValue(mValueCaptor.capture());
        assertThat(mValueCaptor.getValue()[0]).isEqualTo((byte) 0x00);
    }

    @Test
    public void setValue_StartCondition() throws Exception {
        mAction = new I2cSendConditionAction(mService, 0x01);
        mAction.setValue();
        verify(mCharacteristic, times(1)).setValue(mValueCaptor.capture());
        assertThat(mValueCaptor.getValue()[0]).isEqualTo((byte) 0x01);
    }

    @Test
    public void setValue_RestartCondition() throws Exception {
        mAction = new I2cSendConditionAction(mService, 0x02);
        mAction.setValue();
        verify(mCharacteristic, times(1)).setValue(mValueCaptor.capture());
        assertThat(mValueCaptor.getValue()[0]).isEqualTo((byte) 0x02);
    }
}
