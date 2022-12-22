package com.app.ericl_csapp;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

//make this parcelable so we can pass it between pages
public class Course implements Parcelable {
    private String name;
    private String semester;
    private int credits;
    private String key;
    //parcelable wants the latest android to do a boolean so use string to be safe with old device
    private String elective;

    //required for firebase to auto convert the data to class object
    public Course(){

    }

    public Course(String name, String semester, int credits, String elective) {
        this.name = name;
        this.semester = semester;
        this.credits = credits;
        this.elective = elective;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String toString(){
        return name + " : " + semester;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public int getYear(){
        return Integer.parseInt( semester.split(" ")[1] );
    }
    public Course(Parcel in){
       name = in.readString();
       semester= in.readString();
       credits = in.readInt();
       key = in.readString();
       elective = in.readString();
    }

    public String getElective() {
        return elective;
    }

    public void setElective(String isElective) {
        this.elective = isElective;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
       dest.writeString(name);
       dest.writeString(semester);
       dest.writeInt(credits);
       dest.writeString(key);
       dest.writeString(elective);
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Course createFromParcel(Parcel in) {
            return new Course(in);
        }

        public Course[] newArray(int size) {
            return new Course[size];
        }
    };

    public static ArrayList<String> convertCourseListToStrings(ArrayList<Course> courses){
        ArrayList<String> output = new ArrayList<>();
        for(Course c : courses){
            output.add(c.getName());
        }
        return output;
    }
}
