package com.nkm90.HearMeWhenYouCanNotSeeMe;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity {
    private TextToSpeech mTTS;
    public EditText mEditText;
    private SeekBar mSeekBarPitch;
    private SeekBar mSeekBarSpeed;
    private Button mButtonSpeak;
    private static final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        //link elements to the id of them
        Button mButtonMP = findViewById(R.id.btn_MP);
        Button mButtonListen = findViewById(R.id.btn_stt);
        mButtonSpeak = findViewById(R.id.btn_tts);
        mEditText = findViewById(R.id.etResult);
        mSeekBarPitch = findViewById(R.id.seek_bar_pitch);
        mSeekBarSpeed = findViewById(R.id.seek_bar_speed);
        String actualLanguage = getLanguage();

        /*
          mTTS is initialised with the TTS method taking the actual language tag from the app,
          to provide better user experience.
          */
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    int result = 0;
                    if (actualLanguage.equals("en")){
                        result = mTTS.setLanguage(Locale.forLanguageTag(actualLanguage));
                    } else if (actualLanguage.equals("es")){
                        result = mTTS.setLanguage(Locale.forLanguageTag(actualLanguage));
                    }

                    if(result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("TTS", "Language not supported");
                    } else{
                        mButtonSpeak.setEnabled(true);
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });

        /*
          On click listener that launch the MediaPipe activity when buttonMP is pressed.
          The method openMP takes the view and the MediaPipe.class activity as parameters to
          launch the intent.
         */
        mButtonMP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMP(v, MediaPipeActivity.class);
            }
        });

        /*
           On click listener that launch the Speech recognition when button Listen is pressed.
           The method speakIn takes the parameter actual language for better speech recognition
           based on the actual language of the app.
         */
        mButtonListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakIn(actualLanguage);
            }
        });

        /* On click listener for the button Speak that launch the method speakOut.
          This method speaks back the text from the editText results.
         */
        mButtonSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakOut();
            }
        });
    }

    /**Method that launch the intent to open the speech recogniser dialog based on the current language.
     *
     * @param language String value with the tag of the current language
     */
    private void speakIn(String language){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");
        if (language.equals("en")){
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to transfer into text");
        }else if (language.equals("es")){
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla para convertir en texto");
        }

        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to transfer into text");

        try {
            //if there is not error, open dialog. if there is an error, display a message with it
            startActivityForResult(intent, 2);
        } catch (Exception exception){
            Toast.makeText(this, ""+exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method to launch the MediaPipe hand tracking solution from its activity using an intent,
     * that will return an String containing the message obtained on the gesture recognition.
     * This will be handle by the onActivityResult method below.
     *
     * @param view
     * @param activity It makes reference to the activity class for MediaPipe
     */
    public void openMP(View view, Class<MediaPipeActivity> activity) {
        Intent intent = new Intent(this, activity);
        startActivityForResult(intent, 1);
    }

    /**Method that handles the results obtained from the different intents with activityForResults.
     * In case of MediaPipe intent, the back button provides a result of "Back"
     *
     * @param requestCode The code belonging to the different intents, being 1 for MediaPipe, and 2 for speakIn
     * @param resultCode Code that helps to identify the Extra data passed from the intents
     * @param data The information obtained from the intent
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1){
            String message = data.getStringExtra("MESSAGE");
            assert message != null;
            if (!message.equals("Back")){
                mEditText.setText(message);
            }/*else {
                //handling when back was pressed, do nothing
                mEditText.setText("Back is pressed");
            }*/
        }else if (requestCode==2){
            if (resultCode== RESULT_OK && null!=data){
                //set an array list with the results obtained from the voice recogniser intent
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                //set the result on the editText
                mEditText.setText(result.get(0));
            }
        }
    }

    /**
     * Private method that gets the text from the box and the settings from the bars to get the
     * speech back function.
     */
    private void speakOut(){
        String text = mEditText.getText().toString();

        float pitch = (float)mSeekBarPitch.getProgress() / 50;
        if (pitch < 0.1) pitch = 0.1f;

        float speed = (float)mSeekBarSpeed.getProgress() / 50;
        if (speed < 0.1) speed =0.1f;

        mTTS.setPitch(pitch);
        mTTS.setSpeechRate(speed);

        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    /**Load the language saved on the shared preferences.
     *
     * @return language String with the language.
     */
    public String getLanguage(){
        SharedPreferences prefs = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        return prefs.getString("My_Lang", "");
    }

    /*LIFECYCLE INTEGRATION
     * With the aim of keeping track of the different states that this activity is changing.
     * I just basically logs a message to the console as no other function is needed in this case*/
    @Override
    protected void onStart()
    {
        super.onStart();
        Log.d("ActivityLifeCycle", "Menu Activity - onStart");
    }

    @Override
    protected void onRestart()
    {
        Log.d("ActivityLifeCycle", "Menu Activity - onRestart");
        super.onRestart();
    }

    @Override
    protected void onResume()
    {
        Log.d("ActivityLifeCycle", "Menu Activity - onResume");
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        Log.d("ActivityLifeCycle", "Menu Activity - onPause");
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        Log.d("ActivityLifeCycle", "Menu Activity - onStop");
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        if (mTTS != null){
            mTTS.stop();
            mTTS.shutdown();
        }
        Log.d("ActivityLifeCycle", "Menu Activity - onDestroy");
        super.onDestroy();
    }
}