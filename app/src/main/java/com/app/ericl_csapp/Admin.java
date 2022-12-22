package com.app.ericl_csapp;

import static com.app.ericl_csapp.MainActivity.SHARED_PREFS_NAME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.app.ericl_csapp.databinding.ActivityAdminBinding;
import com.app.ericl_csapp.databinding.ActivityCurrentEnrollmentBinding;
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

import java.util.ArrayList;
import java.util.HashMap;




/*

Add Course - shows pop up to add new course
Add Student - shows pop up to add new student
Click on course in list to edit
Long Click on item in Course List - delete course


 */
public class Admin extends AppCompatActivity {
    //TODO do we need to be able to delete a student?
    ActivityAdminBinding binding;
    ArrayList<String> currentStudents = new ArrayList<>();
    ArrayList<Course> requiredCourses = new ArrayList<>();
    ArrayList<String> requiredCoursesStr = new ArrayList<>();
    ArrayAdapter<String> adpStudents, adpCourses;
    DatabaseReference refUsers, refRequiredCourses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        refUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        refRequiredCourses = FirebaseDatabase.getInstance().getReference().child("RequiredCourses_v2");
        //TODO get the firebase data, current students
        loadFirebaseData();

        //FOR TESTING TAKE OUT
        //removeDeletedCourseFromAllStudents("");

