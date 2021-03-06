package com.aero.control.fragments;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.adapter.AppDataAdapter;
import com.aero.control.helpers.PerApp.AppMonitor.AppLogger;
import com.aero.control.helpers.PerApp.AppMonitor.JobManager;
import com.aero.control.helpers.PerApp.AppMonitor.model.AppElement;
import com.aero.control.helpers.PerApp.AppMonitor.model.AppElementDetail;
import com.aero.control.helpers.Util;

import java.util.List;

/**
 * Created by Alexander Christ on 03.05.15.
 */
public class AppMonitorFragment extends Fragment {

    private ViewGroup mRoot;
    private ListView mListView;
    private Context mContext;
    private final String mClassName = getClass().getName();
    private ProgressDialog mProgressDialog;
    private AppMonitorDetailFragment mAppMonitorDetailFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRoot = (ViewGroup) inflater.inflate(R.layout.appmonitor_fragment, null);

        this.mContext = getActivity();

        loadUI();

        return mRoot;
    }

    @Override
    public void onResume() {
        super.onResume();

        clearUI();
        loadUI();
    }

    private void clearUI() {
        this.mListView = null;
        mRoot.invalidate();
    }

    private void loadUI() {

        if (AeroActivity.mJobManager == null) {
            // Generate a new instance;
            AeroActivity.mJobManager = JobManager.instance();
        }

        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(Util.getRandomLoadingText(getActivity()));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.spinner_animation));
        }
        mProgressDialog.show();

        // Reset the back button coutner in our activity
        // TODO: put this logic somewhere else
        AeroActivity.resetBackCounter();

        // Run the main operation in its own thread;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final List<AppElement> appData = AeroActivity.mJobManager.getParentChildData(mContext);

                // Be sure that the activity is running
                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        TextView tmp = (TextView) mRoot.findViewById(R.id.noData);

                        mProgressDialog.dismiss();
                        mProgressDialog.cancel();

                        // Do we have data to display?
                        if (appData.size() > 0) {
                            tmp.setVisibility(View.GONE);
                        } else {
                            tmp.setText(getText(R.string.pref_no_appmonitor_data));
                            tmp.setVisibility(View.VISIBLE);
                        }

                        // Is it even enabled?
                        if (!AeroActivity.mJobManager.getJobManagerState()) {
                            tmp.setText(getText(R.string.pref_appmonitor_disabled));
                            tmp.setVisibility(View.VISIBLE);
                            clearUI();
                            return;
                        }

                        // For debugging purposes;
                        if (AppLogger.getLogLevel() >= 1) {
                            for (AppElement a : appData) {
                                AppLogger.print(mClassName, a.getName(), 1);
                                for (AppElementDetail acd : a.getChilData()) {
                                    AppLogger.print(mClassName, " -------> " + acd.getTitle() + " \n" + acd.getContent(), 1);
                                }
                            }
                        }

                        mListView = (ListView) mRoot.findViewById(R.id.apppstatistics);

                        final AppDataAdapter adapter = new AppDataAdapter(getActivity(),
                                R.layout.perapp_stat_row, appData);

                        mListView.setAdapter(adapter);

                        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                // Pass our data to the sub-fragment;
                                Intent intent = getActivity().getIntent();
                                intent.putExtra("aero_data", appData.get(position));

                                if (mAppMonitorDetailFragment == null)
                                    mAppMonitorDetailFragment = new AppMonitorDetailFragment();

                                AeroActivity.mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        getFragmentManager()
                                                .beginTransaction()
                                                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                                                .replace(R.id.content_frame, mAppMonitorDetailFragment)
                                                .addToBackStack("AppDetail")
                                                .commit();
                                    }
                                }, AeroActivity.genHelper.getDefaultDelay());
                            }
                        });
                    }
                });

            }
        };
        new Thread(runnable).start();
    }
}
