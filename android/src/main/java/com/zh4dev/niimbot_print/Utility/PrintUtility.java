package com.zh4dev.niimbot_print.Utility;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gengcon.www.jcprintersdk.JCPrintApi;
import com.gengcon.www.jcprintersdk.callback.Callback;
import com.gengcon.www.jcprintersdk.callback.PrintCallback;
import com.zh4dev.niimbot_print.Constant.KeyConstant;
import com.zh4dev.niimbot_print.Constant.MessageConstant;
import com.zh4dev.niimbot_print.Helper.LocalDataHelper;
import com.zh4dev.niimbot_print.Model.PrintLabelModel;
import com.zh4dev.niimbot_print.Model.PrintQrCodeModel;
import com.zh4dev.niimbot_print.Model.PrinterConfigurationModel;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.plugin.common.MethodChannel;

public class PrintUtility {

    private static final String TAG = "PrintUtility";
    private static final int LABEL_WIDTH_MM = 50;
    private static final int LABEL_HEIGHT_MM = 30;
    private static final float TEXT_HORIZONTAL_PADDING_MM = 2.0F;
    private static final float TEXT_WIDTH_MM =
            LABEL_WIDTH_MM - (TEXT_HORIZONTAL_PADDING_MM * 2.0F);
    private static final int QR_CODE_TYPE = 31;
    private static final double ROLL_LABEL_FONT_SIZE = 12.0;

    private final Application application;
    private final LocalDataHelper localDataHelper;
    private static final AtomicBoolean PRINT_IN_PROGRESS = new AtomicBoolean(false);

    private static JCPrintApi api;

    public PrintUtility(Context context) {
        application = (Application) context.getApplicationContext();
        localDataHelper = new LocalDataHelper(context);
    }

    private static final Callback CALLBACK = new Callback() {
        @Override
        public void onConnectSuccess(String name, int status) {
        }

        @Override
        public void onDisConnect() {
        }

        @Override
        public void onElectricityChange(int electricity) {
        }

        @Override
        public void onCoverStatus(int status) {
        }

        @Override
        public void onPaperStatus(int status) {
        }

        @Override
        public void onRfidReadStatus(int status) {
        }

        @Override
        public void onRibbonStatus(int status) {
        }

        @Override
        public void onRibbonRfidReadStatus(int status) {
        }

        @Override
        public void onFirmErrors() {
        }
    };

    private void ensureApi() {
        synchronized (PrintUtility.class) {
            if (api == null) {
                api = JCPrintApi.getInstance(CALLBACK);
                api.initSdk(application);
            }
        }
    }

    public int openPrinter(String address) {
        ensureApi();
        return api.connectBluetoothPrinter(address);
    }

    public void close() {
        ensureApi();
        api.close();
    }

    public int connectionStatus() {
        ensureApi();
        return api.isConnection();
    }

    public void printLabel(
            List<PrintLabelModel> models,
            @NonNull MethodChannel.Result result
    ) {
        startPrint(result, () -> {
            api.drawEmptyLabel(LABEL_WIDTH_MM, LABEL_HEIGHT_MM, 0, "");
            int totalLineCount = 0;
            for (PrintLabelModel model : models) {
                totalLineCount += Math.max(model.getText().split("\\r?\\n", -1).length, 1);
            }

            float defaultLineHeight = LABEL_HEIGHT_MM / 5.0F;
            float lineHeight = Math.min(
                    defaultLineHeight,
                    LABEL_HEIGHT_MM / (float) Math.max(totalLineCount, 1)
            );
            float contentHeight = lineHeight * totalLineCount;
            float currentY = Math.max((LABEL_HEIGHT_MM - contentHeight) / 2.0F, 0.0F);

            for (PrintLabelModel model : models) {
                int lineCount = Math.max(model.getText().split("\\r?\\n", -1).length, 1);
                float textBoxHeight = lineHeight * lineCount;
                float fontSize = (float) (model.getFontSize() / 4.5);
                api.drawLabelText(
                        TEXT_HORIZONTAL_PADDING_MM,
                        currentY,
                        TEXT_WIDTH_MM,
                        textBoxHeight,
                        model.getText(),
                        KeyConstant.defaultFontName,
                        fontSize,
                        0,
                        1,
                        1,
                        6,
                        0,
                        1,
                        new boolean[]{false, false, false, false}
                );
                currentY += textBoxHeight;
            }
            return createPrintData();
        });
    }

