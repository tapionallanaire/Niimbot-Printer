package com.zh4dev.niimbot_print.Helper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zh4dev.niimbot_print.Constant.KeyConstant;
import com.zh4dev.niimbot_print.Constant.MessageConstant;
import com.zh4dev.niimbot_print.Constant.PrintConstant;
import com.zh4dev.niimbot_print.Model.BlueDeviceInfoModel;
import com.zh4dev.niimbot_print.Model.PrintLabelModel;
import com.zh4dev.niimbot_print.Model.PrintQrCodeModel;
import com.zh4dev.niimbot_print.Model.PrintRollLabelModel;
import com.zh4dev.niimbot_print.Utility.PrintUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.Result;

public class PrintHelper {

    private static final String TAG = "PrintHelper";
    private static final long PAIRING_TIMEOUT_MILLIS = 20_000L;

    private final LocalDataHelper localDataHelper;
    private final PrintUtility printUtility;
    private final BluetoothAdapter bluetoothAdapter;
    private final ExecutorService executorService;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScanResultStore scanResults = new ScanResultStore();

    private boolean receiverRegistered;
    private Result pendingScanResult;

    public PrintHelper(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        localDataHelper = new LocalDataHelper(context);
        printUtility = new PrintUtility(context);
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(PrintConstant.threadNameFormat)
                .build();
        executorService = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(1024),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context receiverContext, Intent intent) {
            if (!BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                return;
            }
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) {
                return;
            }
            try {
                addDeviceToScanResults(device);
            } catch (SecurityException error) {
                Log.e(TAG, "Missing permission while reading scan result", error);
            }
        }
    };

    private final Runnable finishScanRunnable = this::finishScan;

    public void onDisconnect(@NonNull Result result) {
        if (printUtility.connectionStatus() != 0) {
            result.success(false);
            return;
        }
        executorService.submit(() -> {
            try {
                printUtility.close();
                result.success(true);
            } catch (Exception error) {
                Log.e(TAG, "Unable to disconnect", error);
                result.error(KeyConstant.failedDisconnect, error.getMessage(), null);
            }
        });
    }

    public void isConnected(@NonNull Result result) {
        result.success(printUtility.connectionStatus() == 0);
    }

    public void onStartScan(@NonNull MethodCall call, @NonNull Result result) {
        if (bluetoothAdapter == null) {
            result.error(KeyConstant.errorPrint, "Bluetooth is not supported", null);
            return;
        }
        if (pendingScanResult != null) {
            result.error(KeyConstant.errorPrint, "Another scan is already in progress", null);
            return;
        }

        int scanDuration = call.arguments instanceof Number
                ? ((Number) call.arguments).intValue()
                : 6_000;
        scanResults.beginScan();
        pendingScanResult = result;

        try {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
            );
            receiverRegistered = true;
            addBondedDevicesToScanResults();
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            if (!bluetoothAdapter.startDiscovery()) {
                failScan("Unable to start Bluetooth discovery");
                return;
            }
            mainHandler.postDelayed(finishScanRunnable, Math.max(scanDuration, 1));
        } catch (SecurityException error) {
            failScan(error.getMessage());
        }
    }

    private void addBondedDevicesToScanResults() {
        try {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices == null) {
                return;
            }
            for (BluetoothDevice device : bondedDevices) {
                addDeviceToScanResults(device);
            }
        } catch (SecurityException error) {
            Log.e(TAG, "Missing permission while reading bonded devices", error);
        }
    }

    private void addDeviceToScanResults(BluetoothDevice device) {
        if (device == null) {
            return;
        }

        boolean supportedType = device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC
                || device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL;
        if (!supportedType) {
            return;
        }

        String address = device.getAddress();
        String model = new BlueDeviceInfoModel(
                device.getName(),
                address,
                device.getBondState()
        ).toMap();
        scanResults.add(address, model);
    }

    private void finishScan() {
        if (pendingScanResult == null) {
            return;
        }
        stopDiscovery();
        unregisterReceiver();
        Result result = pendingScanResult;
        pendingScanResult = null;
        result.success(scanResults.takeResults());
    }

    private void failScan(String message) {
        mainHandler.removeCallbacks(finishScanRunnable);
        stopDiscovery();
        unregisterReceiver();
        Result result = pendingScanResult;
        pendingScanResult = null;
        scanResults.clear();
        if (result != null) {
            result.error(KeyConstant.errorPrint, message, null);
        }
    }

    private void stopDiscovery() {
        if (bluetoothAdapter == null) {
            return;
        }
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        } catch (SecurityException error) {
            Log.e(TAG, "Unable to stop Bluetooth discovery", error);
        }
    }

    private void unregisterReceiver() {
        if (!receiverRegistered) {
            return;
        }
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException error) {
            Log.w(TAG, "Bluetooth receiver was already unregistered", error);
        } finally {
            receiverRegistered = false;
        }
    }

    public void onStartConnect(@NonNull MethodCall call, @NonNull Result result) {
        if (bluetoothAdapter == null || call.arguments == null) {
            result.error(KeyConstant.connectionFailed, MessageConstant.connectionFailed, null);
            return;
        }
        stopDiscovery();

        try {
            BlueDeviceInfoModel model = new BlueDeviceInfoModel(
                    "",
                    "",
                    BluetoothDevice.BOND_NONE
            ).fromMap(call.arguments.toString());
            String address = model.getDeviceHardwareAddress();
            if (address == null || address.isEmpty()) {
                result.error(KeyConstant.connectionFailed, MessageConstant.connectionFailed, null);
                return;
            }
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            executorService.submit(() -> pairAndConnect(result, device));
        } catch (Exception error) {
            result.error(KeyConstant.connectionFailed, error.getMessage(), null);
        }
    }

    private void pairAndConnect(Result result, BluetoothDevice device) {
        try {
            int bondState = device.getBondState();
            if (bondState == BluetoothDevice.BOND_NONE && !device.createBond()) {
                result.error(KeyConstant.failedPairing, MessageConstant.failedPairing, null);
                return;
            }

            long deadline = System.currentTimeMillis() + PAIRING_TIMEOUT_MILLIS;
            while (device.getBondState() != BluetoothDevice.BOND_BONDED
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(250L);
            }
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                result.error(KeyConstant.failedPairing, MessageConstant.failedPairing, null);
                return;
            }
            onConnectPrinter(result, device);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            result.error(KeyConstant.connectionFailed, MessageConstant.connectionFailed, null);
        } catch (Exception error) {
            Log.e(TAG, "Unable to connect", error);
            result.error(KeyConstant.connectionFailed, error.getMessage(), null);
        }
    }

    private void onConnectPrinter(@NonNull Result result, BluetoothDevice device) {
        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            result.error(
                    KeyConstant.unsupportedModels,
                    MessageConstant.unsupportedModels,
                    null
            );
            return;
        }
        localDataHelper.setPrinterModel(deviceName);
        int connectionResult = printUtility.openPrinter(device.getAddress());
        switch (connectionResult) {
            case 0:
                result.success(KeyConstant.connectionSuccess);
                return;
            case -1:
                result.error(
                        KeyConstant.connectionFailed,
                        MessageConstant.connectionFailed,
                        null
                );
                return;
            default:
                result.error(
                        KeyConstant.unsupportedModels,
                        MessageConstant.unsupportedModels,
                        null
                );
        }
    }

    @SuppressWarnings("unchecked")
    public void onStartPrintText(@NonNull MethodCall call, @NonNull Result result) {
        List<String> values = (List<String>) call.arguments;
        if (values == null || values.isEmpty()) {
            result.error(KeyConstant.emptyText, MessageConstant.pleaseInputText, null);
            return;
        }
        List<PrintLabelModel> models = new ArrayList<>();
        for (String value : values) {
            models.add(new PrintLabelModel().fromMap(value));
        }
        printUtility.printLabel(models, result);
    }

    public void onStartPrintQrCode(@NonNull MethodCall call, @NonNull Result result) {
        if (call.arguments == null) {
            result.error(KeyConstant.emptyText, MessageConstant.pleaseInputText, null);
            return;
        }
        PrintQrCodeModel qrCode = PrintQrCodeModel.fromJson(call.arguments.toString());
        if (qrCode.getData() == null || qrCode.getData().trim().isEmpty()) {
            result.error(KeyConstant.emptyText, MessageConstant.pleaseInputText, null);
            return;
        }
        printUtility.printQrCode(qrCode, result);
    }

    public void onStartPrintRollLabel(@NonNull MethodCall call, @NonNull Result result) {
        if (call.arguments == null) {
            result.error(KeyConstant.emptyText, MessageConstant.pleaseInputText, null);
            return;
        }
        PrintRollLabelModel model = PrintRollLabelModel.fromJson(call.arguments.toString());
        if (model.getCode() == null || model.getCode().trim().isEmpty()) {
            result.error(KeyConstant.emptyText, MessageConstant.pleaseInputText, null);
            return;
        }
        List<String> lines = model.getLines() != null ? model.getLines() : Collections.emptyList();
        printUtility.printRollLabel(model.getCode(), lines, (float) model.getQrSizeMm(), result);
    }

    public void dispose() {
        mainHandler.removeCallbacks(finishScanRunnable);
        pendingScanResult = null;
        scanResults.clear();
        stopDiscovery();
        unregisterReceiver();
        executorService.shutdownNow();
    }
}
