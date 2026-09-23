package com.zh4dev.niimbot_print.Model;

import com.google.gson.Gson;

import java.util.List;

public class PrintRollLabelModel {

    private String code;
    private List<String> lines;
    private double qrSizeMm;

    public String getCode() {
        return code;
    }

    public List<String> getLines() {
        return lines;
    }

    public double getQrSizeMm() {
        return qrSizeMm;
    }

    public static PrintRollLabelModel fromJson(String json) {
        return new Gson().fromJson(json, PrintRollLabelModel.class);
    }
}
