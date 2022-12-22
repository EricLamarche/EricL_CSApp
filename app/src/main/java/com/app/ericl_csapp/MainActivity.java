package com.app.ericl_csapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.app.ericl_csapp.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/*Project: A Voice-Activated Undergraduate Computer Science Advising System

This product allows a student whose major or minor is Computer Science or IT to obtain automated
advising from the system pertaining to the appropriate course which needs to be taken and which
courses the student permitted to take at a particular juncture during the pursuit of the degree.

*/


/*
test@student.edu
password

Due 12/19
TODO
Admin
    -Each administrator would get added from the backend of the database and they just signin
     and we grab the type of user that they are and show the correct pages

Firebase Data Layout
-Required Classes
    -Unique Id Number
        -name : CS101 : intro to C
        -semester : Spring 2023
        -credits : 3
     -Unique Id Number
        -name : -CS102 : some description
        -semester : Fall 2024
        -credits : 3

-Users
    -Unique ID Number (just for the database no a school id number)
        -type: student
        -name: test
        -email: test@student.edu
        -startYear : 2022
        -classes: (this stores the classes we already took)
            -CS101
     -Unique ID Number (just for the database no a school id number)
        -type: admin
        -email: admin@student.edu
        -name:



 */

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "user_log";
    public static final String SHARED_PREFS_NAME = "preferences";
    public static final String SHARED_PREF_EMAIL = "email";
    public static final String SHARED_PREF_PASSWORD = "password";
    private boolean isWelcomeBack = false;

    FirebaseDatabase db = FirebaseDatabase.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    DatabaseReference refRequiredCourses;

    SharedPreferences sharedPref;
    FirebaseUser user;
    ArrayList<Course> requiredCourses = new ArrayList<>();

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        refRequiredCourses = FirebaseDatabase.getInstance().getReference().child("RequiredCourses_v2");

        sharedPref = getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE);
        //try to get the information from the shared preferences
        //the first argument is the key that we saved our data under, in this case "email"
        //the second arugment is the default value to use if the "email" tag is not fount
        String email = sharedPref.getString(SHARED_PREF_EMAIL, "NONE");
        if (email.equals("NONE")){
            //Toast.makeText(this, "No user logged in on this device yet", Toast.LENGTH_SHORT).show();
        } else {
            String password = sharedPref.getString(SHARED_PREF_PASSWORD, "NONE");
            Toast.makeText(this, "FOUND PREVIOUS USER: " + email, Toast.LENGTH_LONG).show();
            isWelcomeBack = true;
            signIntoFirebase(email,password);
        }


        //if the code gets here then no user was logged int
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Clicked login button");
                //grab the email and password from the screen
                String email = binding.edtUsername.getText().toString();
                String password = binding.edtPassword.getText().toString();

                if (email.length() == 0 || password.length() == 0){
                    Toast.makeText(MainActivity.this, "Please verify all info is correct!", Toast.LENGTH_SHORT).show();
                    return; //exits the current function
                }

                //we only get to this point if the email and password are filled in
                //so try to log into firebase
                signIntoFirebase(email,password);
            }
        });
    }

    void signIntoFirebase(String email, String password){
        //use the authorization variable to sign in with the email and password
        //this comes from firebase library
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //once the sign in task has completed we should check if it was successful
                        //or not
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success"); //this prints to terminal its for developer debugging

                            //since we logged in we should get the information for the current user
                            //from the auth variable
                            user = auth.getCurrentUser();
                            //Since we're logging in that means we didnt have any data already
                            //saved into the shared preferences so we have to save them now
                            //that way in the future we dont need to come back to this screen.
                            //On the main activity we will just log in the last user that was logged in.
                            //Having an auto login feature means we will need to make a log out button at some point

                            //only save this if the check box is checked
                            if (binding.chkRemember.isChecked()) {
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(SHARED_PREF_EMAIL, email);
                                editor.putString(SHARED_PREF_PASSWORD, password);
                                editor.apply();
                            }

                            loadFirebaseData();



                        } else {
                            // If the log in fails suggest to the user that they should sign up
                            //by showing a message
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            //TODO we should try to check why it failed so we can show a
                            //better error (what if this person is already signed up?)
                            Toast.makeText(MainActivity.this, "Verify Username/Password of: " + email, Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    void getCurUserData(){

        DatabaseReference refUser = FirebaseDatabase.getInstance().getReference().child("Users");

        //try to retrieve this persons information
        refUser.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String username = snapshot.child("name").getValue(String.class);
                String type = snapshot.child("type").getValue(String.class);
                //this should return a snapshot with children?
                //type
                //email
               // for(DataSnapshot snap : snapshot.getChildren()){

                   // if (snap.getKey().equals("type")){
                       // String type = snap.getValue(String.class);

                        if (type != null && type.equals("admin")){
                            //show admin page
                            Intent intent = new Intent(MainActivity.this, Admin.class);
                            //we need to pass in the list of required courses
                            intent.putParcelableArrayListExtra("requiredCourses", requiredCourses);
                            startActivity(intent);
                        } else if (type != null && type.equals("student")){
                            //if we logged in we should go back to the main activity.
                            //to do that we just close this activity because it was placed ON TOP
                            //of the main activity. Activities get stacked like cards. You can only ever take
                            //the top card off first. You shouldnt recreate another "Main Activity" to go
                            //on top of this LogIn page
                            finish(); //close this activity, which ends up showing the main activity
                            //that was sitting underneath
                            //the finish() function is built into android
                            //decide which page to open for student or admin
                            //open the next page
                            Intent intent = new Intent(MainActivity.this, SplashScreen.class);
                            intent.putParcelableArrayListExtra("requiredCourses", requiredCourses);
                            intent.putExtra("isWelcomeBack", isWelcomeBack);
                            intent.putExtra("username", username);
                            startActivity(intent);
                        }
                   // }
               // }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
            }

    void loadFirebaseData() {
        //To read the data from firebase use SingleValueEventListener
        refRequiredCourses.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                requiredCourses.clear();
                //DATASNAPSHOT - this is what firebase calls a key-value pair in the database
                //so for us the email snapshot would be     {key: email, value: test@student.edu}
                //The datasnapshot is a list of all of the children,
                //where each child is another datasnapshot
                for (DataSnapshot curCourseNode : snapshot.getChildren()) {

                    Course newCourse = curCourseNode.getValue(Course.class);
                    newCourse.setKey(curCourseNode.getKey());
                    requiredCourses.add(newCourse);
                }
                getCurUserData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }



}