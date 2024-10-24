package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.TimeManager;
import nz.ac.auckland.se206.prompts.PromptEngineering;
import nz.ac.auckland.se206.speech.MP3Player;

/**
 * The SubmitAnswerController class manages the submission of answers in the guessing game. It
 * includes interactions with the OpenAI GPT model, progress bar handling, and timer management.
 */
public class SubmitAnswerController {
  private static String feed; // Feedback from GPT response
  protected static String thief; // Selected thief by the player
  protected static String answer; // Player's answer
  private static boolean isFirstTime = true; // Flag to check if it's the player's first interaction
  public static TimeManager timeManager =
      TimeManager.getInstance(); // Singleton instance of TimeManager
  private static int counter = 0;

  /**
   * Gets the current feedback that the bot has written.
   *
   * @return the feedback from the GPT response
   */
  public static String getFeed() {
    return feed;
  }

  /**
   * Gets the current selected thief that the player has chosen.
   *
   * @return the thief chosen by the player
   */
  public static String getThief() {
    return thief;
  }

  /**
   * Gets the current answer the player has provided.
   *
   * @return the answer provided by the player
   */
  public static String getAnswer() {
    return answer;
  }

  /**
   * Sets the value of the isFirstTime flag.
   *
   * @param isFirstTime boolean indicating if it's the player's first interaction
   */
  public static void setIsFirstTime(boolean isFirstTime) {
    SubmitAnswerController.isFirstTime = isFirstTime;
  }

  /** Resets the submit answer controller state, including the timer, feed, and thief data. */
  public static void reset() {
    feed = null;
    thief = null;
    answer = null;
    isFirstTime = true; // Reset to ensure the timer logic behaves correctly
  }

  /**
   * Initiates the answer submission process.
   *
   * @return a map containing the answer and thief
   */
  public static Map<String, String> intiateanswer() {
    Map<String, String> map = new HashMap<>();
    map.put("answer", answer); // Put the answer in the map
    map.put("thief", thief); // Put the selected thief in the map
    return map;
  }

  @FXML private Button submitButton; // Button to submit the answer
  @FXML private Label mins; // Label to display minutes
  @FXML private Label secs; // Label to display seconds
  @FXML private ProgressBar progressBar; // Progress bar for task progress
  @FXML private Rectangle janitor; // Rectangle representing the janitor suspect
  @FXML private Rectangle hos; // Rectangle representing the head of security suspect
  @FXML private Rectangle curator; // Rectangle representing the curator suspect
  @FXML private TextArea feedback; // Text area for feedback
  @FXML private TextArea feedback2; // Another text area for feedback

  @FXML private Label dot;

  @FXML private TextArea answerTxtArea; // Text area for player's answer
  private Timeline timeline; // Timeline for timer updates
  private ChatCompletionRequest chatCompletionRequest; // Request object for GPT interaction
  private boolean once = true;
  private boolean sendOnce = true;
  private MP3Player player;

  /**
   * Initializes the controller, setting up the timer and resetting the first-time flag if
   * necessary.
   */
  public void initialize() throws URISyntaxException {
    player = new MP3Player("src/main/resources/sounds/button.mp3");
    if (isFirstTime) {
      timeManager.stopTimer(); // Stop the timer if it's the first time
      timeManager.setInterval(60); // Set the timer interval to 60 seconds
    }

    // Initialize the counter to the interval
    timeManager.startTimer(); // Start the timer
    timeManager.setTimerLabel(mins, secs, dot); // Set the timer u

    // Set up the timeline to run every second
    timeline =
        new Timeline(
            new KeyFrame(
                Duration.seconds(1),
                event -> {
                  try {
                    updateTimer(); // Call the updateTimer() method with proper exception handling
                  } catch (IOException e) {
                    e.printStackTrace(); // Handle the IOException
                  }
                }));
    timeline.setCycleCount(Timeline.INDEFINITE); // Repeat indefinitely
    timeline.play(); // Start the timeline
  }

  private void updateTimer() throws IOException {
    counter = timeManager.getInterval();

    if (counter <= 0) {
      timeline.stop(); // Stop the timer when counter reaches 0
      counter = 0; // Ensure counter stays at 0
      if (thief == null) {
        feed = "You did not select a suspect in time!";
        EndingController.setFeed(feed);
        App.setRoot("badending");
        return;
      }

      if ((answer == null || answer.equals("")) && sendOnce) {
        if (thief.equals("hos")) {
          feed =
              "You arrested the head of security however he went to court and the judge found him"
                  + " not guilty due to lack of evidence from the dectetive";
        } else {
          feed =
              "You arrested the "
                  + thief
                  + " however he went to court and the judge found him not guilty due to lack of"
                  + " evidence from the dectetive";
        }
        EndingController.setThief(thief);
        EndingController.setFeed(feed);
        App.setRoot("badending");
        sendOnce = false;
        return;
      }
      if (!answer.equals("") && sendOnce) { // If an answer has been provided, send it
        sendAnswer();
        sendOnce = false;
      }
    }
  }

