package com.app.ericl_csapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.app.ericl_csapp.databinding.ActivityCurrentEnrollmentBinding;
import com.app.ericl_csapp.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CurrentEnrollment extends AppCompatActivity {

    ActivityCurrentEnrollmentBinding binding;
    ArrayList<Course> completedClasses, currentCourses;
    ArrayAdapter<Course> adpCurrentlyEnrolled;
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference refCurUserClasses;
    boolean classWasUnenrolled = false;
    //Make a list of the semesters to show in the drop down
    Set<String> setSemesters = new HashSet();
    ArrayList<String> semesters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCurrentEnrollmentBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        //put the back arrow on the action bar
        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        refCurUserClasses = db.getReference().child("Users").child(user.getUid()).child("classes");

        //get the array list that we passed to this page
        Bundle data = getIntent().getExtras();
        completedClasses = data.getParcelableArrayList("completed");
        currentCourses = new ArrayList();

        //go through all classes and add semesters to the set so we dont have duplicates
        for(Course c : completedClasses){
            setSemesters.add(c.getSemester());
        }
        //convert set to array list
        //TODO check that the student semesters are populated based on their classes
        semesters = new ArrayList<>( setSemesters);

        //tell the spinner to use the semester strings above, to do that we need to use
        //an array adapater. this will take in an array of strings and show them in some
        //list element on our screen, in this case it is the dropdown list
        ArrayAdapter<String> adpSpinner = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, semesters);
        binding.spnSemesters.setAdapter(adpSpinner);

        //in order to make the spinner object react to clicks we need an onItemClickListener
        //that will always listen for clicks
        binding.spnSemesters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //update display will grab the item that is current selected
                //and show the correct classes
                updateDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    void updateDisplay(){

        //This should show the courses we are currently enrolled in based
        //on the spinner

        //get the index of the item that is selected in the spinner
        int spinnerIdx = binding.spnSemesters.getSelectedItemPosition();
        //the spinner elements work just like an array, if the first element is selected
        //it is 0

        //get the string for the semester that is selected, remember we used
        // the "semesters" variable to hold the drop down list
        //so we should take the index of the selected item and use it in that variable
        //to get the text
        String semester = semesters.get(spinnerIdx);


        //TODO ask the user if they want to enroll now or browse the list
        currentCourses.clear();
        //for each Course in the completedClasses list, go through all of them
        //and the current course we are on as we go through them we will call "c"
        for(Course c : completedClasses){
            if (c.getSemester().equals(semester)) {
                currentCourses.add( c );
            }
        }
        adpCurrentlyEnrolled = new ArrayAdapter<Course>(this, android.R.layout.simple_list_item_1, currentCourses){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                //View view = super.getView(position, convertView, parent);
                // if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
                //  }

                TextView txt = (TextView) convertView.findViewById(R.id.txtListItem_name);
                txt.setText(currentCourses.get(position).getName());

                TextView txtSemester = (TextView) convertView.findViewById(R.id.txtListItem_semester);
                txtSemester.setText(currentCourses.get(position).getSemester());

                ConstraintLayout cns = convertView.findViewById(R.id.cnsListItem);
                //if elective change color
                if (currentCourses.get(position).getElective().equals("true")){
                    cns.setBackgroundColor(getResources().getColor(R.color.elective));
                }
                return convertView;
            }
        };
        binding.lstCurrentlyEnrolled.setAdapter(adpCurrentlyEnrolled);

        binding.lstCurrentlyEnrolled.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    String courseToDelete = adpCurrentlyEnrolled.getItem(position).getName();
                    Course clicked = null;
                    //grab the course object for the course that was clicked on
                    for(int i = 0; i < completedClasses.size();i++){
                        Course c = completedClasses.get(i);
                        if (c.getName().equals(courseToDelete)) {
                            clicked = completedClasses.get(i);
                            break;
                        }
                    }

                    if (clicked != null){
                        //now check that the semester is in the future

                        String semester = getCurrentSemester();
                        int year = getYearFromSemester(semester);
                        if (year < clicked.getYear()) {
                            showConfirmUnEnrollInCourse(position);
                        } else if (year == clicked.getYear()){
                            //if the years are the same compare the times of the year
                            String currentSeason = semester.split(" ")[0];
                            String clickedSeason = clicked.getSemester().split(" ")[0];
                            //lay out the times when we should be able to remove
                            if (currentSeason.equals("Winter") && (clickedSeason.equals("Spring") || clickedSeason.equals("Summer") ||  clickedSeason.equals("Fall")  )){
                                showConfirmUnEnrollInCourse(position);
                            } else if (currentSeason.equals("Spring") && (clickedSeason.equals("Summer") ||  clickedSeason.equals("Fall")  )){
                                showConfirmUnEnrollInCourse(position);
                            }  else if (currentSeason.equals("Summer") && ( clickedSeason.equals("Fall")  )){
                                showConfirmUnEnrollInCourse(position);
                            } else {
                                Toast.makeText(CurrentEnrollment.this, "You can no longer unenroll from this past or current class", Toast.LENGTH_LONG).show();
                            }


                        } else {
                            Toast.makeText(CurrentEnrollment.this, "You can no longer unenroll from this past or current class", Toast.LENGTH_LONG).show();
                        }
                    }


            }
        });

    }

    private int getYearFromSemester(String semester){
        return Integer.parseInt( semester.split(" ")[1] );
    }

    private String getCurrentSemester(){
        //TODO do we need to get the days and be more specific with the semesters
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        //split this into 2 numbers
        String dateParts[] =  currentDate.split("/");
        int month = Integer.parseInt(dateParts[0]);
        int day = Integer.parseInt(dateParts[1]);
        int year = Integer.parseInt(dateParts[2]);
        String semester = "";
        //TODO LOOK THIS UP
        //NOTE: this works if you run the app on days that fall in semesters
        //there is a bit of a break from spring to summer and summer to fall
        //spring jan20
        int springStartM = 1;
        int springStartD = 20;
        //end may 20
        int springEndM = 5;
        int springEndD = 20;
        //summer june 20
        int summerStartM = 6;
        int summerStartD = 20;
        //end summer aug 20
        int summerEndM = 8;
        int summerEndD = 20;
        //fall start september 8
        int fallStartM = 9;
        int fallStartD = 8;
        //end fall dec 20;
        int fallEndM = 12;
        int fallEndD = 20;
        //winter start dec 21
        int winterStartM = 1;
        int winterStartD = 1;
        //end winter jan 19
        int winterEndM = 1;
        int winterEndD = springStartD -1;

        //first do the months where the semester splits in the middle
        if (month == springStartM){
            if (day >= springStartD){
                semester = "Spring";
            } else {
                semester = "Winter";
            }
        }  else if ( springStartM < month && month < springEndM ){
            semester = "Spring";
        } else if ( summerStartM <= month && month <=  summerEndM ){
            semester = "Summer";
        } else   if ( fallStartM <= month && month < fallEndM){
            semester = "Fall";
        }
        semester += " "+ String.valueOf(year);
        return semester;
    }


    private void showConfirmUnEnrollInCourse(int courseClicked){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Uncomment the below code to Set the message and title from the strings.xml file
        //Setting message manually and performing action on button click

        String courseToDelete = adpCurrentlyEnrolled.getItem(courseClicked).getName();
        builder.setMessage("Are you sure you want to unenroll from:\n" + courseToDelete)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        //TODO save to firebase

                        refCurUserClasses.child(courseToDelete).removeValue()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            //mark that we unenrolled in something
                                            classWasUnenrolled = true;
                                            //but also removing the course from the required
                                            for(int i = 0; i < completedClasses.size();i++){
                                                Course c = completedClasses.get(i);
                                                if (c.getName().equals(courseToDelete)) {
                                                    completedClasses.remove(i);
                                                    break;
                                                }
                                            }
                                           updateDisplay();
                                        } else {
                                            Toast.makeText(CurrentEnrollment.this, "ERROR: could not connect to online database", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        //Creating dialog box
        AlertDialog alert = builder.create();
        //Setting the title manually
        alert.setTitle("Unenroll in Course");
        alert.show();
    }

    @Override
    public void onBackPressed()
    {
        Intent resultIntent = new Intent();
        if (classWasUnenrolled){
            resultIntent.putParcelableArrayListExtra("completed", completedClasses);
            setResult(Activity.RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        int itemID = item.getItemId();
       if (itemID == android.R.id.home) {

            onBackPressed();

        }

        return true;
    }

}