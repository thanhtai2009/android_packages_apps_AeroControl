package com.aero.control.helpers.PerApp.AppMonitor;


import com.aero.control.AeroActivity;

/**
 * Created by Alexander Christ on 03.05.15.
 * Checks for the current CPU temperature
 */
public class TEMPModule extends AppModule {

    private final String mClassName = getClass().getName();
    private final static String CPU_TEMP_FILE = "/sys/devices/virtual/thermal/thermal_zone4/temp";

    public TEMPModule() {
        super();
        setName(mClassName);
        setIdentifier(AppModule.MODULE_TEMP_IDENTIFIER);
        setPrefix("temperature");
        setSuffix(" °C");
        AppLogger.print(mClassName, "Temperature Module successfully initialized!", 0);
    }

    @Override
    protected void operate() {
        super.operate();
        long temp = System.currentTimeMillis();
        Integer temperature = null;

        try {
            temperature = Integer.parseInt(AeroActivity.shell.getFastInfo(CPU_TEMP_FILE));
        } catch (NumberFormatException e) {
        }

        if (temperature != null)
            addValues(temperature);
        AppLogger.print(mClassName, "TEMPModule.operate() time: " + (System.currentTimeMillis() - temp), 1);
    }

}
