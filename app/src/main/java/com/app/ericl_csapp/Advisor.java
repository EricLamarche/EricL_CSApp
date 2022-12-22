package com.app.ericl_csapp;

import static com.app.ericl_csapp.MainActivity.SHARED_PREFS_NAME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.app.ericl_csapp.databinding.ActivityAdvisorBinding;
import com.app.ericl_csapp.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


/*
Main Page
List of all the courses you are enrolled in for a certain semester
Start it at the semester of today
Drop down to select which semester you want to look at
    -if that semester has no classes then show Text and a button to let them enroll in some classes

    The list adapter needs a filter for the semesters

Button (?) to an enroll page which lets you pick up to
5 classes for a certain semester

https://github.com/sitepoint-editors/SpeechApplication/blob/master/app/src/main/java/com/example/theodhor/speechapplication/MainActivity.java

 */

public class Advisor extends AppCompatActivity {

    ActivityAdvisorBinding binding;
    private TextToSpeech tts;
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference refRootNode = db.getReference();
    DatabaseReference refCurUser, refCurUserClasses, refRequiredClasses;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser user;
    String lastSpeech = "";
    //student info
    String studentName, studentEmail, studentStartYear;
    int creditsTaken = 0, creditsThisSemester = 0;
    //TODO check this with how many electives we have
    public static final int CREDITS_REQUIRED = 75;
    public static final int CREDITS_ELECTIVES_REQUIRED = 15;

    private static final int REQUEST_SPEECH = 100;
    private static final int REQUEST_CURRENT_ENROLLMENT = 101;

