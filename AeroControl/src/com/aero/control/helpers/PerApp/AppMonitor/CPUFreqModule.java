package com.aero.control.helpers.PerApp.AppMonitor;

import com.aero.control.AeroActivity;

/**
 * Created by Alexander Christ on 03.05.15.
 * Checks for the current frequency of cpu(s) and returns the current average
 */
public class CPUFreqModule extends AppModule {

    private final static String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu";
    private final static String SCALE_PATH_NAME = "/cpufreq/scaling_cur_freq";
    private final String mClassName = getClass().getName();

    public CPUFreqModule() {
        super();
        setName(mClassName);
        setIdentifier(AppModule.MODULE_CPU_FREQ_IDENTIFIER);
        setPrefix("CPU Freq");
        setSuffix(" Mhz");
        AppLogger.print(mClassName, "CPU Freq Module successfully initialized!", 0);
    }

    @Override
    protected final void operate() {
        super.operate();
        final long temp = System.currentTimeMillis();

        String complete_path;
        int averageFreq = 0;
        int onlineCPUs = 0;
        final int i = Runtime.getRuntime().availableProcessors();

        if (i == 1) {
            averageFreq += Integer.parseInt(AeroActivity.shell.getFastInfo(SCALE_CUR_FILE + 0 + SCALE_PATH_NAME));
            onlineCPUs++;
        } else {
            // Get the cpu frequency for each cpu;
            for (int k = 0; k < i; k++) {

                if (AeroActivity.shell.getFastInfo(SCALE_CUR_FILE + k + "/online").equals("1")) {
                    complete_path = SCALE_CUR_FILE + k + SCALE_PATH_NAME;
                    try {
                        averageFreq += Integer.parseInt(AeroActivity.shell.getFastInfo(complete_path));
                    } catch (NumberFormatException e) {}
                    onlineCPUs++;
                }
            }
        }
        averageFreq = (averageFreq / onlineCPUs) / 1000;
        addValues(averageFreq);
        AppLogger.print(mClassName, "CPUFreqModule.operate() time: " + (System.currentTimeMillis() - temp), 1);
    }

}
