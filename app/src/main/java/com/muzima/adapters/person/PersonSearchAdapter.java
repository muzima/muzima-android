package com.muzima.adapters.person;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonName;
import com.muzima.listners.LoadMoreListener;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.Utils;
import com.muzima.utils.ViewUtil;
import com.muzima.view.main.HTCMainActivity;
import com.muzima.view.person.PersonRegisterActivity;
import com.muzima.view.person.SearchSESPPersonActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PersonSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements RecyclerAdapter.BackgroundListQueryTaskListener {
    List<PatientItem> records;
    LoadMoreListener loadMoreListener;
    protected final int VIEW_TYPE_ITEM = 0;
    protected final int VIEW_TYPE_LOADING = 1;
    private HTCMainActivity activity;
    private SearchSESPPersonActivity searchSESPPersonActivity;
    protected boolean isLoading;
    protected int lastVisibleItem, totalItemCount;
    private boolean detailsSection;
    private final Context context;
    public PersonSearchAdapter(RecyclerView recyclerView, List<PatientItem> records, Activity activity, Context context) {
        this.records = records;
        try {
            this.activity = (HTCMainActivity) activity;
        } catch (ClassCastException e) {
            this.searchSESPPersonActivity = (SearchSESPPersonActivity) activity;
        }
        this.context = context;

        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (!isLoading && linearLayoutManager.findLastCompletelyVisibleItemPosition() == records.size() - 1) {
                    if (loadMoreListener != null) {
                        loadMoreListener.onLoadMore();
                    }
                    isLoading = true;
                }
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return records.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void setLoaded() {
        isLoading = false;
    }

    public boolean isLoading() {
        return isLoading;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == VIEW_TYPE_ITEM) {
             view = layoutInflater.inflate(R.layout.person_list_item, parent, false);
            return new PersonViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            view = layoutInflater.inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof PersonViewHolder){
            PersonViewHolder personViewHolder = (PersonViewHolder) holder;
            Patient patient = records.get(position).getPatient();
            PersonName name = getPreferedName(patient);
            if (StringUtils.stringHasValue(name.getMiddleName())) {
                personViewHolder.name.setText(name.getGivenName() + " " + name.getMiddleName() +" "+ name.getFamilyName());
            }
            else {
                personViewHolder.name.setText(name.getGivenName() + " "+ name.getFamilyName());
            }



            Date dob = patient.getBirthdate();
            if(dob != null) {
                personViewHolder.age.setText(DateUtils.calculateAge(dob)+" Anos");
            }else{
                personViewHolder.age.setText("");
            }

            personViewHolder.identifier.setText( !StringUtils.isEmpty(patient.getIdentifier()) ? patient.getIdentifier() : "Sem Identifacor");


            personViewHolder.migrationState.setImageResource(((HTCPerson)patient).getSyncStatus().equals("uploaded") ? R.drawable.filled_cloud : R.drawable.empty_cloud);

            if (patient.getGender() != null) {
                personViewHolder.sex.setImageResource(patient.getGender().equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female);
            }
            else {
                personViewHolder.sex.setImageResource(R.drawable.generic_person_24);
            }
            PersonAddress address = getPreferedAddress(patient);
            if (address != null) {
                String addressString = null;
                if (StringUtils.stringHasValue(address.getAddress1())) {
                    addressString = address.getAddress1();
                }

                if (StringUtils.stringHasValue(address.getAddress3())) {
                    addressString += StringUtils.stringHasValue(addressString) ? ", "+address.getAddress3() : address.getAddress3();
                }

                if (StringUtils.stringHasValue(address.getAddress5())) {
                    addressString += StringUtils.stringHasValue(addressString) ? ", "+address.getAddress5() : address.getAddress5();
                }

                if (StringUtils.stringHasValue(address.getAddress6())) {
                    addressString += StringUtils.stringHasValue(addressString) ? ", "+address.getAddress6() : address.getAddress6();
                }

                personViewHolder.address.setText(addressString);
            } else {
                personViewHolder.address.setText("Sem Informação");
            }

            personViewHolder.contact.setText(((HTCPerson)patient).getPhoneNumber());

            personViewHolder.details.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        detailsSection = !detailsSection;
                        if (!detailsSection) {
                            personViewHolder.moreDetailsLyt.setVisibility(View.VISIBLE);
                            personViewHolder.divider.setBackgroundColor(context.getResources().getColor(R.color.person_item_back));
                            ViewUtil.collapse(personViewHolder.moreDetailsLyt);
                            personViewHolder.details.animate().setDuration(200).rotation(0);
                        }
                        else {
                            personViewHolder.moreDetailsLyt.setVisibility(View.GONE);
                            personViewHolder.divider.setBackgroundColor(context.getResources().getColor(R.color.person_item_divider));
                            ViewUtil.expand(personViewHolder.moreDetailsLyt);
                            personViewHolder.details.animate().setDuration(200).rotation(180);
                        }
                }
            });
            personViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, PersonRegisterActivity.class);
                    intent.putExtra("searchResults", (Serializable) records);
                    intent.putExtra("selectedPerson", patient);
                    intent.putExtra("isAddATSForSESPExistingPerson", Boolean.FALSE);
                    intent.putExtra("isEditionFlow", Boolean.TRUE);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                }
            });
        }else
        if (holder instanceof LoadingViewHolder){
            showLoadingView((LoadingViewHolder) holder, position);
        }
    }

    private PersonName getPreferedName(Patient patient) {
        for (PersonName name : patient.getNames()) {
            if (name.isPreferred()) return name;
        }
        return patient.getNames().get(0);
    }

    private PersonAddress getPreferedAddress(Patient patient) {
        if (!Utils.listHasElements((ArrayList<?>) patient.getAddresses())) return null;

        for (PersonAddress name : patient.getAddresses()) {
            if (name.isPreferred()) return name;
        }
        return patient.getAddresses().get(0);
    }
    public void setLoadMoreListener(LoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }

    public LoadMoreListener getLoadMoreListener() {
        return loadMoreListener;
    }

    @Override
    public void onQueryTaskStarted() {

    }

    @Override
    public void onQueryTaskFinish() {

    }

    @Override
    public void onQueryTaskCancelled() {

    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {

    }

    public class PersonViewHolder extends RecyclerView.ViewHolder{
        ImageView sex;
        TextView name;
        TextView identifier;
        TextView age;
        TextView dateCreated;
        TextView contact;
        TextView address;
        ImageView migrationState;
        ImageButton createHTC;
        ImageButton details;
        View divider;

        LinearLayout moreDetailsLyt;


        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            sex = itemView.findViewById(R.id.person_sex);
            name = itemView.findViewById(R.id.name);
            identifier = itemView.findViewById(R.id.identifier);
            age = itemView.findViewById(R.id.person_age);
            //dateCreated = itemView.findViewById(R.id.date_created);
            contact = itemView.findViewById(R.id.contact);
            address = itemView.findViewById(R.id.address);
            migrationState = itemView.findViewById(R.id.migration_state);
            createHTC = itemView.findViewById(R.id.create_htc);
            details = itemView.findViewById(R.id.details);
            divider = itemView.findViewById(R.id.divider);
            moreDetailsLyt = itemView.findViewById(R.id.person_more_details);
            createHTC.setVisibility(View.INVISIBLE);
        }
    }

    public class LoadingViewHolder extends RecyclerView.ViewHolder {

        ProgressBar progressBar;
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);

        }
    }

    protected void showLoadingView(PersonSearchAdapter.LoadingViewHolder viewHolder, int position) {}
}
