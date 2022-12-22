package com.app.ericl_csapp;

import static com.app.ericl_csapp.Advisor.CREDITS_ELECTIVES_REQUIRED;
import static com.app.ericl_csapp.Advisor.CREDITS_REQUIRED;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.app.ericl_csapp.databinding.ActivityAdminBinding;
import com.app.ericl_csapp.databinding.ActivityStudentProfileBinding;

public class StudentProfile extends AppCompatActivity {

    ActivityStudentProfileBinding binding;
    //student info
    String studentName, studentEmail, studentStartYear;
    int creditsTaken = 0, electiveCredits = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentProfileBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);


        Bundle extras = getIntent().getExtras();
        if (extras != null){
            studentName = extras.getString("name");
            studentEmail = extras.getString("email");
            studentStartYear = extras.getString("startYear");
            creditsTaken = extras.getInt("credits");
            electiveCredits = extras.getInt("electiveCredits");

            binding.txtStudentName.setText(studentName);
            binding.txtStudentEmail.setText(studentEmail);
            binding.txtStudentStartYear.setText(studentStartYear);
            binding.txtStudentCreditsTaken.setText(String.valueOf(creditsTaken)+ " (" + String.valueOf(electiveCredits) + " elective credits taken)");
            int creditsLeft = CREDITS_REQUIRED - creditsTaken;
            int electiveCreditsLeft = CREDITS_ELECTIVES_REQUIRED - electiveCredits;
            if (electiveCreditsLeft < 0){
                electiveCreditsLeft = 0;
            }
            binding.txtStudentCreditsRemaining.setText(String.valueOf(creditsLeft)+ " (" + String.valueOf(electiveCreditsLeft) + " elective credits remain)" );


        }

        binding.btnStudentProfileBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
}