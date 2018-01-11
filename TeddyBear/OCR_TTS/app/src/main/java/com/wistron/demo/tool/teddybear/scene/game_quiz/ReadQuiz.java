package com.wistron.demo.tool.teddybear.scene.game_quiz;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * Created by tanbo on 16-4-13.
 */
public class ReadQuiz {
    private int roundNow = 1;
    private final int RANDOM_MAX = 3;
    private int random_number = 0;
    private String answer_got = "";
    private String question_got = "";

    private int randomNumber() {
        Random random = new Random();
        random_number = random.nextInt(RANDOM_MAX);
        return random_number + 1;
    }

    public int getRandomNumber() {
        return random_number + 1;
    }

    public int readQuizNextQustion() {
        roundNow++;
        return roundNow;
    }

    public void setRoundNow(int i) {
        this.roundNow = i;
    }

    public int getRoundNow() {
        return this.roundNow;
    }

    public void readRoundQuiz(Context context, String quizFile) {
        int random_one_0f_quiz = randomNumber();
        boolean isGotRound = false, isGotQuestion = false;
        try {
            InputStreamReader is;
            is = new InputStreamReader(context.getAssets().open(quizFile), "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(is);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("Round" + roundNow)) {
                    isGotRound = true;
                    continue;
                }
                if (isGotRound) {
                    if (!isGotQuestion && line.startsWith("Q" + random_one_0f_quiz)) {
                        question_got = line.substring(line.indexOf(":") + 1);
                        isGotQuestion = true;
                    } else if (line.startsWith("A" + random_one_0f_quiz)) {
                        answer_got = line.substring(line.indexOf(":") + 1);
                        break;
                    }
                }
            }
            is.close();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getQuestion() {
        return question_got;
    }

    public String getAnswer() {
        return answer_got;
    }
}