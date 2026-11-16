package com.mobiletools.systemhelper.deob;

import android.os.Build;
import android.telephony.CellLocation;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceInfo {

    public String manufacturer = null;
    public String model = null;
    public String hwVersion = null;
    public String swVersion = null;
    public String os = null;
    public String osVersion = null;
    public String deviceType = null;

    public String imei1 = null;
    public String iccid1 = null;
    public String imsi1 = null;
    public String msisdn1 = null;
    public String mnc1 = null;
    public String networkClass1 = null;
    public String cellId1 = null;
    public String lac1 = null;
    public String volteEnabled1 = null;
    public String fiveGEnabled1 = null;

    public String imei2 = null;
    public String iccid2 = null;
    public String imsi2 = null;
    public String msisdn2 = null;
    public String mnc2 = null;
    public String networkClass2 = null;
    public String cellId2 = null;
    public String lac2 = null;
    public String volteEnabled2 = null;
    public String fiveGEnabled2 = null;

    public String dataSlot = "0";
    public String accessType = null;
    public String ipv4 = null;
    public String ipv6 = null;
    public JSONObject subType = null;
    public String regType = null;
    public String specVersion = null;
    public String regVersion = null;
    public String time = null;

    public JSONArray appInfo = null;
    public JSONArray history = null;
    public String signalInfo = null;

    private String jsonCache = null;

    private static String or(String value, String def) {
        return value == null ? def : value;
    }

    public synchronized String toJson() {
        try {
            JSONObject json = new JSONObject();

            manufacturer = or(manufacturer, "Unknown");
            json.put("Manuf", manufacturer);
            model = or(model, "Unknown");
            json.put("Model", model);
            hwVersion = or(hwVersion, "Unknown");
            json.put("HWVersion", hwVersion);
            swVersion = or(swVersion, "Unknown");
            json.put("SWVersion", swVersion);
            os = or(os, "Unknown");
            json.put("OS", os);
            osVersion = or(osVersion, "Unknown");
            json.put("OSVersion", osVersion);

            imei1 = or(imei1, "Unknown");
            json.put("IMEI1", imei1);
            iccid1 = or(iccid1, "");
            json.put("ICCID1", iccid1);
            imsi1 = or(imsi1, "");
            json.put("IMSI1", imsi1);
            msisdn1 = or(msisdn1, "");
            json.put("MSISDN1", msisdn1);
            mnc1 = or(mnc1, "");
            json.put("MNC1", mnc1);
            networkClass1 = or(networkClass1, "");
            json.put("NCLS1", networkClass1);
            cellId1 = or(cellId1, "");
            json.put("CellID1", cellId1);
            lac1 = or(lac1, "");
            json.put("LAC1", lac1);
            volteEnabled1 = or(volteEnabled1, "");
            json.put("VolteEnabled1", volteEnabled1);
            fiveGEnabled1 = or(fiveGEnabled1, "");
            json.put("5GEnabled1", fiveGEnabled1);

            imei2 = or(imei2, "");
            json.put("IMEI2", imei2);
            iccid2 = or(iccid2, "");
            json.put("ICCID2", iccid2);
            imsi2 = or(imsi2, "");
            json.put("IMSI2", imsi2);
            msisdn2 = or(msisdn2, "");
            json.put("MSISDN2", msisdn2);
            mnc2 = or(mnc2, "");
            json.put("MNC2", mnc2);
            networkClass2 = or(networkClass2, "");
            json.put("NCLS2", networkClass2);
            cellId2 = or(cellId2, "");
            json.put("CellID2", cellId2);
            lac2 = or(lac2, "");
            json.put("LAC2", lac2);
            volteEnabled2 = or(volteEnabled2, "");
            json.put("VolteEnabled2", volteEnabled2);
            fiveGEnabled2 = or(fiveGEnabled2, "");
            json.put("5GEnabled2", fiveGEnabled2);

            if (dataSlot == null || dataSlot.isEmpty()) {
                dataSlot = "0";
            }
            if (!dataSlot.equals("1") && !dataSlot.equals("2")) {
                dataSlot = "0";
            }
            json.put("DataSlot", dataSlot);

            accessType = or(accessType, "Unknown");
            json.put("AccType", accessType);

            if (AppConfig.fetchIpBeforeUpload) {
                ipv4 = or(ipv4, "");
                json.put("IPv4", ipv4);
                ipv6 = or(ipv6, "");
                json.put("IPv6", ipv6);
            }

            json.put("SubType", subType == null ? "" : subType);

            if (deviceType == null || deviceType.isEmpty()) {
                deviceType = "000";
            }
            json.put("DeviceType", deviceType);

            regType = or(regType, "");
            json.put("RegType", regType);

            specVersion = "3.0";
            json.put("SpecVersion", "3.0");

            regVersion = or(regVersion, "1.0");
            json.put("RegVersion", regVersion);

            time = or(time, "");
            json.put("Time", time);

            if (Build.VERSION.SDK_INT >= 23) {
                json.put("AppInfo", appInfo == null ? "" : appInfo);
            }
            if (Build.VERSION.SDK_INT >= 23 && AppConfig.reportSignalInfo) {
                json.put("SignalInfo", signalInfo == null ? "" : signalInfo);
            }
            if (history != null) {
                json.put("History", history);
            }

            jsonCache = json.toString();
        } catch (JSONException e) {
            LogHelper.error("DeviceInfo", "JSONException->" + e.getMessage());
        }
        return jsonCache;
    }

    public synchronized String toJsonCompact() {
        return toJson();
    }

    public synchronized void readCellLocationSlot1(CellLocation location) {
        if (location == null) {
            LogHelper.debug("DeviceInfo", "sim1 location is null");
            return;
        }
        if (location instanceof GsmCellLocation) {
            GsmCellLocation gsm = (GsmCellLocation) location;
            int cid = gsm.getCid();
            int lac = gsm.getLac();
            if (cid != -1) {
                this.cellId1 = String.valueOf(cid);
            } else {
                LogHelper.debug("DeviceInfo", "gsm location cellId1 = " + cid);
            }
            if (lac != -1) {
                this.lac1 = String.valueOf(lac);
            } else {
                LogHelper.debug("DeviceInfo", "gsm location lac1 = " + lac);
            }
            return;
        }
        if (!(location instanceof CdmaCellLocation)) {
            LogHelper.debug("DeviceInfo", "unknow CellLocation type:" + location);
            return;
        }
        CdmaCellLocation cdma = (CdmaCellLocation) location;
        int baseStationId = cdma.getBaseStationId();
        int longitude = cdma.getBaseStationLongitude();
        int latitude = cdma.getBaseStationLatitude();
        if (baseStationId != -1) {
            this.cellId1 = String.valueOf(baseStationId);
        } else {
            LogHelper.debug("DeviceInfo", "cdma location cellId1 = " + baseStationId);
        }
        if (longitude == Integer.MAX_VALUE || latitude == Integer.MAX_VALUE) {
            LogHelper.debug("DeviceInfo", "cdma location lac1 = " + longitude + latitude);
        } else {
            this.lac1 = String.format("%d:%d", longitude, latitude);
        }
    }

    public synchronized void readCellLocationSlot2(CellLocation location) {
        if (location == null) {
            LogHelper.debug("DeviceInfo", "sim2 location is null");
            return;
        }
        if (location instanceof GsmCellLocation) {
            GsmCellLocation gsm = (GsmCellLocation) location;
            int cid = gsm.getCid();
            int lac = gsm.getLac();
            if (cid != -1) {
                this.cellId2 = String.valueOf(cid);
            } else {
                LogHelper.debug("DeviceInfo", "gsm location cellId2 = " + cid);
            }
            if (lac != -1) {
                this.lac2 = String.valueOf(lac);
            } else {
                LogHelper.debug("DeviceInfo", "gsm location lac2 = " + lac);
            }
            return;
        }
        if (!(location instanceof CdmaCellLocation)) {
            LogHelper.debug("DeviceInfo", "unknow CellLocation type:" + location);
            return;
        }
        CdmaCellLocation cdma = (CdmaCellLocation) location;
        int baseStationId = cdma.getBaseStationId();
        int longitude = cdma.getBaseStationLongitude();
        int latitude = cdma.getBaseStationLatitude();
        if (baseStationId != -1) {
            this.cellId2 = String.valueOf(baseStationId);
        } else {
            LogHelper.debug("DeviceInfo", "cdma location cellId2 = " + baseStationId);
        }
        if (longitude == Integer.MAX_VALUE || latitude == Integer.MAX_VALUE) {
            LogHelper.debug("DeviceInfo", "cdma location lac2 = " + longitude + latitude);
        } else {
            this.lac2 = String.format("%d:%d", longitude, latitude);
        }
    }
}
