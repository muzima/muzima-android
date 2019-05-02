package com.muzima.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.muzima.api.model.Notification;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.github.library.bubbleview.BubbleTextView;
import com.muzima.api.model.Provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.muzima.utils.Constants.STANDARD_DATE_FORMAT;

public class MessageThreadAdapter extends BaseAdapter{

    private final List<Notification> chatModelList;
    private final Context context;
    private final LayoutInflater inflater;
    private String initialDate ="";
    private final List datePosition = new ArrayList();
    private final List date = new ArrayList();



    public MessageThreadAdapter(List<Notification> chatModelList, Context context,Provider selectedProvider) {
        this.chatModelList = chatModelList;
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        return chatModelList.size();
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public Object getItem(int position) {
        return chatModelList.get(position);
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    public void updateResults(List<Notification> results) {
        notifyDataSetChanged();
        chatModelList.addAll(results);
        //Triggers the list update
        notifyDataSetChanged();
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Collections.sort(chatModelList, new NotificationComparator());
        View view = convertView;
        int i=0;



        String loggedInUserUuid = ((MuzimaApplication)context.getApplicationContext()).getAuthenticatedUser().getPerson().getUuid();
        Notification notification = chatModelList.get(position);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(STANDARD_DATE_FORMAT);
        Date dateCreated = notification.getDateCreated();
        String sendDate = "";
        if(dateCreated!=null) {
             sendDate = simpleDateFormat.format(dateCreated);
        }

        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm");
        Date timeCreated = notification.getDateCreated();
        String sendTime = simpleTimeFormat.format(timeCreated);
        String senderUuid = chatModelList.get(position).getSender().getUuid();
        String message = chatModelList.get(position).getPayload();
        if (senderUuid.equals(loggedInUserUuid)) {
            view = inflater.inflate(R.layout.item_layout_send, null);
            BubbleTextView bubbleTextView = view.findViewById(R.id.chat_textview);
            TextView textView = view.findViewById(R.id.date_send_text_view);
            TextView datetextView = view.findViewById(R.id.date_set_text);
            textView.setText(sendTime);
            if(initialDate.compareTo(sendDate)!=0 && initialDate.compareTo(sendDate)!=1 && !date.contains(sendDate)) {
                datetextView.setVisibility(View.VISIBLE);
                datetextView.setText(sendDate);
                datePosition.add(position);
                date.add(sendDate);
                initialDate = sendDate;
            }else if(datePosition.contains(position)) {
                datetextView.setVisibility(View.VISIBLE);
                datetextView.setText(sendDate);
                initialDate = sendDate;
            }else{
                datetextView.setVisibility(View.GONE);
            }
            bubbleTextView.setText(message);
        } else {
            view = inflater.inflate(R.layout.item_layout_receive, null);
            BubbleTextView bubbleTextView = view.findViewById(R.id.chat_textview);
            TextView textView = view.findViewById(R.id.date_send_text_view);
            TextView datetextView = view.findViewById(R.id.date_set_text);
            textView.setText(sendTime);
            if(initialDate.compareTo(sendDate)!=0 && initialDate.compareTo(sendDate)!=1 && !date.contains(sendDate)) {
                datetextView.setVisibility(View.VISIBLE);
                datetextView.setText(sendDate);
                datePosition.add(position);
                date.add(sendDate);
                initialDate = sendDate;
            }else if(datePosition.contains(position)) {
                datetextView.setVisibility(View.VISIBLE);
                datetextView.setText(sendDate);
                initialDate = sendDate;
            }else{
                datetextView.setVisibility(View.GONE);
            }
            bubbleTextView.setText(message);
        }

        return view;
    }

    class NotificationComparator implements Comparator<Notification>{
        @Override
        public int compare(Notification lhs, Notification rhs) {
            if (lhs.getDateCreated()==null)
                return 0;
            if (rhs.getDateCreated()==null)
                return 0;

            return lhs.getDateCreated().compareTo(rhs.getDateCreated());
        }
    }
}

