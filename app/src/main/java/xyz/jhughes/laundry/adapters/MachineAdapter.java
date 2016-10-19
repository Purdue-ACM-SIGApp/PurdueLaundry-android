package xyz.jhughes.laundry.adapters;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import xyz.jhughes.laundry.LaundryParser.Constants;
import xyz.jhughes.laundry.LaundryParser.Machine;
import xyz.jhughes.laundry.LaundryParser.MachineStates;
import xyz.jhughes.laundry.LaundryParser.MachineTypes;
import xyz.jhughes.laundry.R;
import xyz.jhughes.laundry.analytics.AnalyticsHelper;
import xyz.jhughes.laundry.notificationhelpers.NotificationCreator;
import xyz.jhughes.laundry.notificationhelpers.NotificationPublisher;
import xyz.jhughes.laundry.storage.SharedPrefsHelper;

public class MachineAdapter extends RecyclerView.Adapter<MachineAdapter.ViewHolder> {
    private ArrayList<Machine> currentMachines;
    private Context mContext;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        @Bind(R.id.machine_name_text_view)  TextView nameTextView;
        @Bind(R.id.machine_status_text_view)  TextView statusTextView;
        @Bind(R.id.card_view)  CardView cardView;
        private boolean alarmSet = false;

        private ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MachineAdapter(ArrayList<Machine> machines, Context context, Boolean dryers, String options) {
        this.mContext = context;
        currentMachines = new ArrayList<>();

        for (Machine m : machines) {
            machineHelper(m, dryers, options);
        }
    }

    private void machineHelper(Machine m, Boolean dryers, String options) {

        String status = m.getStatus();
        boolean isCorrectType = dryers == m.getType().equals(MachineTypes.DRYER);
        boolean matchesParameters = options.contains(status);
        boolean isStillAllowed = !matchesParameters
                && !MachineStates.FILTERABLE_OPTIONS.contains(status)
                && options.contains(MachineStates.IN_USE);

        if (isCorrectType && (matchesParameters || isStillAllowed)) {
            currentMachines.add(m);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MachineAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_machine, parent, false);
        // set the view's size, margins, paddings and layout parameters

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        final Machine m = currentMachines.get(position);
        holder.nameTextView.setText(m.getName());
        switch (m.getStatus()) {
            case MachineStates.IN_USE:
                // Instead of showing "In Use", show how many minutes are left!
                holder.statusTextView.setText(m.getTime()); // this will need to be updated once people start using the machines again...It should be "xx min. left"

                break;
            case MachineStates.READY:
                holder.statusTextView.setText(mContext.getResources().getStringArray(R.array.options)[1]); // this should be replaced too

                break;
            default:
                holder.statusTextView.setText(m.getStatus());
                break;
        }
        //holder.timeLeftTextView.setText(m.getTime());
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AnalyticsHelper.sendEventHit(m.getType(), AnalyticsHelper.CLICK, m.getStatus());

                try {
                    int minutesInFuture = Integer.parseInt(m.getTime().substring(0, m.getTime().indexOf(' ')));
                    int millisInFuture = minutesInFuture * 60000; //60 seconds * 1000 milliseconds

                    SharedPreferences sharedPreferences = SharedPrefsHelper.getSharedPrefs(mContext);
                    String currentRoom = sharedPreferences.getString("lastRoom", "Cary West");
                    String notificationKey = currentRoom + " " + m.getName();
                    if(NotificationCreator.notificationExists(notificationKey)) {
                        Toast.makeText(mContext, R.string.reminder_already_set, Toast.LENGTH_LONG).show();
                    } else {
                        fireNotificationInFuture(millisInFuture, holder, notificationKey);
                    }
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    Toast.makeText(mContext, R.string.machine_not_running, Toast.LENGTH_LONG).show();
                }
            }
        });

        int color = Constants.getMachineAvailabilityColor(m.getStatus());
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, color));

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return currentMachines.size();
    }

    //Set and Fire Notification
    private void fireNotificationInFuture(final int milliInFuture, final ViewHolder holder, final String notificationKey) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext)
                .setTitle(mContext.getString(R.string.alarm))
                .setMessage(mContext.getString(R.string.ask_set_alarm))
                .setCancelable(true)
                .setPositiveButton(mContext.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AnalyticsHelper.sendEventHit("Reminders", AnalyticsHelper.CLICK, "YES");
                        NotificationCreator.createNotification(mContext, notificationKey, milliInFuture);
                    }
                })
                .setNegativeButton(mContext.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnalyticsHelper.sendEventHit("Reminders", AnalyticsHelper.CLICK, "NO");
                        dialog.cancel();
                        Toast.makeText(mContext, R.string.no_alarm_set, Toast.LENGTH_LONG).show();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void scheduleNotification(Notification notification, int delay) {
        Intent notificationIntent = new Intent(mContext, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    private Notification getNotification(String content) {
        Notification.Builder builder = new Notification.Builder(mContext);
        builder.setContentTitle(mContext.getString(R.string.app_name));
        builder.setContentText(content);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setVibrate(new long[]{1000, 1000, 1000});
        return builder.build();
    }
}