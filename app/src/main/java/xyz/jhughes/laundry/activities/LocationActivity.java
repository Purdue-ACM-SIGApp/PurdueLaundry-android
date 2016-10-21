package xyz.jhughes.laundry.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import xyz.jhughes.laundry.LaundryParser.Location;
import xyz.jhughes.laundry.LaundryParser.MachineList;
import xyz.jhughes.laundry.ModelOperations;
import xyz.jhughes.laundry.analytics.ScreenTrackedActivity;
import xyz.jhughes.laundry.apiclient.MachineService;
import xyz.jhughes.laundry.R;
import xyz.jhughes.laundry.adapters.LocationAdapter;
import xyz.jhughes.laundry.storage.SharedPrefsHelper;

/**
 * The main activity of the app. Lists the locations of
 * laundry and an overview of the availabilities.
 */
public class LocationActivity extends ScreenTrackedActivity {

    @Bind(R.id.recycler_view) RecyclerView recyclerView;
    @Bind(R.id.location_activity_toolbar) Toolbar toolbar;
    @Bind(R.id.progressBar) ProgressBar mLoadingProgressBar;

    private LocationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().getBooleanExtra("forceMainMenu", false)) {
            String lastRoom = SharedPrefsHelper.getSharedPrefs(this)
                    .getString("lastScreenViewed", null);
            if (lastRoom != null && !lastRoom.equals("LocationList")) {
                Intent intent = new Intent(this, MachineActivity.class);
                Bundle b = new Bundle();
                b.putString("locationName", lastRoom);
                intent.putExtras(b);
                startActivity(intent);
            }
        }

        setContentView(R.layout.activity_location);
        ButterKnife.bind(this);

        initRecyclerView();
        initToolbar();
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
        getLaundryCall();
    }

    protected void getLaundryCall() {
        Call<Map<String,MachineList>> allMachineCall = MachineService.getService().getAllMachines();
        allMachineCall.enqueue(new Callback<Map<String, MachineList>>() {
            @Override
            public void onResponse(Response<Map<String, MachineList>> response, Retrofit retrofit) {
                Map<String,MachineList> machineMap = response.body();
                List<Location> locations = ModelOperations.machineMapToLocationList(machineMap);
                adapter = new LocationAdapter(locations, LocationActivity.this.getApplicationContext());
                mLoadingProgressBar.setVisibility(View.GONE);
                recyclerView.setHasFixedSize(true);
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
