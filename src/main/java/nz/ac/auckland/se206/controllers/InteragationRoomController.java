package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.TimeManager;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Added Controller class for the intelroom view. Handles user interactions within the room where
 * the user can chat with customers and guess their profession.
 */
public class InteragationRoomController implements RoomNavigationHandler {
  private static boolean clueHasBeenInteractedWith = false;
  private static boolean isFirstTimeInit = true;
  private static Map<String, Boolean> suspectHasBeenTalkedToMap = new HashMap<>();
  private static Map<String, String> professionToNameMap = new HashMap<>();
  private static GameStateContext context = GameStateContext.getInstance();
  private static boolean isChatOpened = false;

  // make a setter for isChatOpened
  public static void setIsChatOpened(boolean isChatOpened1) {
    isChatOpened = isChatOpened1;
  }

  /**
   * Gets the current game context the game is in.
   *
   * @return the game context.
   */
  public static GameStateContext getGameContext() {
    return context;
  }

  private static void initializeSuspectTalkedToMap() {
    suspectHasBeenTalkedToMap.put("Art Currator", false);
    suspectHasBeenTalkedToMap.put("Art Thief", false);
    suspectHasBeenTalkedToMap.put("Janitor", false);
  }

  /**
   * Returns whether the suspects have been talked to.
   *
   * @return true if all 3 suspects have been talked to, false otherwise
   */
  public static boolean getSuspectsHaveBeenTalkedTo() {
    // Print out the suspects that have been talked to
    System.out.println("Art Currator talked to:" + suspectHasBeenTalkedToMap.get("Art Currator"));
    System.out.println("Janitor has been talked to:" + suspectHasBeenTalkedToMap.get("Janitor"));
    System.out.println(
        "Art thief has been tallked to:" + suspectHasBeenTalkedToMap.get("Art Thief"));
    // Return whether all suspects have been talked to
    return suspectHasBeenTalkedToMap.get("Art Currator")
        && suspectHasBeenTalkedToMap.get("Art Thief")
        && suspectHasBeenTalkedToMap.get("Janitor");
  }

  /**
   * Sets the game over state.
   *
   * @throws IOException if there is an I/O error
   */
  public static void setGameOverState() throws IOException {
    context.setState(context.getGameOverState());
    App.setRoot("badending");
  }

  /**
   * Gets the clueHasBeenInteractedWith boolean to check if the clue has been interacted with.
   *
   * @return the clueHasBeenInteractedWith boolean
   */
  public static boolean getClueHasBeenInteractedWith() {
    return clueHasBeenInteractedWith;
  }

  /** Sets the clueHasBeenInteractedWith boolean to true. */
  public static void resetSuspectsTalkedToMap() {
    suspectHasBeenTalkedToMap.put("Art Currator", false);
    suspectHasBeenTalkedToMap.put("Art Thief", false);
    suspectHasBeenTalkedToMap.put("Janitor", false);
  }

  /** Sets the clueHasBeenInteractedWith boolean to true. */
  private static void initializeRoleToNameMap() {
    professionToNameMap.put("Art Currator", "Frank");
    professionToNameMap.put("Art Thief", "William");
    professionToNameMap.put("Janitor", "John");
    professionToNameMap.put("user", "You");
  }

  /** Gets the professionToNameMap, resets all the static variables here. */
  public static void resetStaticVariables() {
    clueHasBeenInteractedWith = false;
    isFirstTimeInit = true;
    isChatOpened = false;

    // Reset suspectHasBeenTalkedToMap
    suspectHasBeenTalkedToMap.clear();
    initializeSuspectTalkedToMap();

    // Reset professionToNameMap if necessary
    professionToNameMap.clear();
    initializeRoleToNameMap();
  }