    public void printQrCode(
            PrintQrCodeModel qrCode,
            @NonNull MethodChannel.Result result
    ) {
        startPrint(result, () -> {
            float size = (float) qrCode.getSize();
            float x = (LABEL_WIDTH_MM - size) / 2.0F;
            float y = (LABEL_HEIGHT_MM - size) / 2.0F;
            api.drawEmptyLabel(LABEL_WIDTH_MM, LABEL_HEIGHT_MM, 0, "");
            api.drawLabelQrCode(
                    x,
                    y,
                    size,
                    size,
                    qrCode.getData(),
                    QR_CODE_TYPE,
                    0
            );
            return createPrintData();
        });
    }

    public void printRollLabel(
            String code,
            List<String> lines,
            float qrSizeMm,
            @NonNull MethodChannel.Result result
    ) {
        startPrint(result, () -> {
            float size = qrSizeMm;
            api.drawEmptyLabel(LABEL_WIDTH_MM, LABEL_HEIGHT_MM, 0, "");
            api.drawLabelQrCode(
                    2F,
                    (LABEL_HEIGHT_MM - size) / 2F,
                    size,
                    size,
                    code,
                    QR_CODE_TYPE,
                    0
            );

            float textX = size + 4F;
            float textWidth = LABEL_WIDTH_MM - size - 6F;
            int lineCount = Math.max(lines.size(), 1);
            float lineHeight = LABEL_HEIGHT_MM / (float) lineCount;
            float fontSize = (float) (ROLL_LABEL_FONT_SIZE / 4.5);
            float currentY = 0F;
            for (String line : lines) {
                api.drawLabelText(
                        textX,
                        currentY,
                        textWidth,
                        lineHeight,
                        line,
                        KeyConstant.defaultFontName,
                        fontSize,
                        0,
                        1,
                        1,
                        6,
                        0,
                        1,
                        new boolean[]{false, false, false, false}
                );
                currentY += lineHeight;
            }
            return createPrintData();
        });
    }

    private void startPrint(
            @NonNull MethodChannel.Result result,
            LabelRenderer renderer
    ) {
        if (connectionStatus() != 0) {
            result.error(KeyConstant.errorPrint, MessageConstant.printerNotConnected, null);
            return;
        }
        if (!PRINT_IN_PROGRESS.compareAndSet(false, true)) {
            result.error(KeyConstant.errorPrint, "Another print job is in progress", null);
            return;
        }

        AtomicBoolean completed = new AtomicBoolean(false);
        PrinterConfigurationModel configuration = localDataHelper.getPrinterModel();
        api.setTotalPrintQuantity(1);
        api.startPrintJob(
                configuration.printDensity,
                1,
                configuration.printModel,
                new PrintCallback() {
                    private boolean dataCommitted;

                    @Override
                    public void onProgress(
                            int pageIndex,
                            int quantityIndex,
                            HashMap<String, Object> details
                    ) {
                        if (pageIndex == 1 && quantityIndex == 1) {
                            boolean ended = api.endPrintJob();
                            complete(result, completed, ended, MessageConstant.endPrintingFailed);
                        }
                    }

                    @Override
                    public void onError(int errorCode) {
                        complete(result, completed, false, printError(errorCode));
                    }

                    @Override
                    public void onError(int errorCode, int printState) {
                        complete(result, completed, false, printError(errorCode));
                    }

                    @Override
                    public void onCancelJob(boolean success) {
                        complete(result, completed, false, "Print job was cancelled");
                    }

                    @Override
                    public void onPause(boolean success) {
                        Log.d(TAG, "Print pause completed: " + success);
                    }

                    @Override
                    public void onPausing() {
                        Log.d(TAG, "Print job is pausing");
                    }

                    @Override
                    public void onResume(boolean success) {
                        Log.d(TAG, "Print resume completed: " + success);
                    }

                    @Override
                    public void onBufferFree(int pageIndex, int bufferSize) {
                        if (completed.get() || dataCommitted || bufferSize < 1) {
                            return;
                        }
                        try {
                            PrintData data = renderer.render();
                            api.commitData(
                                    Collections.singletonList(data.labelJson),
                                    Collections.singletonList(data.printInfoJson)
                            );
                            dataCommitted = true;
                        } catch (Exception error) {
                            Log.e(TAG, "Unable to prepare print data", error);
                            complete(result, completed, false, error.getMessage());
                        }
                    }
                }
        );
    }