  /**
   * Sends the player's answer and processes the submission asynchronously. It includes progress bar
   * updates and invokes GPT model interactions.
   */
  public void sendAnswer() {
    player.play(); // Play the click sound effect
    if (answer == null) {
      return;
    }
    if (timeline == null) {
      timeline.stop();
    }
    if (TimeManager.getInstance().getPlayer() != null) {
      TimeManager.getInstance().getPlayer().stop();
    }
    if (submitButton != null) {
      submitButton.setDisable(true);
    } // Disable the submit button to prevent multiple submissions
    timeManager.stopTimer(); // Stop the timer
    if (answer.equals("")) {
      return; // Return if the answer text area is empty
    }

    Map<String, String> map = new HashMap<>();
    map.put("answer", answer); // Put the answer in the map
    map.put("thief", thief); // Put the selected thief in the map

    if (progressBar == null) {
      return;
    }
    progressBar.setVisible(true); // Show the progress bar
    Task<Void> task =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            updateProgress(0, 100); // Start progress
            intizliaseAndGpt(map); // Initialize and run GPT interaction
            for (int i = 0; i <= 100; i++) {
              updateProgress(i, 100); // Update the progress
              Thread.sleep(20); // Simulate progress
            }
            return null;
          }
        };

    task.setOnSucceeded(
        event -> {
          progressBar.setVisible(false); // Hide progress bar when task is done
          // Handle successful feedback logic here
        });

    task.setOnFailed(
        event -> {
          progressBar.setVisible(false); // Hide progress bar if the task fails
          task.getException().printStackTrace(); // Print the exception stack trace
        });

    progressBar
        .progressProperty()
        .bind(task.progressProperty()); // Bind progress bar to task progress
    new Thread(task).start(); // Run the task in a background thread
  }

  /** Saves the current answer entered in the text area. */
  @FXML
  private void savefeedback() {
    answer = answerTxtArea.getText(); // Save the answer from the text area
    System.out.println(answer);
  }

  /**
   * Handles the event when a rectangle representing a suspect is clicked.
   *
   * @param event the mouse event triggered by clicking a suspect rectangle
   * @throws IOException if there is an I/O error
   * @throws URISyntaxException if there is a URI error
   */
  @FXML
  private void handleRectangleClick(MouseEvent event) throws IOException, URISyntaxException {
    player.play(); // Play the click sound effect
    Rectangle clickedRectangle = (Rectangle) event.getSource();
    thief = clickedRectangle.getId(); // Set the thief based on the clicked rectangle
    isFirstTime = false; // Set the first-time flag to false
    App.setRoot("submitanswer"); // Change the scene to submit answer
  }

  /**
   * Handles the "Enter" key press to submit the answer.
   *
   * @param event the key event triggered by pressing the "Enter" key
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void handleEnterKey(KeyEvent event) throws IOException {
    if (event.getCode().equals(KeyCode.ENTER) && once) {
      sendAnswer(); // Call the same method for submitting the answer
      once = false; // Set the flag to prevent multiple submissions
    }
  }

  /**
   * Generates the system prompt based on the suspect (thief) selected.
   *
   * @param data a map containing relevant data for prompt generation
   * @return the generated system prompt
   */
  private String getSystemPrompt(Map<String, String> data) {
    if (thief.equals("hos")) {
      return PromptEngineering.getPrompt("win.txt", data); // Get win prompt for head of security
    } else if (thief.equals("curator") || thief.equals("janitor")) {
      return PromptEngineering.getPrompt(
          "loss.txt", data); // Get loss prompt for curator or janitor
    }
    return "error"; // Default error prompt
  }

  /**
   * Initializes and runs the GPT-based interaction asynchronously.
   *
   * @param data a map containing the player's answer and thief
   */
  public void intizliaseAndGpt(Map<String, String> data) {
    Task<Void> task =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            try {
              ApiProxyConfig config = ApiProxyConfig.readConfig(); // Read API proxy configuration
              chatCompletionRequest =
                  new ChatCompletionRequest(config)
                      .setN(1)
                      .setTemperature(0.2)
                      .setTopP(0.5)
                      .setMaxTokens(100); // Set GPT request parameters
              runGpt(
                  new ChatMessage("system", getSystemPrompt(data))); // Run GPT with system prompt
              return null;
            } catch (ApiProxyException | IOException e) {
              e.printStackTrace(); // Print stack trace for exceptions
              return null;
            }
          }
        };

    task.setOnSucceeded(
        event -> {
          // UI updates or logic after GPT response
          try {
            if (thief.equals("janitor") || thief.equals("curator")) {
              App.setRoot("badending"); // Set scene to bad ending
            } else if (thief.equals("hos")) {
              App.setRoot("goodending2"); // Set scene to good ending
            }
          } catch (IOException e) {
            e.printStackTrace(); // Print stack trace for exceptions
          }
          if (submitButton != null) {
            submitButton.setDisable(false); // Enable the submit button
          }
        });

    new Thread(task).start(); // Start the background task
  }

  /**
   * Executes a GPT chat completion request with the given chat message and processes the result.
   *
   * @param msg the chat message to be sent to the GPT model
   * @return the content of the response message from the GPT model
   * @throws ApiProxyException if there is an error with the API proxy
   * @throws IOException if there is an I/O error during the request
   */
  private String runGpt(ChatMessage msg) throws ApiProxyException, IOException {
    chatCompletionRequest.addMessage(msg); // Add the message to the GPT request
    ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
    Choice result = chatCompletionResult.getChoices().iterator().next();
    chatCompletionRequest.addMessage(result.getChatMessage());
    String messageContent =
        result.getChatMessage().getContent(); // Get the content of the response message
    feed = messageContent;
    EndingController.setFeed(feed); // Set the feedback in the ending controller
    EndingController.setThief(thief); // Set the thief in the ending controller
    return messageContent; // Return the response message content
  }
}