  @FXML private BorderPane mainPane;
  @FXML private Button corridorButton;
  @FXML private Button suspect1Button;
  @FXML private Button suspect2Button;
  @FXML private Button suspect3Button;
  @FXML private Button btnBack;
  @FXML private Button btnSend;
  @FXML private Button btnGoBack;
  @FXML private Group chatGroup;
  @FXML private ImageView bubble1;
  @FXML private ImageView bubble2;
  @FXML private ImageView bubble3;
  @FXML private ImageView currator0;
  @FXML private ImageView currator1;
  @FXML private ImageView currator2;
  @FXML private ImageView curratorInitial;
  @FXML private ImageView curratorHover;
  @FXML private ImageView thief0;
  @FXML private ImageView thief1;
  @FXML private ImageView thief2;
  @FXML private ImageView thiefInitial;
  @FXML private ImageView thiefHover;
  @FXML private ImageView janitor0;
  @FXML private ImageView janitor1;
  @FXML private ImageView janitor2;
  @FXML private ImageView janitorInitial;
  @FXML private ImageView janitorHover;
  @FXML private Label mins;
  @FXML private Label secs;
  @FXML private Label dot;
  @FXML private ScrollPane chatScrollPane;
  @FXML private TextField txtInput;
  @FXML private VBox navBar;
  @FXML private VBox chatContainer;

  @SuppressWarnings("unused")
  private Map<String, List<ChatMessage>> chatHistory;

  private List<ChatMessage> conversationHistory;
  private boolean navBarVisible = false;

  @SuppressWarnings("unused")
  private ChatCompletionRequest chatCompletionRequest;

  private MediaPlayer player;
  private Media sound;
  private Media artCurratorHmm;
  private Media thiefHmm;
  private Media janitorHmm;

  private boolean rectangleClicked = false;

  private int originalWidth = 1100;
  private String profession;
  private Random random = new Random();
  // Make sure these match the fx:id you set in Scene Builder
  @FXML private Circle dot0;
  @FXML private Circle dot1;
  @FXML private Circle dot2;

  private Timeline timeline;

  /**
   * Initializes the room view. If it's the first time initialization, it will provide instructions
   * via text-to-speech.
   *
   * @throws URISyntaxException if there is an error with the URI syntax for media files
   */
  @FXML
  public void initialize() throws ApiProxyException {

    timeline =
        new Timeline(
            new KeyFrame(Duration.seconds(0), e -> enlargeDot(dot0)),
            new KeyFrame(Duration.seconds(0.5), e -> resetDot()),
            new KeyFrame(Duration.seconds(0.5), e -> enlargeDot(dot1)),
            new KeyFrame(Duration.seconds(1), e -> resetDot()),
            new KeyFrame(Duration.seconds(1), e -> enlargeDot(dot2)),
            new KeyFrame(Duration.seconds(1.5), e -> resetDot()));

    timeline.setCycleCount(Timeline.INDEFINITE);

    NavBarUtils.setupNavBarAndSuspectButtons(
        navBar, suspect1Button, suspect2Button, suspect3Button, this);

    if (isFirstTimeInit) {
      initializeSuspectTalkedToMap();
      initializeRoleToNameMap();
      isFirstTimeInit = false;
    }
    initializeSounds();
    TimeManager timeManager = TimeManager.getInstance();
    timeManager.setTimerLabel(mins, secs, dot);
    // Initialize the game context with the charHistory
    this.chatHistory = context.getChatHistory();
    // testing purposes
    // System.out.println("Entire Chat history intalizeed");
    isChatOpened = false;
    rectangleClicked = false;
  }

  public void setTime() {
    TimeManager timeManager = TimeManager.getInstance();
    timeManager.setTimerLabel(mins, secs, dot);
  }

  // Method to increase the size of a dot
  private void enlargeDot(Circle dot) {
    dot.setRadius(10); // Increase the radius to 10 (or whatever size you want)
  }

  // Method to reset the size of a dot back to normal
  private void resetDot() {
    dot0.setRadius(5); // Reset the radius to 5
    dot1.setRadius(5); // Reset the radius to 5
    dot2.setRadius(5); // Reset the radius to 5
  }