    private PrintData createPrintData() {
        String labelJson = new String(api.generateLabelJson(), StandardCharsets.UTF_8);
        float multiple = localDataHelper.getPrinterModel().printMultiple;
        String printInfoJson = "{\"printerImageProcessingInfo\":{" +
                "\"orientation\":0," +
                "\"margin\":[0,0,0,0]," +
                "\"printQuantity\":1," +
                "\"horizontalOffset\":0," +
                "\"verticalOffset\":0," +
                "\"width\":" + LABEL_WIDTH_MM + "," +
                "\"height\":" + LABEL_HEIGHT_MM + "," +
                "\"printMultiple\":" + multiple + "," +
                "\"epc\":\"\"}}";
        return new PrintData(labelJson, printInfoJson);
    }

    private void complete(
            MethodChannel.Result result,
            AtomicBoolean completed,
            boolean success,
            String errorMessage
    ) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        PRINT_IN_PROGRESS.set(false);
        if (success) {
            result.success(true);
        } else {
            result.error(KeyConstant.errorPrint, errorMessage, null);
        }
    }

    private String printError(int code) {
        switch (code) {
            case 1:
                return "Lid open";
            case 2:
                return "Out of paper";
            case 3:
                return "Not enough power";
            case 4:
                return "Battery abnormality";
            case 5:
                return "Manual stop";
            case 6:
                return "Data error";
            case 7:
                return "Temperature is too high";
            case 8:
                return "Paper ejection abnormality";
            case 9:
                return "Printer is busy";
            case 10:
                return "No printhead detected";
            case 11:
                return "Ambient temperature is too low";
            case 12:
                return "The print head is not locked";
            case 13:
                return "Ribbon not detected";
            case 14:
                return "Mismatched ribbon";
            case 15:
                return "Ribbon is used up";
            case 16:
                return "Unsupported paper type";
            case 17:
                return "Paper type setting failed";
            case 18:
                return "Print mode setting failed";
            case 19:
                return "Failed to set density";
            case 20:
                return "Failed to write RFID";
            case 21:
                return "Margin setting failed";
            case 22:
                return "Communication error";
            case 23:
                return "Printer disconnected";
            case 24:
                return "Artboard parameter error";
            case 25:
                return "Wrong rotation angle";
            case 26:
                return "JSON parameter error";
            case 27:
                return "Paper ejection abnormality (B3S)";
            case 28:
                return "Check paper type";
            case 29:
                return "RFID tag is not writable";
            case 30:
                return "Density setting is not supported";
            case 31:
                return "Unsupported print mode";
            default:
                return "Unknown print error (" + code + ")";
        }
    }

    private interface LabelRenderer {
        PrintData render();
    }

    private static class PrintData {
        final String labelJson;
        final String printInfoJson;

        PrintData(String labelJson, String printInfoJson) {
            this.labelJson = labelJson;
            this.printInfoJson = printInfoJson;
        }
    }
}
