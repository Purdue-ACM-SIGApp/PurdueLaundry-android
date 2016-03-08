package xyz.jhughes.laundry.activities;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import xyz.jhughes.laundry.LaundryParser.Constants;
import xyz.jhughes.laundry.LaundryParser.Machine;
import xyz.jhughes.laundry.analytics.ScreenTrackedActivity;
import xyz.jhughes.laundry.apiclient.MachineService;
import xyz.jhughes.laundry.R;
import xyz.jhughes.laundry.adapters.LocationAdapter;

/**
 * The main activity of the app. Lists the locations of
 * laundry and an overview of the availabilities.
 */
public class LocationActivity extends ScreenTrackedActivity {

    @Bind(R.id.recycler_view) RecyclerView recyclerView;
    @Bind(R.id.location_activity_toolbar) Toolbar toolbar;
    @Bind(R.id.progressBar) ProgressBar mLoadingProgressBar;

    private HashMap<String, Integer[]> locationHashMap;

    private LocationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        ButterKnife.bind(this);

        initRecyclerView();
        initToolbar();

        locationHashMap = new HashMap<>();
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    protected void onResume() {
        super.onResume();

        recyclerView.setAdapter(null);
        mLoadingProgressBar.setVisibility(View.VISIBLE);

        for (String name : Constants.getListOfRooms()) {
            getLaundryCall(Constants.getName(name));
        }
    }

    protected void getLaundryCall(String name) {
        Call<Map<String,List<Machine>>> allMachineCall = MachineService.getService().getAllMachines();
        allMachineCall.enqueue(new Callback<Map<String, List<Machine>>>() {
            @Override
            public void onResponse(Response<Map<String, List<Machine>>> response, Retrofit retrofit) {
                Map<String,List<Machine>> machineMap = response.body();
                adapter = new LocationAdapter(machineMap, LocationActivity.this.getApplicationContext());
                mLoadingProgressBar.setVisibility(View.GONE);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("LocationActivity", "API ERROR - " + t.getMessage());
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        return super.onCreateOptionsMenu(menu);
    }
}
