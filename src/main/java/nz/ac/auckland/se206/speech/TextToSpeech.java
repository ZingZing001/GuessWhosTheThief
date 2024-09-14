package nz.ac.auckland.se206.speech;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javafx.concurrent.Task;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.apiproxy.tts.TextToSpeechRequest;
import nz.ac.auckland.apiproxy.tts.TextToSpeechRequest.Provider;
import nz.ac.auckland.apiproxy.tts.TextToSpeechRequest.Voice;
import nz.ac.auckland.apiproxy.tts.TextToSpeechResult;

/** A utility class for converting text to speech using the specified API proxy. */
public class TextToSpeech {

  /**
   * Converts the given text to speech and plays the audio.
   *
   * @param text the text to be converted to speech
   * @throws IllegalArgumentException if the text is null or empty
   */
  public static void speak(String text, String profession) {
    if (text == null || text.isEmpty()) {
      throw new IllegalArgumentException("Text should not be null or empty");
    }

    Task<Void> backgroundTask =
        new Task<>() {
          @Override
          protected Void call() {
            try {
              ApiProxyConfig config = ApiProxyConfig.readConfig();
              // the default voice provider if no explicity profession is given
              Provider provider = Provider.GOOGLE;
              Voice voice = Voice.GOOGLE_EN_US_STANDARD_H;
              // changing voice based on what suspect is being spoken to
              switch (profession) {
                case "Art Currator":
                  provider = Provider.GOOGLE;
                  voice = Voice.GOOGLE_EN_AU_STANDARD_C;
                  break;
                case "Art Thief":
                  provider = Provider.GOOGLE;
                  voice = Voice.GOOGLE_EN_GB_STANDARD_B;
                  break;
                case "Janitor":
                  provider = Provider.GOOGLE;
                  voice = Voice.GOOGLE_EN_AU_STANDARD_D;
                  break;
              }

              TextToSpeechRequest ttsRequest = new TextToSpeechRequest(config);
              ttsRequest.setText(text).setProvider(provider).setVoice(voice);

              TextToSpeechResult ttsResult = ttsRequest.execute();
              String audioUrl = ttsResult.getAudioUrl();

              // retrieving audio file from ai:
              System.out.println("playing audio from: " + audioUrl);

              try (InputStream inputStream =
                  new BufferedInputStream(new URL(audioUrl).openStream())) {
                Player player = new Player(inputStream);
                player.play();
              } catch (JavaLayerException | IOException e) {
                e.printStackTrace();
              }

            } catch (ApiProxyException e) {
              e.printStackTrace();
            }
            return null;
          }
        };

    Thread backgroundThread = new Thread(backgroundTask);
    backgroundThread.setDaemon(true); // Ensure the thread does not prevent JVM shutdown
    // backgroundThread.start();
    System.out.println(text);
  }
}