package com.isaacna.projects;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MessagesActivity extends AppCompatActivity {

    public static int lastLength = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        Intent in = getIntent();
        String otherName = in.getStringExtra("other_name"); //name of the person you are messaging (from matches activity)
        TextView otherTextView = findViewById(R.id.messageName);

        otherTextView.setText(otherName); //set who your messaging's name
       // new RetrieveMessagesTask(this).execute(in.getIntExtra("swipe_id",-1)); //get swipe id and pass to retrieve messages task

        makeABunchOfCalls();
    }

    private void makeOneCall(){
        new RetrieveMessagesTask(this).execute(getIntent().getIntExtra("swipe_id",-1));
    }

    public void viewProfile(View view) {
        Intent in = new Intent(this, MatchProfileActivity.class);
        in.putExtras(getIntent());
        startActivity(in);
    }

    private void makeABunchOfCalls() {
        final Handler handler = new Handler();
        Timer timer = new Timer();

        MessagesActivity activity = this;




        TimerTask doRetriveMessages = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            makeOneCall();
                        } catch (Exception e) {
                            System.out.println("uh oh spag get the o");
                        }
                    }
                });
            }
        };
        timer.schedule(doRetriveMessages, 0, 1000);
    }


    public void sendMessage(View view) {
        Intent in = getIntent();

        String user_id = Integer.toString(in.getIntExtra("userID",0)); //replace with global user
        EditText messageBox = findViewById(R.id.messageBox);
        String body = messageBox.getText().toString();
        int swipe_id = in.getIntExtra("swipe_id",-1);
        int other_id = in.getIntExtra("other_id", -1);

    try {
        String s = new SendMessageTask(this).execute(Integer.toString(swipe_id), user_id, Integer.toString(other_id), body).get();
    }
    catch (Exception e){
    }

        new RetrieveMessagesTask(this).execute(in.getIntExtra("swipe_id",-1));
        messageBox.setText("");
    }

    public void unmatch(View view) {
        new UnmatchTask(this).execute();
        Intent in = new Intent(this, MainActivity.class);
        in.putExtras(getIntent());
        startActivity(in);
    }

//change
    class RetrieveMessagesTask extends AsyncTask<Integer, String, String> {

        private Exception exception;
        String response = "";
        StringBuilder result = new StringBuilder();
        public MessagesActivity activity;
        Intent in;

        //this constructor is to pass in the communitiesactivity to access within onpostexecute
        public RetrieveMessagesTask(MessagesActivity a) {
            this.activity = a;
            in = activity.getIntent();

        }

        protected String doInBackground(Integer... params) {

            try {
                int swipe_id = params[0]; //get my swipe_id from params

                URL url = new URL("http://ec2-34-215-159-222.us-west-2.compute.amazonaws.com/alt/getMessages.php?swipe_id=" + swipe_id );
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                //System.out.println(urls);
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));


                //put up here so it happens regardless of message content
//                String otherName = in.getStringExtra("other_name"); //name of the person you are messaging (from matches activity)
//                TextView otherTextView = findViewById(R.id.messageName);
//
//                otherTextView.setText(otherName); //set who your messaging's name


                while ((line = br.readLine()) != null) {
                    result.append(line);
                    response += result.toString();
                    System.out.println(response);

                }
                br.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            //do stuff


            LinearLayout ln = (LinearLayout) findViewById(R.id.messagesLayout);
            ln.removeAllViews();


            try {
                //response is a json array
                JSONArray messagesJson = new JSONArray(result);

                System.out.println("Messages down below");
                System.out.println(messagesJson.toString());


//                otherTextView.setTextSize(30);

                //go through json array and add the id and name of each match to the map
                for(int i=0; i < messagesJson.length(); i++) {

                    JSONObject jsonobject = messagesJson.getJSONObject(i);

                    int receiver_id = jsonobject.getInt("receiver_id"); //id of the person who receives this message

                    String messageBody = jsonobject.getString("body"); //body of the message

                    int otherId = in.getIntExtra("other_id",-1); //id of the person you are messaging (passed in from matches activity)
                    //create new textview
                    TextView tv = new TextView(activity);
                    tv.setText(messageBody);
                    tv.setId(i);
                    tv.setTextSize(20);
//                    System.out.println("receiver id: " + receiver_id + " otherId: " + otherId );
//                    System.out.println("body: " + messageBody);
                    if (receiver_id == otherId) {//message goes to to other person
//                        System.out.println("should go on the right");
                        tv.setBackgroundColor(Color.LTGRAY);
                        tv.setTextColor(Color.BLACK);
                        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT); //align message to right
                    }
                    else { //messages goes to you
//                        System.out.println("should go on the left");
                        tv.setBackgroundColor(Color.BLUE);
                        tv.setTextColor(Color.WHITE);
                        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT); //align message right
                    }
                    ln.addView(tv);
                }
                TextView tv = new TextView(activity);
                tv.setText("\n");
                tv.setId(Integer.MAX_VALUE);
                tv.setTextSize(8);
                ln.addView(tv);
                if (messagesJson.length() > lastLength){
                    ScrollView scroll = findViewById(R.id.scrollView3);
                   // scroll.scrollTo(0, scroll.getBottom());
                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                    lastLength = messagesJson.length();
                }
            }
            catch (JSONException e){
                System.out.println(e);
            }
        }

    }


    class SendMessageTask extends AsyncTask<String, String, String> {

        private Exception exception;
        String response = "";
        StringBuilder result = new StringBuilder();
        public MessagesActivity activity;

        public SendMessageTask(MessagesActivity a) {
            this.activity = a;
        }

        protected String doInBackground(String... params) {

            try {
                String swipe_id = params[0];
                String sender_id = params[1];
                String receiver_id = params[2];
                String body = params[3];

                String u = "http://ec2-34-215-159-222.us-west-2.compute.amazonaws.com/alt/sendMessage.php?swipe_id=" + swipe_id +
                        "&sender_id=" + sender_id + "&receiver_id="+ receiver_id + "&body=" + body;
                URL url = new URL(u);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));


                while ((line = br.readLine()) != null) {
                    result.append(line);
                    response += result.toString();
                    System.out.println(response);
                    //communitiesList.add(result.toString());

                }
                br.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
    }


    class UnmatchTask extends AsyncTask<Integer, String, String> {

        private Exception exception;
        String response = "";
        StringBuilder result = new StringBuilder();
        public MessagesActivity activity;

        //this constructor is to pass in the communitiesactivity to access within onpostexecute
        public UnmatchTask(MessagesActivity a) {
            this.activity = a;
        }

        protected String doInBackground(Integer... params) {

            try {
                int user_id = getIntent().getIntExtra("userID",-1); //get my user_id from intent
                int swipe_id = getIntent().getIntExtra("swipe_id",-1); //get comm_id from intent

                URL url = new URL("http://ec2-34-215-159-222.us-west-2.compute.amazonaws.com/alt/unmatch.php?user_id=" + user_id + "&swipe_id=" + swipe_id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                //System.out.println(urls);
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));


                while ((line = br.readLine()) != null) {
                    result.append(line);
                    response += result.toString();
                    System.out.println(response);

                }
                br.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return result.toString();
        }


    }
}