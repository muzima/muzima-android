package com.muzima.view.person;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.muzima.R;
import com.muzima.utils.ViewUtil;

public class PersonRegisterActivity extends AppCompatActivity {

    private ImageButton identificationDataBtn;
    private ImageButton birthDateSectionBtn;
    private ImageButton contactSectionBtn;
    private ImageButton sexSectionBtn;
    private ImageButton addressSectionBtn;

    private LinearLayout identificationDataLyt;
    private LinearLayout birthDateSectionLyt;
    private LinearLayout contactSectionLyt;
    private LinearLayout sexSectionLyt;
    private LinearLayout addressSectionLyt;

    private boolean identificationSection;
    private boolean birthDateSection;
    private boolean contactSection;
    private boolean sexSection;
    private boolean addressSection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_register);
        initViews();
        setListners();
    }

    private void setListners() {
        setLayoutControlListners();
    }

    private void setLayoutControlListners() {
        identificationDataBtn.setOnClickListener(view -> {
            identificationSection = !identificationSection;
            if (identificationSection) {
                identificationDataLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(identificationDataLyt);
                identificationDataBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                identificationDataLyt.setVisibility(View.GONE);
                ViewUtil.expand(identificationDataLyt);
                identificationDataBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        birthDateSectionBtn.setOnClickListener(view -> {
            birthDateSection = !birthDateSection;
            if (birthDateSection) {
                birthDateSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(birthDateSectionLyt);
                birthDateSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                birthDateSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(birthDateSectionLyt);
                birthDateSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        contactSectionBtn.setOnClickListener(view -> {
            contactSection = !contactSection;
            if (contactSection) {
                contactSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(contactSectionLyt);
                contactSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                contactSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(contactSectionLyt);
                contactSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        sexSectionBtn.setOnClickListener(view -> {
            sexSection = !sexSection;
            if (sexSection) {
                sexSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(sexSectionLyt);
                sexSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                sexSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(sexSectionLyt);
                sexSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        addressSectionBtn.setOnClickListener(view -> {
            addressSection = !addressSection;
            if (addressSection) {
                addressSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(addressSectionLyt);
                addressSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                addressSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(addressSectionLyt);
                addressSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
    }

    private void initViews() {
        identificationDataBtn = findViewById(R.id.btn_identification_data);
        birthDateSectionBtn = findViewById(R.id.btn_birth_data);
        contactSectionBtn = findViewById(R.id.btn_contact_data);
        sexSectionBtn = findViewById(R.id.btn_sex_data);
        addressSectionBtn = findViewById(R.id.btn_address_data);

        identificationDataLyt = findViewById(R.id.identification_data_lyt);
        birthDateSectionLyt = findViewById(R.id.birth_data_lyt);
        contactSectionLyt = findViewById(R.id.contact_data_lyt);
        sexSectionLyt = findViewById(R.id.sex_data_lyt);
        addressSectionLyt = findViewById(R.id.address_data_lyt);

    }
}