        //the required courses get passed in so get them out
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            requiredCourses = extras.getParcelableArrayList("requiredCourses");
        }

        binding.btnAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddStudentDialog();
            }
        });

        binding.btnAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddCourseDialog(null);
            }
        });
        //setup the list of students
        adpStudents = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, currentStudents);
        binding.lstActiveStudents.setAdapter(adpStudents);


        //setup the list of courses
        requiredCoursesStr = Course.convertCourseListToStrings(requiredCourses);
        adpCourses = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, requiredCoursesStr){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                //View view = super.getView(position, convertView, parent);
                //if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
               // }

                TextView txt = (TextView) convertView.findViewById(R.id.txtListItem_name);
                txt.setText(requiredCourses.get(position).getName());

                TextView txtSemester = (TextView) convertView.findViewById(R.id.txtListItem_semester);
                txtSemester.setText(requiredCourses.get(position).getSemester());

                ConstraintLayout cns = convertView.findViewById(R.id.cnsListItem);
                //if elective change color
                if (requiredCourses.get(position).getElective().equals("true")){
                    cns.setBackgroundColor(getResources().getColor(R.color.elective));
                }
                return convertView;
            }
        };
        binding.lstAdminRequiredCourses.setAdapter(adpCourses);
        binding.lstAdminRequiredCourses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showAddCourseDialog(requiredCourses.get(i));
            }
        });
        binding.lstAdminRequiredCourses.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                showDeleteCourseDialog(i);
                return true;
            }
        });
    }

    void loadFirebaseData(){

        refUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //all the children of this snapshot should be every user
                Iterable<DataSnapshot> allUsers = snapshot.getChildren();

                //go through each user and also go through all of their children to grab the
                //sub data, but only add a user if the type is student
                for(DataSnapshot user : allUsers){

                    Iterable<DataSnapshot> values = user.getChildren();
                    String type ="", startYear = "", email = "", name = "";
                    for(DataSnapshot value : values){
                        if (value.getKey().equals("type")){
                            type = value.getValue(String.class);
                        } else if (value.getKey().equals("startYear")){
                            startYear = value.getValue(String.class);
                        } else if (value.getKey().equals("name")){
                            name = value.getValue(String.class);
                        } else if (value.getKey().equals("email")){
                            email = value.getValue(String.class);
                        }
                    }
                    if (type.equals("student")){
                        currentStudents.add(name);
                    }

                }
                adpStudents.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    void showAddStudentDialog(){
        Dialog dialog = new Dialog(Admin.this);
        dialog.setContentView(R.layout.dlg_add_student);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);

        EditText edtName = dialog.findViewById(R.id.edtName);
        EditText edtEmail = dialog.findViewById(R.id.edtEmail);
        EditText edtYear = dialog.findViewById(R.id.edtStartYear);
        Button btnAddStudent = dialog.findViewById(R.id.btnDlgStudentAddStudent);
        ImageButton btnClose = dialog.findViewById(R.id.btnDlgStudentClose);


        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //grab the information from the screen
                String name = edtName.getText().toString();
                String email = edtEmail.getText().toString();
                String startYear = edtYear.getText().toString();
                //check to make sure they are all filled in, and the email is
                //in email format
                if (email.length() == 0 || name.length() == 0 || startYear.length() == 0){
                    Toast.makeText(Admin.this, "Please verify your information is correct!", Toast.LENGTH_LONG).show();
                    return;
                }

                //check email format
                if (!email.contains("@student.edu")){
                    Toast.makeText(Admin.this, "Please enter a valid email address, ending with @student.edu", Toast.LENGTH_LONG).show();
                    return;
                }

                //sign up this used with firebase, for now just make the password "password"
                //but we would generate a 10 digit temporary password that the student would change
                //after logging in
                FirebaseAuth auth = FirebaseAuth.getInstance();
                auth.createUserWithEmailAndPassword(email, "password").addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){

                            //make this student a node in the database
                            FirebaseUser user = auth.getCurrentUser();
                            //check null user
                            if (user == null){
                                Toast.makeText(Admin.this, "User is null after creating account", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            //try to retrieve this persons information
                            HashMap<String,String> firebaseData = new HashMap<>();
                            firebaseData.put("type","student");
                            firebaseData.put("email",email);
                            firebaseData.put("startYear",startYear);
                            firebaseData.put("name",name);


                            refUsers.child(user.getUid()).setValue(firebaseData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        //TODO add this new student to the list and reload the list of students
                                        currentStudents.add(name);
                                        adpStudents.notifyDataSetChanged();
                                        dialog.dismiss();
                                    } else {
                                        Toast.makeText(Admin.this, "Error: could not create student node after signing up!", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                        } else {
                            Toast.makeText(Admin.this, "Error: could not sign up with student information", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialog.dismiss();

            }
        });

        dialog.show();
    }

    void showAddCourseDialog(Course c){
        Dialog dialog = new Dialog(Admin.this);
        dialog.setContentView(R.layout.dlg_add_course);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);

        EditText edtCourseName = dialog.findViewById(R.id.edtCourseName);
        EditText edtCredits = dialog.findViewById(R.id.edtCredits);
        EditText edtYear = dialog.findViewById(R.id.edtYear);
        Button btnAddCourse = dialog.findViewById(R.id.btnDlgCourseAddCourse);
        ImageButton btnClose = dialog.findViewById(R.id.btnDlgCourseClose);
        CheckBox chkElective = dialog.findViewById(R.id.chkElective);
        Spinner spnSemesters = dialog.findViewById(R.id.spnDlgCourseSemesters);

        //setup semester spinner
        String semesters[] = {"Winter", "Spring", "Summer", "Fall"};
        ArrayAdapter<String> adpSemesters = new ArrayAdapter<>(Admin.this, android.R.layout.simple_list_item_1,semesters);
        spnSemesters.setAdapter(adpSemesters);

        //if we passed in a course that means we're trying to edit it so populate the data
        if (c != null){
            edtCourseName.setText(c.getName());
            edtCredits.setText(String.valueOf(c.getCredits()));
            edtYear.setText(c.getSemester().split(" ")[1]);
            String semester = c.getSemester().split(" ")[0];
            String isElective = c.getElective();
            //TODO elective
            if (isElective != null && isElective.equals("true")){
                chkElective.setChecked(true);
            }
            for( int i =0 ; i < semesters.length; i++){
                if (semesters[i].equals(semester)){
                    spnSemesters.setSelection(i);
                    break;
                }
            }
            btnAddCourse.setText("Update Course");
        }

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //first get all the information on the screen
                String courseName = edtCourseName.getText().toString();
                String creditStr = edtCredits.getText().toString();
                String yearStr = edtYear.getText().toString();
                String semester = semesters[spnSemesters.getSelectedItemPosition()];
                String isElective = String.valueOf( chkElective.isChecked() );

                //check to make sure everything was filled in
                if (courseName.length() == 0 || creditStr.length() == 0 || yearStr.length() == 0 || semester.length() == 0 ){
                    Toast.makeText(Admin.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                //check that this name does not already exist
                for(Course c : requiredCourses){
                    if (c.getName().equals(courseName)){
                        Toast.makeText(Admin.this, "Two courses can not have the same name, please rename!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                int year = Integer.parseInt( yearStr);
                int credits = Integer.parseInt( creditStr );

                if ( credits <= 0 || credits > 4){
                    Toast.makeText(Admin.this, "Please enter a number from 1 to 4 for credits.", Toast.LENGTH_SHORT).show();
                    return;
                }

                //combine the semester and year
                semester += " " + String.valueOf(year);

                //Make a map of the data that will be saved in key value pairs on the firebase
                HashMap<String, Object> data = new HashMap<>();
                data.put("name", courseName);
                data.put("semester", semester);
                data.put("credits", credits);
                data.put("elective", isElective);

                //if we have a course coming in then update
                if (c != null){
                    String finalSemester1 = semester;
                    refRequiredCourses.child(c.getKey()).updateChildren(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(Admin.this, "Course information was updated!", Toast.LENGTH_SHORT).show();

                            //find it locally and update
                            boolean rename = false;
                            for (int i = 0; i < requiredCourses.size(); i++) {
                                if (requiredCourses.get(i).getKey().equals(c.getKey())){
                                    //if previous name is not equal to the name now
                                    if (!requiredCourses.get(i).getName().equals(c.getName())){
                                        rename = true;
                                    }


                                    Course newCourse = new Course(courseName, finalSemester1,credits, isElective);
                                    newCourse.setKey(c.getKey());
                                    requiredCourses.set(i,newCourse);
                                    break;
                                }
                            }
                            //if rename happened repopulate the list
                            if (rename) {
                                requiredCoursesStr.clear();
                                for (Course c : requiredCourses) {
                                    requiredCoursesStr.add(c.getName());
                                }
                                adpCourses.notifyDataSetChanged();
                            }
                            dialog.dismiss();
                            }

                        }
                    });
                    return;
                }



                //create a unique node to save this new course
                DatabaseReference newNode = refRequiredCourses.push();
                //set the data at the new node
                String finalSemester = semester;
                newNode.setValue( data ).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            //this means the course was added in firebase, add it to the local list of required
                            //courses
                            Course newCourse = new Course(courseName, finalSemester,credits, isElective);
                            newCourse.setKey(newNode.getKey());
                            requiredCourses.add(newCourse);
                            requiredCoursesStr.add(courseName);
                            adpCourses.notifyDataSetChanged();
                            Log.d("user", "Added a new course to firebase: " + courseName);
                            dialog.dismiss();

                        }
                    }
                });
            }
        });

        dialog.show();
    }

    private void showDeleteCourseDialog(int courseClicked){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Uncomment the below code to Set the message and title from the strings.xml file
        //Setting message manually and performing action on button click

        String courseToDelete = adpCourses.getItem(courseClicked);
        String keyToDelete = requiredCourses.get(courseClicked).getKey();
        builder.setMessage("Are you sure you want to delete:\n" + courseToDelete)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();

                        refRequiredCourses.child(keyToDelete).removeValue()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){

                                            //but also removing the course from the required
                                            for(int i = 0; i < requiredCourses.size();i++){
                                                Course c = requiredCourses.get(i);
                                                if (c.getName().equals(courseToDelete)) {
                                                    requiredCourses.remove(i);
                                                    requiredCoursesStr.remove(i);
                                                    break;
                                                }
                                            }
                                            adpCourses.notifyDataSetChanged();
                                            Toast.makeText(Admin.this, "Deleted " + courseToDelete + " from database.", Toast.LENGTH_SHORT).show();
                                            removeDeletedCourseFromAllStudents(courseToDelete);
                                        } else {
                                            Toast.makeText(Admin.this, "ERROR: could not delete from online database", Toast.LENGTH_SHORT).show();
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
        alert.setTitle("Delete Required Course");
        alert.show();
    }

    void removeDeletedCourseFromAllStudents(String courseToDelete){


        refUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //go through all user nodes
                Iterable<DataSnapshot> allUsers = snapshot.getChildren();
                for(DataSnapshot user : allUsers) {

                    //go through this users sub nodes
                    Iterable<DataSnapshot> subNodes = user.getChildren();
                    for(DataSnapshot subNode : subNodes){
                        //if they have a classes node then get the classes
                        if (subNode.getKey().equals("classes")) {
                           Iterable<DataSnapshot> classes = subNode.getChildren();
                           for(DataSnapshot thisClass : classes){
                               //if this class matches the class we're trying to delete then get rid of the node
                               if (thisClass.getKey().equals(courseToDelete)) {
                                  // Log.d("user", "Removed class from student: " + thisClass.getKey());
                                   refUsers.child(user.getKey()).child("classes").child(courseToDelete).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                       @Override
                                       public void onComplete(@NonNull Task<Void> task) {
                                           if (task.isSuccessful()){
                                               Log.d("user", "Removed class from student: " + thisClass.getKey());
                                           }
                                       }
                                   });
                               }
                            }
                        }
                    }


                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_admin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        int itemID = item.getItemId();
        if (itemID == R.id.menuStudentLogout){
            Toast.makeText(this, "LOGGED OUT", Toast.LENGTH_SHORT).show();

            //sign out of firebase
            FirebaseAuth.getInstance().signOut();

            //when we start the login page it will look in the SharePrefs for email and password
            //and relogin based on who was logged in before so clear those out
            SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putString(MainActivity.SHARED_PREF_EMAIL, "NONE");
            editor.putString(MainActivity.SHARED_PREF_PASSWORD, "NONE");
            editor.apply();

            //After we log out show the main screen
            Intent i = new Intent(Admin.this, MainActivity.class);
            startActivity(i);
            finish();
        }
        return true;
    }
}