  /**
   * Handles the key pressed event.
   *
   * @param event the key event
   */
  @FXML
  public void onKeyPressed(KeyEvent event) {}

  /**
   * Handles the key released event.
   *
   * @param event the key event
   */
  @FXML
  public void onKeyReleased(KeyEvent event) {}

  @Override
  public void goToRoom(String roomName) throws IOException {
    isChatOpened = false;
    rectangleClicked = false;
    chatGroup.setVisible(false);
    // Before navigating, reset the window size if navBar is visible
    Stage stage = (Stage) navBar.getScene().getWindow();
    stage.setWidth(originalWidth);
    // Handle room switching logic
    App.setRoot(roomName);
  }

  /**
   * Handles the event when the corridor button is clicked. It changes the view to the corridor.
   *
   * @param profession the profession of the suspect
   * @throws URISyntaxException if there is an error with the URI syntax for media files
   * @throws InterruptedException if there is an error with the thread
   */
  public void setProfession(String profession) throws URISyntaxException, InterruptedException {
    this.profession = profession;

    // Disable the send button when the profession is being set
    btnSend.setDisable(true);

    // Clear the chat container to avoid mixing messages from different suspects
    chatContainer.getChildren().clear();

    // Get conversation history for the suspect
    String promptFile = getPromptFileForProfession(profession);
    conversationHistory = context.getChatHistory().get(promptFile);

    if (conversationHistory == null) {
      // Initialize the conversation history if it doesn't exist
      conversationHistory = new ArrayList<>();
      context.getChatHistory().put(promptFile, conversationHistory);
    }

    if (conversationHistory.isEmpty()) {
      // First time talking to the suspect
      ChatMessage initialMessage =
          new ChatMessage("user", getInitialMessageForProfession(profession));
      appendChatMessage(initialMessage);

      // Send the initial message to the AI
      sendMessageToBot(initialMessage, conversationHistory, true);
    } else {
      // Revisiting the suspect
      // Create a special system message to prompt the AI to initiate the conversation
      setThinkingBubbleVisibility(true);
      ChatMessage systemMessage = new ChatMessage("system", getSystemPromptForRevisit());

      // Run GPT in a background task
      Task<ChatMessage> task =
          new Task<>() {
            @Override
            protected ChatMessage call() throws ApiProxyException {
              return runGptForRevisit(systemMessage, conversationHistory);
            }
          };

      task.setOnSucceeded(
          event -> {
            ChatMessage resultMessage = task.getValue();
            appendChatMessage(resultMessage);
            btnSend.setDisable(false);
          });

      task.setOnFailed(
          event -> {
            btnSend.setDisable(false);
            task.getException().printStackTrace();
          });

      new Thread(task).start();
    }
  }

  private void sendMessageToBot(
      ChatMessage userMessage, List<ChatMessage> conversationHistory, boolean isFirstInteraction) {
    // Add the user's message to the conversation history
    conversationHistory.add(userMessage);

    // Run GPT in a background task
    Task<ChatMessage> task =
        new Task<>() {
          @Override
          protected ChatMessage call() throws ApiProxyException {
            return runGpt(userMessage, conversationHistory);
          }
        };

    task.setOnSucceeded(
        event -> {
          ChatMessage resultMessage = task.getValue();
          appendChatMessage(resultMessage);
          btnSend.setDisable(false);

          if (isFirstInteraction) {
            // Mark the suspect as talked to
            suspectHasBeenTalkedToMap.put(profession, true);
            System.out.println("Set " + profession + " as talked to.");
          }
        });

    task.setOnFailed(
        event -> {
          btnSend.setDisable(false);
          task.getException().printStackTrace();
        });

    new Thread(task).start();
  }

