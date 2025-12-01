package service;

import com.licel.jcardsim.base.Simulator;
import constants.AppConstants;
import javacard.framework.AID;

import javax.swing.*;

/**
 * Service class for handling JavaCard simulator operations
 */
public class SimulatorService {
    private Simulator simulator;
    private boolean isConnected = false;
    private boolean isPinVerified = false;
    private int pinTriesRemaining = AppConstants.DEFAULT_PIN_TRIES;

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isPinVerified() {
        return isPinVerified;
    }

    public void setPinVerified(boolean pinVerified) {
        isPinVerified = pinVerified;
    }

    public int getPinTriesRemaining() {
        return pinTriesRemaining;
    }

    public void setPinTriesRemaining(int pinTriesRemaining) {
        this.pinTriesRemaining = pinTriesRemaining;
    }

    public void connect() throws Exception {
        simulator = new Simulator();
        AID aid = new AID(AppConstants.APPLET_AID, (short)0, (byte)AppConstants.APPLET_AID.length);

        String appletClassName = "applet.LibraryCardApplet";
        Class<?> appletClass = Class.forName(appletClassName);

        @SuppressWarnings("unchecked")
        Class<? extends javacard.framework.Applet> appletClassCasted =
            (Class<? extends javacard.framework.Applet>) appletClass;

        simulator.installApplet(aid, appletClassCasted);
        simulator.selectApplet(aid);

        isConnected = true;
    }

    public byte[] sendCommand(byte[] command) {
        if (simulator == null) {
            throw new RuntimeException("Simulator chưa được khởi động!");
        }
        return simulator.transmitCommand(command);
    }

    public int getSW(byte[] response) {
        if (response.length < 2) return 0;
        int sw1 = response[response.length - 2] & 0xFF;
        int sw2 = response[response.length - 1] & 0xFF;
        return (sw1 << 8) | sw2;
    }

    public void createDemoPin() throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        String defaultPin = AppConstants.DEFAULT_PIN;

        byte[] cmd = new byte[5 + defaultPin.length() + 1];
        cmd[0] = 0x00;
        cmd[1] = AppConstants.INS_CREATE_PIN;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = (byte)(defaultPin.length() + 1);
        cmd[5] = (byte)defaultPin.length();
        for (int i = 0; i < defaultPin.length(); i++) {
            cmd[6 + i] = (byte)defaultPin.charAt(i);
        }

        byte[] resp = sendCommand(cmd);

        if (getSW(resp) != 0x9000) {
            throw new RuntimeException("Không thể tạo PIN demo. Có thể PIN đã tồn tại.");
        }
    }

    public boolean verifyPin(char[] pinChars) throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        byte[] cmd = new byte[5 + pinChars.length + 1];
        cmd[0] = 0x00;
        cmd[1] = AppConstants.INS_VERIFY_PIN;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = (byte)(pinChars.length + 1);
        cmd[5] = (byte)pinChars.length;
        for (int i = 0; i < pinChars.length; i++) {
            cmd[6 + i] = (byte)pinChars[i];
        }

        byte[] resp = sendCommand(cmd);

        if (getSW(resp) == 0x9000 && resp.length > 2 && resp[0] == 0x01) {
            isPinVerified = true;
            return true;
        } else if (getSW(resp) == 0x9000 && resp.length > 2) {
            pinTriesRemaining = resp[1] & 0xFF;
            return false;
        }

        return false;
    }

    public boolean changePin(char[] oldPin, char[] newPin) throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        byte[] cmd = new byte[5 + oldPin.length + newPin.length + 2];
        int offset = 0;
        cmd[offset++] = 0x00;
        cmd[offset++] = AppConstants.INS_CHANGE_PIN;
        cmd[offset++] = 0x00;
        cmd[offset++] = 0x00;
        cmd[offset++] = (byte)(oldPin.length + newPin.length + 2);
        cmd[offset++] = (byte)oldPin.length;
        for (char c : oldPin) {
            cmd[offset++] = (byte)c;
        }
        cmd[offset++] = (byte)newPin.length;
        for (char c : newPin) {
            cmd[offset++] = (byte)c;
        }

        byte[] resp = sendCommand(cmd);

        if (getSW(resp) == 0x9000) {
            isPinVerified = false;
            return true;
        }

        return false;
    }
}