    ArrayList<Course> requiredCourses = new ArrayList<>();
    ArrayList<Course> completedClasses = new ArrayList<>();
    ArrayList<Course> notCompleteClasses = new ArrayList<>();
    ArrayList<Course> coursesToShow = new ArrayList<>();
    ArrayAdapter<String> adpCoursesToShow;
    int yearShowing = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdvisorBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);


        Bundle extras = getIntent().getExtras();
        if (extras != null){
            requiredCourses = extras.getParcelableArrayList("requiredCourses");
        }

        user = auth.getCurrentUser();

        refCurUser = FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUid());
        refCurUserClasses = refCurUser.child("classes");
        refRequiredClasses = FirebaseDatabase.getInstance().getReference().child("RequiredCourses_v2");
        //refRequiredClasses = refRootNode.child("RequiredClasses");


        loadFirebaseData();

        //FOR TESTING DELETE THIS
        //setupSeniorCourses();
       //loadRequiredCoursesToFirebase();

        binding.btnCheckCurrentEnrollment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Advisor.this, CurrentEnrollment.class);
                i.putParcelableArrayListExtra("completed", completedClasses);
                startActivityForResult(i, REQUEST_CURRENT_ENROLLMENT);
            }
        });


        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("user_log", "This Language is not supported");
                    }

                } else {
                    Log.e("user_log", "Initialization Failed!");
                }
            }
        });

        binding.lstAvailableCourses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                showConfirmEnrollInCourse(pos);
            }
        });

        binding.btnEnroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showPopUp("Ready for Speech", "Please say the semester you want to see classes for\nEx: Spring 2023", "OK",true);

            }
        });

        //populateTheFirebaseData();

    }


    void setupSeniorCourses(){
        //Based on 60 credits instead of 120, we need 9 credits per semester
        //fall 2019 - 9
        refCurUserClasses.child("CS 101 Comp Sci I").setValue("Fall 2019");
        refCurUserClasses.child("CS 103 Java I").setValue("Fall 2019");
        refCurUserClasses.child("CS 105 Python I").setValue("Fall 2019");
        //spring 2020 - 9
        refCurUserClasses.child("CS 102 Comp Sci II").setValue("Spring 2020");
        refCurUserClasses.child("CS 104 Java II").setValue("Spring 2020");
        refCurUserClasses.child("CS 106 Python II").setValue("Spring 2020");
        //fall 2020 - 9
        refCurUserClasses.child("CS 107 Data Structures I").setValue("Fall 2020");
        refCurUserClasses.child("CS 119 Operating Sys I").setValue("Fall 2020");
        refCurUserClasses.child("CS 122 Database Sys I").setValue("Fall 2020");
        //spring 2021 - 9
        refCurUserClasses.child("CS 108 Data Structures II").setValue("Spring 2021");
        refCurUserClasses.child("CS 120 Operating Sys II").setValue("Spring 2021");
        refCurUserClasses.child("CS 123 Database Sys II").setValue("Spring 2021");
        //36 required
        //take 9 elective credits
        refCurUserClasses.child("CS 109 Discrete Math").setValue("Fall 2021");
        refCurUserClasses.child("CS 112 Robotics").setValue("Fall 2021");
        refCurUserClasses.child("CS 113 Algorithms").setValue("Fall 2021");
        //spring 2022
        refCurUserClasses.child("CS 126 Big Data I").setValue("Spring 2022");
        refCurUserClasses.child("CS 124 Networking I").setValue("Spring 2022");
        refCurUserClasses.child("CS 128 C++ I").setValue("Spring 2022");
        //45 credits




        //senior spring
        //leave 6 credits for electives - 1 required course

    }

    void loadRequiredCoursesToFirebase(){
        //Offered fall 2022
        String classFall2022[] = {
                "CS 101 Comp Sci I",
                "CS 103 Java I",
                "CS 105 Python I",

                "CS 107 Data Structures I",
                "CS 119 Operating Systems I",
                "CS 122 Database Systems I",

                "CS 126 Big Data I",
                "CS 124 Networking I",
                "CS 128 C++ I",
                "CS 131 Mobile App Dev I",};

        for(String s : classFall2022){
            //Make a map of the data that will be saved in key value pairs on the firebase
            HashMap<String, Object> data = new HashMap<>();
            data.put("name", s);
            data.put("semester", "Fall 2022");
            data.put("credits", 3);
            data.put("elective", "false");
            DatabaseReference newNode = refRequiredClasses.push();
            //set the data at the new node
            newNode.setValue(data);
        }
        //offered every spring 2023
        String classSpring2023[] = {
                "CS 102 Comp Sci II",
                "CS 104 Java II",
                "CS 106 Python II",
                "CS 108 Data Structures II",
                "CS 120 Operating Sys II",
                "CS 123 Database Sys II",
                "CS 125 Networking II",
                "CS 127 Big Data II",
                "CS 129 C++ II",
                "CS 132 Mobile App Dev II"};
        for(String s : classSpring2023){
            //Make a map of the data that will be saved in key value pairs on the firebase
            HashMap<String, Object> data = new HashMap<>();
            data.put("name", s);
            data.put("semester", "Spring 2023");
            data.put("credits", 3);
            data.put("elective", "false");
            DatabaseReference newNode = refRequiredClasses.push();
            //set the data at the new node
            newNode.setValue(data);
        }

        String electivesFall2022[] = {
                "CS 109 Discrete Math",
                "CS 110 Data Science",
                "CS 111 Data Analytics",
                "CS 112 Robotics",
                "CS 113 Algorithms",
                "CS 115 Cloud Computing",
                };
        for(String s : electivesFall2022){
            //Make a map of the data that will be saved in key value pairs on the firebase
            HashMap<String, Object> data = new HashMap<>();
            data.put("name", s);
            data.put("semester", "Fall 2022");
            data.put("credits", 3);
            data.put("elective", "true");
            DatabaseReference newNode = refRequiredClasses.push();
            //set the data at the new node
            newNode.setValue(data);
        }

        String electivesSpring2023[]={
                "CS 116 Blockchain Tech",
                "CS 117 Architecture",
                "CS 118 Software Eng",
                "CS 121 MIPS",
                "CS 130 Capstone Project"
        };
        for(String s : electivesSpring2023){
            //Make a map of the data that will be saved in key value pairs on the firebase
            HashMap<String, Object> data = new HashMap<>();
            data.put("name", s);
            data.put("semester", "Spring 2023");
            data.put("credits", 3);
            data.put("elective", "true");
            DatabaseReference newNode = refRequiredClasses.push();
            //set the data at the new node
            newNode.setValue(data);
        }


       //60 required
       //require 15 credit electives but there are more than enough
        //75 total credits to graduate
    }

    private void listen() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");

        try {
            startActivityForResult(i, REQUEST_SPEECH);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(Advisor.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPEECH) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                lastSpeech = inSpeech;
                recognition(inSpeech);
            }
        }
        // check to see if the result came from Current Enrollment
        else if (requestCode == REQUEST_CURRENT_ENROLLMENT){
            //if the user did not delete anything then our result will be CANCEL and do nothing
            if (resultCode == RESULT_OK){
                completedClasses = data.getParcelableArrayListExtra("completed");
                //if there is already a list showing on the screen we should reload that list
                //otherwise we dont have to do anything
                //if the user never searched yet the adapter is null
                if (adpCoursesToShow != null){
                    //clear the old list
                    adpCoursesToShow.clear();
                }
                populateNotCompletedClasses("");
                //reload the last search since the classes changed
                if (!lastSpeech.isEmpty()) {
                    recognition(lastSpeech);
                }
            }
        }
    }

    private void recognition(String text) {
        Log.d("Speech", "" + text);
        //String[] speech = text.split(" ");

        lastSpeech = text;

        //convert the text to lowercase so the cases dont matter
        text = text.toLowerCase();


        if (!text.contains("fall") && !text.contains("spring") && !text.contains("winter") && !text.contains("summer")) {
            showPopUp("Invalid Semester", "Say a semester that is open for enrollment. Try again", "Try again", true);
            return;
        }

        yearShowing = Integer.parseInt(text.split(" ")[1]);

        //we need to split this into 2
        //make a list of the items we want to show for this semester
        //go through the not completed classes and if their semester matches
        //the text that was said then show them in the list
        ArrayList<String> coursesToShowStr = new ArrayList<>();
        coursesToShow.clear();
        populateNotCompletedClasses(text);
        for (Course c : notCompleteClasses) {
            if (c.getSemester().toLowerCase().equals(text)) {
                coursesToShowStr.add(c.getName());
                coursesToShow.add(c);
            }
        }


        //if we don't find any courses maybe they said the wrong year? so pop up a message
        if (coursesToShow.size() == 0) {
            //if there are no courses to show that means

            boolean isSemesterAvailable = false;
            for (Course c : completedClasses) {
                int year = Integer.parseInt(text.split(" ")[1]);
                //if we find the year that they said in their completed courses then
                //this must be a valid year and they already took all the classes
                if (year == Integer.parseInt(c.getSemester().split(" ")[1])) {
                    isSemesterAvailable = true;
                    break;
                }
            }

            //1. you have completed/ are enrolled in all the necessary classes
            //2. the user picked a year that is unavailable
            if (isSemesterAvailable) {
                showPopUp("Enrollment Unavailable", "You are have already taken, or are currently enrolled in all courses offered during this specified semester.", "OK",false);
            } else {
                showPopUp("Semester not available", "Did not find any classes for " + text + ". Please try again", "Try Again",true);
                return;
            }

        }

        adpCoursesToShow = new ArrayAdapter<String>(Advisor.this, android.R.layout.simple_list_item_1, coursesToShowStr){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                //View view = super.getView(position, convertView, parent);
               // if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
              //  }

                TextView txt = (TextView) convertView.findViewById(R.id.txtListItem_name);
                txt.setText(coursesToShow.get(position).getName());

                TextView txtSemester = (TextView) convertView.findViewById(R.id.txtListItem_semester);
                txtSemester.setText(coursesToShow.get(position).getSemester());

                ConstraintLayout cns = convertView.findViewById(R.id.cnsListItem);
                //if elective change color
                if (coursesToShow.get(position).getElective().equals("true")){
                    cns.setBackgroundColor(getResources().getColor(R.color.elective));
                }
                return convertView;
            }
        };
        binding.lstAvailableCourses.setAdapter(adpCoursesToShow);
        //change the label above this list to match what they said

        //capitalize the first letter
        text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        binding.lblAvailableClasses.setText(text + " :  Required Courses Remaining");

        binding.txtAdvisorCurrentCredits.setText("Credits this semester: " + creditsThisSemester);


        //parse the speech and look for Fall or Spring, and then the year
        //speak("Say the number of the class you would like to enroll in. or say exit to go back");

    }


    private void showConfirmEnrollInCourse(int courseClicked){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Uncomment the below code to Set the message and title from the strings.xml file
        //Setting message manually and performing action on button click

        String courseName = adpCoursesToShow.getItem(courseClicked);

        if (courseName.contains("Capstone") && creditsTaken < 70){
            Toast.makeText(this, "You can't enroll in Capstone yet. 70 credits required.", Toast.LENGTH_LONG).show();
            return;
        }

        builder.setMessage("Are you sure you want to enroll in:\n" + courseName + "? Courses with a 'II' will be manually verified by your advisor to ensure prerequisites have been met. Please check your University email inbox. ")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();


                        refCurUserClasses.child(coursesToShow.get(courseClicked).getName()).setValue(coursesToShow.get(courseClicked).getSemester())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){


                                            //and the local database by adding the course tot he completed
                                            completedClasses.add( coursesToShow.get(courseClicked));
                                           /* Course courseToDelete = coursesToShow.get(courseClicked);
                                            //but also removing the course from the required
                                            for(int i = 0; i < notCompleteClasses.size();i++){
                                                Course c = notCompleteClasses.get(i);
                                                if (c.getName().equals(courseToDelete.getName()) && c.getSemester().equals(courseToDelete.getSemester())){
                                                    notCompleteClasses.remove(i);
                                                    break;
                                                }
                                            }

                                            //we also need to remove the string from being displayed in the listview
                                            for(int i = 0; i < adpCoursesToShow.getCount();i++){

                                                if (adpCoursesToShow.getItem(i).equals(courseToDelete.getName()) ){
                                                    adpCoursesToShow.remove(adpCoursesToShow.getItem(i));
                                                    break;
                                                }
                                            }
                                            adpCoursesToShow.notifyDataSetChanged();*/
                                            //TODO adding credits
                                            //since each course is 3 credits add to the credits
                                            creditsTaken += 3;
                                            recognition(lastSpeech);
                                        } else {
                                            Toast.makeText(Advisor.this, "ERROR: could not connect to online database", Toast.LENGTH_SHORT).show();
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
        alert.setTitle("Enroll in Course");
        alert.show();
    }

    private void showPopUp(String title, String msg, String posButton, boolean shouldListen) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Uncomment the below code to Set the message and title from the strings.xml file
        //Setting message manually and performing action on button click


        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(posButton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        if (shouldListen) {
                            listen();
                        }
                    }
                });
        if(shouldListen) {
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
        }
        //Creating dialog box
        AlertDialog alert = builder.create();
        //Setting the title manually
        alert.setTitle(title);
        alert.show();
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }


    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    void loadFirebaseData() {

        //get this users information
        refCurUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Iterable<DataSnapshot> children = snapshot.getChildren();
                String name = snapshot.child("name").getValue(String.class);

                for(DataSnapshot keys : snapshot.getChildren()){
                    int stop = 0;
                    if (keys.getKey().equals("name")){
                        studentName = keys.getValue(String.class);
                    } else  if (keys.getKey().equals("email")){
                        studentEmail = keys.getValue(String.class);
                    } else if (keys.getKey().equals("startYear")){
                        studentStartYear = keys.getValue(String.class);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        //To read the data from firebase use SingleValueEventListener
        refCurUserClasses.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                completedClasses.clear();
                //DATASNAPSHOT - this is what firebase calls a key-value pair in the database
                //so for us the email snapshot would be     {key: email, value: test@student.edu}
                //The datasnapshot is a list of all of the children,
                //where each child is another datasnapshot
                for (DataSnapshot curClassNode : snapshot.getChildren()) {
                    //our nodes have no values, it is just the class name as the
                    //key for the node, and a blank string as the value
                    //so we dont need to get values, the key for each snapshot
                    //should be the class name
                    String className = curClassNode.getKey();
                    Log.d("user_log", "COMPLETE: " + className); //log
                    //TODO, for right now I don't think we need to store the credit or isElective under the student
                    //if we change this and not every class is 3 credits we gotta redo this

                    //so we won't be able to grab it here, just use 0
                    Course newCourse = new Course(className, curClassNode.getValue(String.class),0,isClassElective(className) == 0 ? "false" : "true");
                    creditsTaken += 3;
                    completedClasses.add(newCourse);
                }
               populateNotCompletedClasses("");

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void populateNotCompletedClasses(String semester){
        creditsTaken = 0;
        creditsThisSemester = 0;
        //to through required courses, if that course does not exist in the completed list
        //then add it to the courses left
        notCompleteClasses.clear();
        for (Course c : requiredCourses) {
            int found = courseExistsInList(completedClasses,c);
            if (found == -1) {
                notCompleteClasses.add(c);
            } else {
                creditsTaken += 3;
                if (completedClasses.get(found).getSemester().equalsIgnoreCase(semester) ){
                    creditsThisSemester += 3;
                }
            }
        }
    }
    private int courseExistsInList(ArrayList<Course> courseList, Course course){
        for(int i = 0; i < courseList.size(); i++){
            if (courseList.get(i).getName().equals(course.getName())){// && c.getSemester().equals(course.getSemester())){
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_student, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        int itemID = item.getItemId();
        if (itemID == R.id.menuStudentLogout){
            Toast.makeText(this, "LOGOUT", Toast.LENGTH_SHORT).show();

            //sign out of firebase
            FirebaseAuth.getInstance().signOut();

            //when we start the login page it will look in the SharePrefs for email and password
            //and relogin based on who was logged in before so clear those out
            SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE).edit();
            editor.putString(MainActivity.SHARED_PREF_EMAIL, "NONE");
            editor.putString(MainActivity.SHARED_PREF_PASSWORD, "NONE");
            editor.apply();

            //After we log out show the main screen
            Intent i = new Intent(Advisor.this, MainActivity.class);
            startActivity(i);
            finish();
        } else if (itemID == R.id.menuStudentProfile){
            Intent i = new Intent(Advisor.this, StudentProfile.class);
            i.putExtra("name", studentName);
            i.putExtra("email", studentEmail);
            i.putExtra("startYear", studentStartYear);
            i.putExtra("credits", creditsTaken);
            int creditsFromElectives = 0;
            for(Course c : completedClasses){
                //completed classes doesnt save credit count and elective like required courses does
                //so search that to find out the info
                //send back 0 if it is not an elected, and send back the credit count if it is
                creditsFromElectives += isClassElective(c.getName());
            }
            i.putExtra("electiveCredits", creditsFromElectives);
            startActivity(i);
        } else if (itemID == R.id.menuStudentHelp){
            Intent i = new Intent(Advisor.this, Help.class);
            startActivity(i);
        }
        return true;
    }

    int isClassElective(String name){
        for(Course c : requiredCourses){
            //find the course
           if (c.getName().equals(name)){
               if (c.getElective().equals("true")){
                   return c.getCredits();
               } else {
                   return 0;
               }

           }
        }
        return 0;
    }

    void populateTheFirebaseData() {
       /* -Required Classes
                -CS101 : Spring 2023
                -CS102 : Fall 2023
                -CS103 :


        -Students
                -Unique ID Number (just for the database no a school id number)
                    -email: test@student.edu
                    -classes: (this stores the classes we already took)
                        -CS101 : Fall 2022
                        -CS102 : Spring 2022


          */

        //refRootNode.child("RequiredClasses").removeValue();

        String classNames_1[] = {"CS 101 Comp Sci I",
                "CS 103 Java I",
                "CS 105 Python I",
                "CS 107 Data Structures I",
                "CS 119 Operating Sys I",
                "CS 122 Database Sys I",
                "CS 126 Big Data I",
                "CS 124 Networking I",
                "CS 128 C++ I"};
        String classNames_2[] = {
                "CS 102 Comp Sci II",
                "CS 104 Java II",
                "CS 106 Python II",
                "CS 108 Data Structures II",
                "CS 120 Operating Sys II",
                "CS 123 Database Sys II",
                "CS 125 Networking II",
                "CS 127 Big Data II",
                "CS 129 C++ II"};

        String electives[] = {
                "CS 109 Discrete Math",
                "CS 110 Data Science",
                "CS 111 Data Analytics",
                "CS 112 Robotics",
                "CS 113 Algorithms",
                "CS 115 Cloud Computing",
                "CS 116 Blockchain Tech",
                "CS 117 Architecture",
                "CS 118 Software Eng",
                "CS 121 MIPS",
                "CS 130 Capstone Project"};

        //Half of the classes are Spring 2023, the other half are Fall 2023
        for (int i = 0; i < classNames_1.length / 2; i++) {
            refRootNode.child("RequiredClasses").child(classNames_1[i]).setValue("Fall 2022");
        }

        for (int i = 0; i < classNames_2.length; i++) {
            refRootNode.child("RequiredClasses").child(classNames_2[i]).setValue("Spring 2023");
        }

        for (int i = 0; i < electives.length; i++) {
            if (i < electives.length / 2) {
                refRootNode.child("RequiredClasses").child(electives[i]).setValue("Fall 2022");
            } else {
                refRootNode.child("RequiredClasses").child(electives[i]).setValue("Spring 2023");
            }
        }


        refRootNode.child("Students").child(user.getUid()).child("email").setValue(user.getEmail());

        String classesTaken[] = {"CS 101 Comp Sci I",
                "CS 103 Java I",
                "CS 105 Python I", "CS 109 Discrete Math"};

        for (int i = 0; i < classesTaken.length; i++) {
            refRootNode.child("Students").child(user.getUid()).child("classes").child(classesTaken[i]).setValue("Fall 2022");
        }

    }
}