  // NavBar Methods
  @FXML
  private void onToggleNavBar() {
    TranslateTransition translateTransition = new TranslateTransition(Duration.millis(500), navBar);
    FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), navBar);

    if (navBarVisible) {
      // Slide out and fade out, then reduce the window size
      translateTransition.setToX(200); // Move back off-screen to the right
      fadeTransition.setToValue(0); // Fade out to invisible
      navBarVisible = false;
      navBar.setDisable(true); // Disable the navBar
    } else {
      navBar.setDisable(false); // Enable the navBar
      // Slide in and fade in, then increase the window size
      translateTransition.setByX(-200); // Move into view
      fadeTransition.setToValue(1); // Fade in to fully visible
      navBarVisible = true;
    }

    // Play both transitions
    translateTransition.play();
    fadeTransition.play();
  }

  /**
   * Generates the system prompt based on the profession.
   *
   * @return the system prompt string
   */
  private String getSystemPrompt() {
    Map<String, String> map = new HashMap<>();
    map.put("profession", profession);
    return PromptEngineering.getPrompt(getPromptFileForProfession(profession), map);
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  private ChatMessage runGpt(ChatMessage userMessage, List<ChatMessage> conversationHistory)
      throws ApiProxyException {
    // Create a new ChatCompletionRequest
    ApiProxyConfig config = ApiProxyConfig.readConfig();
    ChatCompletionRequest chatCompletionRequest =
        new ChatCompletionRequest(config).setN(1).setTemperature(0.8).setTopP(0.7).setMaxTokens(80);

    // Start with the system prompt
    chatCompletionRequest.addMessage(new ChatMessage("system", getSystemPrompt()));

    // Add all previous messages in the conversation
    for (ChatMessage previousMsg : conversationHistory) {
      chatCompletionRequest.addMessage(previousMsg);
    }

    // Add the new user message
    chatCompletionRequest.addMessage(userMessage);

    // Execute the request
    ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
    Choice result = chatCompletionResult.getChoices().iterator().next();
    ChatMessage aiResponse = result.getChatMessage();

    // Add the assistant's reply to the conversation history
    conversationHistory.add(aiResponse);

    return aiResponse;
  }

  private String getSystemPromptForRevisit() {
    Map<String, String> map = new HashMap<>();
    map.put("profession", profession);
    map.put("conversation_state", "revisit");
    return PromptEngineering.getPrompt(getPromptFileForProfession(profession), map);
  }

  private ChatMessage runGptForRevisit(
      ChatMessage systemMessage, List<ChatMessage> conversationHistory) throws ApiProxyException {
    // Create a new ChatCompletionRequest
    ApiProxyConfig config = ApiProxyConfig.readConfig();
    ChatCompletionRequest chatCompletionRequest =
        new ChatCompletionRequest(config).setN(1).setTemperature(0.8).setTopP(0.7).setMaxTokens(80);

    // Start with the system prompt
    chatCompletionRequest.addMessage(systemMessage);

    // Add all previous messages in the conversation
    for (ChatMessage previousMsg : conversationHistory) {
      chatCompletionRequest.addMessage(previousMsg);
    }

    // Execute the request
    ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
    Choice result = chatCompletionResult.getChoices().iterator().next();
    ChatMessage aiResponse = result.getChatMessage();

    // Add the assistant's reply to the conversation history
    conversationHistory.add(aiResponse);

    return aiResponse;
  }

  /**
   * Generates an initial message based on the given profession.
   *
   * @param profession the profession of the person being addressed (e.g., "Art Currator", "Art
   *     Thief", "Janitor")
   * @return a string containing the initial message tailored to the given profession
   */
  private String getInitialMessageForProfession(String profession) {
    // Switch case to determine the initial message based on the profession
    setThinkingBubbleVisibility(true);
    switch (profession) {
      case "Art Currator":
        return "Hey can you tell me what happened here? I'm investigating this case."; // Case for
      // the art
      // currator
      case "Art Thief":
        return "Hi sir. I'm one of the investigators on this job. Can you tell me what happened"
            + " here?"; // case for Art Thief
      case "Janitor":
        return "Hello. I'm investigating this case on behalf of PI Masters. Can you tell me what"
            + " happened here?"; // case for Janitor
      default:
        return "Hello, I am investigating this case. Can you tell me what happened"
            + " here?"; // default case
    }
  }

  private String getSuspectTypeForProfession(String profession) {
    // Switch case to determine the suspect type based on the profession
    switch (profession) {
      // Return the suspect type based on the profession
      case "Art Currator":
        return "currator";
      case "Art Thief":
        return "thief";
      case "Janitor":
        return "janitor";
      default:
        // Return an empty string if the profession is unknown
        return "";
    }
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    sendMessage();
  }

  /**
   * Handles the "Enter" key press in the TextField to send a message.
   *
   * @param event the KeyEvent triggered by pressing a key
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onHandleEnterKey(KeyEvent event) throws ApiProxyException, IOException {
    if (event.getCode() == KeyCode.ENTER) {
      if (!btnSend.isDisabled()) {
        sendMessage();
      }
    }
  }

  private void setThinkingBubbleVisibility(boolean isVisible) {
    // Set the visibility of the thinking bubble based on the profession
    if (profession.equals("Art Currator")) {
      bubble1.setVisible(isVisible);
    } else if (profession.equals("Art Thief")) {
      bubble3.setVisible(isVisible);
    } else if (profession.equals("Janitor")) {
      bubble2.setVisible(isVisible);
    }
    // set the thinking dots on the screen
    dot0.setVisible(isVisible);
    dot1.setVisible(isVisible);
    dot2.setVisible(isVisible);
    // Start the thinking animation
    if (isVisible) {
      resetDot();
      Task<Void> task =
          new Task<Void>() {
            @Override
            protected Void call() throws Exception {
              Platform.runLater(() -> timeline.play());
              return null;
            }
          };

      Thread thread = new Thread(task);
      thread.setDaemon(true);
      thread.start();
    } else {
      // Stop the thinking animation
      timeline.stop();
    }
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  private void sendMessage() throws ApiProxyException, IOException {

    String message = txtInput.getText().trim(); // Get the user's message

    if (message.isEmpty()) {
      return;
    }

    setThinkingBubbleVisibility(true);

    // Create a ChatMessage for the user's input
    ChatMessage userMessage = new ChatMessage("user", message);

    // Append user's message to the conversation history and UI
    appendChatMessage(userMessage);

    // Clear the input field
    txtInput.clear();

    // Disable send button while processing the message
    btnSend.setDisable(true);

    // Send the message to the AI
    sendMessageToBot(userMessage, conversationHistory, false);
  }

  // Method to play "hmm" sound based on profession
  private void playHmmSound(String profession) {
    // Stop the current player if it is playing
    if (player != null) {
      player.stop();
    }

    // Switch case to determine which sound to play based on profession
    switch (profession) {
      case "Art Currator":
        // If the profession is "Art Currator" and the sound is available, create a new
        // MediaPlayer
        if (artCurratorHmm != null) {
          player = new MediaPlayer(artCurratorHmm);
        }
        break;
      case "Art Thief":
        // If the profession is "Art Thief" and the sound is available, create a new
        // MediaPlayer
        if (thiefHmm != null) {
          player = new MediaPlayer(thiefHmm);
        }
        break;
      case "Janitor":
        // If the profession is "Janitor" and the sound is available, create a new
        // MediaPlayer
        if (janitorHmm != null) {
          player = new MediaPlayer(janitorHmm);
        }
        break;
    }

    // Play the selected sound
    player.play();
  }

  private void appendChatMessage(ChatMessage msg) {

    // Get the conversation history
    Map<String, List<ChatMessage>> chatHistory = context.getChatHistory();
    String promptFile = getPromptFileForProfession(profession);
    List<ChatMessage> conversationHistory = chatHistory.get(promptFile);

    // Add the message to the conversation history
    conversationHistory.add(msg);

    // Update the suspect's image based on the message
    if (!msg.getRole().equals("user")) {
      playHmmSound(profession);
      String suspectType = getSuspectTypeForProfession(profession);
      setImageVisibility(
          suspectType,
          random.nextInt(3)); // initialises a random image of the suspect after each message
    }

    // Append the chat bubble to the UI
    addChatBubble(msg.getRole(), msg.getContent());
  }

  // Method to get the prompt file based on the profession
  private String getPromptFileForProfession(String profession) {
    // Switch case to determine the prompt file based on the profession
    switch (profession) {
      case "Art Currator":
        return "suspect1.txt";
      case "Art Thief":
        return "thief.txt";
      case "Janitor":
        return "suspect2.txt";
      default:
        // Return "unknown.txt" if the profession is unknown
        return "unknown.txt";
    }
  }

  // Add this method to append a chat bubble for each message
  private void addChatBubble(String role, String content) {
    // Create a Label for the sender's name
    Label senderLabel = new Label();
    if (role.equals("user")) {
      senderLabel.setText("You");
    } else {
      String senderName = professionToNameMap.get(profession);
      senderLabel.setText(senderName != null ? senderName : "Unknown");
    }
    senderLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
    senderLabel.setPadding(new Insets(0, 0, 2, 0));

    // Create Text for the message
    Text messageText = new Text(content);
    messageText.setFont(Font.font("Arial", 18));
    messageText.setFill(Color.BLACK);

    // Wrap the Text in a TextFlow
    TextFlow messageFlow = new TextFlow(messageText);
    messageFlow.setMaxWidth(400); // Adjust as needed
    messageFlow.setPadding(new Insets(10));
    messageFlow.setLineSpacing(1.5);

    // Style the TextFlow to look like a chat bubble
    BackgroundFill backgroundFill;
    if (role.equals("user")) {
      backgroundFill = new BackgroundFill(Color.LIGHTGREEN, new CornerRadii(10), Insets.EMPTY);
    } else {
      backgroundFill =
          new BackgroundFill(Color.rgb(210, 210, 210), new CornerRadii(10), Insets.EMPTY);
    }
    messageFlow.setBackground(new Background(backgroundFill));

    // Create a VBox to hold the senderLabel and messageFlow
    VBox messageBox = new VBox(senderLabel, messageFlow);
    messageBox.setAlignment(role.equals("user") ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    messageBox.setMaxWidth(400 + 20); // Adjust max width for padding

    // Create an HBox to hold the messageBox
    HBox messageContainer = new HBox(messageBox);
    messageContainer.setPadding(new Insets(5));
    messageContainer.setAlignment(role.equals("user") ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

    // Allow the messageBox to grow horizontally
    HBox.setHgrow(messageBox, Priority.ALWAYS);
    messageBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
    messageBox.setMinWidth(0);

    // Add the messageContainer to the chatContainer (VBox)
    chatContainer.getChildren().add(messageContainer);

    // pause.play();
    if (!role.equals("user")) {
      setThinkingBubbleVisibility(false);
    }

    // Scroll to the bottom of the chat
    Platform.runLater(
        () -> {
          chatScrollPane.layout();
          chatScrollPane.setVvalue(chatScrollPane.getVmax());
        });
  }

  // Helper method to set visibility based on the random index
  private void setImageVisibility(String suspectType, int randomIndex) {
    // Hide all images first
    hideAllImages(suspectType);

    // Show the selected image based on the random index
    switch (randomIndex) {
      case 0:
        getImageView(suspectType + "0").setVisible(true);
        break;
      case 1:
        getImageView(suspectType + "1").setVisible(true);
        break;
      case 2:
        getImageView(suspectType + "2").setVisible(true);
        break;
    }
  }

  // Helper method to hide all images based on the suspect type
  private void hideAllImages(String suspectType) {
    getImageView(suspectType + "0").setVisible(false);
    getImageView(suspectType + "1").setVisible(false);
    getImageView(suspectType + "2").setVisible(false);
  }

  // Method to get the ImageView by ID (you can implement this based on your FXML
  // IDs)
  private ImageView getImageView(String imageId) {
    // Switch case to determine the ImageView based on the image ID
    switch (imageId) {
      case "curratorInitial":
        return curratorInitial;
      case "curratorHover":
        return curratorHover;
      case "currator0":
        return currator0;
      case "currator1":
        return currator1;
      case "currator2":
        return currator2;
      case "thiefInitial":
        return thiefInitial;
      case "thiefHover":
        return thiefHover;
      case "thief0":
        return thief0;
      case "thief1":
        return thief1;
      case "thief2":
        return thief2;
      case "janitorInitial":
        return janitorInitial;
      case "janitorHover":
        return janitorHover;
      case "janitor0":
        return janitor0;
      case "janitor1":
        return janitor1;
      case "janitor2":
        return janitor2;
      default:
        // Log or handle the unexpected image ID
        System.err.println("No ImageView found for ID: " + imageId);
        return null;
    }
  }

  /**
   * Handles mouse clicks on rectangles representing people in the room.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @throws IOException if there is an I/O error
   * @throws URISyntaxException if there is an error with the URI syntax
   * @throws InterruptedException if there is an error with the thread
   */
  @FXML
  private void onHandleRectangleClick(MouseEvent event)
      throws IOException, URISyntaxException, InterruptedException {
    Rectangle clickedRectangle = (Rectangle) event.getSource();
    clickedRectangle.setDisable(true); // Disable the rectangle after clicking

    rectangleClicked = true;

    // Identify which suspect was clicked and set the profession accordingly
    String suspectId = clickedRectangle.getId();
    switch (suspectId) {
      case "rectPerson1":
        profession = "Art Currator";
        break;
      case "rectPerson2":
        profession = "Art Thief";
        break;
      case "rectPerson3":
        profession = "Janitor";
        break;
      default:
        System.out.println("Invalid suspect ID: " + suspectId);
        return;
    }

    String suspectType = getSuspectTypeForProfession(profession);
    hideInitialImage(suspectType);

    // Display the chat UI
    if (!isChatOpened) {
      chatGroup.setVisible(true); // Ensure chat group is visible
      txtInput.clear();

      setProfession(profession);
      isChatOpened = true; // prevent further calls
    } else {
      clickedRectangle.setDisable(true); // Disable the rectangle after clicking
      System.out.println("Chat has already been opened!");
    }
  }

  /**
   * Handles the guess button click event.
   *
   * @param event the action event triggered by clicking the guess button
   * @throws IOException if there is an I/O error
   * @throws URISyntaxException if there is an error with the URI syntax
   */
  @FXML
  private void onHandleGuessClick(ActionEvent event) throws IOException, URISyntaxException {
    chatGroup.setVisible(false);
    if (clueHasBeenInteractedWith
        && InteragationRoomController.getSuspectsHaveBeenTalkedTo()) { // TO DO: &&
      // chatController.getSuspectHasBeenTalkedTo()
      System.out.println("Now in guessing state");
      context.handleGuessClick();
    } else {
      sound =
          new Media(
              App.class
                  .getResource("/sounds/investigate_more_before_guessing.mp3")
                  .toURI()
                  .toString());
      player = new MediaPlayer(sound);
      player.play();
    }
  }

  @FXML
  private void onHandleBackToCrimeSceneClick(ActionEvent event) throws IOException {
    // Before navigating, reset the window size if navBar is visible
    Stage stage = (Stage) navBar.getScene().getWindow();
    stage.setWidth(originalWidth);
    App.setRoot("room");
  }

  @FXML
  private void handleRoomsClick(MouseEvent event) throws IOException, URISyntaxException {
    Rectangle clickedRoom = (Rectangle) event.getSource();
    context.handleRectangleClick(event, clickedRoom.getId());
  }

  // Initialize sound resources only once
  private void initializeSounds() {
    try {
      artCurratorHmm =
          new Media(App.class.getResource("/sounds/Curatorhmmm.mp3").toURI().toString());
      thiefHmm = new Media(App.class.getResource("/sounds/HOShuh.mp3").toURI().toString());
      janitorHmm = new Media(App.class.getResource("/sounds/janhmmm.mp3").toURI().toString());

      // Check if any Media is null
      if (artCurratorHmm == null || thiefHmm == null || janitorHmm == null) {
        throw new IllegalArgumentException("Failed to load one or more sound files.");
      }
    } catch (URISyntaxException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Navigates back to the previous view.
   *
   * @param event the action event triggered by the go back button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onGoBack(ActionEvent event) throws ApiProxyException, IOException {
    isChatOpened = false;
    rectangleClicked = false;
    chatGroup.setVisible(false); // Ensure chat group is visible
    InteragationRoomController.setIsChatOpened(false);
    App.setRoot("room");
  }

  @FXML
  private void onHoverImage(MouseEvent event) {
    if (!rectangleClicked) {
      if (event.getSource() instanceof ImageView) {
        ImageView hoveredImage = (ImageView) event.getSource();
        String imageId = hoveredImage.getId();
        String suspectType = getSuspectTypeFromImageId(imageId);
        if (!suspectType.isEmpty()) {
          getImageView(suspectType + "Hover").setVisible(true);
        }
      } else {
        // Log or handle the unexpected event source
        System.err.println("onHoverImage called, but source is not an ImageView");
      }
    }
  }

  @FXML
  private void onHoverRectangle(MouseEvent event) {
    // Only show the hover image if the rectangle has not been clicked
    if (!rectangleClicked) {
      // Get the suspect type based on the rectangle ID
      Rectangle hoveredRectangle = (Rectangle) event.getSource();
      String rectangleId = hoveredRectangle.getId();
      // Get the suspect type based on the rectangle ID
      String suspectType = getSuspectTypeFromRectangleId(rectangleId);
      // Show the hover image if the suspect type is not empty
      if (!suspectType.isEmpty()) {
        getImageView(suspectType + "Hover").setVisible(true);
      }
    }
  }

  @FXML
  private void onHideRectangle(MouseEvent event) {
    Rectangle hoveredRectangle = (Rectangle) event.getSource();
    String rectangleId = hoveredRectangle.getId();
    String suspectType = getSuspectTypeFromRectangleId(rectangleId);
    if (!suspectType.isEmpty()) {
      getImageView(suspectType + "Hover").setVisible(false);
    }
  }

  @FXML
  private void onHideImage(MouseEvent event) {
    if (event.getSource() instanceof ImageView) {
      ImageView hoveredImage = (ImageView) event.getSource();
      String imageId = hoveredImage.getId();
      String suspectType = getSuspectTypeFromImageId(imageId);
      if (!suspectType.isEmpty()) {
        getImageView(suspectType + "Hover").setVisible(false);
      }
    } else {
      // Log or handle the unexpected event source
      System.err.println("onHideImage called, but source is not an ImageView");
    }
  }

  private String getSuspectTypeFromRectangleId(String rectangleId) {
    // Switch case to determine the suspect type based on the rectangle ID
    switch (rectangleId) {
      case "rectPerson1":
        return "currator";
      case "rectPerson2":
        return "thief";
      case "rectPerson3":
        return "janitor";
      default:
        // Return an empty string if the rectangle ID is unknown
        return "";
    }
  }

  private String getSuspectTypeFromImageId(String imageId) {
    // Determine the suspect type based on the image ID
    if (imageId.startsWith("currator")) {
      return "currator";
    } else if (imageId.startsWith("thief")) {
      return "thief";
    } else if (imageId.startsWith("janitor")) {
      return "janitor";
    } else {
      // Return an empty string if the image ID is unknown
      return "";
    }
  }

  private void hideInitialImage(String suspectType) {
    getImageView(suspectType + "0").setVisible(true); // Show initial chat image
    getImageView(suspectType + "Initial").setVisible(false);
    getImageView(suspectType + "Hover").setVisible(false);
  }
}
